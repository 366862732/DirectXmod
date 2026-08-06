/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.worldselection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.util.FileUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageException;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class WorldCreationUiState {
    private static final Component DEFAULT_WORLD_NAME = Component.translatable("selectWorld.newWorld");
    private final List<Consumer<WorldCreationUiState>> listeners = new ArrayList<Consumer<WorldCreationUiState>>();
    private String name = DEFAULT_WORLD_NAME.getString();
    private SelectedGameMode gameMode = SelectedGameMode.SURVIVAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private @Nullable Boolean allowCommands;
    private String seed;
    private boolean generateStructures;
    private boolean bonusChest;
    private final Path savesFolder;
    private String targetFolder;
    private WorldCreationContext settings;
    private WorldTypeEntry worldType;
    private final List<WorldTypeEntry> normalPresetList = new ArrayList<WorldTypeEntry>();
    private final List<WorldTypeEntry> altPresetList = new ArrayList<WorldTypeEntry>();
    private GameRules gameRules;

    public WorldCreationUiState(Path savesFolder, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed) {
        this.savesFolder = savesFolder;
        this.settings = settings;
        this.worldType = new WorldTypeEntry(WorldCreationUiState.findPreset(settings, preset).orElse(null));
        this.updatePresetLists();
        this.seed = seed.isPresent() ? Long.toString(seed.getAsLong()) : "";
        this.generateStructures = settings.options().generateStructures();
        this.bonusChest = settings.options().generateBonusChest();
        this.targetFolder = this.findResultFolder(this.name);
        this.gameMode = settings.initialWorldCreationOptions().selectedGameMode();
        this.gameRules = new GameRules(settings.dataConfiguration().enabledFeatures());
        this.gameRules.setAll(settings.initialWorldCreationOptions().gameRuleOverwrites(), null);
        Optional.ofNullable(settings.initialWorldCreationOptions().flatLevelPreset()).flatMap(key -> settings.worldgenLoadContext().lookup(Registries.FLAT_LEVEL_GENERATOR_PRESET).flatMap(registry -> registry.get(key))).map(reference -> ((FlatLevelGeneratorPreset)reference.value()).settings()).ifPresent(generatorSettings -> this.updateDimensions(PresetEditor.flatWorldConfigurator(generatorSettings)));
    }

    public void addListener(Consumer<WorldCreationUiState> action) {
        this.listeners.add(action);
    }

    public void onChanged() {
        boolean generateStructures;
        boolean bonusChest = this.isBonusChest();
        if (bonusChest != this.settings.options().generateBonusChest()) {
            this.settings = this.settings.withOptions(options -> options.withBonusChest(bonusChest));
        }
        if ((generateStructures = this.isGenerateStructures()) != this.settings.options().generateStructures()) {
            this.settings = this.settings.withOptions(options -> options.withStructures(generateStructures));
        }
        for (Consumer<WorldCreationUiState> listener : this.listeners) {
            listener.accept(this);
        }
    }

    public void setName(String name) {
        this.name = name;
        this.targetFolder = this.findResultFolder(name);
        this.onChanged();
    }

    private String findResultFolder(String name) {
        if (!Files.exists(this.savesFolder, new LinkOption[0])) {
            try {
                Files.createDirectory(this.savesFolder, new FileAttribute[0]);
            }
            catch (IOException e) {
                throw new LevelStorageException(Component.literal("Could not create save folder"), e);
            }
        }
        String trimmedName = name.trim();
        try {
            return FileUtil.findAvailableName(this.savesFolder, !trimmedName.isEmpty() ? trimmedName : DEFAULT_WORLD_NAME.getString(), "");
        }
        catch (Exception exception) {
            try {
                return FileUtil.findAvailableName(this.savesFolder, "World", "");
            }
            catch (IOException e) {
                throw new LevelStorageException(Component.literal("Could not create world folder"), e);
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getTargetFolder() {
        return this.targetFolder;
    }

    public void setGameMode(SelectedGameMode gameMode) {
        this.gameMode = gameMode;
        this.onChanged();
    }

    public SelectedGameMode getGameMode() {
        if (this.isDebug()) {
            return SelectedGameMode.DEBUG;
        }
        return this.gameMode;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.onChanged();
    }

    public Difficulty getDifficulty() {
        if (this.isHardcore()) {
            return Difficulty.HARD;
        }
        return this.difficulty;
    }

    public boolean isHardcore() {
        return this.getGameMode() == SelectedGameMode.HARDCORE;
    }

    public void setAllowCommands(boolean allowCommands) {
        this.allowCommands = allowCommands;
        this.onChanged();
    }

    public boolean isAllowCommands() {
        if (this.isDebug()) {
            return true;
        }
        if (this.isHardcore()) {
            return false;
        }
        if (this.allowCommands == null) {
            return this.getGameMode() == SelectedGameMode.CREATIVE;
        }
        return this.allowCommands;
    }

    public void setSeed(String seed) {
        this.seed = seed;
        this.settings = this.settings.withOptions(options -> options.withSeed(WorldOptions.parseSeed(this.getSeed())));
        this.onChanged();
    }

    public String getSeed() {
        return this.seed;
    }

    public void setGenerateStructures(boolean generateStructures) {
        this.generateStructures = generateStructures;
        this.onChanged();
    }

    public boolean isGenerateStructures() {
        if (this.isDebug()) {
            return false;
        }
        return this.generateStructures;
    }

    public void setBonusChest(boolean bonusChest) {
        this.bonusChest = bonusChest;
        this.onChanged();
    }

    public boolean isBonusChest() {
        if (this.isDebug() || this.isHardcore()) {
            return false;
        }
        return this.bonusChest;
    }

    public void setSettings(WorldCreationContext settings) {
        this.settings = settings;
        this.updatePresetLists();
        this.onChanged();
    }

    public WorldCreationContext getSettings() {
        return this.settings;
    }

    public void updateDimensions(WorldCreationContext.DimensionsUpdater modifier) {
        this.settings = this.settings.withDimensions(modifier);
        this.onChanged();
    }

    protected boolean tryUpdateDataConfiguration(WorldDataConfiguration newConfig) {
        WorldDataConfiguration oldConfig = this.settings.dataConfiguration();
        if (oldConfig.dataPacks().getEnabled().equals(newConfig.dataPacks().getEnabled()) && oldConfig.enabledFeatures().equals(newConfig.enabledFeatures())) {
            this.settings = new WorldCreationContext(this.settings.options(), this.settings.datapackDimensions(), this.settings.selectedDimensions(), this.settings.worldgenRegistries(), this.settings.dataPackResources(), newConfig, this.settings.initialWorldCreationOptions());
            return true;
        }
        return false;
    }

    public boolean isDebug() {
        return this.settings.selectedDimensions().isDebug();
    }

    public void setWorldType(WorldTypeEntry worldType) {
        this.worldType = worldType;
        Holder<WorldPreset> preset = worldType.preset();
        if (preset != null) {
            this.updateDimensions((registryAccess, dimensions) -> ((WorldPreset)preset.value()).createWorldDimensions());
        }
    }

    public WorldTypeEntry getWorldType() {
        return this.worldType;
    }

    public @Nullable PresetEditor getPresetEditor() {
        Holder<WorldPreset> preset = this.getWorldType().preset();
        return preset != null ? PresetEditor.EDITORS.get(preset.unwrapKey()) : null;
    }

    public List<WorldTypeEntry> getNormalPresetList() {
        return this.normalPresetList;
    }

    public List<WorldTypeEntry> getAltPresetList() {
        return this.altPresetList;
    }

    private void updatePresetLists() {
        HolderLookup.RegistryLookup presetRegistry = this.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET);
        this.normalPresetList.clear();
        this.normalPresetList.addAll(WorldCreationUiState.getNonEmptyList((Registry<WorldPreset>)presetRegistry, WorldPresetTags.NORMAL).orElseGet(() -> WorldCreationUiState.lambda$updatePresetLists$0((Registry)presetRegistry)));
        this.altPresetList.clear();
        this.altPresetList.addAll((Collection<WorldTypeEntry>)WorldCreationUiState.getNonEmptyList((Registry<WorldPreset>)presetRegistry, WorldPresetTags.EXTENDED).orElse(this.normalPresetList));
        Holder<WorldPreset> preset = this.worldType.preset();
        if (preset != null) {
            boolean isCustomizablePreset;
            WorldTypeEntry newPreset = WorldCreationUiState.findPreset(this.getSettings(), preset.unwrapKey()).map(WorldTypeEntry::new).orElse(this.normalPresetList.getFirst());
            boolean bl = isCustomizablePreset = PresetEditor.EDITORS.get(preset.unwrapKey()) != null;
            if (isCustomizablePreset) {
                this.worldType = newPreset;
            } else {
                this.setWorldType(newPreset);
            }
        }
    }

    private static Optional<Holder<WorldPreset>> findPreset(WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset) {
        return preset.flatMap(k -> settings.worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).get((ResourceKey)k));
    }

    private static Optional<List<WorldTypeEntry>> getNonEmptyList(Registry<WorldPreset> presetRegistry, TagKey<WorldPreset> id) {
        return presetRegistry.get(id).map(tag -> tag.stream().map(WorldTypeEntry::new).toList()).filter(l -> !l.isEmpty());
    }

    public void setGameRules(GameRules gameRules) {
        this.gameRules = gameRules;
        this.onChanged();
    }

    public GameRules getGameRules() {
        return this.gameRules;
    }

    private static /* synthetic */ List lambda$updatePresetLists$0(Registry presetRegistry) {
        return presetRegistry.listElements().map(WorldTypeEntry::new).toList();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum SelectedGameMode {
        SURVIVAL("survival", GameType.SURVIVAL),
        HARDCORE("hardcore", GameType.SURVIVAL),
        CREATIVE("creative", GameType.CREATIVE),
        DEBUG("spectator", GameType.SPECTATOR);

        public final GameType gameType;
        public final Component displayName;
        private final Component info;

        private SelectedGameMode(String name, GameType gameType) {
            this.gameType = gameType;
            this.displayName = Component.translatable("selectWorld.gameMode." + name);
            this.info = Component.translatable("selectWorld.gameMode." + name + ".info");
        }

        public Component getInfo() {
            return this.info;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record WorldTypeEntry(@Nullable Holder<WorldPreset> preset) {
        private static final Component CUSTOM_WORLD_DESCRIPTION = Component.translatable("generator.custom");

        public Component describePreset() {
            return Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).map(key -> Component.translatable(key.identifier().toLanguageKey("generator"))).orElse(CUSTOM_WORLD_DESCRIPTION);
        }

        public boolean isAmplified() {
            return Optional.ofNullable(this.preset).flatMap(Holder::unwrapKey).filter(k -> k.equals(WorldPresets.AMPLIFIED)).isPresent();
        }
    }
}

