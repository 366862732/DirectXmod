package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.setLevel() to log whenever a world level is set.
 * This is the key transition point: once this fires, renderLevel() becomes possible.
 */
@Mixin(Minecraft.class)
public class MinecraftSetLevelDebugMixin {

    @Inject(method = "setLevel", at = @At("HEAD"), remap = false)
    private void dx12_setLevelDebug(ClientLevel level, CallbackInfo ci) {
        System.err.println("[dx12-debug] Minecraft.setLevel() called: level=" + (level != null ? "non-null" : "NULL"));
        System.err.flush();
    }
}
