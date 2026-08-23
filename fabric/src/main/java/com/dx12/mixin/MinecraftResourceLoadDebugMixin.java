package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.onResourceLoadFinished() to log when resources finish loading.
 * This is the gate that flips gameLoadFinished=true, enabling shouldRenderLevel.
 */
@Mixin(Minecraft.class)
public class MinecraftResourceLoadDebugMixin {

    @Inject(method = "onResourceLoadFinished", at = @At("HEAD"), remap = false)
    private void dx12_onResourceLoadFinishedDebug(org.jetbrains.annotations.Nullable Object loadCookie, CallbackInfo ci) {
        System.err.println("[dx12-debug] onResourceLoadFinished() called (cookies=" + (loadCookie != null ? "non-null" : "null") + ")");
        System.err.flush();
    }

    @Inject(method = "onGameLoadFinished", at = @At("HEAD"), remap = false)
    private void dx12_onGameLoadFinishedDebug(org.jetbrains.annotations.Nullable Object cookie, CallbackInfo ci) {
        System.err.println("[dx12-debug] onGameLoadFinished() called — gameLoadFinished will flip to true");
        System.err.flush();
    }
}
