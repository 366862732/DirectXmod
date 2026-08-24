package com.dx12.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injected at Minecraft.runTick() HEAD to log level / gameLoadFinished state
 * each frame, so we can see when (or if) they ever become non-null/true.
 */
@Mixin(Minecraft.class)
public class MinecraftRunTickDebugMixin {

    @Inject(method = "runTick", at = @At("HEAD"), remap = false)
    private void dx12_runTickDebug(boolean advanceGameTime, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        java.lang.reflect.Field levelField, tickCountField, loadField, pendingField, serverField;
        Object level = null;
        long tickCount = 0L;
        boolean gameLoadFinished = false;
        Object pendingConnection = null;
        Object singleplayerServer = null;
        try {
            levelField = Minecraft.class.getDeclaredField("level");
            levelField.setAccessible(true);
            level = levelField.get(mc);
        } catch (Exception ignored) {}
        try {
            tickCountField = Minecraft.class.getDeclaredField("clientTickCount");
            tickCountField.setAccessible(true);
            tickCount = tickCountField.getLong(mc);
        } catch (Exception ignored) {}
        try {
            loadField = Minecraft.class.getDeclaredField("gameLoadFinished");
            loadField.setAccessible(true);
            gameLoadFinished = loadField.getBoolean(mc);
        } catch (Exception ignored) {}
        try {
            pendingField = Minecraft.class.getDeclaredField("pendingConnection");
            pendingField.setAccessible(true);
            pendingConnection = pendingField.get(mc);
        } catch (Exception ignored) {}
        try {
            serverField = Minecraft.class.getDeclaredField("singleplayerServer");
            serverField.setAccessible(true);
            singleplayerServer = serverField.get(mc);
        } catch (Exception ignored) {}
        String levelStr = (level != null) ? "non-null" : "NULL";
        System.err.println("[dx12-debug] runTick tick=" + tickCount
            + " gameLoadFinished=" + gameLoadFinished
            + " level=" + levelStr
            + " pause=" + mc.isPaused()
            + " pendingConn=" + (pendingConnection != null ? "present" : "null")
            + " singleplayerServer=" + (singleplayerServer != null ? "present" : "null"));
        System.err.flush();
    }
}

