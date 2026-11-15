package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.PerfState;
import com.xgdt.dx11.perf.region.RegionVertexSlice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips vanilla's per-section translucent resort for sections whose
 * translucent mesh lives in a region arena.
 *
 * <p>{@code RegionTranslucentSorter} already sorts every visible quad across
 * the region and rebuilds the region index buffer when the camera moves.  A
 * vanilla resort would only allocate and upload a per-section index buffer
 * that region submission never uses.  Vanilla-backed sections keep their
 * normal resort behavior.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class ScheduleResortMixin {
    @Inject(method = "scheduleResort", at = @At("HEAD"), cancellable = true)
    private void dx11$skipRegionTranslucentResort(
        SectionRenderDispatcher.RenderSection renderSection,
        TranslucencyPointOfView pointOfView,
        Vec3 cameraPosition,
        boolean fullResort,
        boolean axisAlignedResort,
        CallbackInfo ci) {
        if (!PerfState.regionActive() || !PerfState.config().translucentRegion) {
            return;
        }
        SectionMesh mesh = renderSection.getSectionMesh();
        if (!(mesh instanceof CompiledSectionMesh compiled)) {
            return;
        }
        SectionBuffers buffers = compiled.getBuffers(ChunkSectionLayer.TRANSLUCENT);
        if (buffers != null && buffers.getVertexBuffer() instanceof RegionVertexSlice) {
            ci.cancel();
        }
    }
}
