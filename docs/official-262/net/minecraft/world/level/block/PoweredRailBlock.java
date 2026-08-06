/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;

public class PoweredRailBlock
extends BaseRailBlock {
    public static final MapCodec<PoweredRailBlock> CODEC = PoweredRailBlock.simpleCodec(PoweredRailBlock::new);
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE_STRAIGHT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public MapCodec<PoweredRailBlock> codec() {
        return CODEC;
    }

    protected PoweredRailBlock(BlockBehaviour.Properties properties) {
        super(true, properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(SHAPE, RailShape.NORTH_SOUTH)).setValue(POWERED, false)).setValue(WATERLOGGED, false));
    }

    protected boolean findPoweredRailSignal(Level level, BlockPos pos, BlockState state, boolean forward, int searchDepth) {
        if (searchDepth >= 8) {
            return false;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean checkBelow = true;
        RailShape shape = state.getValue(SHAPE);
        switch (shape) {
            case NORTH_SOUTH: {
                if (forward) {
                    ++z;
                    break;
                }
                --z;
                break;
            }
            case EAST_WEST: {
                if (forward) {
                    --x;
                    break;
                }
                ++x;
                break;
            }
            case ASCENDING_EAST: {
                if (forward) {
                    --x;
                } else {
                    ++x;
                    ++y;
                    checkBelow = false;
                }
                shape = RailShape.EAST_WEST;
                break;
            }
            case ASCENDING_WEST: {
                if (forward) {
                    --x;
                    ++y;
                    checkBelow = false;
                } else {
                    ++x;
                }
                shape = RailShape.EAST_WEST;
                break;
            }
            case ASCENDING_NORTH: {
                if (forward) {
                    ++z;
                } else {
                    --z;
                    ++y;
                    checkBelow = false;
                }
                shape = RailShape.NORTH_SOUTH;
                break;
            }
            case ASCENDING_SOUTH: {
                if (forward) {
                    ++z;
                    ++y;
                    checkBelow = false;
                } else {
                    --z;
                }
                shape = RailShape.NORTH_SOUTH;
            }
        }
        if (this.isSameRailWithPower(level, new BlockPos(x, y, z), forward, searchDepth, shape)) {
            return true;
        }
        return checkBelow && this.isSameRailWithPower(level, new BlockPos(x, y - 1, z), forward, searchDepth, shape);
    }

    protected boolean isSameRailWithPower(Level level, BlockPos pos, boolean forward, int searchDepth, RailShape dir) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) {
            return false;
        }
        RailShape myShape = state.getValue(SHAPE);
        if (dir == RailShape.EAST_WEST && (myShape == RailShape.NORTH_SOUTH || myShape == RailShape.ASCENDING_NORTH || myShape == RailShape.ASCENDING_SOUTH)) {
            return false;
        }
        if (dir == RailShape.NORTH_SOUTH && (myShape == RailShape.EAST_WEST || myShape == RailShape.ASCENDING_EAST || myShape == RailShape.ASCENDING_WEST)) {
            return false;
        }
        if (state.getValue(POWERED).booleanValue()) {
            if (level.hasNeighborSignal(pos)) {
                return true;
            }
            return this.findPoweredRailSignal(level, pos, state, forward, searchDepth + 1);
        }
        return false;
    }

    @Override
    protected void updateState(BlockState state, Level level, BlockPos pos, Block block) {
        boolean shouldPower;
        boolean isPowered = state.getValue(POWERED);
        boolean bl = shouldPower = level.hasNeighborSignal(pos) || this.findPoweredRailSignal(level, pos, state, true, 0) || this.findPoweredRailSignal(level, pos, state, false, 0);
        if (shouldPower != isPowered) {
            level.setBlock(pos, (BlockState)state.setValue(POWERED, shouldPower), 3);
            level.updateNeighborsAt(pos.below(), this);
            if (state.getValue(SHAPE).isSlope()) {
                level.updateNeighborsAt(pos.above(), this);
            }
        }
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        RailShape currentShape = state.getValue(SHAPE);
        RailShape newShape = this.rotate(currentShape, rotation);
        return (BlockState)state.setValue(SHAPE, newShape);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        RailShape currentShape = state.getValue(SHAPE);
        RailShape newShape = this.mirror(currentShape, mirror);
        return (BlockState)state.setValue(SHAPE, newShape);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, POWERED, WATERLOGGED);
    }
}

