/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PowerParticleOption
implements ParticleOptions {
    private final ParticleType<PowerParticleOption> type;
    private final float power;

    public static MapCodec<PowerParticleOption> codec(ParticleType<PowerParticleOption> type) {
        return Codec.FLOAT.xmap(power -> new PowerParticleOption(type, power.floatValue()), o -> Float.valueOf(o.power)).optionalFieldOf("power", (Object)PowerParticleOption.create(type, 1.0f));
    }

    public static StreamCodec<? super ByteBuf, PowerParticleOption> streamCodec(ParticleType<PowerParticleOption> type) {
        return ByteBufCodecs.FLOAT.map(color -> new PowerParticleOption(type, color.floatValue()), o -> Float.valueOf(o.power));
    }

    private PowerParticleOption(ParticleType<PowerParticleOption> type, float power) {
        this.type = type;
        this.power = power;
    }

    public ParticleType<PowerParticleOption> getType() {
        return this.type;
    }

    public float getPower() {
        return this.power;
    }

    public static PowerParticleOption create(ParticleType<PowerParticleOption> type, float power) {
        return new PowerParticleOption(type, power);
    }
}

