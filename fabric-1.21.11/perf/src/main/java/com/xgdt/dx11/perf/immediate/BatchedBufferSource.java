package com.xgdt.dx11.perf.immediate;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xgdt.dx11.mixin.immediate.RenderSetupAccessor;
import com.xgdt.dx11.mixin.immediate.RenderSetupTextureBindingAccessor;
import com.xgdt.dx11.mixin.immediate.RenderTypeAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * A {@link MultiBufferSource.BufferSource} that defers feature-layer flushes
 * and can keep several pooled builders in flight.
 *
 * <p>Vanilla keeps a single shared builder and flushes the previous render type
 * whenever a different shared type starts, so a feature pass that alternates
 * between entity layers submits many small meshes.  This implementation keeps
 * the streams open, flushes them together at explicit boundaries (before name
 * tags and particles), and orders the flush so text and depth-sorted layers
 * keep vanilla's draw order.</p>
 *
 * <p>When the feature flag is off every override delegates to the vanilla
 * superclass, which keeps A/B runs byte-for-byte comparable.</p>
 */
public final class BatchedBufferSource extends MultiBufferSource.BufferSource
        implements AutoCloseable {
    private static final int SORTED_ORDER_OFFSET = 100_000_000;

    private final Map<RenderType, List<BufferBuilder>> dynamic = new IdentityHashMap<>();
    private final Map<BufferBuilder, ByteBufferBuilder> owned = new IdentityHashMap<>();
    private final Set<RenderType> active = new LinkedHashSet<>();
    private boolean drawDynamicFirst;
    private boolean sharedInUse;

    public BatchedBufferSource(
        ByteBufferBuilder sharedBuffer,
        SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers) {
        super(sharedBuffer, fixedBuffers);
    }

    private static boolean enabled() {
        return ImmediateFlags.batching();
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        if (!enabled() && this.active.isEmpty()) {
            return super.getBuffer(renderType);
        }

        if (!this.drawDynamicFirst
            && this.lastSharedType != null
            && this.lastSharedType != renderType
            && !this.fixedBuffers.containsKey(this.lastSharedType)) {
            this.drawDynamicFirst = true;
        }

        if (!renderType.canConsolidateConsecutiveGeometry()) {
            ByteBufferBuilder backing = this.nextBuilder();
            BufferBuilder builder = new BufferBuilder(
                backing, renderType.mode(), renderType.format());
            this.lastSharedType = renderType;
            this.track(renderType, builder, backing);
            return builder;
        }

        List<BufferBuilder> existing = this.dynamic.get(renderType);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        if (this.fixedBuffers.containsKey(renderType)) {
            BufferBuilder builder = new BufferBuilder(
                this.fixedBuffers.get(renderType), renderType.mode(), renderType.format());
            this.track(renderType, builder, null);
            return builder;
        }

        ByteBufferBuilder backing = this.nextBuilder();
        BufferBuilder builder = new BufferBuilder(
            backing, renderType.mode(), renderType.format());
        this.lastSharedType = renderType;
        this.track(renderType, builder, backing);
        return builder;
    }

    private void track(RenderType renderType, BufferBuilder builder, ByteBufferBuilder ownedBuffer) {
        this.dynamic.computeIfAbsent(renderType, ignored -> new ArrayList<>()).add(builder);
        this.active.add(renderType);
        if (ownedBuffer != null) {
            this.owned.put(builder, ownedBuffer);
        }
        ImmediateStats.onActiveBatches(this.active.size());
    }

    private ByteBufferBuilder nextBuilder() {
        if (!this.sharedInUse) {
            this.sharedInUse = true;
            return this.sharedBuffer;
        }
        return BufferPool.borrow();
    }

    @Override
    public void endLastBatch() {
        if (!enabled() && this.active.isEmpty()) {
            super.endLastBatch();
            return;
        }
        this.lastSharedType = null;
        this.drawDynamicFirst = false;
        if (this.active.isEmpty()) {
            return;
        }

        List<RenderType> order = new ArrayList<>(this.active.size());
        for (RenderType renderType : this.active) {
            if (!this.fixedBuffers.containsKey(renderType)) {
                order.add(renderType);
            }
        }
        if (order.isEmpty()) {
            return;
        }
        order.sort((left, right) -> Integer.compare(this.order(left), this.order(right)));
        ImmediateStats.onBatchFlush();
        for (RenderType renderType : order) {
            this.flush(renderType);
        }
    }

    @Override
    public void endBatch() {
        if (!enabled() && this.active.isEmpty()) {
            super.endBatch();
            return;
        }
        this.endLastBatch();
        for (RenderType renderType : this.fixedBuffers.keySet()) {
            this.flush(renderType);
        }
        this.close();
    }

    @Override
    public void endBatch(RenderType renderType) {
        if (!enabled() && this.active.isEmpty()) {
            super.endBatch(renderType);
            return;
        }
        if (this.drawDynamicFirst) {
            this.endLastBatch();
        }
        this.flush(renderType);
    }

    private void flush(RenderType renderType) {
        this.active.remove(renderType);
        List<BufferBuilder> builders = this.dynamic.remove(renderType);
        if (builders != null) {
            for (BufferBuilder builder : builders) {
                ByteBufferBuilder backing = this.owned.get(builder);
                MeshData mesh = builder.build();
                if (mesh != null) {
                    if (renderType.sortOnUpload()) {
                        // Sort into the builder's own segment: the shared
                        // buffer can still hold another open stream, and
                        // using it as scratch would corrupt that stream.
                        ByteBufferBuilder scratch = backing != null
                            ? backing
                            : this.fixedBuffers.getOrDefault(renderType, this.sharedBuffer);
                        mesh.sortQuads(
                            scratch,
                            RenderSystem.getProjectionType().vertexSorting());
                    }
                    renderType.draw(mesh);
                    ImmediateStats.onBatchDraw();
                }
                if (backing != null) {
                    this.owned.remove(builder);
                    if (backing == this.sharedBuffer) {
                        this.sharedInUse = false;
                    } else {
                        BufferPool.release(backing);
                    }
                }
            }
        }
        if (this.lastSharedType == renderType) {
            this.lastSharedType = null;
        }
    }

    private int order(RenderType renderType) {
        int order = 0;
        if (renderType instanceof RenderTypeAccessor accessor) {
            String name = accessor.dx11$name();
            if (name != null && name.startsWith("text")) {
                // Vanilla-font text must draw after text from custom fonts
                // when both share a batch; otherwise the custom glyphs
                // z-fight with the vanilla sheet.  This mirrors the reference
                // implementation's ordering rule.
                RenderSetup setup = accessor.dx11$state();
                if (setup != null) {
                    Object binding = ((RenderSetupAccessor) (Object) setup)
                        .dx11$textures()
                        .get("Sampler0");
                    if (binding instanceof RenderSetupTextureBindingAccessor bound) {
                        Identifier textureId = bound.dx11$location();
                        if (textureId != null) {
                            order = "minecraft".equals(textureId.getNamespace()) ? 2 : 1;
                        }
                    }
                }
            }
        }
        return renderType.sortOnUpload() ? SORTED_ORDER_OFFSET + order : order;
    }

    @Override
    public void close() {
        for (List<BufferBuilder> builders : this.dynamic.values()) {
            for (BufferBuilder builder : builders) {
                MeshData mesh = builder.build();
                if (mesh != null) {
                    mesh.close();
                }
                ByteBufferBuilder backing = this.owned.remove(builder);
                if (backing != null) {
                    if (backing == this.sharedBuffer) {
                        this.sharedInUse = false;
                    } else {
                        BufferPool.release(backing);
                    }
                }
            }
        }
        this.dynamic.clear();
        this.active.clear();
        this.lastSharedType = null;
        this.drawDynamicFirst = false;
    }
}
