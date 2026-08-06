/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class FramerateLimitTracker {
    private static final int OUT_OF_LEVEL_MENU_LIMIT = 60;
    private static final int ICONIFIED_WINDOW_LIMIT = 10;
    private static final int AFK_LIMIT = 30;
    private static final int LONG_AFK_LIMIT = 10;
    private static final long AFK_THRESHOLD_MS = 60000L;
    private static final long LONG_AFK_THRESHOLD_MS = 600000L;
    private final Options options;
    private final Minecraft minecraft;
    private int framerateLimit;
    private long latestInputTime;

    public FramerateLimitTracker(Options options, Minecraft minecraft) {
        this.options = options;
        this.minecraft = minecraft;
        this.framerateLimit = options.framerateLimit().get();
    }

    public int getFramerateLimit() {
        return switch (this.getThrottleReason().ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.framerateLimit;
            case 1 -> 10;
            case 2 -> 10;
            case 3 -> Math.min(this.framerateLimit, 30);
            case 4 -> 60;
        };
    }

    public FramerateThrottleReason getThrottleReason() {
        InactivityFpsLimit inactivityFpsLimit = this.options.inactivityFpsLimit().get();
        if (this.minecraft.getWindow().isIconified()) {
            return FramerateThrottleReason.WINDOW_ICONIFIED;
        }
        if (inactivityFpsLimit == InactivityFpsLimit.AFK) {
            long afkTimeMillis = Util.getMillis() - this.latestInputTime;
            if (afkTimeMillis > 600000L) {
                return FramerateThrottleReason.LONG_AFK;
            }
            if (afkTimeMillis > 60000L) {
                return FramerateThrottleReason.SHORT_AFK;
            }
        }
        if (this.minecraft.level == null && (this.minecraft.gui.screen() != null || this.minecraft.gui.overlay() != null)) {
            return FramerateThrottleReason.OUT_OF_LEVEL_MENU;
        }
        return FramerateThrottleReason.NONE;
    }

    public boolean isHeavilyThrottled() {
        FramerateThrottleReason reason = this.getThrottleReason();
        return reason == FramerateThrottleReason.WINDOW_ICONIFIED || reason == FramerateThrottleReason.LONG_AFK;
    }

    public void setFramerateLimit(int value) {
        this.framerateLimit = value;
    }

    public void onInputReceived() {
        this.latestInputTime = Util.getMillis();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum FramerateThrottleReason {
        NONE,
        WINDOW_ICONIFIED,
        LONG_AFK,
        SHORT_AFK,
        OUT_OF_LEVEL_MENU;

    }
}

