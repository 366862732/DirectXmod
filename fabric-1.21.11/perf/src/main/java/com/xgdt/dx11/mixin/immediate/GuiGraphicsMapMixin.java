package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.xgdt.dx11.perf.immediate.map.MapAtlas;
import com.xgdt.dx11.perf.immediate.map.MapAtlasRenderStateAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Uses the map's atlas tile for the GUI map blit.
 *
 * <p>The texture view itself already points at the atlas because the map's
 * vanilla location now resolves to it; only the 0..1 UV rectangle has to be
 * narrowed to the tile.  Decoration blits are separate calls and are not
 * touched.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMapMixin {
    @WrapOperation(
        method = "submitMapRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;submitBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
            ordinal = 0))
    private void dx11$atlasMapBlit(
        GuiGraphics graphics,
        RenderPipeline pipeline,
        GpuTextureView textureView,
        GpuSampler sampler,
        int x1,
        int y1,
        int x2,
        int y2,
        float u1,
        float u2,
        float v1,
        float v2,
        int color,
        Operation<Void> original,
        @Local(argsOnly = true) MapRenderState renderState) {
        MapAtlasRenderStateAccess state = (MapAtlasRenderStateAccess) renderState;
        MapAtlas atlas = state.dx11$mapAtlas();
        if (atlas != null && renderState.texture != null
            && atlas.identifier().equals(renderState.texture)) {
            float size = atlas.size();
            int atlasX = state.dx11$mapAtlasX();
            int atlasY = state.dx11$mapAtlasY();
            u1 = atlasX / size;
            u2 = (atlasX + MapAtlas.MAP_SIZE) / size;
            v1 = atlasY / size;
            v2 = (atlasY + MapAtlas.MAP_SIZE) / size;
        }
        original.call(
            graphics, pipeline, textureView, sampler, x1, y1, x2, y2, u1, u2, v1, v2, color);
    }
}
