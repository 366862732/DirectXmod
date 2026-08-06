/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.clock;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

public record ClockState(long totalTicks, float partialTick, float rate, boolean paused) {
    public static final Codec<ClockState> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.LONG.fieldOf("total_ticks").forGetter(ClockState::totalTicks), (App)Codec.FLOAT.optionalFieldOf("partial_tick", (Object)Float.valueOf(0.0f)).forGetter(ClockState::partialTick), (App)ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("rate", (Object)Float.valueOf(1.0f)).forGetter(ClockState::rate), (App)Codec.BOOL.optionalFieldOf("paused", (Object)false).forGetter(ClockState::paused)).apply((Applicative)i, ClockState::new));
}

