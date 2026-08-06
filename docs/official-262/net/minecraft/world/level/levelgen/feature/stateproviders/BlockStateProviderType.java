/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RotatedBlockProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;

public class BlockStateProviderType<P extends BlockStateProvider> {
    public static final BlockStateProviderType<SimpleStateProvider> SIMPLE_STATE_PROVIDER = BlockStateProviderType.register("simple_state_provider", SimpleStateProvider.CODEC);
    public static final BlockStateProviderType<WeightedStateProvider> WEIGHTED_STATE_PROVIDER = BlockStateProviderType.register("weighted_state_provider", WeightedStateProvider.CODEC);
    public static final BlockStateProviderType<NoiseThresholdProvider> NOISE_THRESHOLD_PROVIDER = BlockStateProviderType.register("noise_threshold_provider", NoiseThresholdProvider.CODEC);
    public static final BlockStateProviderType<NoiseProvider> NOISE_PROVIDER = BlockStateProviderType.register("noise_provider", NoiseProvider.CODEC);
    public static final BlockStateProviderType<DualNoiseProvider> DUAL_NOISE_PROVIDER = BlockStateProviderType.register("dual_noise_provider", DualNoiseProvider.CODEC);
    public static final BlockStateProviderType<RotatedBlockProvider> ROTATED_BLOCK_PROVIDER = BlockStateProviderType.register("rotated_block_provider", RotatedBlockProvider.CODEC);
    public static final BlockStateProviderType<RandomizedIntStateProvider> RANDOMIZED_INT_STATE_PROVIDER = BlockStateProviderType.register("randomized_int_state_provider", RandomizedIntStateProvider.CODEC);
    public static final BlockStateProviderType<RuleBasedStateProvider> RULE_BASED_STATE_PROVIDER = BlockStateProviderType.register("rule_based_state_provider", RuleBasedStateProvider.CODEC);
    private final MapCodec<P> codec;

    private static <P extends BlockStateProvider> BlockStateProviderType<P> register(String name, MapCodec<P> codec) {
        return Registry.register(BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE, name, new BlockStateProviderType<P>(codec));
    }

    private BlockStateProviderType(MapCodec<P> codec) {
        this.codec = codec;
    }

    public MapCodec<P> codec() {
        return this.codec;
    }
}

