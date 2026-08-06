/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class InstantNeighborUpdater
implements NeighborUpdater {
    private final Level level;

    public InstantNeighborUpdater(Level level) {
        this.level = level;
    }

    @Override
    public void shapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, @Block.UpdateFlags int updateFlags, int updateLimit) {
        NeighborUpdater.executeShapeUpdate(this.level, direction, pos, neighborPos, neighborState, updateFlags, updateLimit - 1);
    }

    @Override
    public void neighborChanged(BlockPos pos, Block changedBlock, @Nullable Orientation orientation) {
        BlockState state = this.level.getBlockState(pos);
        this.neighborChanged(state, pos, changedBlock, orientation, false);
    }

    @Override
    public void neighborChanged(BlockState state, BlockPos pos, Block changedBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        NeighborUpdater.executeUpdate(this.level, state, pos, changedBlock, orientation, movedByPiston);
    }
}

