package com.xgdt.dx12.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * P0 DEBUG: Injected at ClientPacketListener.handleLogin() to confirm whether
 * the server login packet ever arrives. If this fires but MinecraftSetLevelDebugMixin
 * does NOT fire, then the level assignment inside handleLogin is somehow bypassed.
 * If this never fires, the login packet is never received (server not ready /
 * connection stuck / packet processing broken).
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerLoginDebugMixin {

    @Inject(method = "handleLogin", at = @At("HEAD"), remap = false)
    private void dx12_handleLoginDebug(CallbackInfo ci) {
        System.err.println("[dx12-debug] ClientPacketListener.handleLogin() called — login packet received");
        System.err.flush();
    }
}
