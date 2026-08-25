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
 * called by ALL addVertex overloads (both 3-param and 11-param), so one injection
 * covers all paths.</p>
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {

    /**
     * Zero position.w (bytes 12-15) after putVec3f writes xyz.
     * Covers all addVertex paths since they all eventually call
     * putVec3f(ptr, x, y, z).
     *
     * <p>Diagnostic: prints on EVERY call (unconditionally before the guard)
     * so we can confirm the injection fires and see ptr value.</p>
     */
    @Inject(method = "putVec3f(JFFF)V", at = @At("HEAD"), remap = false)
    private static void gl4dx12$diagnosePutVec3f(long ptr, float x, float y, float z,
            CallbackInfo ci) {
        // HEAD 注入：每次调用都打印，确认注入触发
        System.err.printf(
            "[dx12-java] [BufferBuilderMixin] putVec3f called: ptr=%x x=%.4f y=%.4f z=%.4f ptr>0=%s%n",
            ptr, x, y, z, ptr > 0L ? "true" : "false");
        System.err.flush();
    }

    @Inject(method = "putVec3f(JFFF)V", at = @At("TAIL"), remap = false)
    private static void gl4dx12$zeroWComponent(long ptr, float x, float y, float z,
            CallbackInfo ci) {
        if (ptr <= 0L) {
            // NOT_BUILDING sentinel，跳过
            return;
        }
        float before = MemoryUtil.memGetFloat(ptr + 12);
        boolean isNan = Float.isNaN(before);
        boolean isInf = Float.isInfinite(before);
        if (isNan || isInf) {
            System.err.printf(
                "[dx12-java] [BufferBuilderMixin] NaN/Inf in w at ptr=%x x=%.4f y=%.4f z=%.4f prevW=%s%n",
                ptr, x, y, z,
                isNan ? "NaN" : "Inf");
            System.err.flush();
        }
        // 无论是否 NaN 都清零，保证 position.w 始终为 0
        MemoryUtil.memSet(ptr + 12, 0, 4);
    }
}
