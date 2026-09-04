package com.xgdt.dx12.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P0 DEBUG: Injected at Minecraft.doWorldLoad() HEAD to confirm whether
 * the world loading path is ever entered. This is the critical gate:
 * if this never fires, the user is stuck on the title screen and the
 * singleplayer server is never started.
 */
@Mixin(Minecraft.class)
public class MinecraftDoWorldLoadDebugMixin {

    @Inject(method = "doWorldLoad", at = @At("HEAD"), remap = false)
    private void dx12_doWorldLoadDebug(CallbackInfo ci) {
        System.err.println("[dx12-debug] Minecraft.doWorldLoad() ENTERED — world loading started");
        System.err.flush();
    }
}
