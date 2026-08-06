/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.yggdrasil.ServicesKeySet
 *  com.mojang.brigadier.StringReader
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Lifecycle
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.gizmos.GizmoCollector;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestServer
extends MinecraftServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROGRESS_REPORT_INTERVAL = 20;
    private static final int TEST_POSITION_RANGE = 14999992;
    private static final Services NO_SERVICES = new Services(null, ServicesKeySet.EMPTY, null, new MockUserNameToIdResolver(), new MockProfileResolver());
    private static final FeatureFlagSet ENABLED_FEATURES = FeatureFlags.REGISTRY.allFlags().subtract(FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS));
    private final LocalSampleLogger sampleLogger = new LocalSampleLogger(4);
    private final Optional<String> testSelection;
    private final boolean verify;
    private final int repeatCount;
    private List<GameTestBatch> testBatches = new ArrayList<GameTestBatch>();
    private final Stopwatch stopwatch = Stopwatch.createUnstarted();
    private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);
    private @Nullable MultipleTestTracker testTracker;

    public static GameTestServer create(Thread serverThread, LevelStorageSource.LevelStorageAccess levelStorageSource, PackRepository packRepository, Optional<String> testSelection, boolean verify, int repeatCount) {
        packRepository.reload();
        ArrayList<String> enabledPacks = new ArrayList<String>(packRepository.getAvailableIds());
        enabledPacks.remove("vanilla");
        enabledPacks.addFirst("vanilla");
        WorldDataConfiguration defaultTestConfig = new WorldDataConfiguration(new DataPackConfig(enabledPacks, List.of()), ENABLED_FEATURES);
        LevelSettings testSettings = new LevelSettings("Test Level", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, defaultTestConfig);
        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, defaultTestConfig, false, true);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, LevelBasedPermissionSet.OWNER);
        try {
            LOGGER.debug("Starting resource loading");
            Stopwatch stopwatch = Stopwatch.createStarted();
            WorldStem worldStem = (WorldStem)Util.blockUntilDone(executor -> WorldLoader.load(initConfig, context -> {
                Registry<LevelStem> noDatapackDimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                WorldDimensions worldDimensions = context.datapackWorldgen().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions();
                WorldDimensions.Complete dimensions = worldDimensions.bake(noDatapackDimensions);
                PrimaryLevelData levelData = new PrimaryLevelData(testSettings, dimensions.specialWorldProperty(), dimensions.lifecycle());
                return new WorldLoader.DataLoadOutput<LevelDataAndDimensions.WorldDataAndGenSettings>(new LevelDataAndDimensions.WorldDataAndGenSettings(levelData, new WorldGenSettings(WORLD_OPTIONS, worldDimensions)), dimensions.dimensionsRegistryAccess());
            }, WorldStem::new, Util.backgroundExecutor(), executor)).get();
            stopwatch.stop();
            LOGGER.debug("Finished resource loading after {} ms", (Object)stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return new GameTestServer(serverThread, levelStorageSource, packRepository, worldStem, testSelection, verify, repeatCount);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to load vanilla datapack, bit oops", (Throwable)e);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    private GameTestServer(Thread serverThread, LevelStorageSource.LevelStorageAccess levelStorageSource, PackRepository packRepository, WorldStem worldStem, Optional<String> testSelection, boolean verify, int repeatCount) {
        super(serverThread, levelStorageSource, packRepository, worldStem, Optional.of(new GameRules(ENABLED_FEATURES)), Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, LoggingLevelLoadListener.forDedicatedServer(), false, new NotificationManager());
        this.testSelection = testSelection;
        this.repeatCount = repeatCount;
        this.verify = verify;
    }

    @Override
    protected boolean initServer() {
        this.setPlayerList(new PlayerList(this, this, this.registries(), this.playerDataStorage, new EmptyNotificationService()){
            {
                Objects.requireNonNull(this$0);
                super(server, registries, playerIo, notificationService);
            }
        });
        Gizmos.withCollector(GizmoCollector.NOOP);
        this.loadLevel();
        ServerLevel level = this.overworld();
        this.testBatches = this.evaluateTestsToRun(level);
        LOGGER.info("Started game test server");
        return true;
    }

    private List<GameTestBatch> evaluateTestsToRun(ServerLevel level) {
        GameTestBatchFactory.TestDecorator decorator;
        List<Holder.Reference<GameTestInstance>> tests;
        HolderLookup.RegistryLookup testRegistry = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
        if (this.testSelection.isPresent()) {
            tests = GameTestServer.getTestsForSelection(level.registryAccess(), this.testSelection.get()).filter(test -> !((GameTestInstance)test.value()).manualOnly()).toList();
            if (tests.isEmpty()) {
                LOGGER.warn("Test selection matcher ({}) found no tests", (Object)this.testSelection.get());
                System.exit(-1);
            }
            if (this.verify) {
                decorator = GameTestServer::rotateAndMultiply;
                LOGGER.info("Verify requested. Will run each test that matches {} {} times", (Object)this.testSelection.get(), (Object)(100 * Rotation.values().length));
            } else if (this.repeatCount > 0) {
                decorator = this::multiplyTest;
                LOGGER.info("Each test that matches {} will be run {} times (total: {})", new Object[]{this.testSelection.get(), this.repeatCount, tests.size() * this.repeatCount});
            } else {
                decorator = GameTestBatchFactory.DIRECT;
                LOGGER.info("Will run tests matching {} ({} tests)", (Object)this.testSelection.get(), (Object)tests.size());
            }
        } else {
            tests = testRegistry.listElements().filter(test -> !((GameTestInstance)test.value()).manualOnly()).toList();
            decorator = GameTestBatchFactory.DIRECT;
        }
        return GameTestBatchFactory.divideIntoBatches(tests, decorator, level);
    }

    private static Stream<GameTestInfo> rotateAndMultiply(Holder.Reference<GameTestInstance> test, ServerLevel level) {
        Stream.Builder<GameTestInfo> builder = Stream.builder();
        for (Rotation rotation : Rotation.values()) {
            for (int i = 0; i < 100; ++i) {
                builder.add(new GameTestInfo(test, rotation, level, RetryOptions.noRetries()));
            }
        }
        return builder.build();
    }

    public static Stream<Holder.Reference<GameTestInstance>> getTestsForSelection(RegistryAccess registries, String selection) {
        return ResourceSelectorArgument.parse(new StringReader(selection), registries.lookupOrThrow(Registries.TEST_INSTANCE)).stream();
    }

    private Stream<GameTestInfo> multiplyTest(Holder.Reference<GameTestInstance> test, ServerLevel level) {
        Stream.Builder<GameTestInfo> builder = Stream.builder();
        for (int i = 0; i < this.repeatCount; ++i) {
            builder.add(new GameTestInfo(test, Rotation.NONE, level, RetryOptions.noRetries()));
        }
        return builder.build();
    }

    @Override
    protected void tickServer(BooleanSupplier haveTime) {
        super.tickServer(haveTime);
        ServerLevel level = this.overworld();
        if (!this.haveTestsStarted()) {
            this.startTests(level);
        }
        if (level.getGameTime() % 20L == 0L) {
            LOGGER.info(this.testTracker.getProgressBar());
        }
        if (this.testTracker.isDone()) {
            this.halt(false);
            LOGGER.info(this.testTracker.getProgressBar());
            GlobalTestReporter.finish();
            LOGGER.info("========= {} GAME TESTS COMPLETE IN {} ======================", (Object)this.testTracker.getTotalCount(), (Object)this.stopwatch.stop());
            if (this.testTracker.hasFailedRequired()) {
                LOGGER.info("{} required tests failed :(", (Object)this.testTracker.getFailedRequiredCount());
                this.testTracker.getFailedRequired().forEach(GameTestServer::logFailedTest);
            } else {
                LOGGER.info("All {} required tests passed :)", (Object)this.testTracker.getTotalCount());
            }
            if (this.testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", (Object)this.testTracker.getFailedOptionalCount());
                this.testTracker.getFailedOptional().forEach(GameTestServer::logFailedTest);
            }
            LOGGER.info("====================================================");
        }
    }

    private static void logFailedTest(GameTestInfo testInfo) {
        if (testInfo.getRotation() != Rotation.NONE) {
            LOGGER.info("   - {} with rotation {}: {}", new Object[]{testInfo.id(), testInfo.getRotation().getSerializedName(), testInfo.getError().getDescription().getString()});
        } else {
            LOGGER.info("   - {}: {}", (Object)testInfo.id(), (Object)testInfo.getError().getDescription().getString());
        }
    }

    @Override
    protected SampleLogger getTickTimeLogger() {
        return this.sampleLogger;
    }

    @Override
    public boolean isTickTimeLoggingEnabled() {
        return false;
    }

    @Override
    protected void waitUntilNextTick() {
        this.runAllTasks();
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport systemReport) {
        systemReport.setDetail("Type", "Game test server");
        return systemReport;
    }

    @Override
    protected void onServerExit() {
        super.onServerExit();
        LOGGER.info("Game test server shutting down");
        System.exit(this.testTracker != null ? this.testTracker.getFailedRequiredCount() : -1);
    }

    @Override
    protected void onServerCrash(CrashReport report) {
        super.onServerCrash(report);
        LOGGER.error("Game test server crashed\n{}", (Object)report.getFriendlyReport(ReportType.CRASH));
        System.exit(1);
    }

    private void startTests(ServerLevel level) {
        RandomSource random = level.getRandom();
        BlockPos startPos = new BlockPos(random.nextIntBetweenInclusive(-14999992, 14999992), -59, random.nextIntBetweenInclusive(-14999992, 14999992));
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), startPos, 0.0f, 0.0f));
        GameTestRunner runner = GameTestRunner.Builder.fromBatches(this.testBatches, level).newStructureSpawner(new StructureGridSpawner(startPos, 8, false)).build();
        List<GameTestInfo> testInfos = runner.getTestInfos();
        this.testTracker = new MultipleTestTracker(testInfos);
        LOGGER.info("{} tests are now running at position {}!", (Object)this.testTracker.getTotalCount(), (Object)startPos.toShortString());
        this.stopwatch.reset();
        this.stopwatch.start();
        runner.start();
    }

    private boolean haveTestsStarted() {
        return this.testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public LevelBasedPermissionSet operatorUserPermissions() {
        return LevelBasedPermissionSet.ALL;
    }

    @Override
    public PermissionSet getFunctionCompilationPermissions() {
        return LevelBasedPermissionSet.OWNER;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public int getCommandSpamThresholdSeconds() {
        return 0;
    }

    @Override
    public int getChatSpamThresholdSeconds() {
        return 0;
    }

    @Override
    public boolean useNativeTransport() {
        return false;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(NameAndId nameAndId) {
        return false;
    }

    @Override
    public int getMaxPlayers() {
        return 1;
    }

    private static class MockUserNameToIdResolver
    implements UserNameToIdResolver {
        private final Set<NameAndId> savedIds = new HashSet<NameAndId>();

        private MockUserNameToIdResolver() {
        }

        @Override
        public void add(NameAndId nameAndId) {
            this.savedIds.add(nameAndId);
        }

        @Override
        public Optional<NameAndId> get(String name) {
            return this.savedIds.stream().filter(e -> e.name().equals(name)).findFirst().or(() -> Optional.of(NameAndId.createOffline(name)));
        }

        @Override
        public Optional<NameAndId> get(UUID id) {
            return this.savedIds.stream().filter(e -> e.id().equals(id)).findFirst();
        }

        @Override
        public void resolveOfflineUsers(boolean value) {
        }

        @Override
        public void save() {
        }
    }

    private static class MockProfileResolver
    implements ProfileResolver {
        private MockProfileResolver() {
        }

        @Override
        public Optional<GameProfile> fetchByName(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<GameProfile> fetchById(UUID id) {
            return Optional.empty();
        }
    }
}

