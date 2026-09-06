package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * P32 diagnostic: logs visible sections count and valid chunk mesh count
 * during prepareChunkRenders(). Samples every 60 frames to reduce log noise.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererPrepareChunksDebugMixin {

    private int dx12_prepareFrameCount = 0;

    @Inject(method = "prepareChunkRenders", at = @At("TAIL"), remap = false)
    private void dx12_prepareChunkRendersDebug(org.joml.Matrix4fc modelViewMatrix,
            CallbackInfoReturnable<net.minecraft.client.renderer.chunk.ChunkSectionsToRender> cir) {
        dx12_prepareFrameCount++;
        if (dx12_prepareFrameCount % 60 != 0 && dx12_prepareFrameCount <= 5) return;

        // Access visibleSections via reflection (package-private field in decompiled source)
        int visibleCount = 0;
        int validMeshCount = 0;
        int skippedCount = 0;
        int renderDistance = -1;
        float depthFar = -1f;

        try {
            java.lang.reflect.Field vsField = LevelRenderer.class.getDeclaredField("visibleSections");
            vsField.setAccessible(true);
            Object vs = vsField.get(this);
            if (vs instanceof java.util.Collection) {
                visibleCount = ((java.util.Collection<?>) vs).size();
            }
            java.lang.reflect.Field optsField = LevelRenderer.class.getDeclaredField("optionsRenderState");
            optsField.setAccessible(true);
            Object optsState = optsField.get(this);
            if (optsState != null) {
                java.lang.reflect.Field rdField = optsState.getClass().getDeclaredField("renderDistance");
                rdField.setAccessible(true);
                renderDistance = rdField.getInt(optsState);
            }
        } catch (Exception ignored) {}

        // Access camera depthFar via GameRenderer
        try {
            java.lang.reflect.Field lrField = LevelRenderer.class.getDeclaredField("gameRenderer");
            lrField.setAccessible(true);
            Object gr = lrField.get(this);
            if (gr != null) {
                java.lang.reflect.Field mcField = gr.getClass().getDeclaredField("minecraft");
                mcField.setAccessible(true);
                Object mc = mcField.get(gr);
                if (mc != null) {
                    java.lang.reflect.Field camField = mc.getClass().getDeclaredField("mainCamera");
                    camField.setAccessible(true);
                    Object cam = camField.get(mc);
                    if (cam != null) {
                        java.lang.reflect.Field dfField = cam.getClass().getDeclaredField("depthFar");
                        dfField.setAccessible(true);
                        depthFar = dfField.getFloat(cam);
                    }
                }
            }
        } catch (Exception ignored) {}

        System.err.printf("[dx12-p32] prepareChunkRenders: visibleSections=%d renderDistance=%d depthFar=%.0f validMeshes=? frame=%d backend=%s%n",
                visibleCount, renderDistance, depthFar, dx12_prepareFrameCount,
                Dx12Device.isInitialized() ? "DX12" : "NONE");
        System.err.flush();
    }
}
