package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xgdt.dx11.perf.immediate.ImmediateStats;
import com.xgdt.dx11.perf.immediate.map.MapAtlas;
import com.xgdt.dx11.perf.immediate.map.MapAtlasManagerAccess;
import com.xgdt.dx11.perf.immediate.map.MapAtlasRenderStateAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes world/held/in-frame maps through the atlas.
 *
 * <p>{@code extractRenderState} records which tile belongs to the map and
 * {@code render} replaces only the map quad's UVs.  Decoration sprites keep
 * their own atlas and are untouched.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Shadow
    @Final
    private MapTextureManager mapTextureManager;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void dx11$attachAtlasTile(
        MapId mapId,
        MapItemSavedData data,
        MapRenderState renderState,
        CallbackInfo ci) {
        MapAtlasRenderStateAccess state = (MapAtlasRenderStateAccess) renderState;
        if (!(this.mapTextureManager instanceof MapAtlasManagerAccess manager)) {
            state.dx11$clearMapAtlas();
            return;
        }
        int slot = manager.dx11$atlasSlot(mapId.id());
        MapAtlas atlas = slot == -1 ? null : manager.dx11$atlas(MapAtlas.atlasId(slot));
        ImmediateStats.onMapLookup(atlas != null);
        if (atlas == null) {
            state.dx11$clearMapAtlas();
            return;
        }
        state.dx11$setMapAtlas(
            atlas,
            MapAtlas.column(slot) * MapAtlas.MAP_SIZE,
            MapAtlas.row(slot) * MapAtlas.MAP_SIZE);
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitCustomGeometry(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeCollector$CustomGeometryRenderer;)V",
            ordinal = 0))
    private void dx11$atlasMapQuad(
        SubmitNodeCollector collector,
        com.mojang.blaze3d.vertex.PoseStack pose,
        net.minecraft.client.renderer.rendertype.RenderType renderType,
        SubmitNodeCollector.CustomGeometryRenderer geometry,
        Operation<Void> original,
        @Local(argsOnly = true) MapRenderState renderState,
        @Local(argsOnly = true) int light) {
        MapAtlasRenderStateAccess state = (MapAtlasRenderStateAccess) renderState;
        MapAtlas atlas = state.dx11$mapAtlas();
        if (atlas == null || renderState.texture == null
            || !atlas.identifier().equals(renderState.texture)) {
            original.call(collector, pose, renderType, geometry);
            return;
        }
        float size = atlas.size();
        int atlasX = state.dx11$mapAtlasX();
        int atlasY = state.dx11$mapAtlasY();
        float u1 = atlasX / size;
        float u2 = (atlasX + MapAtlas.MAP_SIZE) / size;
        float v1 = atlasY / size;
        float v2 = (atlasY + MapAtlas.MAP_SIZE) / size;
        SubmitNodeCollector.CustomGeometryRenderer atlasGeometry = (entry, consumer) -> {
            consumer.addVertex(entry, 0.0F, MapAtlas.MAP_SIZE, -0.01F)
                .setColor(-1).setUv(u1, v2).setLight(light);
            consumer.addVertex(entry, MapAtlas.MAP_SIZE, MapAtlas.MAP_SIZE, -0.01F)
                .setColor(-1).setUv(u2, v2).setLight(light);
            consumer.addVertex(entry, MapAtlas.MAP_SIZE, 0.0F, -0.01F)
                .setColor(-1).setUv(u2, v1).setLight(light);
            consumer.addVertex(entry, 0.0F, 0.0F, -0.01F)
                .setColor(-1).setUv(u1, v1).setLight(light);
        };
        original.call(collector, pose, renderType, atlasGeometry);
    }
}
