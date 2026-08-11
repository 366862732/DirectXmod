package com.dx12.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link BufferBuilder} to zero out uninitialized float components.
 *
 * <p>{@code BufferBuilder.putVec3f()} only writes 3 floats (x, y, z), but DX12
 * maps {@code GpuFormat.RGB32_FLOAT} to {@code DXGI_FORMAT_R32G32B32A32_FLOAT}
 * (4 floats). The unwritten w and trailing elements read as -nan from the UPLOAD
 * staging heap. This mixin zeroes all floats from offset 12 to vertexSize.
 * {@code setUv()} / {@code setColor()} called later overwrite these zeros.</p>
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
    @Shadow private long vertexPointer;
    @Shadow private int vertexSize;

    /**
     * Zeros all floats from position.w (offset 12) to end of vertex.
     * Fixes -nan in position.w and any trailing uninitialized floats (e.g. UV.v).
     */
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("TAIL"), remap = true)
    private void gl4dx12$zeroTrailingFloats(float x, float y, float z,
            CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        int startFloat = 3; // offset 12 / 4
        int floatCount = vertexSize / 4;
        for (int i = startFloat; i < floatCount; ++i) {
            MemoryUtil.memPutFloat(vertexPointer + (long) (i * 4), 0.0f);
        }
    }
}
