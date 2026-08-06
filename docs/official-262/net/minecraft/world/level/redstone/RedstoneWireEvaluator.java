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
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public abstract class RedstoneWireEvaluator {
    protected final RedStoneWireBlock wireBlock;

    protected RedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        this.wireBlock = wireBlock;
    }

    public abstract void updatePowerStrength(Level var1, BlockPos var2, BlockState var3, @Nullable Orientation var4, boolean var5);

    protected int getBlockSignal(Level level, BlockPos pos) {
        return this.wireBlock.getBlockSignal(level, pos);
    }

    protected int getWireSignal(BlockPos pos, BlockState state) {
        return state.is(this.wireBlock) ? state.getValue(RedStoneWireBlock.POWER) : 0;
    }

    protected int getIncomingWireSignal(Level level, BlockPos pos) {
        int wireSignal = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            wireSignal = Math.max(wireSignal, this.getWireSignal(neighborPos, neighborState));
            BlockPos abovePos = pos.above();
            if (neighborState.isRedstoneConductor(level, neighborPos) && !level.getBlockState(abovePos).isRedstoneConductor(level, abovePos)) {
                BlockPos aboveNeighborPos = neighborPos.above();
                wireSignal = Math.max(wireSignal, this.getWireSignal(aboveNeighborPos, level.getBlockState(aboveNeighborPos)));
                continue;
            }
            if (neighborState.isRedstoneConductor(level, neighborPos)) continue;
            BlockPos belowNeighborPos = neighborPos.below();
            wireSignal = Math.max(wireSignal, this.getWireSignal(belowNeighborPos, level.getBlockState(belowNeighborPos)));
        }
        return Math.max(0, wireSignal - 1);
    }
}

