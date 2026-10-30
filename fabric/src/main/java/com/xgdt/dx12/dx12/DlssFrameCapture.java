package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import com.xgdt.dx12.config.Dx12Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * DLSS 超分 / 帧生成所需的逐帧数据捕获。
 *
 * <p>每帧在 {@code GameRenderer.render} 开头调用 {@link #beginFrame()}：
 * 决定本帧是否启用 DLSS、选择 Halton 亚像素抖动，并清空原生侧上一帧的捕获状态。
 *
 * <p>世界渲染时 {@code GameRenderer.renderLevel} 会把最终投影矩阵交给
 * {@code ProjectionMatrixBuffer.getBuffer(Matrix4f)}。Mixin 在这里调用
 * {@link #applyProjectionJitter(Matrix4f)}：先保存未抖动矩阵，再把 DLSS 抖动
 * 叠加到投影矩阵上（这样世界几何、手部、GUI 都使用同一抖动），最后计算
 * viewProj / invViewProj / 逆投影等矩阵。
 *
 * <p>世界几何渲染完成后、原版清深度之前，Mixin 调用
 * {@link #captureWorldDepth()}：把深度纹理和矩阵数据交给原生侧，由 compute
 * shader 生成相机运动矢量并保存一份深度，供 blit 时的 DLSS 求值使用。
 */
@Environment(EnvType.CLIENT)
public final class DlssFrameCapture {
    /** 与 native/src/ngx_dlss_render.h 中的 98-float 布局保持一致。 */
    private static final int CAPTURE_FLOAT_COUNT = 98;

    private static volatile boolean srAvailable;
    private static volatile boolean fgAvailable;

    private static boolean frameActive;
    private static boolean jitterActive;
    private static boolean matricesReady;

    /** Halton 采样序号，重置时间历史时归零。 */
    private static int sampleIndex;
    private static float jitterX;
    private static float jitterY;

    private static int renderWidth = 1;
    private static int renderHeight = 1;

    /** 上一帧的关卡 / 尺寸 / 相机，用于检测时间历史失效。 */
    private static Object lastLevel;
    private static boolean haveCamera;
    private static double lastCamX;
    private static double lastCamY;
    private static double lastCamZ;
    private static float lastFwdX;
    private static float lastFwdY;
    private static float lastFwdZ;

    private static final Matrix4f projCol = new Matrix4f();
    private static final Matrix4f jitteredProjCol = new Matrix4f();
    private static final Matrix4f viewCol = new Matrix4f();
    private static final Matrix4f viewProjCol = new Matrix4f();
    private static final Matrix4f invViewProjCol = new Matrix4f();
    private static final Matrix4f jitteredViewProjCol = new Matrix4f();
    private static final Matrix4f invJitteredViewProjCol = new Matrix4f();
    private static final Matrix4f invProjCol = new Matrix4f();
    private static final Matrix4f jitterMat = new Matrix4f();

    private static final Vector3f camUp = new Vector3f();
    private static final Vector3f camRight = new Vector3f();
    private static final Vector3f camFwd = new Vector3f();

    private static final float[] captureData = new float[CAPTURE_FLOAT_COUNT];

    private DlssFrameCapture() {
    }

    /** 由 {@code Dx12Backend.pushDlssConfig()} 在设备创建/配置变化时写入。 */
    public static void setSrAvailable(boolean available) {
        srAvailable = available;
    }

    /** 由 {@code Dx12Backend.pushDlssConfig()} 在设备创建/配置变化时写入。 */
    public static void setFgAvailable(boolean available) {
        fgAvailable = available;
    }

    public static boolean isSrAvailable() {
        return srAvailable;
    }

    public static boolean isFgAvailable() {
        return fgAvailable;
    }

    /**
     * 每帧开始：判断 DLSS 是否启用、更新抖动，并通知原生侧。
     * 即使 DLSS 关闭也会调用原生 beginFrame，以清空上一帧的捕获有效位。
     */
    public static void beginFrame() {
        Minecraft minecraft = Minecraft.getInstance();
        Dx12Config cfg = Dx12Config.getInstance();

        boolean srRequested = cfg.getDlssSr() == 1 && srAvailable;
        boolean fgRequested = cfg.getDlssFg() == 1 && fgAvailable;
        boolean active = Dx12Device.isInitialized() && Dx12Native.isLoaded()
            && !OfflineRender.active() && (srRequested || fgRequested);

        frameActive = active;
        jitterActive = active && srRequested;
        matricesReady = false;

        boolean notRenderingGameFrames = minecraft.gui.screen() != null;
        if (!active) {
            sampleIndex = 0;
            haveCamera = false;
            jitterX = 0.0f;
            jitterY = 0.0f;
            if (Dx12Native.isLoaded()) {
                Dx12Native.dx12DlssBeginFrame(false, notRenderingGameFrames);
            }
            return;
        }

        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        int width = Math.max(1, target.width);
        int height = Math.max(1, target.height);
        boolean reset = false;

        if (minecraft.level != lastLevel || width != renderWidth || height != renderHeight) {
            reset = true;
        }
        lastLevel = minecraft.level;
        renderWidth = width;
        renderHeight = height;

        CameraRenderState camera = cameraState(minecraft);
        if (camera != null && camera.initialized) {
            camFwd.set(0.0f, 0.0f, -1.0f).rotate(camera.orientation);
            if (haveCamera) {
                double dx = camera.pos.x - lastCamX;
                double dy = camera.pos.y - lastCamY;
                double dz = camera.pos.z - lastCamZ;
                // 传送 / 维度切换：位置跳变超过 32 格。
                if (dx * dx + dy * dy + dz * dz > 32.0 * 32.0) {
                    reset = true;
                }
                // 单帧视角跳变超过约 75°：历史帧已无法复用。
                float dot = camFwd.x * lastFwdX + camFwd.y * lastFwdY + camFwd.z * lastFwdZ;
                if (dot < 0.25f) {
                    reset = true;
                }
            }
            lastCamX = camera.pos.x;
            lastCamY = camera.pos.y;
            lastCamZ = camera.pos.z;
            lastFwdX = camFwd.x;
            lastFwdY = camFwd.y;
            lastFwdZ = camFwd.z;
            haveCamera = true;
        }

        if (reset) {
            sampleIndex = 0;
            Dx12Native.dx12DlssReset();
        }

        if (jitterActive) {
            // Halton(2,3) 在 [-0.5, 0.5] 像素范围内抖动。
            sampleIndex++;
            jitterX = halton(sampleIndex, 2) - 0.5f;
            jitterY = halton(sampleIndex, 3) - 0.5f;
        } else {
            jitterX = 0.0f;
            jitterY = 0.0f;
        }

        Dx12Native.dx12DlssBeginFrame(true, notRenderingGameFrames);
    }

    /**
     * 在最终关卡投影矩阵上传之前调用。
     *
     * <p>投影矩阵是局部变量（已叠加 bob / 反胃效果），这里直接修改它，
     * 让世界、手部与 GUI 共用同一亚像素抖动；同时保存未抖动矩阵供 DLSS 使用。
     */
    public static void applyProjectionJitter(Matrix4f projection) {
        if (!frameActive || projection == null) {
            matricesReady = false;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        CameraRenderState camera = cameraState(minecraft);
        if (camera == null || !camera.initialized) {
            matricesReady = false;
            return;
        }

        projCol.set(projection);

        if (jitterActive) {
            // 行向量约定下 Pj = T_clip * P；等价于在 NDC 上平移。
            float dx = 2.0f * jitterX / (float) renderWidth;
            float dy = -2.0f * jitterY / (float) renderHeight;
            jitterMat.identity().translation(dx, dy, 0.0f).mul(projection);
            projection.set(jitterMat);
        }
        jitteredProjCol.set(projection);

        // 列向量约定：V = R * T(-pos)；VP = P * V。
        viewCol.set(camera.viewRotationMatrix)
            .translate((float) -camera.pos.x, (float) -camera.pos.y, (float) -camera.pos.z);

        viewProjCol.set(projCol).mul(viewCol);
        invViewProjCol.set(viewProjCol).invert();

        jitteredViewProjCol.set(jitteredProjCol).mul(viewCol);
        invJitteredViewProjCol.set(jitteredViewProjCol).invert();

        invProjCol.set(projCol).invert();

        camUp.set(0.0f, 1.0f, 0.0f).rotate(camera.orientation);
        camRight.set(1.0f, 0.0f, 0.0f).rotate(camera.orientation);
        camFwd.set(0.0f, 0.0f, -1.0f).rotate(camera.orientation);

        matricesReady = true;
    }

    /**
     * 世界几何渲染完成后、原版清深度之前调用。
     * 把深度纹理和矩阵数据交给原生侧生成运动矢量。
     */
    public static void captureWorldDepth() {
        if (!frameActive || !matricesReady) {
            return;
        }
        if (!Dx12Native.isLoaded()) {
            return;
        }
        Dx12CommandEncoderBackend encoder = Dx12Device.sharedEncoder();
        if (encoder == null) {
            return;
        }
        long surface = Dx12Native.dx12GetActiveSurfaceHandle();
        if (surface == 0L) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        GpuTexture depthTexture = target.getDepthTexture();
        if (!(depthTexture instanceof Dx12GpuTexture depth)) {
            return;
        }

        // 行主序 / 行向量约定：JOML 的列主序 get() 正好等于矩阵转置的行主序。
        viewProjCol.get(captureData, 0);
        invViewProjCol.get(captureData, 16);
        invJitteredViewProjCol.get(captureData, 32);
        projCol.get(captureData, 48);
        invProjCol.get(captureData, 64);

        CameraRenderState camera = cameraState(minecraft);
        captureData[80] = (float) camera.pos.x;
        captureData[81] = (float) camera.pos.y;
        captureData[82] = (float) camera.pos.z;
        captureData[83] = camUp.x;
        captureData[84] = camUp.y;
        captureData[85] = camUp.z;
        captureData[86] = camRight.x;
        captureData[87] = camRight.y;
        captureData[88] = camRight.z;
        captureData[89] = camFwd.x;
        captureData[90] = camFwd.y;
        captureData[91] = camFwd.z;

        float near = 0.05f;
        float far = camera.depthFar > near ? camera.depthFar : 1024.0f;
        captureData[92] = near;
        captureData[93] = far;

        // 垂直 FOV：JOML perspective 的 m11 = 1 / tan(fov / 2)。
        float m11 = camera.projectionMatrix.m11();
        float fov = m11 > 1.0e-6f ? (2.0f * (float) Math.atan(1.0f / m11))
            : (70.0f * (float) Math.PI / 180.0f);
        captureData[94] = fov;
        captureData[95] = (float) renderWidth / (float) renderHeight;
        captureData[96] = jitterX;
        captureData[97] = jitterY;

        Dx12Native.dx12DlssCaptureFrame(encoder.nativeHandle(), surface,
            depth.handle(), captureData);
    }

    private static CameraRenderState cameraState(Minecraft minecraft) {
        if (minecraft.gameRenderer == null) {
            return null;
        }
        return minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
    }

    /** Halton 序列：index 从 1 开始，返回 [0,1)。 */
    private static float halton(int index, int base) {
        float result = 0.0f;
        float fraction = 1.0f;
        int i = index;
        while (i > 0) {
            fraction /= base;
            result += fraction * (i % base);
            i /= base;
        }
        return result;
    }
}
