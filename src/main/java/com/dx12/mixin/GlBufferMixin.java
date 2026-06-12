package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

@Mixin(targets = "org.lwjgl.opengl.GL15", remap = false)
public class GlBufferMixin {

    @Inject(method = "glGenBuffers", at = @At("HEAD"), cancellable = true)
    private static void onGlGenBuffers(IntBuffer buffers, CallbackInfo ci) {
        for (int i = 0; i < buffers.remaining(); i++) {
            int id = D3D12Bridge.glGenBuffers();
            buffers.put(buffers.position() + i, id);
        }
        ci.cancel();
    }

    @Inject(method = "glBindBuffer", at = @At("HEAD"), cancellable = true)
    private static void onGlBindBuffer(int target, int buffer, CallbackInfo ci) {
        D3D12Bridge.glBindBuffer(target, buffer);
        ci.cancel();
    }

    @Inject(method = "glBufferData", at = @At("HEAD"), cancellable = true)
    private static void onGlBufferData(int target, ByteBuffer data, int usage, CallbackInfo ci) {
        if (data != null && data.remaining() > 0) {
            D3D12Bridge.glBufferData(target, data, usage);
        }
        ci.cancel();
    }
}
