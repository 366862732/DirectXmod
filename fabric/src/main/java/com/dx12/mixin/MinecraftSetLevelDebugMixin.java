package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.setLevel() to log whenever a world level is set.
 * This is the key transition point: once this fires, renderLevel() becomes possible.
 *
 * P0 DEBUG: If this mixin never fires, setLevel() is genuinely not called.
 * Possible causes:
 *   1. The login packet (ClientboundLoginPacket) is never received
 *   2. handleLogin() path is bypassed (e.g. direct level assignment)
 *   3. Mixin injection fails silently (check gl4dx12.mixins.json registration)
 */
@Mixin(Minecraft.class)
public class MinecraftSetLevelDebugMixin {

    @Inject(method = "setLevel", at = @At("HEAD"), remap = false)
    private void dx12_setLevelDebug(ClientLevel level, CallbackInfo ci) {
        System.err.println("[dx12-debug] Minecraft.setLevel() called: level=" + (level != null ? "non-null" : "NULL"));
        System.err.flush();
    }
}
