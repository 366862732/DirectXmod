package com.xgdt.dx12.dx12;

import com.xgdt.dx12.shaderpack.ShaderPackManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ITRP 风格离线渲染：同一机位下逐帧做亚像素抖动，并把结果按 1/n 权重累积，
 * 得到一张超采样级别的干净画面（抗锯齿、光追阴影噪声都会被平均掉）。
 *
 * <p>工作方式：
 * <ul>
 *   <li>按下按键后冻结着色器时间 {@code Time.x}（云、水面波纹不再移动）；</li>
 *   <li>每帧在 R2 低差异序列上取一个 [-0.5, 0.5] 像素的抖动，通过平移相机投影
 *       矩阵生效——世界光栅与光影包的 {@code InvViewProj} 同步抖动，光追阴影的
 *       世界坐标重建也因此与深度缓冲保持一致；</li>
 *   <li>{@link Dx12ShaderPackRenderer} 在 pass 链之后把本帧混入 fx:6/fx:7 乒乓
 *       累积缓冲，并把累积结果贴回主目标显示；</li>
 *   <li>攒满 {@link #DEFAULT_SAMPLES} 帧后用原版 {@link Screenshot#grab} 保存
 *       PNG——此时手部与 GUI 尚未绘制，画面是干净的。</li>
 * </ul>
 *
 * <p>累积期间镜头必须保持静止：相邻样本之间检测到明显位移/旋转会直接取消，
 * 避免输出一张重影截图。
 */
@Environment(EnvType.CLIENT)
public final class OfflineRender {

    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-shaderpack");

    /** 默认累积帧数：64 帧刚好铺满 8x8 抖动网格。 */
    public static final int DEFAULT_SAMPLES = 64;

    /** R2 低差异序列的两个无理数步长。 */
    private static final double R2_A = 0.7548776662466927;
    private static final double R2_B = 0.5698402909980532;

    /** 镜头静止判定：相邻样本位移超过该值（方块）或旋转超过该角度（度）即取消。 */
    private static final double MOVE_EPS_SQR = 0.015 * 0.015;
    private static final float ROT_EPS_DEG = 0.05f;

    private static boolean active;
    private static boolean prepared;
    private static int targetSamples = DEFAULT_SAMPLES;
    private static int sampleIndex;
    private static float frozenTime;
    private static float jitterX;
    private static float jitterY;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static boolean havePose;
    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastXRot;
    private static float lastYRot;

    private OfflineRender() {
    }

    public static boolean active() {
        return active;
    }

    /** 本帧抖动已就绪：renderFrame 端据此决定要不要累积这一帧。 */
    public static boolean framePrepared() {
        return active && prepared;
    }

    /** 本帧要渲染的是第几个样本（0 基）。 */
    public static int sampleIndex() {
        return sampleIndex;
    }

    /** 着色器时间：累积期间冻结，保证云/波纹在样本之间不动。 */
    public static float shaderTime() {
        return active ? frozenTime : (float) (System.nanoTime() / 1_000_000_000.0);
    }

    /** 按键切换：开始累积，或取消当前累积。 */
    public static void toggle(Minecraft minecraft) {
        if (active) {
            cancel("已取消");
            return;
        }
        if (minecraft.level == null) {
            return;
        }
        if (ShaderPackManager.getInstance().activeIfEnabled() == null) {
            message(minecraft, "离线渲染需要先启用光影包");
            return;
        }
        active = true;
        prepared = false;
        sampleIndex = 0;
        frozenTime = (float) (System.nanoTime() / 1_000_000_000.0);
        lastWidth = -1;
        lastHeight = -1;
        havePose = false;
        LOGGER.info("[shaderpack] 离线渲染开始：累积 {} 帧（冻结时间 + 亚像素抖动）", targetSamples);
        message(minecraft, "离线渲染开始：" + targetSamples + " 帧累积，请保持镜头静止（再按一次取消）");
    }

    /** 静默中止（切换光影包 / 关闭光影 / 世界卸载时）。 */
    public static void abort() {
        active = false;
        prepared = false;
        sampleIndex = 0;
    }

    /** 带提示取消。 */
    public static void cancel(String reason) {
        if (!active) {
            return;
        }
        active = false;
        prepared = false;
        sampleIndex = 0;
        LOGGER.info("[shaderpack] 离线渲染取消：{}", reason);
        message(Minecraft.getInstance(), "离线渲染已取消：" + reason);
    }

    /**
     * 每帧在 {@code GameRenderer.renderLevel} 开头调用（世界几何取矩阵之前）：
     * 选好本帧的亚像素抖动并写进相机投影矩阵。
     */
    public static void beginFrame() {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gameRenderer == null
            || ShaderPackManager.getInstance().activeIfEnabled() == null) {
            abort();
            return;
        }
        CameraRenderState camera = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (!camera.initialized) {
            return;
        }
        int width = minecraft.gameRenderer.mainRenderTarget().width;
        int height = minecraft.gameRenderer.mainRenderTarget().height;
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width != lastWidth || height != lastHeight) {
            // 分辨率变化会重建累积缓冲：从头开始。
            lastWidth = width;
            lastHeight = height;
            sampleIndex = 0;
            havePose = false;
        }
        if (!checkCameraStill(camera)) {
            cancel("检测到镜头移动");
            return;
        }
        jitterX = (float) (fract(0.5 + sampleIndex * R2_A) - 0.5);
        jitterY = (float) (fract(0.5 + sampleIndex * R2_B) - 0.5);
        applyJitter(camera.projectionMatrix, jitterX, jitterY, width, height);
        prepared = true;
    }

    /** 累积 pass 完成后推进计数；到达目标帧数就保存截图并结束。 */
    public static void onFrameAccumulated() {
        if (!active) {
            return;
        }
        prepared = false;
        sampleIndex++;
        Minecraft minecraft = Minecraft.getInstance();
        if (sampleIndex < targetSamples) {
            if (minecraft.gui != null && sampleIndex % 8 == 0) {
                minecraft.gui.hud.setOverlayMessage(Component.literal(
                    "离线渲染 " + sampleIndex + "/" + targetSamples + "（保持静止）"), false);
            }
            return;
        }
        active = false;
        LOGGER.info("[shaderpack] 离线渲染完成：{} 帧已累积，保存截图", targetSamples);
        // 此刻主目标是刚展示的累积结果——手部与 GUI 尚未绘制，抓下来就是干净的画面。
        Screenshot.grab(minecraft.gameDirectory, minecraft.gameRenderer.mainRenderTarget(),
            text -> minecraft.execute(() -> {
                if (minecraft.gui != null) {
                    minecraft.gui.hud.setOverlayMessage(text, false);
                }
            }));
        sampleIndex = 0;
    }

    /** 帧间位移/旋转检测：拿相邻样本的姿态比较，动过就取消。 */
    private static boolean checkCameraStill(CameraRenderState camera) {
        double x = camera.pos.x;
        double y = camera.pos.y;
        double z = camera.pos.z;
        boolean moved = false;
        if (havePose) {
            double dx = x - lastX;
            double dy = y - lastY;
            double dz = z - lastZ;
            moved = dx * dx + dy * dy + dz * dz > MOVE_EPS_SQR
                || Math.abs(camera.xRot - lastXRot) > ROT_EPS_DEG
                || Math.abs(camera.yRot - lastYRot) > ROT_EPS_DEG;
        }
        havePose = true;
        lastX = x;
        lastY = y;
        lastZ = z;
        lastXRot = camera.xRot;
        lastYRot = camera.yRot;
        return !moved;
    }

    /**
     * 亚像素平移：给 x_clip 加上 {@code dx * w_clip}（y 同理），等价于把 NDC
     * 整体平移 dx/dy。JOML 字段布局下 x 的系数列是 (m00, m10, m20, m30)、
     * w 的系数列是 (m03, m13, m23, m33)。
     */
    private static void applyJitter(Matrix4f projection, float px, float py, int width, int height) {
        float dx = px * 2.0f / width;
        float dy = py * 2.0f / height;
        float w0 = projection.m03();
        float w1 = projection.m13();
        float w2 = projection.m23();
        float w3 = projection.m33();
        projection.m00(projection.m00() + dx * w0);
        projection.m10(projection.m10() + dx * w1);
        projection.m20(projection.m20() + dx * w2);
        projection.m30(projection.m30() + dx * w3);
        projection.m01(projection.m01() + dy * w0);
        projection.m11(projection.m11() + dy * w1);
        projection.m21(projection.m21() + dy * w2);
        projection.m31(projection.m31() + dy * w3);
    }

    private static double fract(double v) {
        return v - Math.floor(v);
    }

    private static void message(Minecraft minecraft, String text) {
        if (minecraft.gui != null) {
            minecraft.gui.hud.setOverlayMessage(Component.literal(text), false);
        }
    }
}
