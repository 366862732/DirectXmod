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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantHeadBlock
extends GrowingPlantBlock
implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_25;
    public static final int MAX_AGE = 25;
    private final double growPerTickProbability;

    protected GrowingPlantHeadBlock(BlockBehaviour.Properties properties, Direction growthDirection, VoxelShape shape, boolean scheduleFluidTicks, double growPerTickProbability) {
        super(properties, growthDirection, shape, scheduleFluidTicks);
        this.growPerTickProbability = growPerTickProbability;
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0));
    }

    protected abstract MapCodec<? extends GrowingPlantHeadBlock> codec();

    @Override
    public BlockState getStateForPlacement(RandomSource random) {
        return (BlockState)this.defaultBlockState().setValue(AGE, random.nextInt(25));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 25;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos growthPos;
        if (state.getValue(AGE) < 25 && random.nextDouble() < this.growPerTickProbability && this.canGrowInto(level.getBlockState(growthPos = pos.relative(this.growthDirection)))) {
            level.setBlockAndUpdate(growthPos, this.getGrowIntoState(state, level.getRandom()));
        }
    }

    protected BlockState getGrowIntoState(BlockState growFromState, RandomSource random) {
        return (BlockState)growFromState.cycle(AGE);
    }

    public BlockState getMaxAgeState(BlockState fromState) {
        return (BlockState)fromState.setValue(AGE, 25);
    }

    public boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) == 25;
    }

    protected BlockState updateBodyAfterConvertedFromHead(BlockState headState, BlockState bodyState) {
        return bodyState;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == this.growthDirection.getOpposite()) {
            if (!state.canSurvive(level, pos)) {
                ticks.scheduleTick(pos, this, 1);
            } else {
                BlockState neighborInGrowthDirection = level.getBlockState(pos.relative(this.growthDirection));
                if (neighborInGrowthDirection.is(this) || neighborInGrowthDirection.is(this.getBodyBlock())) {
                    return this.updateBodyAfterConvertedFromHead(state, this.getBodyBlock().defaultBlockState());
                }
            }
        }
        if (directionToNeighbour == this.growthDirection && (neighbourState.is(this) || neighbourState.is(this.getBodyBlock()))) {
            return this.updateBodyAfterConvertedFromHead(state, this.getBodyBlock().defaultBlockState());
        }
        if (this.scheduleFluidTicks) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos growthPos = pos.relative(this.growthDirection);
        return this.canGrowInto(level.getBlockState(growthPos)) && level.isInsideBuildHeight(growthPos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos forwardPos = pos.relative(this.growthDirection);
        int nextAge = Math.min(state.getValue(AGE) + 1, 25);
        int blocksToGrow = this.getBlocksToGrowWhenBonemealed(random);
        for (int i = 0; i < blocksToGrow && this.canGrowInto(level.getBlockState(forwardPos)) && !level.isOutsideBuildHeight(forwardPos); ++i) {
            level.setBlockAndUpdate(forwardPos, (BlockState)state.setValue(AGE, nextAge));
            forwardPos = forwardPos.relative(this.growthDirection);
            nextAge = Math.min(nextAge + 1, 25);
        }
    }

    protected abstract int getBlocksToGrowWhenBonemealed(RandomSource var1);

    protected abstract boolean canGrowInto(BlockState var1);

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return this;
    }
}

