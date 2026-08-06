/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.redstone;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CollectingNeighborUpdater
implements NeighborUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Level level;
    private final int maxChainedNeighborUpdates;
    private final ArrayDeque<NeighborUpdates> stack = new ArrayDeque();
    private final List<NeighborUpdates> addedThisLayer = new ArrayList<NeighborUpdates>();
    private int count = 0;
    private @Nullable Consumer<BlockPos> debugListener;

    public CollectingNeighborUpdater(Level level, int maxChainedNeighborUpdates) {
        this.level = level;
        this.maxChainedNeighborUpdates = maxChainedNeighborUpdates;
    }

    public void setDebugListener(@Nullable Consumer<BlockPos> debugListener) {
        this.debugListener = debugListener;
    }

    @Override
    public void shapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit) {
        this.addAndRun(pos, new ShapeUpdate(direction, neighborState, pos.immutable(), neighborPos.immutable(), updateFlags, updateLimit));
    }

    @Override
    public void neighborChanged(BlockPos pos, Block block, @Nullable Orientation orientation) {
        this.addAndRun(pos, new SimpleNeighborUpdate(pos, block, orientation));
    }

    @Override
    public void neighborChanged(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        this.addAndRun(pos, new FullNeighborUpdate(state, pos.immutable(), block, orientation, movedByPiston));
    }

    @Override
    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, @Nullable Direction skipDirection, @Nullable Orientation orientation) {
        this.addAndRun(pos, new MultiNeighborUpdate(pos.immutable(), block, orientation, skipDirection));
    }

    private void addAndRun(BlockPos pos, NeighborUpdates update) {
        boolean runningAlready = this.count > 0;
        boolean tooManyUpdates = this.maxChainedNeighborUpdates >= 0 && this.count >= this.maxChainedNeighborUpdates;
        ++this.count;
        if (!tooManyUpdates) {
            if (runningAlready) {
                this.addedThisLayer.add(update);
            } else {
                this.stack.push(update);
            }
        } else if (this.count - 1 == this.maxChainedNeighborUpdates) {
            LOGGER.error("Too many chained neighbor updates. Skipping the rest. First skipped position: {}", (Object)pos.toShortString());
        }
        if (!runningAlready) {
            this.runUpdates();
        }
    }

    private void runUpdates() {
        try {
            block3: while (!this.stack.isEmpty() || !this.addedThisLayer.isEmpty()) {
                for (int i = this.addedThisLayer.size() - 1; i >= 0; --i) {
                    this.stack.push(this.addedThisLayer.get(i));
                }
                this.addedThisLayer.clear();
                NeighborUpdates nextUpdates = this.stack.peek();
                if (this.debugListener != null) {
                    nextUpdates.forEachUpdatedPos(this.debugListener);
                }
                while (this.addedThisLayer.isEmpty()) {
                    if (nextUpdates.runNext(this.level)) continue;
                    this.stack.pop();
                    continue block3;
                }
            }
        }
        finally {
            this.stack.clear();
            this.addedThisLayer.clear();
            this.count = 0;
        }
    }

    private record ShapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit) implements NeighborUpdates
    {
        @Override
        public boolean runNext(Level level) {
            NeighborUpdater.executeShapeUpdate(level, this.direction, this.pos, this.neighborPos, this.neighborState, this.updateFlags, this.updateLimit);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> output) {
            output.accept(this.pos);
        }
    }

    private static interface NeighborUpdates {
        public boolean runNext(Level var1);

        public void forEachUpdatedPos(Consumer<BlockPos> var1);
    }

    private record SimpleNeighborUpdate(BlockPos pos, Block block, @Nullable Orientation orientation) implements NeighborUpdates
    {
        @Override
        public boolean runNext(Level level) {
            BlockState state = level.getBlockState(this.pos);
            NeighborUpdater.executeUpdate(level, state, this.pos, this.block, this.orientation, false);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> output) {
            output.accept(this.pos);
        }
    }

    private record FullNeighborUpdate(BlockState state, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) implements NeighborUpdates
    {
        @Override
        public boolean runNext(Level level) {
            NeighborUpdater.executeUpdate(level, this.state, this.pos, this.block, this.orientation, this.movedByPiston);
            return false;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> output) {
            output.accept(this.pos);
        }
    }

    private static final class MultiNeighborUpdate
    implements NeighborUpdates {
        private final BlockPos sourcePos;
        private final Block sourceBlock;
        private @Nullable Orientation orientation;
        private final @Nullable Direction skipDirection;
        private int idx = 0;

        public MultiNeighborUpdate(BlockPos sourcePos, Block sourceBlock, @Nullable Orientation orientation, @Nullable Direction skipDirection) {
            this.sourcePos = sourcePos;
            this.sourceBlock = sourceBlock;
            this.orientation = orientation;
            this.skipDirection = skipDirection;
            if (NeighborUpdater.UPDATE_ORDER[this.idx] == skipDirection) {
                ++this.idx;
            }
        }

        @Override
        public boolean runNext(Level level) {
            Direction direction = NeighborUpdater.UPDATE_ORDER[this.idx++];
            BlockPos neighborPos = this.sourcePos.relative(direction);
            BlockState state = level.getBlockState(neighborPos);
            Orientation orientation = null;
            if (level.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
                if (this.orientation == null) {
                    this.orientation = ExperimentalRedstoneUtils.initialOrientation(level, this.skipDirection == null ? null : this.skipDirection.getOpposite(), null);
                }
                orientation = this.orientation.withFront(direction);
            }
            NeighborUpdater.executeUpdate(level, state, neighborPos, this.sourceBlock, orientation, false);
            if (this.idx < NeighborUpdater.UPDATE_ORDER.length && NeighborUpdater.UPDATE_ORDER[this.idx] == this.skipDirection) {
                ++this.idx;
            }
            return this.idx < NeighborUpdater.UPDATE_ORDER.length;
        }

        @Override
        public void forEachUpdatedPos(Consumer<BlockPos> output) {
            for (Direction direction : NeighborUpdater.UPDATE_ORDER) {
                if (direction == this.skipDirection) continue;
                BlockPos neighborPos = this.sourcePos.relative(direction);
                output.accept(neighborPos);
            }
        }
    }
}

