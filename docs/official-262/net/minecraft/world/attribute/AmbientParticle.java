/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.attribute;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

public record AmbientParticle(ParticleOptions particle, float probability) {
    public static final Codec<AmbientParticle> CODEC = RecordCodecBuilder.create(i -> i.group((App)ParticleTypes.CODEC.fieldOf("particle").forGetter(s -> s.particle), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("probability").forGetter(s -> Float.valueOf(s.probability))).apply((Applicative)i, AmbientParticle::new));

    public boolean canSpawn(RandomSource random) {
        return random.nextFloat() <= this.probability;
    }

    public static List<AmbientParticle> of(ParticleOptions particle, float probability) {
        return List.of(new AmbientParticle(particle, probability));
    }
}

