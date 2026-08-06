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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record RootSystemConfiguration(Holder<PlacedFeature> treeFeature, int requiredVerticalSpaceForTree, int levelTestDistance, int maxLevelDeviation, int rootRadius, HolderSet<Block> rootReplaceable, BlockStateProvider rootStateProvider, int rootPlacementAttempts, int rootColumnMaxHeight, int hangingRootRadius, int hangingRootsVerticalSpan, BlockStateProvider hangingRootStateProvider, int hangingRootPlacementAttempts, int allowedVerticalWaterForTree, BlockPredicate allowedTreePosition) implements FeatureConfiguration
{
    public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)PlacedFeature.CODEC.fieldOf("feature").forGetter(RootSystemConfiguration::treeFeature), (App)Codec.intRange((int)1, (int)64).fieldOf("required_vertical_space_for_tree").forGetter(RootSystemConfiguration::requiredVerticalSpaceForTree), (App)Codec.intRange((int)0, (int)16).fieldOf("level_test_distance").forGetter(RootSystemConfiguration::levelTestDistance), (App)Codec.intRange((int)0, (int)64).fieldOf("max_level_deviation").forGetter(RootSystemConfiguration::maxLevelDeviation), (App)Codec.intRange((int)1, (int)64).fieldOf("root_radius").forGetter(RootSystemConfiguration::rootRadius), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("root_replaceable").forGetter(RootSystemConfiguration::rootReplaceable), (App)BlockStateProvider.CODEC.fieldOf("root_state_provider").forGetter(RootSystemConfiguration::rootStateProvider), (App)Codec.intRange((int)1, (int)256).fieldOf("root_placement_attempts").forGetter(RootSystemConfiguration::rootPlacementAttempts), (App)Codec.intRange((int)1, (int)4096).fieldOf("root_column_max_height").forGetter(RootSystemConfiguration::rootColumnMaxHeight), (App)Codec.intRange((int)1, (int)64).fieldOf("hanging_root_radius").forGetter(RootSystemConfiguration::hangingRootRadius), (App)Codec.intRange((int)1, (int)16).fieldOf("hanging_roots_vertical_span").forGetter(RootSystemConfiguration::hangingRootsVerticalSpan), (App)BlockStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter(RootSystemConfiguration::hangingRootStateProvider), (App)Codec.intRange((int)1, (int)256).fieldOf("hanging_root_placement_attempts").forGetter(RootSystemConfiguration::hangingRootPlacementAttempts), (App)Codec.intRange((int)1, (int)64).fieldOf("allowed_vertical_water_for_tree").forGetter(RootSystemConfiguration::allowedVerticalWaterForTree), (App)BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter(RootSystemConfiguration::allowedTreePosition)).apply((Applicative)i, RootSystemConfiguration::new));
}

