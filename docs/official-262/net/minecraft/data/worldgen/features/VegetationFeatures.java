/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseThresholdProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluids;

public class VegetationFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_NO_PODZOL = FeatureUtils.createKey("bamboo_no_podzol");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_SOME_PODZOL = FeatureUtils.createKey("bamboo_some_podzol");
    public static final ResourceKey<ConfiguredFeature<?, ?>> VINES = FeatureUtils.createKey("vines");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BROWN_MUSHROOM = FeatureUtils.createKey("brown_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RED_MUSHROOM = FeatureUtils.createKey("red_mushroom");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUNFLOWER = FeatureUtils.createKey("sunflower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PUMPKIN = FeatureUtils.createKey("pumpkin");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BERRY_BUSH = FeatureUtils.createKey("berry_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TAIGA_GRASS = FeatureUtils.createKey("taiga_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS = FeatureUtils.createKey("grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GRASS_JUNGLE = FeatureUtils.createKey("grass_jungle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_BUSH = FeatureUtils.createKey("dead_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DRY_GRASS = FeatureUtils.createKey("dry_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MELON = FeatureUtils.createKey("melon");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WATERLILY = FeatureUtils.createKey("waterlily");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TALL_GRASS = FeatureUtils.createKey("tall_grass");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_FERN = FeatureUtils.createKey("large_fern");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BUSH = FeatureUtils.createKey("bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LEAF_LITTER = FeatureUtils.createKey("leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FIREFLY_BUSH = FeatureUtils.createKey("firefly_bush");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CACTUS = FeatureUtils.createKey("cactus");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SUGAR_CANE = FeatureUtils.createKey("sugar_cane");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_DEFAULT = FeatureUtils.createKey("flower_default");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_FLOWER_FOREST = FeatureUtils.createKey("flower_flower_forest");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_SWAMP = FeatureUtils.createKey("flower_swamp");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PLAIN = FeatureUtils.createKey("flower_plain");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_MEADOW = FeatureUtils.createKey("flower_meadow");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_CHERRY = FeatureUtils.createKey("flower_cherry");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FLOWER_PALE_GARDEN = FeatureUtils.createKey("flower_pale_garden");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WILDFLOWER = FeatureUtils.createKey("wildflower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> FOREST_FLOWERS = FeatureUtils.createKey("forest_flowers");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_FOREST_FLOWER = FeatureUtils.createKey("pale_forest_flower");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DARK_FOREST_VEGETATION = FeatureUtils.createKey("dark_forest_vegetation");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_GARDEN_VEGETATION = FeatureUtils.createKey("pale_garden_vegetation");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_VEGETATION = FeatureUtils.createKey("pale_moss_vegetation");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH = FeatureUtils.createKey("pale_moss_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH_BONEMEAL = FeatureUtils.createKey("pale_moss_patch_bonemeal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_FLOWER_FOREST = FeatureUtils.createKey("trees_flower_forest");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MEADOW_TREES = FeatureUtils.createKey("meadow_trees");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_TAIGA = FeatureUtils.createKey("trees_taiga");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BADLANDS = FeatureUtils.createKey("trees_badlands");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_GROVE = FeatureUtils.createKey("trees_grove");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SAVANNA = FeatureUtils.createKey("trees_savanna");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SNOWY = FeatureUtils.createKey("trees_snowy");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BIRCH = FeatureUtils.createKey("trees_birch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BIRCH_TALL = FeatureUtils.createKey("birch_tall");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_WINDSWEPT_HILLS = FeatureUtils.createKey("trees_windswept_hills");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_WATER = FeatureUtils.createKey("trees_water");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_BIRCH_AND_OAK_LEAF_LITTER = FeatureUtils.createKey("trees_birch_and_oak_leaf_litter");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_PLAINS = FeatureUtils.createKey("trees_plains");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_SPARSE_JUNGLE = FeatureUtils.createKey("trees_sparse_jungle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_OLD_GROWTH_SPRUCE_TAIGA = FeatureUtils.createKey("trees_old_growth_spruce_taiga");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_OLD_GROWTH_PINE_TAIGA = FeatureUtils.createKey("trees_old_growth_pine_taiga");
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREES_JUNGLE = FeatureUtils.createKey("trees_jungle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAMBOO_VEGETATION = FeatureUtils.createKey("bamboo_vegetation");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MUSHROOM_ISLAND_VEGETATION = FeatureUtils.createKey("mushroom_island_vegetation");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MANGROVE_VEGETATION = FeatureUtils.createKey("mangrove_vegetation");
    private static final float FALLEN_TREE_ONE_IN_CHANCE = 80.0f;

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        Holder.Reference<ConfiguredFeature<?, ?>> hugeBrownMushroom = configuredFeatures.getOrThrow(TreeFeatures.HUGE_BROWN_MUSHROOM);
        Holder.Reference<ConfiguredFeature<?, ?>> hugeRedMushroom = configuredFeatures.getOrThrow(TreeFeatures.HUGE_RED_MUSHROOM);
        Holder.Reference<ConfiguredFeature<?, ?>> fancyOakBees005 = configuredFeatures.getOrThrow(TreeFeatures.FANCY_OAK_BEES_005);
        Holder.Reference<ConfiguredFeature<?, ?>> oakBees005 = configuredFeatures.getOrThrow(TreeFeatures.OAK_BEES_005);
        Holder.Reference<ConfiguredFeature<?, ?>> grassJungle = configuredFeatures.getOrThrow(GRASS_JUNGLE);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        Holder.Reference<PlacedFeature> paleOakChecked = placedFeatures.getOrThrow(TreePlacements.PALE_OAK_CHECKED);
        Holder.Reference<PlacedFeature> paleOakCreakingChecked = placedFeatures.getOrThrow(TreePlacements.PALE_OAK_CREAKING_CHECKED);
        Holder.Reference<PlacedFeature> fancyOakChecked = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_CHECKED);
        Holder.Reference<PlacedFeature> birchBees002 = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_002);
        Holder.Reference<PlacedFeature> fancyOakBees002 = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES_002);
        Holder.Reference<PlacedFeature> fancyOakBees = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES);
        Holder.Reference<PlacedFeature> pineChecked = placedFeatures.getOrThrow(TreePlacements.PINE_CHECKED);
        Holder.Reference<PlacedFeature> spruceChecked = placedFeatures.getOrThrow(TreePlacements.SPRUCE_CHECKED);
        Holder.Reference<PlacedFeature> pineOnSnow = placedFeatures.getOrThrow(TreePlacements.PINE_ON_SNOW);
        Holder.Reference<PlacedFeature> acaciaChecked = placedFeatures.getOrThrow(TreePlacements.ACACIA_CHECKED);
        Holder.Reference<PlacedFeature> superBirchBees0002 = placedFeatures.getOrThrow(TreePlacements.SUPER_BIRCH_BEES_0002);
        Holder.Reference<PlacedFeature> birchBees0002Placed = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_0002_PLACED);
        Holder.Reference<PlacedFeature> birchBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.BIRCH_BEES_0002_LEAF_LITTER);
        Holder.Reference<PlacedFeature> fancyOakBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_BEES_0002_LEAF_LITTER);
        Holder.Reference<PlacedFeature> jungleBush = placedFeatures.getOrThrow(TreePlacements.JUNGLE_BUSH);
        Holder.Reference<PlacedFeature> megaSpruceChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_SPRUCE_CHECKED);
        Holder.Reference<PlacedFeature> megaPineChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_PINE_CHECKED);
        Holder.Reference<PlacedFeature> megaJungleTreeChecked = placedFeatures.getOrThrow(TreePlacements.MEGA_JUNGLE_TREE_CHECKED);
        Holder.Reference<PlacedFeature> tallMangroveChecked = placedFeatures.getOrThrow(TreePlacements.TALL_MANGROVE_CHECKED);
        Holder.Reference<PlacedFeature> oakChecked = placedFeatures.getOrThrow(TreePlacements.OAK_CHECKED);
        Holder.Reference<PlacedFeature> oakBees002 = placedFeatures.getOrThrow(TreePlacements.OAK_BEES_002);
        Holder.Reference<PlacedFeature> superBirchBees = placedFeatures.getOrThrow(TreePlacements.SUPER_BIRCH_BEES);
        Holder.Reference<PlacedFeature> spruceOnSnow = placedFeatures.getOrThrow(TreePlacements.SPRUCE_ON_SNOW);
        Holder.Reference<PlacedFeature> oakBees0002LeafLitter = placedFeatures.getOrThrow(TreePlacements.OAK_BEES_0002_LEAF_LITTER);
        Holder.Reference<PlacedFeature> jungleTreeChecked = placedFeatures.getOrThrow(TreePlacements.JUNGLE_TREE_CHECKED);
        Holder.Reference<PlacedFeature> mangroveChecked = placedFeatures.getOrThrow(TreePlacements.MANGROVE_CHECKED);
        Holder.Reference<PlacedFeature> oakLeafLitter = placedFeatures.getOrThrow(TreePlacements.OAK_LEAF_LITTER);
        Holder.Reference<PlacedFeature> darkOakLeafLitter = placedFeatures.getOrThrow(TreePlacements.DARK_OAK_LEAF_LITTER);
        Holder.Reference<PlacedFeature> birchLeafLitter = placedFeatures.getOrThrow(TreePlacements.BIRCH_LEAF_LITTER);
        Holder.Reference<PlacedFeature> fancyOakLeafLitter = placedFeatures.getOrThrow(TreePlacements.FANCY_OAK_LEAF_LITTER);
        Holder.Reference<PlacedFeature> fallenOak = placedFeatures.getOrThrow(TreePlacements.FALLEN_OAK_TREE);
        Holder.Reference<PlacedFeature> fallenBirch = placedFeatures.getOrThrow(TreePlacements.FALLEN_BIRCH_TREE);
        Holder.Reference<PlacedFeature> fallenSuperBirch = placedFeatures.getOrThrow(TreePlacements.FALLEN_SUPER_BIRCH_TREE);
        Holder.Reference<PlacedFeature> fallenJungle = placedFeatures.getOrThrow(TreePlacements.FALLEN_JUNGLE_TREE);
        Holder.Reference<PlacedFeature> fallenSpruce = placedFeatures.getOrThrow(TreePlacements.FALLEN_SPRUCE_TREE);
        FeatureUtils.register(context, BAMBOO_NO_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.0f));
        FeatureUtils.register(context, BAMBOO_SOME_PODZOL, Feature.BAMBOO, new ProbabilityFeatureConfiguration(0.2f));
        FeatureUtils.register(context, VINES, Feature.VINES);
        FeatureUtils.register(context, BROWN_MUSHROOM, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BROWN_MUSHROOM)));
        FeatureUtils.register(context, RED_MUSHROOM, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.RED_MUSHROOM)));
        FeatureUtils.register(context, SUNFLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SUNFLOWER)));
        FeatureUtils.register(context, PUMPKIN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PUMPKIN)));
        FeatureUtils.register(context, BERRY_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple((BlockState)Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3))));
        FeatureUtils.register(context, TAIGA_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(WeightedList.builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 1).add(Blocks.FERN.defaultBlockState(), 4))));
        FeatureUtils.register(context, GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SHORT_GRASS)));
        FeatureUtils.register(context, LEAF_LITTER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(VegetationFeatures.leafLitterPatchBuilder(1, 3))));
        FeatureUtils.register(context, GRASS_JUNGLE, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(WeightedList.builder().add(Blocks.SHORT_GRASS.defaultBlockState(), 3).add(Blocks.FERN.defaultBlockState(), 1))));
        FeatureUtils.register(context, DEAD_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.DEAD_BUSH)));
        FeatureUtils.register(context, DRY_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(WeightedList.builder().add(Blocks.SHORT_DRY_GRASS.defaultBlockState(), 1).add(Blocks.TALL_DRY_GRASS.defaultBlockState(), 1))));
        FeatureUtils.register(context, MELON, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.MELON)));
        FeatureUtils.register(context, WATERLILY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_PAD)));
        FeatureUtils.register(context, TALL_GRASS, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.TALL_GRASS)));
        FeatureUtils.register(context, LARGE_FERN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LARGE_FERN)));
        FeatureUtils.register(context, BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BUSH)));
        FeatureUtils.register(context, CACTUS, Feature.BLOCK_COLUMN, new BlockColumnConfiguration(List.of(BlockColumnConfiguration.layer(BiasedToBottomInt.of(1, 3), BlockStateProvider.simple(Blocks.CACTUS)), BlockColumnConfiguration.layer(new WeightedListInt(WeightedList.builder().add(ConstantInt.of(0), 3).add(ConstantInt.of(1), 1).build()), BlockStateProvider.simple(Blocks.CACTUS_FLOWER))), Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false));
        FeatureUtils.register(context, SUGAR_CANE, Feature.BLOCK_COLUMN, BlockColumnConfiguration.simple(BiasedToBottomInt.of(2, 4), BlockStateProvider.simple(Blocks.SUGAR_CANE)));
        FeatureUtils.register(context, FIREFLY_BUSH, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.FIREFLY_BUSH)));
        WeightedStateProvider provider = new WeightedStateProvider(WeightedList.builder().add(Blocks.POPPY.defaultBlockState(), 2).add(Blocks.DANDELION.defaultBlockState(), 1));
        FeatureUtils.register(context, FLOWER_DEFAULT, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(provider));
        FeatureUtils.register(context, FLOWER_FLOWER_FOREST, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new NoiseProvider(2345L, new NormalNoise.NoiseParameters(0, 1.0, new double[0]), 0.020833334f, List.of(Blocks.DANDELION.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(), Blocks.ORANGE_TULIP.defaultBlockState(), Blocks.WHITE_TULIP.defaultBlockState(), Blocks.PINK_TULIP.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()))));
        FeatureUtils.register(context, FLOWER_SWAMP, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.BLUE_ORCHID)));
        FeatureUtils.register(context, FLOWER_PLAIN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new NoiseThresholdProvider(2345L, new NormalNoise.NoiseParameters(0, 1.0, new double[0]), 0.005f, -0.8f, 0.33333334f, Blocks.DANDELION.defaultBlockState(), List.of(Blocks.ORANGE_TULIP.defaultBlockState(), Blocks.RED_TULIP.defaultBlockState(), Blocks.PINK_TULIP.defaultBlockState(), Blocks.WHITE_TULIP.defaultBlockState()), List.of(Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState()))));
        FeatureUtils.register(context, FLOWER_MEADOW, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new DualNoiseProvider(new InclusiveRange<Integer>(1, 3), new NormalNoise.NoiseParameters(-10, 1.0, new double[0]), 1.0f, 2345L, new NormalNoise.NoiseParameters(-3, 1.0, new double[0]), 1.0f, List.of(Blocks.TALL_GRASS.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.POPPY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState()))));
        FeatureUtils.register(context, FLOWER_CHERRY, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(VegetationFeatures.flowerBedPatchBuilder(Blocks.PINK_PETALS))));
        FeatureUtils.register(context, WILDFLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(VegetationFeatures.flowerBedPatchBuilder(Blocks.WILDFLOWERS))));
        FeatureUtils.register(context, FLOWER_PALE_GARDEN, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true));
        FeatureUtils.register(context, FOREST_FLOWERS, Feature.SIMPLE_RANDOM_SELECTOR, new CompositeFeatureConfiguration(HolderSet.direct(PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILAC)), CountPlacement.of(96), RandomOffsetPlacement.ofTriangle(7, 3), BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)), PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.ROSE_BUSH)), CountPlacement.of(96), RandomOffsetPlacement.ofTriangle(7, 3), BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)), PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.PEONY)), CountPlacement.of(96), RandomOffsetPlacement.ofTriangle(7, 3), BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)), PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.LILY_OF_THE_VALLEY)), CountPlacement.of(96), RandomOffsetPlacement.ofTriangle(7, 3), BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE)))));
        FeatureUtils.register(context, PALE_FOREST_FLOWER, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.CLOSED_EYEBLOSSOM), true));
        FeatureUtils.register(context, DARK_FOREST_VEGETATION, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(hugeBrownMushroom, new PlacementModifier[0]), 0.025f), new WeightedPlacedFeature(PlacementUtils.inlinePlaced(hugeRedMushroom, new PlacementModifier[0]), 0.05f), new WeightedPlacedFeature(darkOakLeafLitter, 0.6666667f), new WeightedPlacedFeature(fallenBirch, 0.0025f), new WeightedPlacedFeature(birchLeafLitter, 0.2f), new WeightedPlacedFeature(fallenOak, 0.0125f), new WeightedPlacedFeature(fancyOakLeafLitter, 0.1f)), oakLeafLitter));
        FeatureUtils.register(context, PALE_GARDEN_VEGETATION, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(paleOakCreakingChecked, 0.1f), new WeightedPlacedFeature(paleOakChecked, 0.9f)), paleOakChecked));
        FeatureUtils.register(context, PALE_MOSS_VEGETATION, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(WeightedList.builder().add(Blocks.PALE_MOSS_CARPET.defaultBlockState(), 25).add(Blocks.SHORT_GRASS.defaultBlockState(), 25).add(Blocks.TALL_GRASS.defaultBlockState(), 10))));
        FeatureUtils.register(context, PALE_MOSS_PATCH, Feature.VEGETATION_PATCH, new VegetationPatchConfiguration(blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE), BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK), PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(PALE_MOSS_VEGETATION), new PlacementModifier[0]), CaveSurface.FLOOR, ConstantInt.of(1), 0.0f, 5, 0.3f, UniformInt.of(2, 4), 0.75f));
        FeatureUtils.register(context, PALE_MOSS_PATCH_BONEMEAL, Feature.VEGETATION_PATCH, new VegetationPatchConfiguration(blocks.getOrThrow(BlockTags.MOSS_REPLACEABLE), BlockStateProvider.simple(Blocks.PALE_MOSS_BLOCK), PlacementUtils.inlinePlaced(configuredFeatures.getOrThrow(PALE_MOSS_VEGETATION), new PlacementModifier[0]), CaveSurface.FLOOR, ConstantInt.of(1), 0.0f, 5, 0.6f, UniformInt.of(1, 2), 0.75f));
        FeatureUtils.register(context, TREES_FLOWER_FOREST, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenBirch, 0.0025f), new WeightedPlacedFeature(birchBees002, 0.2f), new WeightedPlacedFeature(fancyOakBees002, 0.1f)), oakBees002));
        FeatureUtils.register(context, MEADOW_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakBees, 0.5f)), superBirchBees));
        FeatureUtils.register(context, TREES_TAIGA, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(pineChecked, 0.33333334f), new WeightedPlacedFeature(fallenSpruce, 0.0125f)), spruceChecked));
        FeatureUtils.register(context, TREES_BADLANDS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenOak, 0.0125f)), oakLeafLitter));
        FeatureUtils.register(context, TREES_GROVE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(pineOnSnow, 0.33333334f)), spruceOnSnow));
        FeatureUtils.register(context, TREES_SAVANNA, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(acaciaChecked, 0.8f), new WeightedPlacedFeature(fallenOak, 0.0125f)), oakChecked));
        FeatureUtils.register(context, TREES_SNOWY, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenSpruce, 0.0125f)), spruceChecked));
        FeatureUtils.register(context, TREES_BIRCH, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenBirch, 0.0125f)), birchBees0002Placed));
        FeatureUtils.register(context, BIRCH_TALL, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenSuperBirch, 0.00625f), new WeightedPlacedFeature(superBirchBees0002, 0.5f), new WeightedPlacedFeature(fallenBirch, 0.0125f)), birchBees0002Placed));
        FeatureUtils.register(context, TREES_WINDSWEPT_HILLS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenSpruce, 0.008325f), new WeightedPlacedFeature(spruceChecked, 0.666f), new WeightedPlacedFeature(fancyOakChecked, 0.1f), new WeightedPlacedFeature(fallenOak, 0.0125f)), oakChecked));
        FeatureUtils.register(context, TREES_WATER, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakChecked, 0.1f)), oakChecked));
        FeatureUtils.register(context, TREES_BIRCH_AND_OAK_LEAF_LITTER, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fallenBirch, 0.0025f), new WeightedPlacedFeature(birchBees0002LeafLitter, 0.2f), new WeightedPlacedFeature(fancyOakBees0002LeafLitter, 0.1f), new WeightedPlacedFeature(fallenOak, 0.0125f)), oakBees0002LeafLitter));
        FeatureUtils.register(context, TREES_PLAINS, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(fancyOakBees005, new PlacementModifier[0]), 0.33333334f), new WeightedPlacedFeature(fallenOak, 0.0125f)), PlacementUtils.inlinePlaced(oakBees005, new PlacementModifier[0])));
        FeatureUtils.register(context, TREES_SPARSE_JUNGLE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakChecked, 0.1f), new WeightedPlacedFeature(jungleBush, 0.5f), new WeightedPlacedFeature(fallenJungle, 0.0125f)), jungleTreeChecked));
        FeatureUtils.register(context, TREES_OLD_GROWTH_SPRUCE_TAIGA, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(megaSpruceChecked, 0.33333334f), new WeightedPlacedFeature(pineChecked, 0.33333334f), new WeightedPlacedFeature(fallenSpruce, 0.0125f)), spruceChecked));
        FeatureUtils.register(context, TREES_OLD_GROWTH_PINE_TAIGA, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(megaSpruceChecked, 0.025641026f), new WeightedPlacedFeature(megaPineChecked, 0.30769232f), new WeightedPlacedFeature(pineChecked, 0.33333334f), new WeightedPlacedFeature(fallenSpruce, 0.0125f)), spruceChecked));
        FeatureUtils.register(context, TREES_JUNGLE, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakChecked, 0.1f), new WeightedPlacedFeature(jungleBush, 0.5f), new WeightedPlacedFeature(megaJungleTreeChecked, 0.33333334f), new WeightedPlacedFeature(fallenJungle, 0.0125f)), jungleTreeChecked));
        FeatureUtils.register(context, BAMBOO_VEGETATION, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(fancyOakChecked, 0.05f), new WeightedPlacedFeature(jungleBush, 0.15f), new WeightedPlacedFeature(megaJungleTreeChecked, 0.7f)), PlacementUtils.inlinePlaced(grassJungle, CountPlacement.of(32), RandomOffsetPlacement.ofTriangle(7, 3), BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.not(BlockPredicate.matchesBlocks(Direction.DOWN.getUnitVec3i(), Blocks.PODZOL)))))));
        FeatureUtils.register(context, MUSHROOM_ISLAND_VEGETATION, Feature.RANDOM_BOOLEAN_SELECTOR, new RandomBooleanFeatureConfiguration(PlacementUtils.inlinePlaced(hugeRedMushroom, new PlacementModifier[0]), PlacementUtils.inlinePlaced(hugeBrownMushroom, new PlacementModifier[0])));
        FeatureUtils.register(context, MANGROVE_VEGETATION, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(new WeightedPlacedFeature(tallMangroveChecked, 0.85f)), mangroveChecked));
    }

    private static WeightedList.Builder<BlockState> flowerBedPatchBuilder(Block flowerBedBlock) {
        return VegetationFeatures.segmentedBlockPatchBuilder(flowerBedBlock, 1, 4, FlowerBedBlock.AMOUNT, FlowerBedBlock.FACING);
    }

    public static WeightedList.Builder<BlockState> leafLitterPatchBuilder(int minState, int maxState) {
        return VegetationFeatures.segmentedBlockPatchBuilder(Blocks.LEAF_LITTER, minState, maxState, LeafLitterBlock.AMOUNT, LeafLitterBlock.FACING);
    }

    private static WeightedList.Builder<BlockState> segmentedBlockPatchBuilder(Block block, int minState, int maxState, IntegerProperty amountProperty, EnumProperty<Direction> directionProperty) {
        WeightedList.Builder<BlockState> segmentedBlockBuild = WeightedList.builder();
        for (int amount = minState; amount <= maxState; ++amount) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                segmentedBlockBuild.add((BlockState)((BlockState)block.defaultBlockState().setValue(amountProperty, amount)).setValue(directionProperty, direction), 1);
            }
        }
        return segmentedBlockBuild;
    }

    public static BlockPredicateFilter nearWaterPredicate(Block block) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO), BlockPredicate.anyOf(BlockPredicate.matchesFluids((Vec3i)new BlockPos(1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER), BlockPredicate.matchesFluids((Vec3i)new BlockPos(-1, -1, 0), Fluids.WATER, Fluids.FLOWING_WATER), BlockPredicate.matchesFluids((Vec3i)new BlockPos(0, -1, 1), Fluids.WATER, Fluids.FLOWING_WATER), BlockPredicate.matchesFluids((Vec3i)new BlockPos(0, -1, -1), Fluids.WATER, Fluids.FLOWING_WATER))));
    }
}

