/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

public class OrePlacements {
    public static final ResourceKey<PlacedFeature> ORE_MAGMA = PlacementUtils.createKey("ore_magma");
    public static final ResourceKey<PlacedFeature> ORE_SOUL_SAND = PlacementUtils.createKey("ore_soul_sand");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_DELTAS = PlacementUtils.createKey("ore_gold_deltas");
    public static final ResourceKey<PlacedFeature> ORE_QUARTZ_DELTAS = PlacementUtils.createKey("ore_quartz_deltas");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_NETHER = PlacementUtils.createKey("ore_gold_nether");
    public static final ResourceKey<PlacedFeature> ORE_QUARTZ_NETHER = PlacementUtils.createKey("ore_quartz_nether");
    public static final ResourceKey<PlacedFeature> ORE_GRAVEL_NETHER = PlacementUtils.createKey("ore_gravel_nether");
    public static final ResourceKey<PlacedFeature> ORE_BLACKSTONE = PlacementUtils.createKey("ore_blackstone");
    public static final ResourceKey<PlacedFeature> ORE_DIRT = PlacementUtils.createKey("ore_dirt");
    public static final ResourceKey<PlacedFeature> ORE_GRAVEL = PlacementUtils.createKey("ore_gravel");
    public static final ResourceKey<PlacedFeature> ORE_GRANITE_UPPER = PlacementUtils.createKey("ore_granite_upper");
    public static final ResourceKey<PlacedFeature> ORE_GRANITE_LOWER = PlacementUtils.createKey("ore_granite_lower");
    public static final ResourceKey<PlacedFeature> ORE_DIORITE_UPPER = PlacementUtils.createKey("ore_diorite_upper");
    public static final ResourceKey<PlacedFeature> ORE_DIORITE_LOWER = PlacementUtils.createKey("ore_diorite_lower");
    public static final ResourceKey<PlacedFeature> ORE_ANDESITE_UPPER = PlacementUtils.createKey("ore_andesite_upper");
    public static final ResourceKey<PlacedFeature> ORE_ANDESITE_LOWER = PlacementUtils.createKey("ore_andesite_lower");
    public static final ResourceKey<PlacedFeature> ORE_TUFF = PlacementUtils.createKey("ore_tuff");
    public static final ResourceKey<PlacedFeature> ORE_COAL_UPPER = PlacementUtils.createKey("ore_coal_upper");
    public static final ResourceKey<PlacedFeature> ORE_COAL_LOWER = PlacementUtils.createKey("ore_coal_lower");
    public static final ResourceKey<PlacedFeature> ORE_IRON_UPPER = PlacementUtils.createKey("ore_iron_upper");
    public static final ResourceKey<PlacedFeature> ORE_IRON_MIDDLE = PlacementUtils.createKey("ore_iron_middle");
    public static final ResourceKey<PlacedFeature> ORE_IRON_SMALL = PlacementUtils.createKey("ore_iron_small");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_EXTRA = PlacementUtils.createKey("ore_gold_extra");
    public static final ResourceKey<PlacedFeature> ORE_GOLD = PlacementUtils.createKey("ore_gold");
    public static final ResourceKey<PlacedFeature> ORE_GOLD_LOWER = PlacementUtils.createKey("ore_gold_lower");
    public static final ResourceKey<PlacedFeature> ORE_REDSTONE = PlacementUtils.createKey("ore_redstone");
    public static final ResourceKey<PlacedFeature> ORE_REDSTONE_LOWER = PlacementUtils.createKey("ore_redstone_lower");
    public static final ResourceKey<PlacedFeature> ORE_DIAMOND = PlacementUtils.createKey("ore_diamond");
    public static final ResourceKey<PlacedFeature> ORE_DIAMOND_MEDIUM = PlacementUtils.createKey("ore_diamond_medium");
    public static final ResourceKey<PlacedFeature> ORE_DIAMOND_LARGE = PlacementUtils.createKey("ore_diamond_large");
    public static final ResourceKey<PlacedFeature> ORE_DIAMOND_BURIED = PlacementUtils.createKey("ore_diamond_buried");
    public static final ResourceKey<PlacedFeature> ORE_LAPIS = PlacementUtils.createKey("ore_lapis");
    public static final ResourceKey<PlacedFeature> ORE_LAPIS_BURIED = PlacementUtils.createKey("ore_lapis_buried");
    public static final ResourceKey<PlacedFeature> ORE_INFESTED = PlacementUtils.createKey("ore_infested");
    public static final ResourceKey<PlacedFeature> ORE_EMERALD = PlacementUtils.createKey("ore_emerald");
    public static final ResourceKey<PlacedFeature> ORE_ANCIENT_DEBRIS_LARGE = PlacementUtils.createKey("ore_ancient_debris_large");
    public static final ResourceKey<PlacedFeature> ORE_ANCIENT_DEBRIS_SMALL = PlacementUtils.createKey("ore_debris_small");
    public static final ResourceKey<PlacedFeature> ORE_COPPER = PlacementUtils.createKey("ore_copper");
    public static final ResourceKey<PlacedFeature> ORE_COPPER_LARGE = PlacementUtils.createKey("ore_copper_large");
    public static final ResourceKey<PlacedFeature> ORE_CLAY = PlacementUtils.createKey("ore_clay");

