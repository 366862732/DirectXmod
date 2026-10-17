package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.IndexType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.SectionPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P68：世界几何光追场景的 Java 侧入口。
 *
 * <p>区块网格在 worker 线程上完成编译后，Mixin 把 CPU 侧顶点/索引直接交给
 * native（Uber 缓冲是 DEFAULT 堆，CPU 无法 Map，只能在这里截获原始 ByteBuffer）。
 * native 按 (section, 渲染层) 建独立 BLAS，渲染线程每帧合成 TLAS 后 DispatchRays。
 *
 * <p>只有当前光影包存在 {@code type: "raytrace"} 的 pass 时才开启（{@link #enabled}），
 * 否则不产生任何拷贝开销。
 */
public final class Dx12RtScene {

    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx12-shaderpack");

    /** 当前光影包是否包含光追 pass（由 {@code Dx12ShaderPackRenderer} 设置）。 */
    public static volatile boolean enabled = false;

    // ---- 诊断计数（区块 worker 线程并发自增，仅用于日志） ----
    /** 进入 native 调用的上传尝试次数。 */
    private static final AtomicLong uploadAttempts = new AtomicLong();
    /** native 返回 true（接受）的次数。 */
    private static final AtomicLong uploadAccepted = new AtomicLong();
    /** native 返回 false（静默拒绝，例如 DXR 不可用 / 显存不足）的次数。 */
    private static final AtomicLong uploadRejected = new AtomicLong();
    /** 前置条件不满足（无绘制层 / 缓冲为空 / 顶点索引过小）的次数。 */
    private static final AtomicLong uploadSkipped = new AtomicLong();
    /** 移除 section 几何的次数。 */
    private static final AtomicLong removedSections = new AtomicLong();
    private static final AtomicBoolean loggedFirstAttempt = new AtomicBoolean();
    private static final AtomicBoolean loggedFirstAccepted = new AtomicBoolean();
    private static final AtomicBoolean loggedFirstRejected = new AtomicBoolean();

    private Dx12RtScene() {}

    /**
     * 把一个区块 section 的某一渲染层网格送入 native 光追场景。
     * 只在 SOLID/CUTOUT 上调用——不透明几何才是太阳阴影的遮挡体。
     */
    public static void uploadSection(long node, ChunkSectionLayer layer,
            CompiledSectionMesh mesh, ByteBuffer vertexBuffer, ByteBuffer indexBuffer) {
        if (!enabled || !Dx12Native.isLoaded()) {
            return;
        }
        if (layer != ChunkSectionLayer.SOLID && layer != ChunkSectionLayer.CUTOUT) {
            uploadSkipped.incrementAndGet();
            return;
        }
        SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
        if (draw == null || vertexBuffer == null || indexBuffer == null) {
            uploadSkipped.incrementAndGet();
            return;
        }
        int stride = layer.vertexFormat().getVertexSize();
        if (stride <= 0) {
            uploadSkipped.incrementAndGet();
            return;
        }
        int vertexCount = vertexBuffer.remaining() / stride;
        int indexCount = draw.indexCount();
        boolean index32 = draw.indexType() == IndexType.INT;
        int indexBytes = indexCount * (index32 ? 4 : 2);
        if (vertexCount < 3 || indexCount < 3 || indexBytes > indexBuffer.remaining()) {
            uploadSkipped.incrementAndGet();
            return;
        }
        uploadAttempts.incrementAndGet();
        if (loggedFirstAttempt.compareAndSet(false, true)) {
            LOGGER.info("[shaderpack] RT 首次捕获区块几何: node={} layer={} verts={} idx={} (stride={})",
                node, layer, vertexCount, indexCount, stride);
        }
        try {
            boolean ok = Dx12Native.dx12RtUpdateSection(node, layer.ordinal(),
                SectionPos.x(node), SectionPos.y(node), SectionPos.z(node),
                vertexBuffer, stride, vertexCount, indexBuffer, indexCount, index32);
            if (ok) {
                uploadAccepted.incrementAndGet();
                if (loggedFirstAccepted.compareAndSet(false, true)) {
                    LOGGER.info("[shaderpack] RT native 接受首个 section 几何: node={} layer={}",
                        node, layer);
                }
            } else {
                uploadRejected.incrementAndGet();
                if (loggedFirstRejected.compareAndSet(false, true)) {
                    LOGGER.warn("[shaderpack] RT native 拒绝 section 几何（返回 false）: node={} layer={}"
                        + " verts={} idx={} —— 后续拒绝不再逐条记录", node, layer, vertexCount, indexCount);
                }
            }
        } catch (Throwable t) {
            // 区块 worker 线程上抛异常会打断整批构建，这里只记录一次。
            if (!reportedUploadError) {
                reportedUploadError = true;
                LOGGER.error("[shaderpack] RT section 上传异常", t);
            }
        }
    }

    /** section 被回收/重绑定时移除其几何。 */
    public static void removeSection(long node) {
        if (!enabled || !Dx12Native.isLoaded()) {
            return;
        }
        removedSections.incrementAndGet();
        Dx12Native.dx12RtRemoveSection(node);
    }

    /** 清空整个场景（换维度/退出世界/切换光影包）。 */
    public static void clear() {
        if (!Dx12Native.isLoaded()) {
            return;
        }
        Dx12Native.dx12RtClearScene();
    }

    /** 追加在 RT stats 日志行尾的诊断摘要。 */
    public static String debugSummary() {
        return "uploads[try=" + uploadAttempts.get() + " ok=" + uploadAccepted.get()
            + " nativeFalse=" + uploadRejected.get() + " skip=" + uploadSkipped.get()
            + " removed=" + removedSections.get() + "]";
    }

    private static volatile boolean reportedUploadError = false;
}
