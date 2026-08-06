/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.clock;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;

public record ClockTimeMarker(Holder<WorldClock> clock, int ticks, Optional<Integer> periodTicks, boolean showInCommands) {
    public static final Codec<ResourceKey<ClockTimeMarker>> KEY_CODEC = ResourceKey.codec(ClockTimeMarkers.ROOT_ID);

    public long getRepetitionCount(long totalTicks) {
        if (this.periodTicks.isEmpty()) {
            return totalTicks >= (long)this.ticks ? 1L : 0L;
        }
        int periodTicks = this.periodTicks.get();
        return totalTicks / (long)periodTicks + (long)(totalTicks % (long)periodTicks >= (long)this.ticks ? 1 : 0);
    }

    public long resolveTimeToMoveTo(long totalTicks) {
        if (this.periodTicks.isEmpty()) {
            return this.ticks;
        }
        int periodTicks = this.periodTicks.get();
        return totalTicks + ClockTimeMarker.durationToNext(periodTicks, totalTicks % (long)periodTicks, this.ticks);
    }

    public boolean occursAt(long totalTicks) {
        if (this.periodTicks.isEmpty()) {
            return (long)this.ticks == totalTicks;
        }
        return (long)this.ticks == totalTicks % (long)this.periodTicks.get().intValue();
    }

    private static long durationToNext(int periodTicks, long from, long to) {
        long duration = to - from;
        return duration > 0L ? duration : (long)periodTicks + duration;
    }
}

