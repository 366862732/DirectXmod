package com.dx12.mixin;

import com.dx12.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.IntBuffer;

/**
 * Passive monitor — tracks buffer IDs. Actual VBO data capture now
 * happens via BufferBuilderMixin, not through OpenGL hooks.
 */
@Mixin(targets = "org.lwjgl.opengl.GL15", remap = false)
public class GlBufferMixin {

    @Inject(method = "glGenBuffers", at = @At("HEAD"))
    private static void onGlGenBuffers(IntBuffer buffers, CallbackInfo ci) {
        for (int i = 0; i < buffers.remaining(); i++)
            D3D12Bridge.glGenBuffers();
    }

    @Inject(method = "glBindBuffer", at = @At("HEAD"))
    private static void onGlBindBuffer(int target, int buffer, CallbackInfo ci) {
        D3D12Bridge.glBindBuffer(target, buffer);
    }
}
