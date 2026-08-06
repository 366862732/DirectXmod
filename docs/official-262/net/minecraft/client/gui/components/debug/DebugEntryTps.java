/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.Connection;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryTps
implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        String tps;
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        ClientPacketListener connectionListener = minecraft.getConnection();
        if (connectionListener == null || serverOrClientLevel == null) {
            return;
        }
        Connection connection = connectionListener.getConnection();
        float averageSentPackets = connection.getAverageSentPackets();
        float averageReceivedPackets = connection.getAverageReceivedPackets();
        TickRateManager tickRateManager = serverOrClientLevel.tickRateManager();
        String runStatus = tickRateManager.isSteppingForward() ? " (frozen - stepping)" : (tickRateManager.isFrozen() ? " (frozen)" : "");
        if (server != null) {
            ServerTickRateManager serverTickRateManager = server.tickRateManager();
            boolean isSpriting = serverTickRateManager.isSprinting();
            if (isSpriting) {
                runStatus = " (sprinting)";
            }
            String tpsTarget = isSpriting ? "-" : String.format(Locale.ROOT, "%.1f", Float.valueOf(tickRateManager.millisecondsPerTick()));
            tps = String.format(Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", Float.valueOf(server.getCurrentSmoothedTickTime()), tpsTarget, runStatus, Float.valueOf(averageSentPackets), Float.valueOf(averageReceivedPackets));
        } else {
            tps = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", connectionListener.serverBrand(), runStatus, Float.valueOf(averageSentPackets), Float.valueOf(averageReceivedPackets));
        }
        displayer.addLine(tps);
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }
}

