/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BambooStalkBlock
extends Block
implements BonemealableBlock {
    public static final MapCodec<BambooStalkBlock> CODEC = BambooStalkBlock.simpleCodec(BambooStalkBlock::new);
    private static final VoxelShape SHAPE_SMALL = Block.column(6.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_LARGE = Block.column(10.0, 0.0, 16.0);
    private static final VoxelShape SHAPE_COLLISION = Block.column(3.0, 0.0, 16.0);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
    public static final EnumProperty<BambooLeaves> LEAVES = BlockStateProperties.BAMBOO_LEAVES;
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    public static final int MAX_HEIGHT = 16;
    public static final int STAGE_GROWING = 0;
    public static final int STAGE_DONE_GROWING = 1;
    public static final int AGE_THIN_BAMBOO = 0;
    public static final int AGE_THICK_BAMBOO = 1;

    public MapCodec<BambooStalkBlock> codec() {
        return CODEC;
    }

    public BambooStalkBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(AGE, 0)).setValue(LEAVES, BambooLeaves.NONE)).setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, LEAVES, STAGE);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(LEAVES) == BambooLeaves.LARGE ? SHAPE_LARGE : SHAPE_SMALL;
        return shape.move(state.getOffset(pos));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_COLLISION.move(state.getOffset(pos));
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (!fluidState.isEmpty()) {
            return null;
        }
        BlockState belowState = context.getLevel().getBlockState(context.getClickedPos().below());
        if (belowState.is(BlockTags.SUPPORTS_BAMBOO)) {
            if (belowState.is(Blocks.BAMBOO_SAPLING)) {
                return (BlockState)this.defaultBlockState().setValue(AGE, 0);
            }
            if (belowState.is(Blocks.BAMBOO)) {
                int age = belowState.getValue(AGE) > 0 ? 1 : 0;
                return (BlockState)this.defaultBlockState().setValue(AGE, age);
            }
            BlockState aboveState = context.getLevel().getBlockState(context.getClickedPos().above());
            if (aboveState.is(Blocks.BAMBOO)) {
                return (BlockState)this.defaultBlockState().setValue(AGE, aboveState.getValue(AGE));
            }
            return Blocks.BAMBOO_SAPLING.defaultBlockState();
        }
        return null;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(STAGE) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int height;
        if (state.getValue(STAGE) != 0) {
            return;
        }
        if (random.nextInt(3) == 0 && level.isEmptyBlock(pos.above()) && level.getRawBrightness(pos.above(), 0) >= 9 && (height = this.getHeightBelowUpToMax(level, pos) + 1) < 16) {
            this.growBamboo(state, level, pos, random, height);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.SUPPORTS_BAMBOO);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        if (directionToNeighbour == Direction.UP && neighbourState.is(Blocks.BAMBOO) && neighbourState.getValue(AGE) > state.getValue(AGE)) {
            return (BlockState)state.cycle(AGE);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        int heightAbove = this.getHeightAboveUpToMax(level, pos);
        int heightBelow = this.getHeightBelowUpToMax(level, pos);
        BlockPos growthPos = pos.above(heightAbove + 1);
        return heightAbove + heightBelow + 1 < 16 && level.getBlockState(pos.above(heightAbove)).getValue(STAGE) != 1 && level.isInsideBuildHeight(growthPos) && level.isEmptyBlock(growthPos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int heightAbove = this.getHeightAboveUpToMax(level, pos);
        int heightBelow = this.getHeightBelowUpToMax(level, pos);
        int totalHeight = heightAbove + heightBelow + 1;
        int newBamboo = 1 + random.nextInt(2);
        for (int i = 0; i < newBamboo; ++i) {
            BlockPos topPos = pos.above(heightAbove);
            BlockState topState = level.getBlockState(topPos);
            BlockPos growthPos = topPos.above();
            if (totalHeight >= 16 || topState.getValue(STAGE) == 1 || !level.isEmptyBlock(growthPos) || level.isOutsideBuildHeight(growthPos)) {
                return;
            }
            this.growBamboo(topState, level, topPos, random, totalHeight);
            ++heightAbove;
            ++totalHeight;
        }
    }

    protected void growBamboo(BlockState state, Level level, BlockPos pos, RandomSource random, int height) {
        BlockState belowState = level.getBlockState(pos.below());
        BlockPos twoBelowPos = pos.below(2);
        BlockState twoBelowState = level.getBlockState(twoBelowPos);
        BambooLeaves leaves = BambooLeaves.NONE;
        if (height >= 1) {
            if (!belowState.is(Blocks.BAMBOO) || belowState.getValue(LEAVES) == BambooLeaves.NONE) {
                leaves = BambooLeaves.SMALL;
            } else if (belowState.is(Blocks.BAMBOO) && belowState.getValue(LEAVES) != BambooLeaves.NONE) {
                leaves = BambooLeaves.LARGE;
                if (twoBelowState.is(Blocks.BAMBOO)) {
                    level.setBlock(pos.below(), (BlockState)belowState.setValue(LEAVES, BambooLeaves.SMALL), 3);
                    level.setBlock(twoBelowPos, (BlockState)twoBelowState.setValue(LEAVES, BambooLeaves.NONE), 3);
                }
            }
        }
        int age = state.getValue(AGE) == 1 || twoBelowState.is(Blocks.BAMBOO) ? 1 : 0;
        int stage = height >= 11 && random.nextFloat() < 0.25f || height == 15 ? 1 : 0;
        level.setBlock(pos.above(), (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(AGE, age)).setValue(LEAVES, leaves)).setValue(STAGE, stage), 3);
    }

    protected int getHeightAboveUpToMax(BlockGetter level, BlockPos pos) {
        int height;
        for (height = 0; height < 16 && level.getBlockState(pos.above(height + 1)).is(Blocks.BAMBOO); ++height) {
        }
        return height;
    }

    protected int getHeightBelowUpToMax(BlockGetter level, BlockPos pos) {
        int height;
        for (height = 0; height < 16 && level.getBlockState(pos.below(height + 1)).is(Blocks.BAMBOO); ++height) {
        }
        return height;
    }
}