    private static List<PlacementModifier> orePlacement(PlacementModifier frequencyModifier, PlacementModifier heightRange) {
        return List.of(frequencyModifier, InSquarePlacement.spread(), heightRange, BiomeFilter.biome());
    }

    private static List<PlacementModifier> commonOrePlacement(int count, PlacementModifier heightRange) {
        return OrePlacements.orePlacement(CountPlacement.of(count), heightRange);
    }

    private static List<PlacementModifier> rareOrePlacement(int rarity, PlacementModifier heightRange) {
        return OrePlacements.orePlacement(RarityFilter.onAverageOnceEvery(rarity), heightRange);
    }

    public static void bootstrap(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreMagma = configuredFeatures.getOrThrow(OreFeatures.ORE_MAGMA);
        Holder.Reference<ConfiguredFeature<?, ?>> oreSoulSand = configuredFeatures.getOrThrow(OreFeatures.ORE_SOUL_SAND);
        Holder.Reference<ConfiguredFeature<?, ?>> oreNetherGold = configuredFeatures.getOrThrow(OreFeatures.ORE_NETHER_GOLD);
        Holder.Reference<ConfiguredFeature<?, ?>> oreQuartz = configuredFeatures.getOrThrow(OreFeatures.ORE_QUARTZ);
        Holder.Reference<ConfiguredFeature<?, ?>> oreGravelNether = configuredFeatures.getOrThrow(OreFeatures.ORE_GRAVEL_NETHER);
        Holder.Reference<ConfiguredFeature<?, ?>> oreBlackstone = configuredFeatures.getOrThrow(OreFeatures.ORE_BLACKSTONE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDirt = configuredFeatures.getOrThrow(OreFeatures.ORE_DIRT);
        Holder.Reference<ConfiguredFeature<?, ?>> oreGravel = configuredFeatures.getOrThrow(OreFeatures.ORE_GRAVEL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreGranite = configuredFeatures.getOrThrow(OreFeatures.ORE_GRANITE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDiorite = configuredFeatures.getOrThrow(OreFeatures.ORE_DIORITE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreAndesite = configuredFeatures.getOrThrow(OreFeatures.ORE_ANDESITE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreTuff = configuredFeatures.getOrThrow(OreFeatures.ORE_TUFF);
        Holder.Reference<ConfiguredFeature<?, ?>> oreCoal = configuredFeatures.getOrThrow(OreFeatures.ORE_COAL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreCoalBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_COAL_BURIED);
        Holder.Reference<ConfiguredFeature<?, ?>> oreIron = configuredFeatures.getOrThrow(OreFeatures.ORE_IRON);
        Holder.Reference<ConfiguredFeature<?, ?>> oreIronSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_IRON_SMALL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreGold = configuredFeatures.getOrThrow(OreFeatures.ORE_GOLD);
        Holder.Reference<ConfiguredFeature<?, ?>> oreGoldBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_GOLD_BURIED);
        Holder.Reference<ConfiguredFeature<?, ?>> oreRedstone = configuredFeatures.getOrThrow(OreFeatures.ORE_REDSTONE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDiamondSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_SMALL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDiamondMedium = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_MEDIUM);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDiamondLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_LARGE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreDiamondBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_DIAMOND_BURIED);
        Holder.Reference<ConfiguredFeature<?, ?>> oreLapis = configuredFeatures.getOrThrow(OreFeatures.ORE_LAPIS);
        Holder.Reference<ConfiguredFeature<?, ?>> oreLapisBuried = configuredFeatures.getOrThrow(OreFeatures.ORE_LAPIS_BURIED);
        Holder.Reference<ConfiguredFeature<?, ?>> oreInfested = configuredFeatures.getOrThrow(OreFeatures.ORE_INFESTED);
        Holder.Reference<ConfiguredFeature<?, ?>> oreEmerald = configuredFeatures.getOrThrow(OreFeatures.ORE_EMERALD);
        Holder.Reference<ConfiguredFeature<?, ?>> oreAncientDebrisLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_LARGE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreAncientDebrisSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_ANCIENT_DEBRIS_SMALL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreCoppperSmall = configuredFeatures.getOrThrow(OreFeatures.ORE_COPPPER_SMALL);
        Holder.Reference<ConfiguredFeature<?, ?>> oreCopperLarge = configuredFeatures.getOrThrow(OreFeatures.ORE_COPPER_LARGE);
        Holder.Reference<ConfiguredFeature<?, ?>> oreClay = configuredFeatures.getOrThrow(OreFeatures.ORE_CLAY);
        PlacementUtils.register(context, ORE_MAGMA, oreMagma, OrePlacements.commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.absolute(27), VerticalAnchor.absolute(36))));
        PlacementUtils.register(context, ORE_SOUL_SAND, oreSoulSand, OrePlacements.commonOrePlacement(12, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(31))));
        PlacementUtils.register(context, ORE_GOLD_DELTAS, oreNetherGold, OrePlacements.commonOrePlacement(20, PlacementUtils.RANGE_10_10));
        PlacementUtils.register(context, ORE_QUARTZ_DELTAS, oreQuartz, OrePlacements.commonOrePlacement(32, PlacementUtils.RANGE_10_10));
        PlacementUtils.register(context, ORE_GOLD_NETHER, oreNetherGold, OrePlacements.commonOrePlacement(10, PlacementUtils.RANGE_10_10));
        PlacementUtils.register(context, ORE_QUARTZ_NETHER, oreQuartz, OrePlacements.commonOrePlacement(16, PlacementUtils.RANGE_10_10));
        PlacementUtils.register(context, ORE_GRAVEL_NETHER, oreGravelNether, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(41))));
        PlacementUtils.register(context, ORE_BLACKSTONE, oreBlackstone, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(31))));
        PlacementUtils.register(context, ORE_DIRT, oreDirt, OrePlacements.commonOrePlacement(7, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(160))));
        PlacementUtils.register(context, ORE_GRAVEL, oreGravel, OrePlacements.commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top())));
        PlacementUtils.register(context, ORE_GRANITE_UPPER, oreGranite, OrePlacements.rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128))));
        PlacementUtils.register(context, ORE_GRANITE_LOWER, oreGranite, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60))));
        PlacementUtils.register(context, ORE_DIORITE_UPPER, oreDiorite, OrePlacements.rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128))));
        PlacementUtils.register(context, ORE_DIORITE_LOWER, oreDiorite, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60))));
        PlacementUtils.register(context, ORE_ANDESITE_UPPER, oreAndesite, OrePlacements.rareOrePlacement(6, HeightRangePlacement.uniform(VerticalAnchor.absolute(64), VerticalAnchor.absolute(128))));
        PlacementUtils.register(context, ORE_ANDESITE_LOWER, oreAndesite, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60))));
        PlacementUtils.register(context, ORE_TUFF, oreTuff, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(0))));
        PlacementUtils.register(context, ORE_COAL_UPPER, oreCoal, OrePlacements.commonOrePlacement(30, HeightRangePlacement.uniform(VerticalAnchor.absolute(136), VerticalAnchor.top())));
        PlacementUtils.register(context, ORE_COAL_LOWER, oreCoalBuried, OrePlacements.commonOrePlacement(20, HeightRangePlacement.triangle(VerticalAnchor.absolute(0), VerticalAnchor.absolute(192))));
        PlacementUtils.register(context, ORE_IRON_UPPER, oreIron, OrePlacements.commonOrePlacement(90, HeightRangePlacement.triangle(VerticalAnchor.absolute(80), VerticalAnchor.absolute(384))));
        PlacementUtils.register(context, ORE_IRON_MIDDLE, oreIron, OrePlacements.commonOrePlacement(10, HeightRangePlacement.triangle(VerticalAnchor.absolute(-24), VerticalAnchor.absolute(56))));
        PlacementUtils.register(context, ORE_IRON_SMALL, oreIronSmall, OrePlacements.commonOrePlacement(10, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(72))));
        PlacementUtils.register(context, ORE_GOLD_EXTRA, oreGold, OrePlacements.commonOrePlacement(50, HeightRangePlacement.uniform(VerticalAnchor.absolute(32), VerticalAnchor.absolute(256))));
        PlacementUtils.register(context, ORE_GOLD, oreGoldBuried, OrePlacements.commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(32))));
        PlacementUtils.register(context, ORE_GOLD_LOWER, oreGoldBuried, OrePlacements.orePlacement(CountPlacement.of(UniformInt.of(0, 1)), HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-48))));
        PlacementUtils.register(context, ORE_REDSTONE, oreRedstone, OrePlacements.commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(15))));
        PlacementUtils.register(context, ORE_REDSTONE_LOWER, oreRedstone, OrePlacements.commonOrePlacement(8, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-32), VerticalAnchor.aboveBottom(32))));
        PlacementUtils.register(context, ORE_DIAMOND, oreDiamondSmall, OrePlacements.commonOrePlacement(7, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80))));
        PlacementUtils.register(context, ORE_DIAMOND_MEDIUM, oreDiamondMedium, OrePlacements.commonOrePlacement(2, HeightRangePlacement.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(-4))));
        PlacementUtils.register(context, ORE_DIAMOND_LARGE, oreDiamondLarge, OrePlacements.rareOrePlacement(9, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80))));
        PlacementUtils.register(context, ORE_DIAMOND_BURIED, oreDiamondBuried, OrePlacements.commonOrePlacement(4, HeightRangePlacement.triangle(VerticalAnchor.aboveBottom(-80), VerticalAnchor.aboveBottom(80))));
        PlacementUtils.register(context, ORE_LAPIS, oreLapis, OrePlacements.commonOrePlacement(2, HeightRangePlacement.triangle(VerticalAnchor.absolute(-32), VerticalAnchor.absolute(32))));
        PlacementUtils.register(context, ORE_LAPIS_BURIED, oreLapisBuried, OrePlacements.commonOrePlacement(4, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64))));
        PlacementUtils.register(context, ORE_INFESTED, oreInfested, OrePlacements.commonOrePlacement(14, HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(63))));
        PlacementUtils.register(context, ORE_EMERALD, oreEmerald, OrePlacements.commonOrePlacement(100, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(480))));
        PlacementUtils.register(context, ORE_ANCIENT_DEBRIS_LARGE, oreAncientDebrisLarge, InSquarePlacement.spread(), HeightRangePlacement.triangle(VerticalAnchor.absolute(8), VerticalAnchor.absolute(24)), BiomeFilter.biome());
        PlacementUtils.register(context, ORE_ANCIENT_DEBRIS_SMALL, oreAncientDebrisSmall, InSquarePlacement.spread(), PlacementUtils.RANGE_8_8, BiomeFilter.biome());
        PlacementUtils.register(context, ORE_COPPER, oreCoppperSmall, OrePlacements.commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112))));
        PlacementUtils.register(context, ORE_COPPER_LARGE, oreCopperLarge, OrePlacements.commonOrePlacement(16, HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112))));
        PlacementUtils.register(context, ORE_CLAY, oreClay, OrePlacements.commonOrePlacement(46, PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT));
    }
}

