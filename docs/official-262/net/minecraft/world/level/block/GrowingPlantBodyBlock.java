/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBodyBlock
extends GrowingPlantBlock
implements BonemealableBlock {
    protected GrowingPlantBodyBlock(BlockBehaviour.Properties properties, Direction growthDirection, VoxelShape shape, boolean scheduleFluidTicks) {
        super(properties, growthDirection, shape, scheduleFluidTicks);
    }

    protected abstract MapCodec<? extends GrowingPlantBodyBlock> codec();

    protected BlockState updateHeadAfterConvertedFromBody(BlockState bodyState, BlockState headState) {
        return headState;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == this.growthDirection.getOpposite() && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        GrowingPlantHeadBlock headBlock = this.getHeadBlock();
        if (directionToNeighbour == this.growthDirection && !neighbourState.is(this) && !neighbourState.is(headBlock)) {
            return this.updateHeadAfterConvertedFromBody(state, headBlock.getStateForPlacement(random));
        }
        if (this.scheduleFluidTicks) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this.getHeadBlock());
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        Optional<BlockPos> headPos = this.getHeadPos(level, pos, state.getBlock());
        if (headPos.isEmpty()) {
            return false;
        }
        BlockPos growthPos = headPos.get().relative(this.growthDirection);
        return this.getHeadBlock().canGrowInto(level.getBlockState(growthPos)) && level.isInsideBuildHeight(growthPos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        Optional<BlockPos> headPos = this.getHeadPos(level, pos, state.getBlock());
        if (headPos.isPresent()) {
            BlockState forwardState = level.getBlockState(headPos.get());
            ((GrowingPlantHeadBlock)forwardState.getBlock()).performBonemeal(level, random, headPos.get(), forwardState);
        }
    }

    private Optional<BlockPos> getHeadPos(BlockGetter level, BlockPos pos, Block bodyBlock) {
        return BlockUtil.getTopConnectedBlock(level, pos, bodyBlock, this.growthDirection, this.getHeadBlock());
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        boolean result = super.canBeReplaced(state, context);
        if (result && context.getItemInHand().is(this.getHeadBlock().asItem())) {
            return false;
        }
        return result;
    }

    @Override
    protected Block getBodyBlock() {
        return this;
    }
}

