package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts BufferBuilder.build() / buildOrThrow() to capture
 * MeshData BEFORE it reaches GL upload.
 *
 * Minecraft 26.1.2 (Mojang): com.mojang.blaze3d.vertex.BufferBuilder
 *  - build()    → MeshData
 *  - buildOrThrow() → MeshData
 *
 * MeshData.drawState() → DrawState { mode(), vertexCount(), indexCount(),
 *                                      format(), indexType() }
 * MeshData.vertexBuffer() → ByteBuffer
 * MeshData.indexBuffer()  → ByteBuffer
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.BufferBuilder", remap = false)
public class BufferBuilderMixin {

    @Inject(method = "build", at = @At("RETURN"), remap = false)
    private void onBuild(CallbackInfoReturnable<Object> cir) {
        Object meshData = cir.getReturnValue();
        if (meshData != null) D3D12Bridge.onMeshDataBuild(meshData);
    }

    @Inject(method = "buildOrThrow", at = @At("RETURN"), remap = false)
    private void onBuildOrThrow(CallbackInfoReturnable<Object> cir) {
        Object meshData = cir.getReturnValue();
        if (meshData != null) D3D12Bridge.onMeshDataBuild(meshData);
    }
}
