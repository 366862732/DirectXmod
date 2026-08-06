/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen.presets;

import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class WorldPresets {
    public static final ResourceKey<WorldPreset> NORMAL = WorldPresets.register("normal");
    public static final ResourceKey<WorldPreset> FLAT = WorldPresets.register("flat");
    public static final ResourceKey<WorldPreset> FLAT_ALL_DIMENSIONS = WorldPresets.register("flat_all_dimensions");
    public static final ResourceKey<WorldPreset> LARGE_BIOMES = WorldPresets.register("large_biomes");
    public static final ResourceKey<WorldPreset> AMPLIFIED = WorldPresets.register("amplified");
    public static final ResourceKey<WorldPreset> SINGLE_BIOME_SURFACE = WorldPresets.register("single_biome_surface");
    public static final ResourceKey<WorldPreset> DEBUG = WorldPresets.register("debug_all_block_states");

    public static void bootstrap(BootstrapContext<WorldPreset> context) {
        new Bootstrap(context).bootstrap();
    }

    private static ResourceKey<WorldPreset> register(String name) {
        return ResourceKey.create(Registries.WORLD_PRESET, Identifier.withDefaultNamespace(name));
    }

    public static Optional<ResourceKey<WorldPreset>> fromSettings(WorldDimensions dimensions) {
        return dimensions.get(LevelStem.OVERWORLD).flatMap(levelStem -> {
            ChunkGenerator chunkGenerator = levelStem.generator();
            Objects.requireNonNull(chunkGenerator);
            ChunkGenerator selector0$temp = chunkGenerator;
            int index$1 = 0;
            return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{FlatLevelSource.class, DebugLevelSource.class, NoiseBasedChunkGenerator.class}, (ChunkGenerator)selector0$temp, index$1)) {
                case 0 -> {
                    FlatLevelSource ignored = (FlatLevelSource)selector0$temp;
                    yield Optional.of(FLAT);
                }
                case 1 -> {
                    DebugLevelSource ignored = (DebugLevelSource)selector0$temp;
                    yield Optional.of(DEBUG);
                }
                case 2 -> {
                    NoiseBasedChunkGenerator ignored = (NoiseBasedChunkGenerator)selector0$temp;
                    yield Optional.of(NORMAL);
                }
                default -> Optional.empty();
            };
        });
    }

    public static WorldDimensions createNormalWorldDimensions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(NORMAL).value().createWorldDimensions();
    }

    public static LevelStem getNormalOverworld(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(NORMAL).value().overworld().orElseThrow();
    }

    public static WorldDimensions createTestWorldDimensions(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(FLAT_ALL_DIMENSIONS).value().createWorldDimensions();
    }

    private static class Bootstrap {
        private final BootstrapContext<WorldPreset> context;
        private final HolderGetter<NoiseGeneratorSettings> noiseSettings;
        private final HolderGetter<Biome> biomes;
        private final HolderGetter<PlacedFeature> placedFeatures;
        private final HolderGetter<StructureSet> structureSets;
        private final HolderGetter<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists;
        private final Holder<DimensionType> overworldDimensionType;
        private final Holder<DimensionType> netherDimensionType;
        private final Holder<DimensionType> endDimensionType;
        private final LevelStem netherStem;
        private final LevelStem endStem;

        private Bootstrap(BootstrapContext<WorldPreset> context) {
            this.context = context;
            HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
            this.noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
            this.biomes = context.lookup(Registries.BIOME);
            this.placedFeatures = context.lookup(Registries.PLACED_FEATURE);
            this.structureSets = context.lookup(Registries.STRUCTURE_SET);
            this.multiNoiseBiomeSourceParameterLists = context.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
            this.overworldDimensionType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
            this.netherDimensionType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.NETHER);
            this.endDimensionType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.END);
            Holder.Reference<NoiseGeneratorSettings> netherNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER);
            Holder.Reference<MultiNoiseBiomeSourceParameterList> netherBiomePreset = this.multiNoiseBiomeSourceParameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
            this.netherStem = new LevelStem(this.netherDimensionType, new NoiseBasedChunkGenerator((BiomeSource)MultiNoiseBiomeSource.createFromPreset(netherBiomePreset), netherNoiseSettings));
            Holder.Reference<NoiseGeneratorSettings> endNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.END);
            this.endStem = new LevelStem(this.endDimensionType, new NoiseBasedChunkGenerator((BiomeSource)TheEndBiomeSource.create(this.biomes), endNoiseSettings));
        }

        private LevelStem makeOverworld(ChunkGenerator generator) {
            return new LevelStem(this.overworldDimensionType, generator);
        }

        private LevelStem makeNether(ChunkGenerator generator) {
            return new LevelStem(this.netherDimensionType, generator);
        }

        private LevelStem makeEnd(ChunkGenerator generator) {
            return new LevelStem(this.endDimensionType, generator);
        }

        private LevelStem makeNoiseBasedOverworld(BiomeSource overworldBiomeSource, Holder<NoiseGeneratorSettings> noiseSettings) {
            return this.makeOverworld(new NoiseBasedChunkGenerator(overworldBiomeSource, noiseSettings));
        }

        private WorldPreset createPresetWithCustomOverworld(LevelStem overworldStem) {
            return new WorldPreset(Map.of(LevelStem.OVERWORLD, overworldStem, LevelStem.NETHER, this.netherStem, LevelStem.END, this.endStem));
        }

        private void registerCustomOverworldPreset(ResourceKey<WorldPreset> debug, LevelStem overworld) {
            this.context.register(debug, this.createPresetWithCustomOverworld(overworld));
        }

        private void registerOverworlds(BiomeSource biomeSource) {
            Holder.Reference<NoiseGeneratorSettings> overworldNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            this.registerCustomOverworldPreset(NORMAL, this.makeNoiseBasedOverworld(biomeSource, overworldNoiseSettings));
            Holder.Reference<NoiseGeneratorSettings> largeBiomesNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
            this.registerCustomOverworldPreset(LARGE_BIOMES, this.makeNoiseBasedOverworld(biomeSource, largeBiomesNoiseSettings));
            Holder.Reference<NoiseGeneratorSettings> amplifiedNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.AMPLIFIED);
            this.registerCustomOverworldPreset(AMPLIFIED, this.makeNoiseBasedOverworld(biomeSource, amplifiedNoiseSettings));
        }

        public void bootstrap() {
            Holder.Reference<MultiNoiseBiomeSourceParameterList> overworldPreset = this.multiNoiseBiomeSourceParameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
            this.registerOverworlds(MultiNoiseBiomeSource.createFromPreset(overworldPreset));
            Holder.Reference<NoiseGeneratorSettings> overworldNoiseSettings = this.noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            Holder.Reference<Biome> plains = this.biomes.getOrThrow(Biomes.PLAINS);
            this.registerCustomOverworldPreset(SINGLE_BIOME_SURFACE, this.makeNoiseBasedOverworld(new FixedBiomeSource(plains), overworldNoiseSettings));
            this.registerCustomOverworldPreset(FLAT, this.makeOverworld(new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(this.biomes, this.structureSets, this.placedFeatures))));
            this.context.register(FLAT_ALL_DIMENSIONS, this.createFlatAllDimensionsPreset());
            this.registerCustomOverworldPreset(DEBUG, this.makeOverworld(new DebugLevelSource(plains)));
        }

        private FlatLevelGeneratorSettings flatSettingsForBiomeAndLayers(ResourceKey<Biome> biomeKey, List<FlatLayerInfo> layers) {
            return FlatLevelGeneratorSettings.getDefault(this.biomes, this.structureSets, this.placedFeatures).withBiomeAndLayers(layers, Optional.empty(), this.biomes.getOrThrow(biomeKey));
        }

        private WorldPreset createFlatAllDimensionsPreset() {
            LevelStem overworldFlat = this.makeOverworld(new FlatLevelSource(this.flatSettingsForBiomeAndLayers(Biomes.DESERT, List.of(new FlatLayerInfo(1, Blocks.BEDROCK), new FlatLayerInfo(67, Blocks.SANDSTONE)))));
            LevelStem netherFlat = this.makeNether(new FlatLevelSource(this.flatSettingsForBiomeAndLayers(Biomes.BASALT_DELTAS, List.of(new FlatLayerInfo(1, Blocks.BEDROCK), new FlatLayerInfo(3, Blocks.BASALT)))));
            LevelStem endFlat = this.makeEnd(new FlatLevelSource(this.flatSettingsForBiomeAndLayers(Biomes.THE_END, List.of(new FlatLayerInfo(1, Blocks.BEDROCK), new FlatLayerInfo(3, Blocks.END_STONE)))));
            return new WorldPreset(Map.of(LevelStem.OVERWORLD, overworldFlat, LevelStem.NETHER, netherFlat, LevelStem.END, endFlat));
        }
    }
}

