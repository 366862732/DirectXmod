package com.dx12.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link BufferBuilder} to zero out all uninitialized float components
 * before any data is written into a new vertex slot.
 *
 * <p>Two root causes of NaN in vertex buffers:
 * <ol>
 *   <li>{@code putVec3f()} writes only 3 floats (x, y, z), but DX12 expands
 *       {@code GpuFormat.RGB32_FLOAT} to {@code R32G32B32A32_FLOAT} (4 floats).
 *       The unwritten w component reads whatever was previously in the ring
 *       buffer — often -nan from prior vertex reuse.</li>
 *   <li>In LINE primitive mode, {@code endLastVertex()} copies the full
 *       {@code vertexSize} bytes from the previous vertex via
 *       {@code memCopy}. If that vertex had uninitialized tail bytes, the copy
 *       propagates -nan into the next vertex before the per-component setters
 *       run.</li>
 * </ol>
 *
 * <p>Fix: zero the entire vertex buffer slot the moment {@code addVertex(x,y,z)}
 * is called. By that point {@code beginVertex()} has reserved fresh ring-buffer
 * memory and {@code putVec3f()} has written x/y/z. Zeroing the remaining bytes
 * (and, for safety, re-zeroing position too) guarantees no garbage floats are
 * ever uploaded to the GPU.</p>
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
    @Shadow private long vertexPointer;
    @Shadow private int vertexSize;

    /**
     * Zeros the entire vertex slot at the end of {@code addVertex(x,y,z)}.
     * Eliminates NaN propagation from uninitialized ring-buffer memory,
     * covering position.w and all trailing floats.
     */
    @Inject(method = "addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("TAIL"), remap = true)
    private void gl4dx12$zeroVertexOnAdd(float x, float y, float z,
            CallbackInfoReturnable<com.mojang.blaze3d.vertex.VertexConsumer> cir) {
        if (vertexPointer != 0L && vertexSize > 0) {
            MemoryUtil.memSet(vertexPointer, 0, vertexSize);
        }
    }
}
