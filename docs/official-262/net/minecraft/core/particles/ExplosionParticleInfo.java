/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.core.particles;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ExplosionParticleInfo(ParticleOptions particle, float scaling, float speed) {
    public static final MapCodec<ExplosionParticleInfo> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ParticleTypes.CODEC.fieldOf("particle").forGetter(ExplosionParticleInfo::particle), (App)Codec.FLOAT.optionalFieldOf("scaling", (Object)Float.valueOf(1.0f)).forGetter(ExplosionParticleInfo::scaling), (App)Codec.FLOAT.optionalFieldOf("speed", (Object)Float.valueOf(1.0f)).forGetter(ExplosionParticleInfo::speed)).apply((Applicative)i, ExplosionParticleInfo::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionParticleInfo> STREAM_CODEC = StreamCodec.composite(ParticleTypes.STREAM_CODEC, ExplosionParticleInfo::particle, ByteBufCodecs.FLOAT, ExplosionParticleInfo::scaling, ByteBufCodecs.FLOAT, ExplosionParticleInfo::speed, ExplosionParticleInfo::new);
}

