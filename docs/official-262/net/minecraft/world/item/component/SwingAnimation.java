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
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.SwingAnimationType;

public record SwingAnimation(SwingAnimationType type, int duration) {
    public static final SwingAnimation DEFAULT = new SwingAnimation(SwingAnimationType.WHACK, 6);
    public static final Codec<SwingAnimation> CODEC = RecordCodecBuilder.create(i -> i.group((App)SwingAnimationType.CODEC.optionalFieldOf("type", (Object)SwingAnimation.DEFAULT.type).forGetter(SwingAnimation::type), (App)ExtraCodecs.POSITIVE_INT.optionalFieldOf("duration", (Object)SwingAnimation.DEFAULT.duration).forGetter(SwingAnimation::duration)).apply((Applicative)i, SwingAnimation::new));
    public static final StreamCodec<ByteBuf, SwingAnimation> STREAM_CODEC = StreamCodec.composite(SwingAnimationType.STREAM_CODEC, SwingAnimation::type, ByteBufCodecs.VAR_INT, SwingAnimation::duration, SwingAnimation::new);
}

