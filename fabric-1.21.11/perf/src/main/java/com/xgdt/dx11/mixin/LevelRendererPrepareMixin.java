package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.culling.VisibleSections;
import com.xgdt.dx11.perf.region.RegionFrame;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the frame's model-view matrix, the visible-section walk and the
 * occlusion-filtered visible section list.
 *
 * <p>{@code prepareChunkRenders} is the only place where vanilla still has the
 * camera matrix, the atlas size and every visible section's fade visibility
 * together.  Region draws need the same information once per region rather
 * than once per section, so the frame is opened here and closed at RETURN,
 * where {@link RegionFrame} writes the region uniforms before the render
 * passes start.</p>
 *
 * <p>{@code applyFrustum} is the only place vanilla rebuilds the visible
 * section list before the section uploads and entity extraction run.
 * {@link VisibleSections} snapshots its origins there for the Phase 6 upload
 * ranking and entity culling.  Capturing at the rebuild (rather than every
 * frame) keeps the copy and sort out of steady-state frames.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class LevelRendererPrepareMixin {
    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    private void dx11$beginRegionFrame(
        Matrix4fc modelView,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        RegionFrame.begin(modelView, cameraX, cameraY, cameraZ);
    }

    @Inject(method = "prepareChunkRenders", at = @At("RETURN"))
    private void dx11$endRegionFrame(
        Matrix4fc modelView,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        RegionFrame.end();
    }

    @Inject(method = "applyFrustum", at = @At("RETURN"))
    private void dx11$captureVisibleSections(Frustum frustum, CallbackInfo ci) {
        VisibleSections.capture(((LevelRenderer) (Object) this).getVisibleSections());
    }
}
