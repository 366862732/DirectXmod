package com.xgdt.dx12.mixin;

import com.xgdt.dx12.dx12.Dx12Device;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * P32 diagnostic: logs getRenderSectionSlice null returns in SectionRenderDispatcher.
 * When slice is null, the chunk's vertex buffer hasn't been uploaded yet — these
 * chunks will be skipped during rendering, causing distant chunks to appear black.
 */
@Mixin(SectionRenderDispatcher.class)
public class SectionRenderDispatcherGetSliceDebugMixin {

    @Unique private int dx12_sliceCheckCount = 0;
    @Unique private int dx12_nullSliceCount = 0;

    @Inject(method = "getRenderSectionSlice", at = @At("TAIL"), remap = false)
    private void dx12_getSliceDebug(SectionMesh sectionMesh, ChunkSectionLayer layer,
            CallbackInfoReturnable<SectionRenderDispatcher.RenderSectionBufferSlice> cir) {
        dx12_sliceCheckCount++;
        SectionRenderDispatcher.RenderSectionBufferSlice slice = cir.getReturnValue();
        if (slice == null) {
            dx12_nullSliceCount++;
            // Log first 3 nulls, then sample every 60 checks to avoid spam
            if (dx12_nullSliceCount <= 3 || dx12_sliceCheckCount % 60 == 0) {
                String meshStatus = (sectionMesh == null) ? "null"
                        : (sectionMesh == CompiledSectionMesh.UNCOMPILED) ? "UNCOMPILED" : "compiled";
                System.err.printf("[dx12-p32] getRenderSectionSlice: NULL slice (layer=%s mesh=%s) totalChecks=%d nullCount=%d backend=%s%n",
                        layer, meshStatus, dx12_sliceCheckCount, dx12_nullSliceCount,
                        Dx12Device.isInitialized() ? "DX12" : "NONE");
                System.err.flush();
            }
        }
    }
}
