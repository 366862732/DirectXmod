/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FenceBlock
extends CrossCollisionBlock {
    public static final MapCodec<FenceBlock> CODEC = FenceBlock.simpleCodec(FenceBlock::new);
    private final Function<BlockState, VoxelShape> occlusionShapes;

    public MapCodec<FenceBlock> codec() {
        return CODEC;
    }

    public FenceBlock(BlockBehaviour.Properties properties) {
        super(4.0f, 16.0f, 4.0f, 16.0f, 24.0f, properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(NORTH, false)).setValue(EAST, false)).setValue(SOUTH, false)).setValue(WEST, false)).setValue(WATERLOGGED, false));
        this.occlusionShapes = this.makeShapes(4.0f, 16.0f, 2.0f, 6.0f, 15.0f);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return this.occlusionShapes.apply(state);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    public boolean connectsTo(BlockState state, boolean faceSolid, Direction direction) {
        Block block = state.getBlock();
        boolean sameFence = this.isSameFence(state);
        boolean gate = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, direction);
        return !FenceBlock.isExceptionForConnection(state) && faceSolid || sameFence || gate;
    }

    private boolean isSameFence(BlockState state) {
        return state.is(BlockTags.FENCES) && state.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return !level.isClientSide() ? LeadItem.bindPlayerMobs(player, level, pos) : InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockPos north = pos.north();
        BlockPos east = pos.east();
        BlockPos south = pos.south();
        BlockPos west = pos.west();
        BlockState northState = level.getBlockState(north);
        BlockState eastState = level.getBlockState(east);
        BlockState southState = level.getBlockState(south);
        BlockState westState = level.getBlockState(west);
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)super.getStateForPlacement(context).setValue(NORTH, this.connectsTo(northState, northState.isFaceSturdy(level, north, Direction.SOUTH), Direction.SOUTH))).setValue(EAST, this.connectsTo(eastState, eastState.isFaceSturdy(level, east, Direction.WEST), Direction.WEST))).setValue(SOUTH, this.connectsTo(southState, southState.isFaceSturdy(level, south, Direction.NORTH), Direction.NORTH))).setValue(WEST, this.connectsTo(westState, westState.isFaceSturdy(level, west, Direction.EAST), Direction.EAST))).setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour.getAxis().isHorizontal()) {
            return (BlockState)state.setValue((Property)PROPERTY_BY_DIRECTION.get(directionToNeighbour), this.connectsTo(neighbourState, neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour.getOpposite()), directionToNeighbour.getOpposite()));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}

