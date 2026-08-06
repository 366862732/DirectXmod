/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.worldselection;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

@Environment(value=EnvType.CLIENT)
public interface PresetEditor {
    public static final Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> EDITORS = Map.of(Optional.of(WorldPresets.FLAT), (parent, settings) -> {
        FlatLevelGeneratorSettings flatLevelGeneratorSettings;
        ChunkGenerator overworld = settings.selectedDimensions().overworld();
        RegistryAccess.Frozen registryAccess = settings.worldgenLoadContext();
        HolderLookup.RegistryLookup biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup structureSets = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        Consumer<FlatLevelGeneratorSettings> consumer = flatWorldSettings -> parent.getUiState().updateDimensions(PresetEditor.flatWorldConfigurator(flatWorldSettings));
        if (overworld instanceof FlatLevelSource) {
            FlatLevelSource flatLevelSource = (FlatLevelSource)overworld;
            flatLevelGeneratorSettings = flatLevelSource.settings();
        } else {
            flatLevelGeneratorSettings = FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures);
        }
        return new CreateFlatWorldScreen(parent, consumer, flatLevelGeneratorSettings);
    }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (parent, settings) -> new CreateBuffetWorldScreen(parent, settings, biome -> parent.getUiState().updateDimensions(PresetEditor.fixedBiomeConfigurator(biome))));

    public Screen createEditScreen(CreateWorldScreen var1, WorldCreationContext var2);

    public static WorldCreationContext.DimensionsUpdater flatWorldConfigurator(FlatLevelGeneratorSettings generatorSettings) {
        return (registryAccess, dimensions) -> {
            FlatLevelSource generator = new FlatLevelSource(generatorSettings);
            return dimensions.replaceOverworldGenerator((HolderLookup.Provider)registryAccess, generator);
        };
    }

    private static WorldCreationContext.DimensionsUpdater fixedBiomeConfigurator(Holder<Biome> biome) {
        return (registryAccess, dimensions) -> {
            HolderLookup.RegistryLookup noiseGeneratorSettings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS);
            Holder.Reference noiseSettings = noiseGeneratorSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            FixedBiomeSource biomeSource = new FixedBiomeSource(biome);
            NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator((BiomeSource)biomeSource, noiseSettings);
            return dimensions.replaceOverworldGenerator((HolderLookup.Provider)registryAccess, generator);
        };
    }
}

