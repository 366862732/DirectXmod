package com.dx12.mixin;

import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.onResourceLoadFinished() and onGameLoadFinished()
 * to log when resource loading completes and the initial screen is shown.
 */
@Mixin(Minecraft.class)
public class MinecraftResourceLoadDebugMixin {

    @Inject(method = "onResourceLoadFinished", at = @At("HEAD"), remap = false)
    private void dx12_onResourceLoadFinishedDebug(GameLoadCookie loadCookie, CallbackInfo ci) {
        System.err.println("[dx12-debug] onResourceLoadFinished() called");
        System.err.flush();
    }

    @Inject(method = "onGameLoadFinished", at = @At("HEAD"), remap = false)
    private void dx12_onGameLoadFinishedDebug(GameLoadCookie cookie, CallbackInfo ci) {
        System.err.println("[dx12-debug] onGameLoadFinished() called — gameLoadFinished flips to true");
        System.err.flush();
    }
}
