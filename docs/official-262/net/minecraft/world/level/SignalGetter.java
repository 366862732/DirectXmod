/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public interface SignalGetter
extends BlockGetter {
    public static final Direction[] DIRECTIONS = Direction.values();

    default public int getDirectSignal(BlockPos pos, Direction direction) {
        return this.getBlockState(pos).getDirectSignal(this, pos, direction);
    }

    default public int getDirectSignalTo(BlockPos pos) {
        int result = 0;
        if ((result = Math.max(result, this.getDirectSignal(pos.below(), Direction.DOWN))) >= 15) {
            return result;
        }
        if ((result = Math.max(result, this.getDirectSignal(pos.above(), Direction.UP))) >= 15) {
            return result;
        }
        if ((result = Math.max(result, this.getDirectSignal(pos.north(), Direction.NORTH))) >= 15) {
            return result;
        }
        if ((result = Math.max(result, this.getDirectSignal(pos.south(), Direction.SOUTH))) >= 15) {
            return result;
        }
        if ((result = Math.max(result, this.getDirectSignal(pos.west(), Direction.WEST))) >= 15) {
            return result;
        }
        if ((result = Math.max(result, this.getDirectSignal(pos.east(), Direction.EAST))) >= 15) {
            return result;
        }
        return result;
    }

    default public int getControlInputSignal(BlockPos pos, Direction direction, boolean onlyDiodes) {
        BlockState blockState = this.getBlockState(pos);
        if (onlyDiodes) {
            return DiodeBlock.isDiode(blockState) ? this.getDirectSignal(pos, direction) : 0;
        }
        if (blockState.is(Blocks.REDSTONE_BLOCK)) {
            return 15;
        }
        if (blockState.is(Blocks.REDSTONE_WIRE)) {
            return blockState.getValue(RedStoneWireBlock.POWER);
        }
        if (blockState.isSignalSource()) {
            return this.getDirectSignal(pos, direction);
        }
        return 0;
    }

    default public boolean hasSignal(BlockPos pos, Direction direction) {
        return this.getSignal(pos, direction) > 0;
    }

    default public int getSignal(BlockPos pos, Direction direction) {
        BlockState state = this.getBlockState(pos);
        int signal = state.getSignal(this, pos, direction);
        if (state.isRedstoneConductor(this, pos)) {
            return Math.max(signal, this.getDirectSignalTo(pos));
        }
        return signal;
    }

    default public int getBestOwnOrNeighbourSignal(BlockPos pos) {
        BlockState blockState = this.getBlockState(pos);
        return Math.max(this.getBestNeighborSignal(pos), blockState.isSignalSource() ? blockState.getOwnSignal(this, pos) : 0);
    }

    default public boolean hasNeighborSignal(BlockPos blockPos) {
        if (this.getSignal(blockPos.below(), Direction.DOWN) > 0) {
            return true;
        }
        if (this.getSignal(blockPos.above(), Direction.UP) > 0) {
            return true;
        }
        if (this.getSignal(blockPos.north(), Direction.NORTH) > 0) {
            return true;
        }
        if (this.getSignal(blockPos.south(), Direction.SOUTH) > 0) {
            return true;
        }
        if (this.getSignal(blockPos.west(), Direction.WEST) > 0) {
            return true;
        }
        return this.getSignal(blockPos.east(), Direction.EAST) > 0;
    }

    default public int getBestNeighborSignal(BlockPos pos) {
        int best = 0;
        for (Direction direction : DIRECTIONS) {
            int signal = this.getSignal(pos.relative(direction), direction);
            if (signal >= 15) {
                return 15;
            }
            if (signal <= best) continue;
            best = signal;
        }
        return best;
    }
}

