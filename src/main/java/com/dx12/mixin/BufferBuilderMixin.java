package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MC 26.1.2: BufferBuilder.build() returns MeshData (not the old BuiltBuffer).
 * Hook at RETURN to get the fully-built MeshData with sorted vertex buffer.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.BufferBuilder", remap = false)
public class BufferBuilderMixin {

    @Inject(method = "build", at = @At("RETURN"), remap = false)
    private void onBuild(CallbackInfoReturnable<Object> cir) {
        System.out.println("[GL4DX12] === BufferBuilder.build() RETURN triggered ===");

        Object result = cir.getReturnValue();
        if (result == null) {
            System.out.println("[GL4DX12] meshData IS NULL, skipping processMeshData");
            return;
        }

        System.out.println("[GL4DX12] meshData class: " + result.getClass().getName());
        System.out.println("[GL4DX12] About to call D3D12Bridge.processMeshData");

        try {
            D3D12Bridge.processMeshData(result);
            System.out.println("[GL4DX12] D3D12Bridge.processMeshData returned successfully");
        } catch (Throwable t) {
            System.err.println("[GL4DX12] processMeshData threw exception: " + t.getMessage());
            t.printStackTrace();
        }
    }

    static {
        System.out.println("[GL4DX12] BufferBuilderMixin v23 loaded -> build()@RETURN");
    }
}
