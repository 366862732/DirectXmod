/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature
extends Feature<BlockPileConfiguration> {
    public BlockPileFeature(Codec<BlockPileConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockPileConfiguration> context) {
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPileConfiguration config = context.config();
        if (origin.getY() < level.getMinY() + 5) {
            return false;
        }
        int xr = 2 + random.nextInt(2);
        int zr = 2 + random.nextInt(2);
        for (BlockPos blockPos : BlockPos.betweenClosed(origin.offset(-xr, 0, -zr), origin.offset(xr, 1, zr))) {
            int zd;
            int xd = origin.getX() - blockPos.getX();
            if ((float)(xd * xd + (zd = origin.getZ() - blockPos.getZ()) * zd) <= random.nextFloat() * 10.0f - random.nextFloat() * 6.0f) {
                this.tryPlaceBlock(level, blockPos, random, config);
                continue;
            }
            if (!((double)random.nextFloat() < 0.031)) continue;
            this.tryPlaceBlock(level, blockPos, random, config);
        }
        return true;
    }

    private boolean mayPlaceOn(LevelAccessor level, BlockPos blockPos, RandomSource random) {
        BlockPos below = blockPos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.DIRT_PATH)) {
            return random.nextBoolean();
        }
        return belowState.isFaceSturdy(level, below, Direction.UP);
    }

    private void tryPlaceBlock(WorldGenLevel level, BlockPos blockPos, RandomSource random, BlockPileConfiguration config) {
        if (level.isEmptyBlock(blockPos) && this.mayPlaceOn(level, blockPos, random)) {
            level.setBlock(blockPos, config.stateProvider.getState(level, random, blockPos), 260);
        }
    }
}

