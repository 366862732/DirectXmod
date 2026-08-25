package com.dx12.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches {@link BufferBuilder} to zero out uninitialized tail bytes of each
 * vertex slot after position data has been written into it.
 *
 * <p>Root cause: {@code putVec3f(ptr, x, y, z)} writes only 3 floats (12 bytes)
 * at offsets 0/4/8, but DX12 expands {@code GpuFormat.RGB32_FLOAT} to
 * {@code R32G32B32A32_FLOAT} (4 floats, 16 bytes). The unwritten w component
 * (offset 12) reads whatever was previously in the ring buffer — often -nan.</p>
 *
 * <p>Fix: at TAIL of {@code putVec3f}, zero bytes 12..15 (position.w). This is
 * called by ALL addVertex overloads, so one injection covers all paths.</p>
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {

    /**
     * Zero position.w (bytes 12-15) after putVec3f writes xyz.
     * This single injection covers all addVertex call paths since they all
     * eventually call putVec3f(ptr, x, y, z).
     */
    @Inject(method = "putVec3f(JFFF)V", at = @At("TAIL"), remap = false)
    private static void gl4dx12$zeroWComponent(long ptr, float x, float y, float z,
            CallbackInfo ci) {
        // 诊断：记录每次调用，确认注入生效
        if (ptr > 0L) {
            // 打印前几个浮点值帮助定位污染源
            float before = MemoryUtil.memGetFloat(ptr + 12);
            if (Float.isNaN(before) || Float.isInfinite(before)) {
                System.err.printf(
                    "[dx12-java] [BufferBuilderMixin] NaN in w at ptr=%x x=%.4f y=%.4f z=%.4f prevW=%s%n",
                    ptr, x, y, z,
                    Float.isNaN(before) ? "NaN" : "Inf");
                System.err.flush();
            }
            MemoryUtil.memSet(ptr + 12, 0, 4);
        }
    }
}
