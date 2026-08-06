/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import com.mojang.blaze3d.systems.GpuSurface;
import java.lang.runtime.SwitchBootstraps;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugEntryFps
implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level serverOrClientLevel, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        Minecraft minecraft = Minecraft.getInstance();
        int framerateLimit = minecraft.getFramerateLimitTracker().getFramerateLimit();
        Optional<GpuSurface.Configuration> surfaceConfiguration = minecraft.windowSurface().currentConfiguration();
        displayer.addPriorityLine(String.format(Locale.ROOT, "%d fps T: %s%s", minecraft.getFps(), framerateLimit == 260 ? "inf" : Integer.valueOf(framerateLimit), DebugEntryFps.presentModeName(surfaceConfiguration.map(GpuSurface.Configuration::presentMode).orElse(null))));
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }

    private static String presentModeName(@Nullable GpuSurface.PresentMode mode) {
        GpuSurface.PresentMode presentMode = mode;
        int n = 0;
        return switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"IMMEDIATE", "MAILBOX", "FIFO", "FIFO_RELAXED"}, (GpuSurface.PresentMode)presentMode, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> " (immediate)";
            case 1 -> " (mailbox)";
            case 2 -> " (fifo)";
            case 3 -> " (fifo relaxed)";
            case -1 -> "";
        };
    }
}

