/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.clock;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.WorldClock;

public record PackedClockStates(Map<Holder<WorldClock>, ClockState> clocks) {
    public static final PackedClockStates EMPTY = new PackedClockStates(Map.of());
    public static final Codec<PackedClockStates> CODEC = Codec.unboundedMap(WorldClock.CODEC, ClockState.CODEC).xmap(PackedClockStates::new, PackedClockStates::clocks);
}

