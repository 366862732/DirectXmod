/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.JsonOps
 *  com.mojang.serialization.Lifecycle
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldCallback;
import net.minecraft.client.gui.screens.worldselection.DataPackReloadCookie;
import net.minecraft.client.gui.screens.worldselection.ExperimentsScreen;
import net.minecraft.client.gui.screens.worldselection.InitialWorldCreationOptions;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.SwitchGrid;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContextMapper;
import net.minecraft.client.gui.screens.worldselection.WorldCreationGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class CreateWorldScreen
extends Screen {
    private static final int GROUP_BOTTOM = 1;
    private static final int TAB_COLUMN_WIDTH = 210;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMP_WORLD_PREFIX = "mcworld-";
    private static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
    private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
    private static final Component EXPERIMENTS_LABEL = Component.translatable("selectWorld.experiments");
    private static final Component ALLOW_COMMANDS_INFO = Component.translatable("selectWorld.allowCommands.info");
    private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
    private static final int HORIZONTAL_BUTTON_SPACING = 10;
    private static final int VERTICAL_BUTTON_SPACING = 8;
    public static final Identifier TAB_HEADER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/tab_header_background.png");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final WorldCreationUiState uiState;
    private final TabManager tabManager;
    private boolean recreated;
    private final DirectoryValidator packValidator;
    private final CreateWorldCallback createWorldCallback;
    private final Runnable onClose;
    private @Nullable Path tempDataPackDir;
    private @Nullable PackRepository tempDataPackRepository;
    private @Nullable MenuTabBar tabNavigationBar;

    public static void openFresh(Minecraft minecraft, Runnable onClose) {
        CreateWorldScreen.openFresh(minecraft, onClose, (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(finalLayers, worldDataAndGenSettings, gameRules));
    }

    public static void openFresh(Minecraft minecraft, Runnable onClose, CreateWorldCallback createWorld) {
        WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration());
        Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings = context -> new WorldGenSettings(WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(context.datapackWorldgen()));
        CreateWorldScreen.openCreateWorldScreen(minecraft, onClose, worldGenSettings, worldCreationContext, WorldPresets.NORMAL, createWorld);
    }

    public static void testWorld(Minecraft minecraft, Runnable onClose) {
        WorldCreationContextMapper worldCreationContext = (managers, registries, cookie) -> new WorldCreationContext(cookie.worldGenSettings().options(), cookie.worldGenSettings().dimensions(), registries, managers, cookie.dataConfiguration(), new InitialWorldCreationOptions(WorldCreationUiState.SelectedGameMode.CREATIVE, new GameRuleMap.Builder().set(GameRules.ADVANCE_TIME, false).set(GameRules.ADVANCE_WEATHER, false).set(GameRules.SPAWN_MOBS, false).build(), null));
        Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings = context -> new WorldGenSettings(WorldOptions.testWorldWithRandomSeed(), WorldPresets.createTestWorldDimensions(context.datapackWorldgen()));
        CreateWorldScreen.openCreateWorldScreen(minecraft, onClose, worldGenSettings, worldCreationContext, WorldPresets.FLAT_ALL_DIMENSIONS, (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(finalLayers, worldDataAndGenSettings, gameRules));
    }

    private static void openCreateWorldScreen(Minecraft minecraft, Runnable onClose, Function<WorldLoader.DataLoadContext, WorldGenSettings> worldGenSettings, WorldCreationContextMapper worldCreationContext, ResourceKey<WorldPreset> worldPreset, CreateWorldCallback createWorld) {
        CreateWorldScreen.queueLoadScreen(minecraft, PREPARING_WORLD_DATA);
        long start = Util.getMillis();
        PackRepository vanillaOnlyPackRepository = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
        WorldDataConfiguration dataConfig = SharedConstants.IS_RUNNING_IN_IDE ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "tests"), List.of()), FeatureFlags.DEFAULT_FLAGS) : WorldDataConfiguration.DEFAULT;
        WorldLoader.InitConfig loadConfig = CreateWorldScreen.createDefaultLoadConfig(vanillaOnlyPackRepository, dataConfig);
        CompletableFuture<WorldCreationContext> loadResult = WorldLoader.load(loadConfig, context -> new WorldLoader.DataLoadOutput<DataPackReloadCookie>(new DataPackReloadCookie((WorldGenSettings)worldGenSettings.apply(context), context.dataConfiguration()), context.datapackDimensions()), (resources, managers, registries, cookie) -> {
            resources.close();
            return worldCreationContext.apply(managers, registries, (DataPackReloadCookie)cookie);
        }, Util.backgroundExecutor(), minecraft);
        minecraft.managedBlock(loadResult::isDone);
        long end = Util.getMillis();
        LOGGER.debug("Resource load for world creation blocked for {} ms", (Object)(end - start));
        minecraft.gui.setScreen(new CreateWorldScreen(minecraft, onClose, loadResult.join(), Optional.of(worldPreset), OptionalLong.empty(), createWorld));
    }

    public static CreateWorldScreen createFromExisting(Minecraft minecraft, Runnable onClose, LevelSettings levelSettings, WorldCreationContext worldCreationContext, @Nullable Path newDataPackDir) {
        CreateWorldScreen result = new CreateWorldScreen(minecraft, onClose, worldCreationContext, WorldPresets.fromSettings(worldCreationContext.selectedDimensions()), OptionalLong.of(worldCreationContext.options().seed()), (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> createWorldScreen.createNewWorld(finalLayers, worldDataAndGenSettings, gameRules));
        result.recreated = true;
        result.uiState.setName(levelSettings.levelName());
        result.uiState.setAllowCommands(levelSettings.allowCommands());
        result.uiState.setDifficulty(levelSettings.difficultySettings().difficulty());
        result.uiState.getGameRules().setAll(worldCreationContext.initialWorldCreationOptions().gameRuleOverwrites(), null);
        if (levelSettings.difficultySettings().hardcore()) {
            result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
        } else if (levelSettings.gameType().isSurvival()) {
            result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
        } else if (levelSettings.gameType().isCreative()) {
            result.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        }
        result.tempDataPackDir = newDataPackDir;
        return result;
    }

    private CreateWorldScreen(Minecraft minecraft, Runnable onClose, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed, CreateWorldCallback createWorldCallback) {
        super(Component.translatable("selectWorld.create"));
        CreateWorldScreen createWorldScreen = this;
        Consumer<AbstractWidget> consumer = x$0 -> createWorldScreen.addRenderableWidget(x$0);
        createWorldScreen = this;
        this.tabManager = new TabManager(consumer, x$0 -> createWorldScreen.removeWidget((GuiEventListener)x$0));
        this.onClose = onClose;
        this.packValidator = minecraft.directoryValidator();
        this.createWorldCallback = createWorldCallback;
        this.uiState = new WorldCreationUiState(minecraft.getLevelSource().getBaseDir(), settings, preset, seed);
    }

    public WorldCreationUiState getUiState() {
        return this.uiState;
    }

    @Override
    protected void init() {
        this.tabNavigationBar = MenuTabBar.builder(this.tabManager, this.width).addTabs(new GameTab(this), new WorldTab(this), new MoreTab(this)).build();
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(Component.translatable("selectWorld.create"), button -> this.onCreate()).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.popScreen()).build());
        this.layout.visitWidgets(button -> {
            button.setTabOrderGroup(1);
            this.addRenderableWidget(button);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.uiState.onChanged();
        this.repositionElements();
    }

    @Override
    protected void setInitialFocus() {
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar == null) {
            return;
        }
        this.tabNavigationBar.arrangeElements(this.width);
        int tabAreaTop = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle tabArea = new ScreenRectangle(0, tabAreaTop, this.width, this.height - this.layout.getFooterHeight() - tabAreaTop);
        this.tabManager.setTabArea(tabArea);
        this.layout.setHeaderHeight(tabAreaTop);
        this.layout.arrangeElements();
    }

    private static void queueLoadScreen(Minecraft minecraft, Component message) {
        minecraft.setScreenAndShow(new GenericMessageScreen(message));
    }

    private void onCreate() {
        GameRules gameRules;
        WorldCreationContext context = this.uiState.getSettings();
        WorldDimensions worldDimensions = context.selectedDimensions();
        WorldDimensions.Complete finalDimensions = worldDimensions.bake(context.datapackDimensions());
        LayeredRegistryAccess<RegistryLayer> finalLayers = context.worldgenRegistries().replaceFrom(RegistryLayer.DIMENSIONS, finalDimensions.dimensionsRegistryAccess());
        FeatureFlagSet enabledFeatures = context.dataConfiguration().enabledFeatures();
        Lifecycle lifecycleFromFeatures = FeatureFlags.isExperimental(enabledFeatures) ? Lifecycle.experimental() : Lifecycle.stable();
        Lifecycle lifecycleFromRegistries = finalLayers.compositeAccess().allRegistriesLifecycle();
        Lifecycle lifecycle = lifecycleFromRegistries.add(lifecycleFromFeatures);
        boolean skipWarning = !this.recreated && lifecycleFromRegistries == Lifecycle.stable();
        boolean isDebug = finalDimensions.specialWorldProperty() == PrimaryLevelData.SpecialWorldProperty.DEBUG;
        LevelSettings levelSettings = this.createLevelSettings(isDebug);
        if (isDebug) {
            gameRules = MinecraftServer.DEFAULT_GAME_RULES.get();
            gameRules.set(GameRules.ADVANCE_TIME, false, null);
        } else {
            gameRules = this.uiState.getGameRules().copy(enabledFeatures);
        }
        PrimaryLevelData worldData = new PrimaryLevelData(levelSettings, finalDimensions.specialWorldProperty(), lifecycle);
        WorldOptions options = this.uiState.getSettings().options();
        WorldGenSettings worldGenSettings = new WorldGenSettings(options, worldDimensions);
        LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings = new LevelDataAndDimensions.WorldDataAndGenSettings(worldData, worldGenSettings);
        WorldOpenFlows.confirmWorldCreation(this.minecraft, this, lifecycle, () -> this.createWorldAndCleanup(finalLayers, worldDataAndGenSettings, Optional.of(gameRules)), skipWarning);
    }

    private void createWorldAndCleanup(LayeredRegistryAccess<RegistryLayer> finalLayers, LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings, Optional<GameRules> gameRules) {
        boolean worldCreationSuccessful = this.createWorldCallback.create(this, finalLayers, worldDataAndGenSettings, gameRules, this.tempDataPackDir);
        this.removeTempDataPackDir();
        if (!worldCreationSuccessful) {
            this.popScreen();
        }
    }

    private boolean createNewWorld(LayeredRegistryAccess<RegistryLayer> finalLayers, LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings, Optional<GameRules> gameRules) {
        String worldFolder = this.uiState.getTargetFolder();
        WorldCreationContext context = this.uiState.getSettings();
        CreateWorldScreen.queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
        Optional<LevelStorageSource.LevelStorageAccess> newWorldAccess = CreateWorldScreen.createNewWorldDirectory(this.minecraft, worldFolder, this.tempDataPackDir);
        if (newWorldAccess.isEmpty()) {
            SystemToast.onPackCopyFailure(this.minecraft, worldFolder);
            return false;
        }
        this.minecraft.createWorldOpenFlows().createLevelFromExistingSettings(newWorldAccess.get(), context.dataPackResources(), finalLayers, worldDataAndGenSettings, gameRules);
        return true;
    }

    private LevelSettings createLevelSettings(boolean isDebug) {
        String name = this.uiState.getName().trim();
        if (isDebug) {
            return new LevelSettings(name, GameType.SPECTATOR, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        }
        return new LevelSettings(name, this.uiState.getGameMode().gameType, new LevelSettings.DifficultySettings(this.uiState.getDifficulty(), this.uiState.isHardcore(), false), this.uiState.isAllowCommands(), this.uiState.getSettings().dataConfiguration());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.tabNavigationBar.keyPressed(event)) {
            return true;
        }
        if (super.keyPressed(event)) {
            return true;
        }
        if (event.isConfirmation()) {
            this.onCreate();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.popScreen();
    }

    public void popScreen() {
        this.onClose.run();
        this.removeTempDataPackDir();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0f, 0.0f, this.width, 2, 32, 2);
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TAB_HEADER_BACKGROUND, 0, 0, 0.0f, 0.0f, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.extractMenuBackground(graphics, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    private @Nullable Path getOrCreateTempDataPackDir() {
        if (this.tempDataPackDir == null) {
            try {
                this.tempDataPackDir = Files.createTempDirectory(TEMP_WORLD_PREFIX, new FileAttribute[0]);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to create temporary dir", (Throwable)e);
                SystemToast.onPackCopyFailure(this.minecraft, this.uiState.getTargetFolder());
                this.popScreen();
            }
        }
        return this.tempDataPackDir;
    }

    private void openExperimentsScreen(WorldDataConfiguration dataConfiguration) {
        Pair<Path, PackRepository> settings = this.getDataPackSelectionSettings(dataConfiguration);
        if (settings != null) {
            this.minecraft.gui.setScreen(new ExperimentsScreen(this, (PackRepository)settings.getSecond(), packRepository -> this.tryApplyNewDataPacks((PackRepository)packRepository, false, this::openExperimentsScreen)));
        }
    }

    private void openDataPackSelectionScreen(WorldDataConfiguration dataConfiguration) {
        Pair<Path, PackRepository> settings = this.getDataPackSelectionSettings(dataConfiguration);
        if (settings != null) {
            this.minecraft.gui.setScreen(new PackSelectionScreen((PackRepository)settings.getSecond(), packRepository -> this.tryApplyNewDataPacks((PackRepository)packRepository, true, this::openDataPackSelectionScreen), (Path)settings.getFirst(), Component.translatable("dataPack.title")));
        }
    }

    private void tryApplyNewDataPacks(PackRepository packRepository, boolean isDataPackScreen, Consumer<WorldDataConfiguration> onAbort) {
        List newDisabled;
        ImmutableList newEnabled = ImmutableList.copyOf(packRepository.getSelectedIds());
        WorldDataConfiguration newConfig = new WorldDataConfiguration(new DataPackConfig((List<String>)newEnabled, newDisabled = (List)packRepository.getAvailableIds().stream().filter(arg_0 -> CreateWorldScreen.lambda$tryApplyNewDataPacks$0((List)newEnabled, arg_0)).collect(ImmutableList.toImmutableList())), this.uiState.getSettings().dataConfiguration().enabledFeatures());
        if (this.uiState.tryUpdateDataConfiguration(newConfig)) {
            this.minecraft.gui.setScreen(this);
            return;
        }
        FeatureFlagSet requestedFeatureFlags = packRepository.getRequestedFeatureFlags();
        if (FeatureFlags.isExperimental(requestedFeatureFlags) && isDataPackScreen) {
            this.minecraft.gui.setScreen(new ConfirmExperimentalFeaturesScreen(packRepository.getSelectedPacks(), accepted -> {
                if (accepted) {
                    this.applyNewPackConfig(packRepository, newConfig, onAbort);
                } else {
                    onAbort.accept(this.uiState.getSettings().dataConfiguration());
                }
            }));
        } else {
            this.applyNewPackConfig(packRepository, newConfig, onAbort);
        }
    }

    private void applyNewPackConfig(PackRepository packRepository, WorldDataConfiguration newConfig, Consumer<WorldDataConfiguration> onAbort) {
        this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("dataPack.validation.working")));
        WorldLoader.InitConfig config = CreateWorldScreen.createDefaultLoadConfig(packRepository, newConfig);
        ((CompletableFuture)((CompletableFuture)WorldLoader.load(config, context -> {
            if (context.datapackWorldgen().lookupOrThrow(Registries.WORLD_PRESET).listElements().findAny().isEmpty()) {
                throw new IllegalStateException("Needs at least one world preset to continue");
            }
            if (context.datapackWorldgen().lookupOrThrow(Registries.BIOME).listElements().findAny().isEmpty()) {
                throw new IllegalStateException("Needs at least one biome continue");
            }
            WorldCreationContext existingContext = this.uiState.getSettings();
            RegistryOps writeOps = existingContext.worldgenLoadContext().createSerializationContext(JsonOps.INSTANCE);
            DataResult encoded = WorldGenSettings.CODEC.encodeStart(writeOps, (Object)new WorldGenSettings(existingContext.options(), existingContext.selectedDimensions())).setLifecycle(Lifecycle.stable());
            RegistryOps readOps = context.datapackWorldgen().createSerializationContext(JsonOps.INSTANCE);
            WorldGenSettings settings = (WorldGenSettings)encoded.flatMap(r -> WorldGenSettings.CODEC.parse(readOps, r)).getOrThrow(error -> new IllegalStateException("Error parsing worldgen settings after loading data packs: " + error));
            return new WorldLoader.DataLoadOutput<DataPackReloadCookie>(new DataPackReloadCookie(settings, context.dataConfiguration()), context.datapackDimensions());
        }, (resources, managers, registries, cookie) -> {
            resources.close();
            return new WorldCreationContext(cookie.worldGenSettings(), registries, managers, cookie.dataConfiguration());
        }, Util.backgroundExecutor(), this.minecraft).thenApply(settings -> {
            settings.validate();
            return settings;
        })).thenAcceptAsync(this.uiState::setSettings, (Executor)this.minecraft)).handleAsync((nothing, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Failed to validate datapack", throwable);
                this.minecraft.gui.setScreen(new ConfirmScreen(retry -> {
                    if (retry) {
                        onAbort.accept(this.uiState.getSettings().dataConfiguration());
                    } else {
                        onAbort.accept(WorldDataConfiguration.DEFAULT);
                    }
                }, Component.translatable("dataPack.validation.failed"), CommonComponents.EMPTY, Component.translatable("dataPack.validation.back"), Component.translatable("dataPack.validation.reset")));
            } else {
                this.minecraft.gui.setScreen(this);
            }
            return null;
        }, (Executor)this.minecraft);
    }

    private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository packRepository, WorldDataConfiguration config) {
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, config, false, true);
        return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER);
    }

    private void removeTempDataPackDir() {
        if (this.tempDataPackDir != null && Files.exists(this.tempDataPackDir, new LinkOption[0])) {
            try (Stream<Path> files = Files.walk(this.tempDataPackDir, new FileVisitOption[0]);){
                files.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    }
                    catch (IOException e) {
                        LOGGER.warn("Failed to remove temporary file {}", path, (Object)e);
                    }
                });
            }
            catch (IOException e) {
                LOGGER.warn("Failed to list temporary dir {}", (Object)this.tempDataPackDir);
            }
        }
        this.tempDataPackDir = null;
    }

    private static void copyBetweenDirs(Path sourceDir, Path targetDir, Path sourcePath) {
        try {
            Util.copyBetweenDirs(sourceDir, targetDir, sourcePath);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", (Object)sourcePath, (Object)targetDir);
            throw new UncheckedIOException(e);
        }
    }

    /*
     * WARNING - bad return control flow
     */
    private static Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory(Minecraft minecraft, String worldFolder, @Nullable Path tempDataPackDir) {
        Optional<LevelStorageSource.LevelStorageAccess> optional;
        block12: {
            LevelStorageSource.LevelStorageAccess access;
            block11: {
                access = minecraft.getLevelSource().createAccess(worldFolder);
                if (tempDataPackDir != null) break block11;
                return Optional.of(access);
            }
            Stream<Path> files = Files.walk(tempDataPackDir, new FileVisitOption[0]);
            try {
                Path targetDir = access.getLevelPath(LevelResource.DATAPACK_DIR);
                FileUtil.createDirectoriesSafe(targetDir);
                files.filter(f -> !f.equals(tempDataPackDir)).forEach(source -> CreateWorldScreen.copyBetweenDirs(tempDataPackDir, targetDir, source));
                optional = Optional.of(access);
                if (files == null) break block12;
            }
            catch (Throwable throwable) {
                try {
                    try {
                        if (files != null) {
                            try {
                                files.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (IOException | UncheckedIOException e) {
                        LOGGER.warn("Failed to copy datapacks to world {}", (Object)worldFolder, (Object)e);
                        access.close();
                    }
                }
                catch (IOException | UncheckedIOException e2) {
                    LOGGER.warn("Failed to create access for {}", (Object)worldFolder, (Object)e2);
                }
            }
            files.close();
        }
        return optional;
        return Optional.empty();
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public static @Nullable Path createTempDataPackDirFromExistingWorld(Path sourcePackDir, Minecraft minecraft) {
        @Nullable MutableObject tempDataPackDir = new MutableObject();
        try (Stream<Path> dataPackContents = Files.walk(sourcePackDir, new FileVisitOption[0]);){
            dataPackContents.filter(p -> !p.equals(sourcePackDir)).forEach(source -> {
                Path targetDir = (Path)tempDataPackDir.get();
                if (targetDir == null) {
                    try {
                        targetDir = Files.createTempDirectory(TEMP_WORLD_PREFIX, new FileAttribute[0]);
                    }
                    catch (IOException e) {
                        LOGGER.warn("Failed to create temporary dir");
                        throw new UncheckedIOException(e);
                    }
                    tempDataPackDir.setValue((Object)targetDir);
                }
                CreateWorldScreen.copyBetweenDirs(sourcePackDir, targetDir, source);
            });
        }
        catch (IOException | UncheckedIOException e) {
            LOGGER.warn("Failed to copy datapacks from world {}", (Object)sourcePackDir, (Object)e);
            SystemToast.onPackCopyFailure(minecraft, sourcePackDir.toString());
            return null;
        }
        return (Path)tempDataPackDir.get();
    }

    private @Nullable Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration dataConfiguration) {
        Path dataPackDir = this.getOrCreateTempDataPackDir();
        if (dataPackDir != null) {
            if (this.tempDataPackRepository == null) {
                this.tempDataPackRepository = ServerPacksSource.createPackRepository(dataPackDir, this.packValidator);
                this.tempDataPackRepository.reload();
            }
            this.tempDataPackRepository.setSelected(dataConfiguration.dataPacks().getEnabled());
            return Pair.of((Object)dataPackDir, (Object)this.tempDataPackRepository);
        }
        return null;
    }

    private static /* synthetic */ boolean lambda$tryApplyNewDataPacks$0(List newEnabled, String id) {
        return !newEnabled.contains(id);
    }

    @Environment(value=EnvType.CLIENT)
    private class GameTab
    extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.game.title");
        private static final Component ALLOW_COMMANDS = Component.translatable("selectWorld.allowCommands");
        private final EditBox nameEdit;

        private GameTab(CreateWorldScreen createWorldScreen) {
            Objects.requireNonNull(createWorldScreen);
            super(TITLE);
            GridLayout.RowHelper helper = this.layout.rowSpacing(8).createRowHelper(1);
            LayoutSettings buttonLayoutSettings = helper.newCellSettings();
            this.nameEdit = new EditBox(createWorldScreen.font, 208, 20, Component.translatable("selectWorld.enterName"));
            this.nameEdit.setValue(createWorldScreen.uiState.getName());
            this.nameEdit.setResponder(createWorldScreen.uiState::setName);
            createWorldScreen.uiState.addListener(uiState -> this.nameEdit.setTooltip(Tooltip.create(Component.translatable("selectWorld.targetFolder", Component.literal(uiState.getTargetFolder()).withStyle(ChatFormatting.ITALIC)))));
            createWorldScreen.setInitialFocus(this.nameEdit);
            helper.addChild(CommonLayouts.labeledElement(createWorldScreen.font, this.nameEdit, NAME_LABEL), helper.newCellSettings().alignHorizontallyCenter());
            CycleButton<WorldCreationUiState.SelectedGameMode> gameModeButton = helper.addChild(CycleButton.builder(selectedGameMode -> selectedGameMode.displayName, createWorldScreen.uiState.getGameMode()).withValues((WorldCreationUiState.SelectedGameMode[])new WorldCreationUiState.SelectedGameMode[]{WorldCreationUiState.SelectedGameMode.SURVIVAL, WorldCreationUiState.SelectedGameMode.HARDCORE, WorldCreationUiState.SelectedGameMode.CREATIVE}).create(0, 0, 210, 20, GAME_MODEL_LABEL, (button, gameMode) -> this$0.uiState.setGameMode((WorldCreationUiState.SelectedGameMode)((Object)gameMode))), buttonLayoutSettings);
            createWorldScreen.uiState.addListener(data -> {
                gameModeButton.setValue(data.getGameMode());
                gameModeButton.active = !data.isDebug();
                gameModeButton.setTooltip(Tooltip.create(data.getGameMode().getInfo()));
            });
            CycleButton<Difficulty> difficultyButton = helper.addChild(CycleButton.builder(Difficulty::getDisplayName, createWorldScreen.uiState.getDifficulty()).withValues((Difficulty[])Difficulty.values()).create(0, 0, 210, 20, Component.translatable("options.difficulty"), (button, value) -> this$0.uiState.setDifficulty((Difficulty)value)), buttonLayoutSettings);
            createWorldScreen.uiState.addListener(d -> {
                difficultyButton.setValue(this$0.uiState.getDifficulty());
                difficultyButton.active = !this$0.uiState.isHardcore();
                difficultyButton.setTooltip(Tooltip.create(this$0.uiState.getDifficulty().getInfo()));
            });
            CycleButton<Boolean> allowCommandsButton = helper.addChild(CycleButton.onOffBuilder(createWorldScreen.uiState.isAllowCommands()).withTooltip(state -> Tooltip.create(ALLOW_COMMANDS_INFO)).create(0, 0, 210, 20, ALLOW_COMMANDS, (b, state) -> this$0.uiState.setAllowCommands((boolean)state)));
            createWorldScreen.uiState.addListener(d -> {
                allowCommandsButton.setValue(this$0.uiState.isAllowCommands());
                allowCommandsButton.active = !this$0.uiState.isDebug() && !this$0.uiState.isHardcore();
            });
            if (!SharedConstants.getCurrentVersion().stable()) {
                helper.addChild(Button.builder(EXPERIMENTS_LABEL, button -> createWorldScreen.openExperimentsScreen(this$0.uiState.getSettings().dataConfiguration())).width(210).build());
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class WorldTab
    extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.world.title");
        private static final Component AMPLIFIED_HELP_TEXT = Component.translatable("generator.minecraft.amplified.info");
        private static final Component GENERATE_STRUCTURES = Component.translatable("selectWorld.mapFeatures");
        private static final Component GENERATE_STRUCTURES_INFO = Component.translatable("selectWorld.mapFeatures.info");
        private static final Component BONUS_CHEST = Component.translatable("selectWorld.bonusItems");
        private static final Component SEED_LABEL = Component.translatable("selectWorld.enterSeed");
        private static final Component SEED_EMPTY_HINT = Component.translatable("selectWorld.seedInfo");
        private static final int WORLD_TAB_WIDTH = 310;
        private final EditBox seedEdit;
        private final Button customizeTypeButton;
        final /* synthetic */ CreateWorldScreen this$0;

        private WorldTab(CreateWorldScreen createWorldScreen) {
            CreateWorldScreen createWorldScreen2 = createWorldScreen;
            Objects.requireNonNull(createWorldScreen2);
            this.this$0 = createWorldScreen2;
            super(TITLE);
            GridLayout.RowHelper helper = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);
            CycleButton<WorldCreationUiState.WorldTypeEntry> typeButton = helper.addChild(CycleButton.builder(WorldCreationUiState.WorldTypeEntry::describePreset, createWorldScreen.uiState.getWorldType()).withValues(this.createWorldTypeValueSupplier()).create(0, 0, 150, 20, Component.translatable("selectWorld.mapType"), (button, newPreset) -> this$0.uiState.setWorldType((WorldCreationUiState.WorldTypeEntry)newPreset)));
            typeButton.setValue(createWorldScreen.uiState.getWorldType());
            createWorldScreen.uiState.addListener(data -> {
                WorldCreationUiState.WorldTypeEntry worldType = data.getWorldType();
                typeButton.setValue(worldType);
                if (worldType.isAmplified()) {
                    typeButton.setTooltip(Tooltip.create(AMPLIFIED_HELP_TEXT));
                } else {
                    typeButton.setTooltip(null);
                }
                typeButton.active = this$0.uiState.getWorldType().preset() != null;
            });
            this.customizeTypeButton = helper.addChild(Button.builder(Component.translatable("selectWorld.customizeType"), b -> this.openPresetEditor()).build());
            createWorldScreen.uiState.addListener(data -> {
                this.customizeTypeButton.active = !data.isDebug() && data.getPresetEditor() != null;
            });
            this.seedEdit = new EditBox(this, createWorldScreen.font, 308, 20, (Component)Component.translatable("selectWorld.enterSeed")){
                {
                    Objects.requireNonNull(this$1);
                    super(font, width, height, narration);
                }

                @Override
                protected MutableComponent createNarrationMessage() {
                    return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(SEED_EMPTY_HINT);
                }
            };
            this.seedEdit.setHint(SEED_EMPTY_HINT);
            this.seedEdit.setValue(createWorldScreen.uiState.getSeed());
            this.seedEdit.setResponder(value -> this$0.uiState.setSeed(this.seedEdit.getValue()));
            helper.addChild(CommonLayouts.labeledElement(createWorldScreen.font, this.seedEdit, SEED_LABEL), 2);
            SwitchGrid.Builder switchGridBuilder = SwitchGrid.builder(310);
            switchGridBuilder.addSwitch(GENERATE_STRUCTURES, createWorldScreen.uiState::isGenerateStructures, createWorldScreen.uiState::setGenerateStructures).withIsActiveCondition(() -> !this$0.uiState.isDebug()).withInfo(GENERATE_STRUCTURES_INFO);
            switchGridBuilder.addSwitch(BONUS_CHEST, createWorldScreen.uiState::isBonusChest, createWorldScreen.uiState::setBonusChest).withIsActiveCondition(() -> !this$0.uiState.isHardcore() && !this$0.uiState.isDebug());
            SwitchGrid switchGrid = switchGridBuilder.build();
            helper.addChild(switchGrid.layout(), 2);
            createWorldScreen.uiState.addListener(d -> switchGrid.refreshStates());
        }

        private void openPresetEditor() {
            PresetEditor editor = this.this$0.uiState.getPresetEditor();
            if (editor != null) {
                ((CreateWorldScreen)this.this$0).minecraft.gui.setScreen(editor.createEditScreen(this.this$0, this.this$0.uiState.getSettings()));
            }
        }

        private CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry> createWorldTypeValueSupplier() {
            return new CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry>(this){
                final /* synthetic */ WorldTab this$1;
                {
                    WorldTab worldTab = this$1;
                    Objects.requireNonNull(worldTab);
                    this.this$1 = worldTab;
                }

                @Override
                public List<WorldCreationUiState.WorldTypeEntry> getSelectedList() {
                    return CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean() ? this.this$1.this$0.uiState.getAltPresetList() : this.this$1.this$0.uiState.getNormalPresetList();
                }

                @Override
                public List<WorldCreationUiState.WorldTypeEntry> getDefaultList() {
                    return this.this$1.this$0.uiState.getNormalPresetList();
                }
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class MoreTab
    extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.more.title");
        private static final Component GAME_RULES_LABEL = Component.translatable("selectWorld.gameRules");
        private static final Component DATA_PACKS_LABEL = Component.translatable("selectWorld.dataPacks");
        final /* synthetic */ CreateWorldScreen this$0;

        private MoreTab(CreateWorldScreen createWorldScreen) {
            CreateWorldScreen createWorldScreen2 = createWorldScreen;
            Objects.requireNonNull(createWorldScreen2);
            this.this$0 = createWorldScreen2;
            super(TITLE);
            GridLayout.RowHelper helper = this.layout.rowSpacing(8).createRowHelper(1);
            helper.addChild(Button.builder(GAME_RULES_LABEL, b -> this.openGameRulesScreen()).width(210).build());
            helper.addChild(Button.builder(EXPERIMENTS_LABEL, b -> createWorldScreen.openExperimentsScreen(this$0.uiState.getSettings().dataConfiguration())).width(210).build());
            helper.addChild(Button.builder(DATA_PACKS_LABEL, b -> createWorldScreen.openDataPackSelectionScreen(this$0.uiState.getSettings().dataConfiguration())).width(210).build());
        }

        private void openGameRulesScreen() {
            ((CreateWorldScreen)this.this$0).minecraft.gui.setScreen(new WorldCreationGameRulesScreen(this.this$0.uiState.getGameRules().copy(this.this$0.uiState.getSettings().dataConfiguration().enabledFeatures()), gameRules -> {
                ((CreateWorldScreen)this.this$0).minecraft.gui.setScreen(this.this$0);
                gameRules.ifPresent(this.this$0.uiState::setGameRules);
            }));
        }
    }
}

