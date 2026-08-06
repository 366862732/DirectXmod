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
package net.minecraft.util.valueproviders;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;

public record ConstantFloat(float value) implements FloatProvider
{
    public static final ConstantFloat ZERO = new ConstantFloat(0.0f);
    public static final MapCodec<ConstantFloat> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.FLOAT.fieldOf("value").forGetter(ConstantFloat::value)).apply((Applicative)i, ConstantFloat::of));

    public static ConstantFloat of(float value) {
        if (value == 0.0f) {
            return ZERO;
        }
        return new ConstantFloat(value);
    }

    @Override
    public float sample(RandomSource random) {
        return this.value;
    }

    @Override
    public float min() {
        return this.value;
    }

    @Override
    public float max() {
        return this.value;
    }

    public MapCodec<ConstantFloat> codec() {
        return MAP_CODEC;
    }

    @Override
    public String toString() {
        return Float.toString(this.value);
    }
}

