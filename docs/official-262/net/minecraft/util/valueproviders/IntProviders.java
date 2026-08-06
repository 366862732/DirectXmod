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
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ClampedInt;
import net.minecraft.util.valueproviders.ClampedNormalInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.TrapezoidInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;

public class IntProviders {
    private static final Codec<Either<Integer, IntProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either((Codec)Codec.INT, (Codec)BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().dispatch(IntProvider::codec, t -> t));
    public static final Codec<IntProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(either -> (IntProvider)either.map(ConstantInt::of, f -> f), f -> {
        Either either;
        if (f instanceof ConstantInt) {
            ConstantInt constantInt = (ConstantInt)f;
            either = Either.left((Object)constantInt.value());
        } else {
            either = Either.right((Object)f);
        }
        return either;
    });
    public static final Codec<IntProvider> NON_NEGATIVE_CODEC = IntProviders.codec(0, Integer.MAX_VALUE);
    public static final Codec<IntProvider> POSITIVE_CODEC = IntProviders.codec(1, Integer.MAX_VALUE);

    public static Codec<IntProvider> codec(int minValue, int maxValue) {
        return IntProviders.validateCodec(minValue, maxValue, CODEC);
    }

    public static <T extends IntProvider> Codec<T> validateCodec(int minValue, int maxValue, Codec<T> codec) {
        return codec.validate(value -> IntProviders.validate(minValue, maxValue, value));
    }

    private static <T extends IntProvider> DataResult<T> validate(int minValue, int maxValue, T value) {
        if (value.minInclusive() < minValue) {
            return DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.minInclusive() + "-" + value.maxInclusive() + "]");
        }
        if (value.maxInclusive() > maxValue) {
            return DataResult.error(() -> "Value provider too high: " + maxValue + " [" + value.minInclusive() + "-" + value.maxInclusive() + "]");
        }
        return DataResult.success(value);
    }

    public static MapCodec<? extends IntProvider> bootstrap(Registry<MapCodec<? extends IntProvider>> registry) {
        Registry.register(registry, "constant", ConstantInt.MAP_CODEC);
        Registry.register(registry, "uniform", UniformInt.MAP_CODEC);
        Registry.register(registry, "biased_to_bottom", BiasedToBottomInt.MAP_CODEC);
        Registry.register(registry, "clamped", ClampedInt.MAP_CODEC);
        Registry.register(registry, "weighted_list", WeightedListInt.MAP_CODEC);
        Registry.register(registry, "clamped_normal", ClampedNormalInt.MAP_CODEC);
        return Registry.register(registry, "trapezoid", TrapezoidInt.MAP_CODEC);
    }
}

