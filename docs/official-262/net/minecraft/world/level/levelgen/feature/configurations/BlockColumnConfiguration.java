/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record BlockColumnConfiguration(List<Layer> layers, Direction direction, BlockPredicate allowedPlacement, boolean prioritizeTip) implements FeatureConfiguration
{
    public static final Codec<BlockColumnConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Layer.CODEC.listOf().fieldOf("layers").forGetter(BlockColumnConfiguration::layers), (App)Direction.CODEC.fieldOf("direction").forGetter(BlockColumnConfiguration::direction), (App)BlockPredicate.CODEC.fieldOf("allowed_placement").forGetter(BlockColumnConfiguration::allowedPlacement), (App)Codec.BOOL.fieldOf("prioritize_tip").forGetter(BlockColumnConfiguration::prioritizeTip)).apply((Applicative)i, BlockColumnConfiguration::new));

    public static Layer layer(IntProvider height, BlockStateProvider state) {
        return new Layer(height, state);
    }

    public static BlockColumnConfiguration simple(IntProvider height, BlockStateProvider state) {
        return new BlockColumnConfiguration(List.of(BlockColumnConfiguration.layer(height, state)), Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false);
    }

    public record Layer(IntProvider height, BlockStateProvider state) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group((App)IntProviders.NON_NEGATIVE_CODEC.fieldOf("height").forGetter(Layer::height), (App)BlockStateProvider.CODEC.fieldOf("provider").forGetter(Layer::state)).apply((Applicative)i, Layer::new));
    }
}

