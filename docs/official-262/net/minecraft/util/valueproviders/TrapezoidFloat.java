/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.util.valueproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;

public record TrapezoidFloat(float min, float max, float plateau) implements FloatProvider
{
    public static final MapCodec<TrapezoidFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("min").forGetter(TrapezoidFloat::min), (App)Codec.FLOAT.fieldOf("max").forGetter(TrapezoidFloat::max), (App)Codec.FLOAT.fieldOf("plateau").forGetter(TrapezoidFloat::plateau)).apply((Applicative)i, TrapezoidFloat::new)).validate(c -> {
        if (c.max < c.min) {
            return DataResult.error(() -> "Max must be larger than min: [" + c.min + ", " + c.max + "]");
        }
        if (c.plateau > c.max - c.min) {
            return DataResult.error(() -> "Plateau can at most be the full span: [" + c.min + ", " + c.max + "]");
        }
        return DataResult.success((Object)c);
    });

    public static TrapezoidFloat of(float min, float max, float plateau) {
        return new TrapezoidFloat(min, max, plateau);
    }

    @Override
    public float sample(RandomSource random) {
        float range = this.max - this.min;
        float plateauStart = (range - this.plateau) / 2.0f;
        float plateauEnd = range - plateauStart;
        return this.min + random.nextFloat() * plateauEnd + random.nextFloat() * plateauStart;
    }

    public MapCodec<TrapezoidFloat> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return "trapezoid(" + this.plateau + ") in [" + this.min + "-" + this.max + "]";
    }
}

