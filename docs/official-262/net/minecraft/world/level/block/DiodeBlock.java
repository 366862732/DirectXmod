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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

public abstract class DiodeBlock
extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 2.0);

    protected DiodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected abstract MapCodec<? extends DiodeBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return this.canSurviveOn(level, belowPos, level.getBlockState(belowPos));
    }

    protected boolean canSurviveOn(LevelReader level, BlockPos neightborPos, BlockState neighborState) {
        return neighborState.isFaceSturdy(level, neightborPos, Direction.UP, SupportType.RIGID);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.isLocked(level, pos, state)) {
            return;
        }
        boolean on = state.getValue(POWERED);
        boolean shouldTurnOn = this.shouldTurnOn(level, pos, state);
        if (on && !shouldTurnOn) {
            level.setBlock(pos, (BlockState)state.setValue(POWERED, false), 2);
        } else if (!on) {
            level.setBlock(pos, (BlockState)state.setValue(POWERED, true), 2);
            if (!shouldTurnOn) {
                level.scheduleTick(pos, this, this.getDelay(state), TickPriority.VERY_HIGH);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        if (state.canSurvive(level, pos)) {
            this.checkTickOnNeighbor(level, pos, state);
            return;
        }
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        DiodeBlock.dropResources(state, level, pos, blockEntity);
        level.removeBlock(pos, false);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
    }

    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        boolean shouldTurnOn;
        if (this.isLocked(level, pos, state)) {
            return;
        }
        boolean on = state.getValue(POWERED);
        if (on != (shouldTurnOn = this.shouldTurnOn(level, pos, state)) && !level.getBlockTicks().willTickThisTick(pos, this)) {
            TickPriority priority = TickPriority.HIGH;
            if (this.shouldPrioritize(level, pos, state)) {
                priority = TickPriority.EXTREMELY_HIGH;
            } else if (on) {
                priority = TickPriority.VERY_HIGH;
            }
            level.scheduleTick(pos, this, this.getDelay(state), priority);
        }
    }

    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        return this.getInputSignal(level, pos, state) > 0;
    }

    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = (Direction)state.getValue(FACING);
        BlockPos targetPos = pos.relative(direction);
        int input = level.getSignal(targetPos, direction);
        if (input >= 15) {
            return input;
        }
        BlockState targetBlockState = level.getBlockState(targetPos);
        return Math.max(input, targetBlockState.is(Blocks.REDSTONE_WIRE) ? targetBlockState.getValue(RedStoneWireBlock.POWER) : 0);
    }

    protected int getAlternateSignal(SignalGetter level, BlockPos pos, BlockState state) {
        Direction direction = (Direction)state.getValue(FACING);
        Direction clockWise = direction.getClockWise();
        Direction counterClockWise = direction.getCounterClockWise();
        boolean sideInputDiodesOnly = this.sideInputDiodesOnly();
        return Math.max(level.getControlInputSignal(pos.relative(clockWise), clockWise, sideInputDiodesOnly), level.getControlInputSignal(pos.relative(counterClockWise), counterClockWise, sideInputDiodesOnly));
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWERED) != false ? this.getOutputSignal(level, pos, state) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(FACING) == direction ? this.ownSignal(state, level, pos) : 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (this.shouldTurnOn(level, pos, state)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        this.updateNeighborsInFront(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) {
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = (Direction)state.getValue(FACING);
        BlockPos oppositePos = pos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction.getOpposite(), Direction.UP);
        level.neighborChanged(oppositePos, this, orientation);
        level.updateNeighborsAtExceptFromFacing(oppositePos, this, direction, orientation);
    }

    protected boolean sideInputDiodesOnly() {
        return false;
    }

    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return 15;
    }

    public static boolean isDiode(BlockState state) {
        return state.getBlock() instanceof DiodeBlock;
    }

    public boolean shouldPrioritize(BlockGetter level, BlockPos pos, BlockState state) {
        Direction direction = ((Direction)state.getValue(FACING)).getOpposite();
        BlockState oppositeState = level.getBlockState(pos.relative(direction));
        return DiodeBlock.isDiode(oppositeState) && oppositeState.getValue(FACING) != direction;
    }

    protected abstract int getDelay(BlockState var1);
}

