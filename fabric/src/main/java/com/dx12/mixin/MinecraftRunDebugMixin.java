package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.run() HEAD to log window / level / paused state
 * before the per-frame loop starts.  This lets us confirm whether the game
 * ever reaches a point where render() can be called (level must be non-null).
 */
@Mixin(Minecraft.class)
public class MinecraftRunDebugMixin {

    @Inject(method = "run", at = @At("HEAD"), remap = false)
    private void dx12_runDebug(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        // Shadow fields are not directly accessible from mixin without @Shadow,
        // but we can use reflection on the target object to read them.
        System.err.println("[dx12-debug] Minecraft.run() HEAD");
        System.err.flush();
    }
}
