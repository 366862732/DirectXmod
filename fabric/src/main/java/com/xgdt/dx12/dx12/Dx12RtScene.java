package com.xgdt.dx12.dx12;

import com.mojang.blaze3d.IndexType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.SectionPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    /** 由本类合成了四边形索引缓冲的次数（SOLID/CUTOUT 常态）。 */
    private static final AtomicLong synthesizedIndices = new AtomicLong();
    /** 非 SOLID/CUTOUT 层被跳过的次数。 */
    private static final AtomicLong skipLayer = new AtomicLong();
    /** 缺绘制层 / 顶点缓冲 / 索引不匹配而跳过的次数。 */
    private static final AtomicLong skipMissing = new AtomicLong();
    /** 顶点数或缓冲尺寸过小、stride 异常而跳过的次数。 */
    private static final AtomicLong skipSmall = new AtomicLong();
    /** 移除 section 几何的次数。 */
    private static final AtomicLong removedSections = new AtomicLong();
    private static final AtomicBoolean loggedFirstAttempt = new AtomicBoolean();
    private static final AtomicBoolean loggedFirstAccepted = new AtomicBoolean();
    private static final AtomicBoolean loggedFirstRejected = new AtomicBoolean();

    private Dx12RtScene() {}

    /**
     * 把一个区块 section 的某一渲染层网格送入 native 光追场景。
     * 只在 SOLID/CUTOUT 上调用——不透明几何才是太阳阴影的遮挡体。
     *
     * <p>注意：SOLID/CUTOUT 层没有自定义索引缓冲（渲染时由 SV_VertexID 展开四边形），
     * 只有 TRANSLUCENT 经 sortQuads 才会带上索引。这里为前者合成标准四边形索引
     * （每 4 顶点 6 索引，模式 0-1-2 / 2-3-0，与 {@code MeshData.SortState.writeIndices}
     * 一致）；光追阴影射线未开启背面剔除，绕序不影响命中。
     */
    public static void uploadSection(long node, ChunkSectionLayer layer,
            CompiledSectionMesh mesh, ByteBuffer vertexBuffer, ByteBuffer indexBuffer) {
        if (!enabled || !Dx12Native.isLoaded()) {
            return;
        }
        if (layer != ChunkSectionLayer.SOLID && layer != ChunkSectionLayer.CUTOUT) {
            skipLayer.incrementAndGet();
            return;
        }
        SectionMesh.SectionDraw draw = mesh.getSectionDraw(layer);
        if (draw == null || vertexBuffer == null) {
            skipMissing.incrementAndGet();
            return;
        }
        int stride = layer.vertexFormat().getVertexSize();
        if (stride <= 0) {
            skipSmall.incrementAndGet();
            return;
        }
        int vertexCount = vertexBuffer.remaining() / stride;
        if (vertexCount < 4) {
            skipSmall.incrementAndGet();
            return;
        }
        int indexCount;
        boolean index32;
        ByteBuffer indices;
        if (draw.hasCustomIndexBuffer()) {
            if (indexBuffer == null || draw.indexCount() < 3) {
                skipMissing.incrementAndGet();
                return;
            }
            indices = indexBuffer;
            indexCount = draw.indexCount();
            index32 = draw.indexType() == IndexType.INT;
            int indexBytes = indexCount * (index32 ? 4 : 2);
            if (indexBytes > indices.remaining()) {
                skipSmall.incrementAndGet();
                return;
            }
        } else {
            int quads = vertexCount / 4;
            indexCount = quads * 6;
            index32 = vertexCount > 65535;
            indices = buildQuadIndices(quads, index32);
            synthesizedIndices.incrementAndGet();
        }
        uploadAttempts.incrementAndGet();
        if (loggedFirstAttempt.compareAndSet(false, true)) {
            LOGGER.info("[shaderpack] RT 首次捕获区块几何: node={} layer={} verts={} idx={}"
                + " (stride={} 合成索引={})", node, layer, vertexCount, indexCount, stride,
                !draw.hasCustomIndexBuffer());
        }
        try {
            boolean ok = Dx12Native.dx12RtUpdateSection(node, layer.ordinal(),
                SectionPos.x(node), SectionPos.y(node), SectionPos.z(node),
                vertexBuffer, stride, vertexCount, indices, indexCount, index32);
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

    /** 为四边形网格合成索引：每 4 顶点 6 索引（0-1-2 / 2-3-0），与官方排序索引一致。 */
    private static ByteBuffer buildQuadIndices(int quads, boolean index32) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(quads * 6 * (index32 ? 4 : 2))
            .order(ByteOrder.LITTLE_ENDIAN);
        for (int quad = 0; quad < quads; quad++) {
            int base = quad * 4;
            if (index32) {
                buffer.putInt(base).putInt(base + 1).putInt(base + 2)
                      .putInt(base + 2).putInt(base + 3).putInt(base);
            } else {
                buffer.putShort((short) base).putShort((short) (base + 1)).putShort((short) (base + 2))
                      .putShort((short) (base + 2)).putShort((short) (base + 3)).putShort((short) base);
            }
        }
        buffer.flip();
        return buffer;
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
            + " nativeFalse=" + uploadRejected.get() + " synthIdx=" + synthesizedIndices.get()
            + " skip(layer=" + skipLayer.get() + " miss=" + skipMissing.get()
            + " small=" + skipSmall.get() + ") removed=" + removedSections.get() + "]";
    }

    private static volatile boolean reportedUploadError = false;
}
