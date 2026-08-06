/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.worldselection;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.worldselection.InitialWorldCreationOptions;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;

@Environment(value=EnvType.CLIENT)
public record WorldCreationContext(WorldOptions options, Registry<LevelStem> datapackDimensions, WorldDimensions selectedDimensions, LayeredRegistryAccess<RegistryLayer> worldgenRegistries, ReloadableServerResources dataPackResources, WorldDataConfiguration dataConfiguration, InitialWorldCreationOptions initialWorldCreationOptions) {
    public WorldCreationContext(WorldGenSettings worldGenSettings, LayeredRegistryAccess<RegistryLayer> loadedRegistries, ReloadableServerResources dataPackResources, WorldDataConfiguration dataConfiguration) {
        this(worldGenSettings.options(), worldGenSettings.dimensions(), loadedRegistries, dataPackResources, dataConfiguration, new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.SURVIVAL, GameRuleMap.of(), null));
    }

    public WorldCreationContext(WorldOptions worldOptions, WorldDimensions worldDimensions, LayeredRegistryAccess<RegistryLayer> loadedRegistries, ReloadableServerResources dataPackResources, WorldDataConfiguration dataConfiguration, InitialWorldCreationOptions initialWorldCreationOptions) {
        this(worldOptions, (Registry<LevelStem>)loadedRegistries.getLayer(RegistryLayer.DIMENSIONS).lookupOrThrow(Registries.LEVEL_STEM), worldDimensions, loadedRegistries.replaceFrom(RegistryLayer.DIMENSIONS, new RegistryAccess.Frozen[0]), dataPackResources, dataConfiguration, initialWorldCreationOptions);
    }

    public WorldCreationContext withSettings(WorldOptions options, WorldDimensions dimensions) {
        return new WorldCreationContext(options, this.datapackDimensions, dimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions);
    }

    public WorldCreationContext withOptions(OptionsModifier modifier) {
        return new WorldCreationContext((WorldOptions)modifier.apply(this.options), this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions);
    }

    public WorldCreationContext withDimensions(DimensionsUpdater modifier) {
        return new WorldCreationContext(this.options, this.datapackDimensions, (WorldDimensions)modifier.apply(this.worldgenLoadContext(), this.selectedDimensions), this.worldgenRegistries, this.dataPackResources, this.dataConfiguration, this.initialWorldCreationOptions);
    }

    public RegistryAccess.Frozen worldgenLoadContext() {
        return this.worldgenRegistries.compositeAccess();
    }

    public void validate() {
        for (LevelStem stem : this.datapackDimensions()) {
            stem.generator().validate();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface OptionsModifier
    extends UnaryOperator<WorldOptions> {
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface DimensionsUpdater
    extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
    }
}

