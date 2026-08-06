/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core.particles;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record GeyserParticleOptions(ParticleType<GeyserParticleOptions> type, int waterBlocks) implements ParticleOptions
{
    public static MapCodec<GeyserParticleOptions> codec(ParticleType<GeyserParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.fieldOf("water_blocks").forGetter(o -> o.waterBlocks)).apply((Applicative)i, waterBlocks -> new GeyserParticleOptions(type, (int)waterBlocks)));
    }

    public static StreamCodec<? super ByteBuf, GeyserParticleOptions> streamCodec(ParticleType<GeyserParticleOptions> type) {
        return StreamCodec.composite(ByteBufCodecs.INT, o -> o.waterBlocks, waterBlocks -> new GeyserParticleOptions(type, (int)waterBlocks));
    }

    public ParticleType<GeyserParticleOptions> getType() {
        return this.type;
    }
}

