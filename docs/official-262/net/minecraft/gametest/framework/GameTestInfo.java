/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Stopwatch
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.objects.Object2LongMap
 *  it.unimi.dsi.fastutil.objects.Object2LongMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.GameTestTimeoutException;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.UnknownGameTestException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.LevelTicks;
import org.jspecify.annotations.Nullable;

public class GameTestInfo {
    private final Holder.Reference<GameTestInstance> test;
    private @Nullable BlockPos testBlockPos;
    private final ServerLevel level;
    private final Collection<GameTestListener> listeners = Lists.newArrayList();
    private final int timeoutTicks;
    private final Collection<GameTestSequence> sequences = Lists.newCopyOnWriteArrayList();
    private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap();
    private boolean placedStructure;
    private boolean chunksLoaded;
    private int tickCount;
    private boolean started;
    private final RetryOptions retryOptions;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private boolean done;
    private final Rotation extraRotation;
    private @Nullable GameTestException error;
    private @Nullable TestInstanceBlockEntity testInstanceBlockEntity;

    public GameTestInfo(Holder.Reference<GameTestInstance> test, Rotation extraRotation, ServerLevel level, RetryOptions retryOptions) {
        this.test = test;
        this.level = level;
        this.retryOptions = retryOptions;
        this.timeoutTicks = test.value().maxTicks();
        this.extraRotation = extraRotation;
    }

    public void setTestBlockPos(@Nullable BlockPos testBlockPos) {
        this.testBlockPos = testBlockPos;
    }

    public GameTestInfo startExecution(int tickDelay) {
        this.tickCount = -(this.test.value().setupTicks() + tickDelay + 1);
        return this;
    }

    public void placeStructure() {
        if (this.placedStructure) {
            return;
        }
        TestInstanceBlockEntity test = this.getTestInstanceBlockEntity();
        if (!test.placeStructure()) {
            this.fail(Component.translatable("test.error.structure.failure", test.getTestName().getString()));
        }
        this.placedStructure = true;
        test.encaseStructure();
        BoundingBox boundingBox = test.getTestBoundingBox();
        ((LevelTicks)this.level.getBlockTicks()).clearArea(boundingBox);
        this.level.clearBlockEvents(boundingBox);
        this.listeners.forEach(listener -> listener.testStructureLoaded(this));
    }

    public void tick(GameTestRunner runner) {
        if (this.isDone()) {
            return;
        }
        if (!this.placedStructure) {
            this.fail(Component.translatable("test.error.ticking_without_structure"));
        }
        if (this.testInstanceBlockEntity == null) {
            this.fail(Component.translatable("test.error.missing_block_entity"));
        }
        if (this.error != null) {
            this.finish();
        } else {
            if (!this.chunksLoaded) {
                if (!this.testInstanceBlockEntity.getStructureBoundingBox().intersectingChunks().allMatch(this.level::areEntitiesActuallyLoadedAndTicking)) {
                    return;
                }
            }
            this.chunksLoaded = true;
        }
        this.tickInternal();
        if (this.isDone()) {
            if (this.error != null) {
                this.listeners.forEach(listener -> listener.testFailed(this, runner));
            } else {
                this.listeners.forEach(listener -> listener.testPassed(this, runner));
            }
        }
    }

