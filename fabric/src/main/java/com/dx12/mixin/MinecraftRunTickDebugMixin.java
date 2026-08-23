package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.runTick() HEAD to log level / gameLoadFinished state
 * each frame, so we can see when (or if) they ever become non-null/true.
 */
@Mixin(Minecraft.class)
public class MinecraftRunTickDebugMixin {

    @Shadow(remap = false) private boolean gameLoadFinished;

    @Inject(method = "runTick", at = @At("HEAD"), remap = false)
    private void dx12_runTickDebug(boolean advanceGameTime, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        // Access level field via reflection (no @Shadow needed for nullable)
        java.lang.reflect.Field levelField;
        Object level = null;
        try {
            levelField = Minecraft.class.getDeclaredField("level");
            levelField.setAccessible(true);
            level = levelField.get(mc);
        } catch (Exception ignored) {}
        String levelStr = (level != null) ? "non-null" : "NULL";
        System.err.println("[dx12-debug] runTick frame=" + mc.clientTickCount
            + " gameLoadFinished=" + gameLoadFinished
            + " level=" + levelStr
            + " pause=" + mc.isPaused());
        System.err.flush();
    }
}
