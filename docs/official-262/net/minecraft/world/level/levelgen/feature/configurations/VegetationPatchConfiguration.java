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
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record VegetationPatchConfiguration(HolderSet<Block> replaceable, BlockStateProvider groundState, Holder<PlacedFeature> vegetationFeature, CaveSurface surface, IntProvider depth, float extraBottomBlockChance, int verticalRange, float vegetationChance, IntProvider xzRadius, float extraEdgeColumnChance) implements FeatureConfiguration
{
    public static final Codec<VegetationPatchConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(VegetationPatchConfiguration::replaceable), (App)BlockStateProvider.CODEC.fieldOf("ground_state").forGetter(VegetationPatchConfiguration::groundState), (App)PlacedFeature.CODEC.fieldOf("vegetation_feature").forGetter(VegetationPatchConfiguration::vegetationFeature), (App)CaveSurface.CODEC.fieldOf("surface").forGetter(VegetationPatchConfiguration::surface), (App)IntProviders.codec(1, 128).fieldOf("depth").forGetter(VegetationPatchConfiguration::depth), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("extra_bottom_block_chance").forGetter(VegetationPatchConfiguration::extraBottomBlockChance), (App)Codec.intRange((int)1, (int)256).fieldOf("vertical_range").forGetter(VegetationPatchConfiguration::verticalRange), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("vegetation_chance").forGetter(VegetationPatchConfiguration::vegetationChance), (App)IntProviders.CODEC.fieldOf("xz_radius").forGetter(VegetationPatchConfiguration::xzRadius), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("extra_edge_column_chance").forGetter(VegetationPatchConfiguration::extraEdgeColumnChance)).apply((Applicative)i, VegetationPatchConfiguration::new));
}

