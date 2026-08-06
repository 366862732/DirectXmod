/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.core.particles;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public record TrailParticleOption(Vec3 target, int color, int duration) implements ParticleOptions
{
    public static final MapCodec<TrailParticleOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Vec3.CODEC.fieldOf("target").forGetter(TrailParticleOption::target), (App)ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(TrailParticleOption::color), (App)ExtraCodecs.POSITIVE_INT.fieldOf("duration").forGetter(TrailParticleOption::duration)).apply((Applicative)i, TrailParticleOption::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrailParticleOption> STREAM_CODEC = StreamCodec.composite(Vec3.STREAM_CODEC, TrailParticleOption::target, ByteBufCodecs.INT, TrailParticleOption::color, ByteBufCodecs.VAR_INT, TrailParticleOption::duration, TrailParticleOption::new);

    public ParticleType<TrailParticleOption> getType() {
        return ParticleTypes.TRAIL;
    }
}