    private void tickInternal() {
        ++this.tickCount;
        if (this.tickCount < 0) {
            return;
        }
        if (!this.started) {
            this.startTest();
        }
        ObjectIterator it = this.runAtTickTimeMap.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            Object2LongMap.Entry entry = (Object2LongMap.Entry)it.next();
            if (entry.getLongValue() > (long)this.tickCount) continue;
            try {
                ((Runnable)entry.getKey()).run();
            }
            catch (GameTestException error) {
                this.fail(error);
            }
            catch (Exception exception) {
                this.fail(new UnknownGameTestException(exception));
            }
            it.remove();
        }
        if (this.tickCount > this.timeoutTicks) {
            if (this.sequences.isEmpty()) {
                this.fail(new GameTestTimeoutException(Component.translatable("test.error.timeout.no_result", this.test.value().maxTicks())));
            } else {
                this.sequences.forEach(ticker -> ticker.tickAndFailIfNotComplete(this.tickCount));
                if (this.error == null) {
                    this.fail(new GameTestTimeoutException(Component.translatable("test.error.timeout.no_sequences_finished", this.test.value().maxTicks())));
                }
            }
        } else {
            this.sequences.forEach(ticker -> ticker.tickAndContinue(this.tickCount));
        }
    }

    private void startTest() {
        if (this.started) {
            return;
        }
        this.started = true;
        this.timer.start();
        this.getTestInstanceBlockEntity().setRunning();
        try {
            this.test.value().run(new GameTestHelper(this));
        }
        catch (GameTestException e) {
            this.fail(e);
        }
        catch (Exception e) {
            this.fail(new UnknownGameTestException(e));
        }
    }

    public void setRunAtTickTime(long time, Runnable assertAtTickTime) {
        this.runAtTickTimeMap.put((Object)assertAtTickTime, time);
    }

    public Identifier id() {
        return this.test.key().identifier();
    }

    public @Nullable BlockPos getTestBlockPos() {
        return this.testBlockPos;
    }

    public BlockPos getTestOrigin() {
        return this.testInstanceBlockEntity.getStartCorner();
    }

    public AABB getStructureBounds() {
        TestInstanceBlockEntity blockEntity = this.getTestInstanceBlockEntity();
        return blockEntity.getStructureBounds();
    }

    public TestInstanceBlockEntity getTestInstanceBlockEntity() {
        if (this.testInstanceBlockEntity == null) {
            if (this.testBlockPos == null) {
                throw new IllegalStateException("This GameTestInfo has no position");
            }
            BlockEntity blockEntity = this.level.getBlockEntity(this.testBlockPos);
            if (blockEntity instanceof TestInstanceBlockEntity) {
                TestInstanceBlockEntity blockEntity2;
                this.testInstanceBlockEntity = blockEntity2 = (TestInstanceBlockEntity)blockEntity;
            }
            if (this.testInstanceBlockEntity == null) {
                throw new IllegalStateException("Could not find a test instance block entity at the given coordinate " + String.valueOf(this.testBlockPos));
            }
        }
        return this.testInstanceBlockEntity;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public boolean hasSucceeded() {
        return this.done && this.error == null;
    }

    public boolean hasFailed() {
        return this.error != null;
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isDone() {
        return this.done;
    }

    public long getRunTime() {
        return this.timer.elapsed(TimeUnit.MILLISECONDS);
    }

    private void finish() {
        if (!this.done) {
            this.done = true;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    public void succeed() {
        if (this.error == null) {
            this.finish();
            AABB bounds = this.getStructureBounds();
            List<Entity> entities = this.getLevel().getEntitiesOfClass(Entity.class, bounds.inflate(1.0), mob -> !(mob instanceof Player));
            entities.forEach(e -> e.remove(Entity.RemovalReason.DISCARDED));
        }
    }

    public void fail(Component message) {
        this.fail(new GameTestAssertException(message, this.tickCount));
    }

    public void fail(GameTestException error) {
        this.error = error;
    }

    public @Nullable GameTestException getError() {
        return this.error;
    }

    public String toString() {
        return this.id().toString();
    }

    public void addListener(GameTestListener listener) {
        this.listeners.add(listener);
    }

    public @Nullable GameTestInfo prepareTestStructure() {
        TestInstanceBlockEntity testInstanceBlock = this.createTestInstanceBlock(Objects.requireNonNull(this.testBlockPos), this.extraRotation, this.level);
        if (testInstanceBlock != null) {
            this.testInstanceBlockEntity = testInstanceBlock;
            this.placeStructure();
            return this;
        }
        return null;
    }

    private @Nullable TestInstanceBlockEntity createTestInstanceBlock(BlockPos testPos, Rotation rotation, ServerLevel level) {
        level.setBlockAndUpdate(testPos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
        BlockEntity blockEntity = level.getBlockEntity(testPos);
        if (blockEntity instanceof TestInstanceBlockEntity) {
            TestInstanceBlockEntity blockEntity2 = (TestInstanceBlockEntity)blockEntity;
            ResourceKey<GameTestInstance> test = this.getTestHolder().key();
            Vec3i size = TestInstanceBlockEntity.getStructureSize(level, test).orElse(new Vec3i(1, 1, 1));
            blockEntity2.set(new TestInstanceBlockEntity.Data(Optional.of(test), size, rotation, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty()));
            return blockEntity2;
        }
        return null;
    }

    public int getTick() {
        return this.tickCount;
    }

    public GameTestSequence createSequence() {
        GameTestSequence sequence = new GameTestSequence(this);
        this.sequences.add(sequence);
        return sequence;
    }

    public boolean isRequired() {
        return this.test.value().required();
    }

    public boolean isOptional() {
        return !this.test.value().required();
    }

    public Identifier getStructure() {
        return this.test.value().structure();
    }

    public Rotation getRotation() {
        return this.test.value().info().rotation().getRotated(this.extraRotation);
    }

    public GameTestInstance getTest() {
        return this.test.value();
    }

    public Holder.Reference<GameTestInstance> getTestHolder() {
        return this.test;
    }

    public int getTimeoutTicks() {
        return this.timeoutTicks;
    }

    public boolean isFlaky() {
        return this.test.value().maxAttempts() > 1;
    }

    public int maxAttempts() {
        return this.test.value().maxAttempts();
    }

    public int requiredSuccesses() {
        return this.test.value().requiredSuccesses();
    }

    public RetryOptions retryOptions() {
        return this.retryOptions;
    }

    public Stream<GameTestListener> getListeners() {
        return this.listeners.stream();
    }

    public GameTestInfo copyReset() {
        GameTestInfo i = new GameTestInfo(this.test, this.extraRotation, this.level, this.retryOptions());
        if (this.testBlockPos != null) {
            i.setTestBlockPos(this.testBlockPos);
        }
        return i;
    }
}

