/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class BuddingAmethystBlock
extends AmethystBlock {
    public static final MapCodec<BuddingAmethystBlock> CODEC = BuddingAmethystBlock.simpleCodec(BuddingAmethystBlock::new);
    public static final int GROWTH_CHANCE = 5;
    private static final Direction[] DIRECTIONS = Direction.values();

    public MapCodec<BuddingAmethystBlock> codec() {
        return CODEC;
    }

    public BuddingAmethystBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction growDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos growPos = pos.relative(growDirection);
        BlockState relativeState = level.getBlockState(growPos);
        Block nextStage = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(relativeState)) {
            nextStage = Blocks.SMALL_AMETHYST_BUD;
        } else if (relativeState.is(Blocks.SMALL_AMETHYST_BUD) && relativeState.getValue(AmethystClusterBlock.FACING) == growDirection) {
            nextStage = Blocks.MEDIUM_AMETHYST_BUD;
        } else if (relativeState.is(Blocks.MEDIUM_AMETHYST_BUD) && relativeState.getValue(AmethystClusterBlock.FACING) == growDirection) {
            nextStage = Blocks.LARGE_AMETHYST_BUD;
        } else if (relativeState.is(Blocks.LARGE_AMETHYST_BUD) && relativeState.getValue(AmethystClusterBlock.FACING) == growDirection) {
            nextStage = Blocks.AMETHYST_CLUSTER;
        }
        if (nextStage != null) {
            BlockState targetState = (BlockState)((BlockState)nextStage.defaultBlockState().setValue(AmethystClusterBlock.FACING, growDirection)).setValue(AmethystClusterBlock.WATERLOGGED, relativeState.getFluidState().is(Fluids.WATER));
            level.setBlockAndUpdate(growPos, targetState);
        }
    }

    public static boolean canClusterGrowAtState(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isFull();
    }
}

