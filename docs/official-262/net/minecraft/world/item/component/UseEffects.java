/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record UseEffects(boolean canSprint, boolean interactVibrations, float speedMultiplier) {
    public static final UseEffects DEFAULT = new UseEffects(false, true, 0.2f);
    public static final Codec<UseEffects> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("can_sprint", (Object)UseEffects.DEFAULT.canSprint).forGetter(UseEffects::canSprint), (App)Codec.BOOL.optionalFieldOf("interact_vibrations", (Object)UseEffects.DEFAULT.interactVibrations).forGetter(UseEffects::interactVibrations), (App)Codec.floatRange((float)0.0f, (float)1.0f).optionalFieldOf("speed_multiplier", (Object)Float.valueOf(UseEffects.DEFAULT.speedMultiplier)).forGetter(UseEffects::speedMultiplier)).apply((Applicative)i, UseEffects::new));
    public static final StreamCodec<ByteBuf, UseEffects> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, UseEffects::canSprint, ByteBufCodecs.BOOL, UseEffects::interactVibrations, ByteBufCodecs.FLOAT, UseEffects::speedMultiplier, UseEffects::new);
}

