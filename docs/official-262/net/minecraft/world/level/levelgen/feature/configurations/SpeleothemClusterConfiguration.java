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
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record SpeleothemClusterConfiguration(BlockState baseBlock, BlockState pointedBlock, HolderSet<Block> replaceableBlocks, int floorToCeilingSearchRange, IntProvider height, IntProvider radius, int maxStalagmiteStalactiteHeightDiff, int heightDeviation, IntProvider speleothemBlockLayerThickness, FloatProvider density, FloatProvider wetness, float chanceOfSpeleothemAtMaxDistanceFromCenter, int maxDistanceFromEdgeAffectingChanceOfSpeleothem, int maxDistanceFromCenterAffectingHeightBias) implements FeatureConfiguration
{
    public static final Codec<SpeleothemClusterConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockState.CODEC.fieldOf("base_block").forGetter(c -> c.baseBlock), (App)BlockState.CODEC.fieldOf("pointed_block").forGetter(c -> c.pointedBlock), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks), (App)Codec.intRange((int)1, (int)512).fieldOf("floor_to_ceiling_search_range").forGetter(c -> c.floorToCeilingSearchRange), (App)IntProviders.codec(1, 128).fieldOf("height").forGetter(c -> c.height), (App)IntProviders.codec(1, 128).fieldOf("radius").forGetter(c -> c.radius), (App)Codec.intRange((int)0, (int)64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter(c -> c.maxStalagmiteStalactiteHeightDiff), (App)Codec.intRange((int)1, (int)64).fieldOf("height_deviation").forGetter(c -> c.heightDeviation), (App)IntProviders.codec(0, 128).fieldOf("speleothem_block_layer_thickness").forGetter(c -> c.speleothemBlockLayerThickness), (App)FloatProviders.codec(0.0f, 2.0f).fieldOf("density").forGetter(c -> c.density), (App)FloatProviders.codec(0.0f, 2.0f).fieldOf("wetness").forGetter(c -> c.wetness), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("chance_of_speleothem_at_max_distance_from_center").forGetter(c -> Float.valueOf(c.chanceOfSpeleothemAtMaxDistanceFromCenter)), (App)Codec.intRange((int)1, (int)64).fieldOf("max_distance_from_edge_affecting_chance_of_speleothem").forGetter(c -> c.maxDistanceFromEdgeAffectingChanceOfSpeleothem), (App)Codec.intRange((int)1, (int)64).fieldOf("max_distance_from_center_affecting_height_bias").forGetter(c -> c.maxDistanceFromCenterAffectingHeightBias)).apply((Applicative)i, SpeleothemClusterConfiguration::new));
}

