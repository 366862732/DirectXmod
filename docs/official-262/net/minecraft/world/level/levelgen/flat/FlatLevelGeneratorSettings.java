/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.flat;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.LayerConfiguration;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class FlatLevelGeneratorSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<FlatLevelGeneratorSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).lenientOptionalFieldOf("structure_overrides").forGetter(c -> c.structureOverrides), (App)FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(FlatLevelGeneratorSettings::getLayersInfo), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "lakes", false).forGetter(s -> s.addLakes), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "features", false).forGetter(s -> s.decoration), (App)Biome.CODEC.lenientOptionalFieldOf("biome").orElseGet(Optional::empty).forGetter(s -> Optional.of(s.biome)), RegistryOps.retrieveElement(Biomes.PLAINS), RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND), RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_SURFACE)).apply((Applicative)i, FlatLevelGeneratorSettings::new)).comapFlatMap(FlatLevelGeneratorSettings::validateHeight, Function.identity()).stable();
    private final Optional<HolderSet<StructureSet>> structureOverrides;
    private final List<FlatLayerInfo> layersInfo = Lists.newArrayList();
    private final Holder<Biome> biome;
    private final List<BlockState> layers;
    private boolean voidGen;
    private boolean decoration;
    private boolean addLakes;
    private final List<Holder<PlacedFeature>> lakes;

    private static DataResult<FlatLevelGeneratorSettings> validateHeight(FlatLevelGeneratorSettings settings) {
        int totalHeight = settings.layersInfo.stream().mapToInt(FlatLayerInfo::getHeight).sum();
        if (totalHeight > DimensionType.Y_SIZE) {
            return DataResult.error(() -> "Sum of layer heights is > " + DimensionType.Y_SIZE, (Object)settings);
        }
        return DataResult.success((Object)settings);
    }

    private FlatLevelGeneratorSettings(Optional<HolderSet<StructureSet>> structureOverrides, List<FlatLayerInfo> layers, boolean lakes, boolean features, Optional<Holder<Biome>> biome, Holder.Reference<Biome> fallbackBiome, Holder<PlacedFeature> lavaUnderground, Holder<PlacedFeature> lavaSurface) {
        this(structureOverrides, FlatLevelGeneratorSettings.getBiome(biome, fallbackBiome), List.of(lavaUnderground, lavaSurface));
        if (lakes) {
            this.setAddLakes();
        }
        if (features) {
            this.setDecoration();
        }
        this.layersInfo.addAll(layers);
        this.updateLayers();
    }

    private static Holder<Biome> getBiome(Optional<? extends Holder<Biome>> biome, Holder<Biome> fallbackBiome) {
        if (biome.isEmpty()) {
            LOGGER.error("Unknown biome, defaulting to plains");
            return fallbackBiome;
        }
        return biome.get();
    }

    public FlatLevelGeneratorSettings(Optional<HolderSet<StructureSet>> structureOverrides, Holder<Biome> biome, List<Holder<PlacedFeature>> lakes) {
        this.structureOverrides = structureOverrides;
        this.biome = biome;
        this.layers = Lists.newArrayList();
        this.lakes = lakes;
    }

    public FlatLevelGeneratorSettings withBiomeAndLayers(List<FlatLayerInfo> layers, Optional<HolderSet<StructureSet>> structureOverrides, Holder<Biome> biome) {
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(structureOverrides, biome, this.lakes);
        for (FlatLayerInfo layerInfo : layers) {
            settings.layersInfo.add(layerInfo);
            settings.updateLayers();
        }
        if (this.decoration) {
            settings.setDecoration();
        }
        if (this.addLakes) {
            settings.setAddLakes();
        }
        return settings;
    }

    public void setDecoration() {
        this.decoration = true;
    }

    public void setAddLakes() {
        this.addLakes = true;
    }

    public BiomeGenerationSettings adjustGenerationSettings(Holder<Biome> sourceBiome) {
        boolean biomeDecoration;
        if (!sourceBiome.equals(this.biome)) {
            return sourceBiome.value().getGenerationSettings();
        }
        BiomeGenerationSettings biomeGenerationSettings = this.getBiome().value().getGenerationSettings();
        BiomeGenerationSettings.PlainBuilder newGenerationSettings = new BiomeGenerationSettings.PlainBuilder();
        if (this.addLakes) {
            for (Holder<PlacedFeature> lake : this.lakes) {
                newGenerationSettings.addFeature(GenerationStep.Decoration.LAKES, lake);
            }
        }
        boolean bl = biomeDecoration = (!this.voidGen || sourceBiome.is(Biomes.THE_VOID)) && this.decoration;
        if (biomeDecoration) {
            List<HolderSet<PlacedFeature>> features = biomeGenerationSettings.features();
            for (int stepIndex = 0; stepIndex < features.size(); ++stepIndex) {
                if (stepIndex == GenerationStep.Decoration.UNDERGROUND_STRUCTURES.ordinal() || stepIndex == GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal() || this.addLakes && stepIndex == GenerationStep.Decoration.LAKES.ordinal()) continue;
                HolderSet<PlacedFeature> featureList = features.get(stepIndex);
                for (Holder holder : featureList) {
                    newGenerationSettings.addFeature(stepIndex, (Holder<PlacedFeature>)holder);
                }
            }
        }
        List<BlockState> layers = this.getLayers();
        for (int i = 0; i < layers.size(); ++i) {
            BlockState layer = layers.get(i);
            if (Heightmap.Types.MOTION_BLOCKING.isOpaque().test(layer)) continue;
            layers.set(i, null);
            newGenerationSettings.addFeature(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PlacementUtils.inlinePlaced(Feature.FILL_LAYER, new LayerConfiguration(i, layer), new PlacementModifier[0]));
        }
        return newGenerationSettings.build();
    }

    public Optional<HolderSet<StructureSet>> structureOverrides() {
        return this.structureOverrides;
    }

    public Holder<Biome> getBiome() {
        return this.biome;
    }

    public List<FlatLayerInfo> getLayersInfo() {
        return this.layersInfo;
    }

    public List<BlockState> getLayers() {
        return this.layers;
    }

    public void updateLayers() {
        this.layers.clear();
        for (FlatLayerInfo layer : this.layersInfo) {
            for (int y = 0; y < layer.getHeight(); ++y) {
                this.layers.add(layer.getBlockState());
            }
        }
        this.voidGen = this.layers.stream().allMatch(s -> s.is(Blocks.AIR));
    }

    public static FlatLevelGeneratorSettings getDefault(HolderGetter<Biome> biomes, HolderGetter<StructureSet> structureSets, HolderGetter<PlacedFeature> placedFeatures) {
        HolderSet.Direct structureSettings = HolderSet.direct(structureSets.getOrThrow(BuiltinStructureSets.STRONGHOLDS), structureSets.getOrThrow(BuiltinStructureSets.VILLAGES));
        FlatLevelGeneratorSettings result = new FlatLevelGeneratorSettings(Optional.of(structureSettings), FlatLevelGeneratorSettings.getDefaultBiome(biomes), FlatLevelGeneratorSettings.createLakesList(placedFeatures));
        result.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
        result.getLayersInfo().add(new FlatLayerInfo(2, Blocks.DIRT));
        result.getLayersInfo().add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
        result.updateLayers();
        return result;
    }

    public static Holder<Biome> getDefaultBiome(HolderGetter<Biome> biomes) {
        return biomes.getOrThrow(Biomes.PLAINS);
    }

    public static List<Holder<PlacedFeature>> createLakesList(HolderGetter<PlacedFeature> placedFeatures) {
        return List.of(placedFeatures.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND), placedFeatures.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_SURFACE));
    }
}

