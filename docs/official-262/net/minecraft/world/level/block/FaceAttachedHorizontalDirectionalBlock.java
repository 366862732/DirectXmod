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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public abstract class FaceAttachedHorizontalDirectionalBlock
extends HorizontalDirectionalBlock {
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    protected FaceAttachedHorizontalDirectionalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected abstract MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec();

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return FaceAttachedHorizontalDirectionalBlock.canAttach(level, pos, FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state).getOpposite());
    }

    public static boolean canAttach(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos relative = pos.relative(direction);
        return level.getBlockState(relative).isFaceSturdy(level, relative, direction.getOpposite());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState state = direction.getAxis() == Direction.Axis.Y ? (BlockState)((BlockState)this.defaultBlockState().setValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)).setValue(FACING, context.getHorizontalDirection()) : (BlockState)((BlockState)this.defaultBlockState().setValue(FACE, AttachFace.WALL)).setValue(FACING, direction.getOpposite());
            if (!state.canSurvive(context.getLevel(), context.getClickedPos())) continue;
            return state;
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state).getOpposite() == directionToNeighbour && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    protected static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(FACE)) {
            case AttachFace.CEILING -> Direction.DOWN;
            case AttachFace.FLOOR -> Direction.UP;
            default -> (Direction)state.getValue(FACING);
        };
    }
}

