/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.EnchantmentLevelProvider;
import net.minecraft.world.level.storage.loot.providers.number.EnvironmentAttributeValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.ScoreboardValue;
import net.minecraft.world.level.storage.loot.providers.number.StorageValue;
import net.minecraft.world.level.storage.loot.providers.number.Sum;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class NumberProviders {
    private static final Codec<NumberProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE.byNameCodec().dispatch(NumberProvider::codec, c -> c);
    public static final Codec<NumberProvider> CODEC = Codec.lazyInitialized(() -> {
        Codec typedCodecWithFallback = Codec.withAlternative(TYPED_CODEC, (Codec)UniformGenerator.MAP_CODEC.codec());
        return Codec.either(ConstantValue.INLINE_CODEC, (Codec)typedCodecWithFallback).xmap(Either::unwrap, provider -> {
            Either either;
            if (provider instanceof ConstantValue) {
                ConstantValue constant = (ConstantValue)provider;
                either = Either.left((Object)constant);
            } else {
                either = Either.right((Object)provider);
            }
            return either;
        });
    });

    public static MapCodec<? extends NumberProvider> bootstrap(Registry<MapCodec<? extends NumberProvider>> registry) {
        Registry.register(registry, "constant", ConstantValue.MAP_CODEC);
        Registry.register(registry, "uniform", UniformGenerator.MAP_CODEC);
        Registry.register(registry, "binomial", BinomialDistributionGenerator.MAP_CODEC);
        Registry.register(registry, "score", ScoreboardValue.MAP_CODEC);
        Registry.register(registry, "storage", StorageValue.MAP_CODEC);
        Registry.register(registry, "sum", Sum.MAP_CODEC);
        Registry.register(registry, "enchantment_level", EnchantmentLevelProvider.MAP_CODEC);
        return Registry.register(registry, "environment_attribute", EnvironmentAttributeValue.MAP_CODEC);
    }
}

