/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core.particles;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record GeyserBaseParticleOptions(ParticleType<GeyserBaseParticleOptions> type, int waterBlocks, float burstImpulseBase) implements ParticleOptions
{
    public static MapCodec<GeyserBaseParticleOptions> codec(ParticleType<GeyserBaseParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.fieldOf("water_blocks").forGetter(o -> o.waterBlocks), (App)Codec.FLOAT.fieldOf("burst_impulse_base").forGetter(o -> Float.valueOf(o.burstImpulseBase))).apply((Applicative)i, (waterBlocks, burstImpulseBase) -> new GeyserBaseParticleOptions(type, (int)waterBlocks, burstImpulseBase.floatValue())));
    }

    public static StreamCodec<? super ByteBuf, GeyserBaseParticleOptions> streamCodec(ParticleType<GeyserBaseParticleOptions> type) {
        return StreamCodec.composite(ByteBufCodecs.INT, o -> o.waterBlocks, ByteBufCodecs.FLOAT, o -> Float.valueOf(o.burstImpulseBase), (waterBlocks, burstImpulseBase) -> new GeyserBaseParticleOptions(type, (int)waterBlocks, burstImpulseBase.floatValue()));
    }

    public ParticleType<GeyserBaseParticleOptions> getType() {
        return this.type;
    }
}

