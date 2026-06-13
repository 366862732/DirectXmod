package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
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
        Object meshData = cir.getReturnValue();
        if (meshData != null)
            D3D12Bridge.processMeshData(meshData);
    }

    static {
        System.out.println("[GL4DX12] BufferBuilderMixin v23 loaded -> build()@RETURN");
    }
}
