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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ChorusPlantBlock
extends PipeBlock {
    public static final MapCodec<ChorusPlantBlock> CODEC = ChorusPlantBlock.simpleCodec(ChorusPlantBlock::new);

    public MapCodec<ChorusPlantBlock> codec() {
        return CODEC;
    }

    protected ChorusPlantBlock(BlockBehaviour.Properties properties) {
        super(10.0f, properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(NORTH, false)).setValue(EAST, false)).setValue(SOUTH, false)).setValue(WEST, false)).setValue(UP, false)).setValue(DOWN, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return ChorusPlantBlock.getStateWithConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    public static BlockState getStateWithConnections(BlockGetter level, BlockPos pos, BlockState defaultState) {
        BlockState down = level.getBlockState(pos.below());
        BlockState up = level.getBlockState(pos.above());
        BlockState north = level.getBlockState(pos.north());
        BlockState east = level.getBlockState(pos.east());
        BlockState south = level.getBlockState(pos.south());
        BlockState west = level.getBlockState(pos.west());
        Block block = defaultState.getBlock();
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)defaultState.trySetValue(DOWN, down.is(block) || down.is(Blocks.CHORUS_FLOWER) || down.is(BlockTags.SUPPORTS_CHORUS_PLANT))).trySetValue(UP, up.is(block) || up.is(Blocks.CHORUS_FLOWER))).trySetValue(NORTH, north.is(block) || north.is(Blocks.CHORUS_FLOWER))).trySetValue(EAST, east.is(block) || east.is(Blocks.CHORUS_FLOWER))).trySetValue(SOUTH, south.is(block) || south.is(Blocks.CHORUS_FLOWER))).trySetValue(WEST, west.is(block) || west.is(Blocks.CHORUS_FLOWER));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        boolean connect = neighbourState.is(this) || neighbourState.is(Blocks.CHORUS_FLOWER) || directionToNeighbour == Direction.DOWN && neighbourState.is(BlockTags.SUPPORTS_CHORUS_PLANT);
        return (BlockState)state.setValue((Property)PROPERTY_BY_DIRECTION.get(directionToNeighbour), connect);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        boolean blockAboveOrBelow = !level.getBlockState(pos.above()).isAir() && !belowState.isAir();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.is(this)) continue;
            if (blockAboveOrBelow) {
                return false;
            }
            BlockState below = level.getBlockState(neighborPos.below());
            if (!below.is(this) && !below.is(BlockTags.SUPPORTS_CHORUS_PLANT)) continue;
            return true;
        }
        return belowState.is(this) || belowState.is(BlockTags.SUPPORTS_CHORUS_PLANT);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

