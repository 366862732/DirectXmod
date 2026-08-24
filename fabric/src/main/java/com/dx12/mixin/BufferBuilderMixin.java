package com.dx12.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link BufferBuilder} to zero out uninitialized tail bytes of each
 * vertex slot after position data has been written into it.
 *
 * <p>Root causes of NaN in vertex buffers:
 * <ol>
 *   <li>{@code putVec3f()} writes only 3 floats (x, y, z at offsets 0,4,8),
 *       but DX12 expands {@code GpuFormat.RGB32_FLOAT} to
 *       {@code R32G32B32A32_FLOAT} (4 floats, 16 bytes). The unwritten w
 *       component (offset 12) reads whatever was previously in the ring buffer —
 *       often -nan from prior vertex reuse.</li>
 *   <li>In LINE primitive mode, {@code endLastVertex()} copies the full
 *       {@code vertexSize} bytes from the previous vertex via
 *       {@code memCopy}. If that vertex had uninitialized tail bytes, the copy
 *       propagates -nan into the next vertex.</li>
 * </ol>
 *
 * <p>Fix: at TAIL of {@code addVertex(x,y,z)}, zero only bytes 12..vertexSize-1
 * (the tail AFTER the written xyz). This preserves the valid x/y/z floats
 * (written by putVec3f at offsets 0/4/8) while eliminating NaN in position.w
 * and all trailing floats.</p>
 *
 * <p>Must use TAIL (not HEAD): beginVertex() sets vertexPointer inside the
 * method body, so HEAD injection would see vertexPointer=-1 (NOT_BUILDING)
 * and crash when passed to memSet().</p>
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
    @Shadow private long vertexPointer;
    @Shadow private int vertexSize;

    /**
     * Zeros bytes 12..15 of each vertex slot: the unwritten w component after
     * putVec3f(x,y,z) writes only offsets 0/4/8. Native side expands RGB32_FLOAT
     * to R32G32B32A32_FLOAT (16 bytes), reading .xyz and leaving .w as -nan.
     * TAIL injection ensures beginVertex() has already set vertexPointer.
     */
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("TAIL"), remap = false)
    private void gl4dx12$zeroVertexTail(float x, float y, float z,
            CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        if (vertexPointer > 0L) {
            MemoryUtil.memSet(vertexPointer + 12, 0, 4);
        }
    }

    /**
     * Same tail-zeroing for the 11-argument overload:
     * {@code addVertex(x,y,z, color, u, v, overlay, light, nx, ny, nz)}.
     * This overload is used by most block/entity tessellators. The 3-arg version
     * is only reached when no per-vertex attributes are set.
     */
    @Inject(method = "addVertex(FFFIIFFFF)V",
            at = @At("TAIL"), remap = false)
    private void gl4dx12$zeroVertexTailFull(float x, float y, float z,
            int color, float u, float v, int overlay, int light,
            float nx, float ny, float nz,
            CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        if (vertexPointer > 0L) {
            MemoryUtil.memSet(vertexPointer + 12, 0, 4);
        }
    }
}
