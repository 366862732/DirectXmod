/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jspecify.annotations.Nullable;

public interface SpawnPlacementTypes {
    public static final SpawnPlacementType NO_RESTRICTIONS = (level, blockPos, type) -> true;
    public static final SpawnPlacementType IN_WATER = (level, blockPos, type) -> {
        if (type == null || !level.getWorldBorder().isWithinBounds(blockPos)) {
            return false;
        }
        BlockPos above = blockPos.above();
        return level.getFluidState(blockPos).is(FluidTags.WATER) && !level.getBlockState(above).isRedstoneConductor(level, above);
    };
    public static final SpawnPlacementType IN_LAVA = (level, blockPos, type) -> {
        if (type == null || !level.getWorldBorder().isWithinBounds(blockPos)) {
            return false;
        }
        return level.getFluidState(blockPos).is(FluidTags.LAVA);
    };
    public static final SpawnPlacementType ON_GROUND = new SpawnPlacementType(){

        @Override
        public boolean isSpawnPositionOk(LevelReader level, BlockPos blockPos, @Nullable EntityType<?> type) {
            if (type == null || !level.getWorldBorder().isWithinBounds(blockPos)) {
                return false;
            }
            BlockPos above = blockPos.above();
            BlockPos below = blockPos.below();
            BlockState belowState = level.getBlockState(below);
            if (!belowState.isValidSpawn(level, below, type)) {
                return false;
            }
            return this.isValidEmptySpawnBlock(level, blockPos, type) && this.isValidEmptySpawnBlock(level, above, type);
        }

        private boolean isValidEmptySpawnBlock(LevelReader level, BlockPos blockPos, EntityType<?> type) {
            BlockState blockState = level.getBlockState(blockPos);
            return NaturalSpawner.isValidEmptySpawnBlock(level, blockPos, blockState, blockState.getFluidState(), type);
        }

        @Override
        public BlockPos adjustSpawnPosition(LevelReader level, BlockPos candidate) {
            BlockPos below = candidate.below();
            if (level.getBlockState(below).isPathfindable(PathComputationType.LAND)) {
                return below;
            }
            return candidate;
        }
    };
}

