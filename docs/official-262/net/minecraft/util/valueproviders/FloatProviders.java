/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.TrapezoidFloat;
import net.minecraft.util.valueproviders.UniformFloat;

public class FloatProviders {
    private static final Codec<Either<Float, FloatProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either((Codec)Codec.FLOAT, (Codec)BuiltInRegistries.FLOAT_PROVIDER_TYPE.byNameCodec().dispatch(FloatProvider::codec, t -> t));
    public static final Codec<FloatProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(either -> (FloatProvider)either.map(ConstantFloat::of, f -> f), f -> {
        Either either;
        if (f instanceof ConstantFloat) {
            ConstantFloat constantFloat = (ConstantFloat)f;
            either = Either.left((Object)Float.valueOf(constantFloat.value()));
        } else {
            either = Either.right((Object)f);
        }
        return either;
    });

    public static Codec<FloatProvider> codec(float minValue, float maxValue) {
        return CODEC.validate(value -> {
            if (value.min() < minValue) {
                return DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.min() + "-" + value.max() + "]");
            }
            if (value.max() > maxValue) {
                return DataResult.error(() -> "Value provider too high: " + maxValue + " [" + value.min() + "-" + value.max() + "]");
            }
            return DataResult.success((Object)value);
        });
    }

    public static Codec<FloatProvider> codec(float minValue) {
        return CODEC.validate(value -> {
            if (value.min() < minValue) {
                return DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.min() + "-" + value.max() + "]");
            }
            return DataResult.success((Object)value);
        });
    }

    public static MapCodec<? extends FloatProvider> bootstrap(Registry<MapCodec<? extends FloatProvider>> registry) {
        Registry.register(registry, "constant", ConstantFloat.MAP_CODEC);
        Registry.register(registry, "uniform", UniformFloat.MAP_CODEC);
        Registry.register(registry, "clamped_normal", ClampedNormalFloat.MAP_CODEC);
        return Registry.register(registry, "trapezoid", TrapezoidFloat.MAP_CODEC);
    }
}

