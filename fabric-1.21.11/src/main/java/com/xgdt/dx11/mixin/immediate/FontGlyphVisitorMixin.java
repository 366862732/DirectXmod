package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import com.xgdt.dx11.perf.immediate.ImmediateStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fast text lookup: remembers the last render type/consumer pair while one text
 * run is submitted.
 *
 * <p>A run of glyphs almost always alternates between the regular and the
 * see-through text sheets only when the style changes.  Vanilla still has to
 * re-run the buffer-source bookkeeping for every glyph; reusing the consumer
 * for a repeated type removes that work without changing the emitted vertex
 * stream.  The cache is per glyph-visitor instance, i.e. it never survives a
 * draw call, so a buffer that is flushed afterwards cannot be reused stale.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.Font$GlyphVisitor$1")
public abstract class FontGlyphVisitorMixin {
    @Unique
    private RenderType dx11$lastType;

    @Unique
    private VertexConsumer dx11$lastConsumer;

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer dx11$reuseTextBuffer(
        MultiBufferSource source, RenderType renderType, Operation<VertexConsumer> original) {
        if (!ImmediateFlags.fastTextLookup()) {
            return original.call(source, renderType);
        }
        if (renderType == this.dx11$lastType) {
            ImmediateStats.onTextLookup(true);
            return this.dx11$lastConsumer;
        }
        ImmediateStats.onTextLookup(false);
        VertexConsumer consumer = original.call(source, renderType);
        this.dx11$lastType = renderType;
        this.dx11$lastConsumer = consumer;
        return consumer;
    }
}
