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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class SlabBlock
extends Block
implements SimpleWaterloggedBlock {
    public static final MapCodec<SlabBlock> CODEC = SlabBlock.simpleCodec(SlabBlock::new);
    public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE_BOTTOM = Block.column(16.0, 0.0, 8.0);
    private static final VoxelShape SHAPE_TOP = Block.column(16.0, 8.0, 16.0);

    public MapCodec<? extends SlabBlock> codec() {
        return CODEC;
    }

    public SlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM)).setValue(WATERLOGGED, false));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(TYPE) != SlabType.DOUBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(TYPE)) {
            default -> throw new MatchException(null, null);
            case SlabType.TOP -> SHAPE_TOP;
            case SlabType.BOTTOM -> SHAPE_BOTTOM;
            case SlabType.DOUBLE -> Shapes.block();
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState replacedBlockState = context.getLevel().getBlockState(pos);
        if (replacedBlockState.is(this)) {
            return (BlockState)((BlockState)replacedBlockState.setValue(TYPE, SlabType.DOUBLE)).setValue(WATERLOGGED, false);
        }
        FluidState replacedFluidState = context.getLevel().getFluidState(pos);
        BlockState result = (BlockState)((BlockState)this.defaultBlockState().setValue(TYPE, SlabType.BOTTOM)).setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN || clickedFace != Direction.UP && context.getClickLocation().y - (double)pos.getY() > 0.5) {
            return (BlockState)result.setValue(TYPE, SlabType.TOP);
        }
        return result;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        ItemStack itemStack = context.getItemInHand();
        SlabType type = state.getValue(TYPE);
        if (type == SlabType.DOUBLE || !itemStack.is(this.asItem())) {
            return false;
        }
        if (context.replacingClickedOnBlock()) {
            boolean above = context.getClickLocation().y - (double)context.getClickedPos().getY() > 0.5;
            Direction clickedFace = context.getClickedFace();
            if (type == SlabType.BOTTOM) {
                return clickedFace == Direction.UP || above && clickedFace.getAxis().isHorizontal();
            }
            return clickedFace == Direction.DOWN || !above && clickedFace.getAxis().isHorizontal();
        }
        return true;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (state.getValue(TYPE) != SlabType.DOUBLE) {
            return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
        }
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
        if (state.getValue(TYPE) != SlabType.DOUBLE) {
            return SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case PathComputationType.LAND -> false;
            case PathComputationType.WATER -> state.getFluidState().is(FluidTags.WATER);
            case PathComputationType.AIR -> false;
            default -> false;
        };
    }
}

