package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MC 26.1.2: BufferBuilder.build() returns MeshData.
 * Intercept return value and pass vertex data to D3D12 bridge.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.BufferBuilder", remap = false)
public class BufferBuilderMixin {

    @Inject(method = "build", at = @At("RETURN"), remap = false)
    private void onBuild(CallbackInfoReturnable<Object> cir) {
        if (!D3D12Bridge.isD3D12Active()) return;

        Object meshData = cir.getReturnValue();
        if (meshData == null) return;

        D3D12Bridge.processMeshData(meshData);
    }

    static {
        System.out.println("[GL4DX12] BufferBuilderMixin loaded -> build()@RETURN (active capture)");
    }
}
