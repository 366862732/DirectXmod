package com.xgdt.dx11.mixin;

import com.xgdt.dx11.perf.PerfCommand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Handles {@code /dx11perf} locally so the command works in single-player and
 * on servers without a server-side command.  The injection is at the head of
 * {@code sendCommand}; unknown commands continue to the server unchanged.
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class PerfCommandMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void dx11$handlePerfCommand(String command, CallbackInfo ci) {
        if (!PerfCommand.isPerfCommand(command)) {
            return;
        }

        String response = PerfCommand.execute(command);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            for (String line : response.split("\n")) {
                minecraft.player.displayClientMessage(Component.literal(line), false);
            }
        } else {
            LOGGER.info("[dx11-perf] {}", response.replace('\n', ' '));
        }
        if (PerfCommand.requiresChunkRebuild(command)
            && minecraft.level != null
            && minecraft.levelRenderer != null) {
            minecraft.levelRenderer.allChanged();
        }
        ci.cancel();
    }
}
