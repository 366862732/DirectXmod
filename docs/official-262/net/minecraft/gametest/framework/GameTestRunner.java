/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.LongArraySet
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.gametest.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.GameTestBatchListener;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.gametest.framework.ReportGameListener;
import net.minecraft.gametest.framework.StructureGridSpawner;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestRunner {
    public static final int DEFAULT_TESTS_PER_ROW = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServerLevel level;
    private final GameTestTicker testTicker;
    private final List<GameTestInfo> allTestInfos;
    private ImmutableList<GameTestBatch> batches;
    private final List<GameTestBatchListener> batchListeners = Lists.newArrayList();
    private final List<GameTestInfo> scheduledForRerun = Lists.newArrayList();
    private final GameTestBatcher testBatcher;
    private boolean stopped = true;
    private @Nullable TestEnvironmentDefinition.Activation<?> currentEnvironment;
    private final StructureSpawner existingStructureSpawner;
    private final StructureSpawner newStructureSpawner;
    private final boolean haltOnError;
    private final boolean clearBetweenBatches;

    protected GameTestRunner(GameTestBatcher batcher, Collection<GameTestBatch> batches, ServerLevel level, GameTestTicker testTicker, StructureSpawner existingStructureSpawner, StructureSpawner newStructureSpawner, boolean haltOnError, boolean clearBetweenBatches) {
        this.level = level;
        this.testTicker = testTicker;
        this.testBatcher = batcher;
        this.existingStructureSpawner = existingStructureSpawner;
        this.newStructureSpawner = newStructureSpawner;
        this.batches = ImmutableList.copyOf(batches);
        this.haltOnError = haltOnError;
        this.clearBetweenBatches = clearBetweenBatches;
        this.allTestInfos = this.batches.stream().flatMap(batch -> batch.gameTestInfos().stream()).collect(Util.toMutableList());
        testTicker.setRunner(this);
        this.allTestInfos.forEach(info -> info.addListener(new ReportGameListener()));
    }

    public List<GameTestInfo> getTestInfos() {
        return this.allTestInfos;
    }

    public void start() {
        this.stopped = false;
        this.runBatch(0);
    }

    public void stop() {
        this.stopped = true;
        if (this.currentEnvironment != null) {
            this.endCurrentEnvironment();
        }
    }

    public void rerunTest(GameTestInfo info) {
        GameTestInfo copy = info.copyReset();
        info.getListeners().forEach(listener -> listener.testAddedForRerun(info, copy, this));
        this.allTestInfos.add(copy);
        this.scheduledForRerun.add(copy);
        if (this.stopped) {
            this.runScheduledRerunTests();
        }
    }

    private void runBatch(final int batchIndex) {
        if (batchIndex >= this.batches.size()) {
            this.endCurrentEnvironment();
            this.runScheduledRerunTests();
            return;
        }
        if (batchIndex > 0 && this.clearBetweenBatches) {
            GameTestBatch lastBatch = (GameTestBatch)this.batches.get(batchIndex - 1);
            lastBatch.gameTestInfos().forEach(gameTestInfo -> {
                TestInstanceBlockEntity testInstanceBlockEntity = gameTestInfo.getTestInstanceBlockEntity();
                StructureUtils.clearSpaceForStructure(testInstanceBlockEntity.getTestBoundingBox(), this.level);
                this.level.destroyBlock(testInstanceBlockEntity.getBlockPos(), false);
            });
        }
        final GameTestBatch currentBatch = (GameTestBatch)this.batches.get(batchIndex);
        this.existingStructureSpawner.onBatchStart(this.level);
        this.newStructureSpawner.onBatchStart(this.level);
        Collection<GameTestInfo> testInfosForThisBatch = this.createStructuresForBatch(currentBatch.gameTestInfos());
        LOGGER.info("Running test environment '{}' batch {} ({} tests)...", new Object[]{currentBatch.environment().getRegisteredName(), currentBatch.index(), testInfosForThisBatch.size()});
        this.endCurrentEnvironment();
        this.currentEnvironment = TestEnvironmentDefinition.activate(currentBatch.environment().value(), this.level);
        this.batchListeners.forEach(listener -> listener.testBatchStarting(currentBatch));
        final MultipleTestTracker currentBatchTracker = new MultipleTestTracker();
        testInfosForThisBatch.forEach(currentBatchTracker::addTestToTrack);
        currentBatchTracker.addListener(new GameTestListener(){
            final /* synthetic */ GameTestRunner this$0;
            {
                GameTestRunner gameTestRunner = this$0;
                Objects.requireNonNull(gameTestRunner);
                this.this$0 = gameTestRunner;
            }

            private void testCompleted(GameTestInfo testInfo) {
                if (currentBatchTracker.isDone()) {
                    this.this$0.batchListeners.forEach(listener -> listener.testBatchFinished(currentBatch));
                    LongArraySet forcedChunks = new LongArraySet(this.this$0.level.getForceLoadedChunks());
                    forcedChunks.forEach(pos -> this.this$0.level.setChunkForced(ChunkPos.getX(pos), ChunkPos.getZ(pos), false));
                    this.this$0.runBatch(batchIndex + 1);
                }
            }

            @Override
            public void testStructureLoaded(GameTestInfo testInfo) {
            }

            @Override
            public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
                testInfo.getTestInstanceBlockEntity().removeBarriers();
                this.testCompleted(testInfo);
            }

            @Override
            public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
                if (this.this$0.haltOnError) {
                    this.this$0.endCurrentEnvironment();
                    LongArraySet forcedChunks = new LongArraySet(this.this$0.level.getForceLoadedChunks());
                    forcedChunks.forEach(pos -> this.this$0.level.setChunkForced(ChunkPos.getX(pos), ChunkPos.getZ(pos), false));
                    GameTestTicker.SINGLETON.clear();
                } else {
                    this.testCompleted(testInfo);
                }
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }
        });
        testInfosForThisBatch.forEach(this.testTicker::add);
    }

    private void endCurrentEnvironment() {
        if (this.currentEnvironment != null) {
            this.currentEnvironment.teardown();
            this.currentEnvironment = null;
        }
    }

    private void runScheduledRerunTests() {
        if (!this.scheduledForRerun.isEmpty()) {
            LOGGER.info("Starting re-run of tests: {}", (Object)this.scheduledForRerun.stream().map(info -> info.id().toString()).collect(Collectors.joining(", ")));
            this.batches = ImmutableList.copyOf(this.testBatcher.batch(this.scheduledForRerun));
            this.scheduledForRerun.clear();
            this.stopped = false;
            this.runBatch(0);
        } else {
            this.batches = ImmutableList.of();
            this.stopped = true;
        }
    }

    public void addListener(GameTestBatchListener listener) {
        this.batchListeners.add(listener);
    }

    private Collection<GameTestInfo> createStructuresForBatch(Collection<GameTestInfo> batch) {
        return batch.stream().map(this::spawn).flatMap(Optional::stream).toList();
    }

    private Optional<GameTestInfo> spawn(GameTestInfo testInfo) {
        if (testInfo.getTestBlockPos() == null) {
            return this.newStructureSpawner.spawnStructure(testInfo);
        }
        return this.existingStructureSpawner.spawnStructure(testInfo);
    }

    public static interface GameTestBatcher {
        public Collection<GameTestBatch> batch(Collection<GameTestInfo> var1);
    }

    public static interface StructureSpawner {
        public static final StructureSpawner IN_PLACE = testInfo -> Optional.ofNullable(testInfo.prepareTestStructure()).map(e -> e.startExecution(1));
        public static final StructureSpawner NOT_SET = testInfo -> Optional.empty();

        public Optional<GameTestInfo> spawnStructure(GameTestInfo var1);

        default public void onBatchStart(ServerLevel level) {
        }
    }

    public static class Builder {
        private final ServerLevel level;
        private final GameTestTicker testTicker = GameTestTicker.SINGLETON;
        private GameTestBatcher batcher = GameTestBatchFactory.fromGameTestInfo();
        private StructureSpawner existingStructureSpawner = StructureSpawner.IN_PLACE;
        private StructureSpawner newStructureSpawner = StructureSpawner.NOT_SET;
        private final Collection<GameTestBatch> batches;
        private boolean haltOnError = false;
        private boolean clearBetweenBatches = false;

        private Builder(Collection<GameTestBatch> batches, ServerLevel level) {
            this.batches = batches;
            this.level = level;
        }

        public static Builder fromBatches(Collection<GameTestBatch> batches, ServerLevel level) {
            return new Builder(batches, level);
        }

        public static Builder fromInfo(Collection<GameTestInfo> tests, ServerLevel level) {
            return Builder.fromBatches(GameTestBatchFactory.fromGameTestInfo().batch(tests), level);
        }

        public Builder haltOnError() {
            this.haltOnError = true;
            return this;
        }

        public Builder clearBetweenBatches() {
            this.clearBetweenBatches = true;
            return this;
        }

        public Builder newStructureSpawner(StructureSpawner structureSpawner) {
            this.newStructureSpawner = structureSpawner;
            return this;
        }

        public Builder existingStructureSpawner(StructureGridSpawner spawner) {
            this.existingStructureSpawner = spawner;
            return this;
        }

        public Builder batcher(GameTestBatcher batcher) {
            this.batcher = batcher;
            return this;
        }

        public GameTestRunner build() {
            return new GameTestRunner(this.batcher, this.batches, this.level, this.testTicker, this.existingStructureSpawner, this.newStructureSpawner, this.haltOnError, this.clearBetweenBatches);
        }
    }
}

