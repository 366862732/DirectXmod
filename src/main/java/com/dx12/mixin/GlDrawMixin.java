package com.dx12.mixin;

import com.dx12.client.D3D12Bridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.lwjgl.opengl.GL11", remap = false)
public class GlDrawMixin {

    @Inject(method = "glClear", at = @At("HEAD"), cancellable = true)
    private static void onGlClear(int mask, CallbackInfo ci) {
        D3D12Bridge.glClear(mask);
        ci.cancel();
    }

    @Inject(method = "glClearColor", at = @At("HEAD"), cancellable = true)
    private static void onGlClearColor(float r, float g, float b, float a, CallbackInfo ci) {
        D3D12Bridge.glClearColor(r, g, b, a);
        ci.cancel();
    }

    @Inject(method = "glDrawArrays", at = @At("HEAD"), cancellable = true)
    private static void onGlDrawArrays(int mode, int first, int count, CallbackInfo ci) {
        D3D12Bridge.glDrawArrays(mode, first, count);
        ci.cancel();
    }
}
