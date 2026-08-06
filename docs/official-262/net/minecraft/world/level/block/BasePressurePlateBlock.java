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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BasePressurePlateBlock
extends Block {
    private static final VoxelShape SHAPE_PRESSED = Block.column(14.0, 0.0, 0.5);
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 1.0);
    protected static final AABB TOUCH_AABB = Block.column(14.0, 0.0, 4.0).toAabbs().getFirst();
    protected final BlockSetType type;

    protected BasePressurePlateBlock(BlockBehaviour.Properties properties, BlockSetType type) {
        super(properties.sound(type.soundType()));
        this.type = type;
    }

    protected abstract MapCodec<? extends BasePressurePlateBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getSignalForState(state) > 0 ? SHAPE_PRESSED : SHAPE;
    }

    protected int getPressedTime() {
        return 20;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState state) {
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return BasePressurePlateBlock.canSupportRigidBlock(level, below) || BasePressurePlateBlock.canSupportCenter(level, below, Direction.UP);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int signal = this.getSignalForState(state);
        if (signal > 0) {
            this.checkPressed(null, level, pos, state, signal);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) {
            return;
        }
        int signal = this.getSignalForState(state);
        if (signal == 0) {
            this.checkPressed(entity, level, pos, state, signal);
        }
    }

    private void checkPressed(@Nullable Entity sourceEntity, Level level, BlockPos pos, BlockState state, int oldSignal) {
        boolean isPressed;
        int signal = this.getSignalStrength(level, pos);
        boolean wasPressed = oldSignal > 0;
        boolean bl = isPressed = signal > 0;
        if (oldSignal != signal) {
            BlockState newState = this.setSignalForState(state, signal);
            level.setBlock(pos, newState, 2);
            this.updateNeighbours(level, pos);
            level.setBlocksDirty(pos, state, newState);
        }
        if (!isPressed && wasPressed) {
            level.playSound(null, pos, this.type.pressurePlateClickOff(), SoundSource.BLOCKS);
            level.gameEvent(sourceEntity, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (isPressed && !wasPressed) {
            level.playSound(null, pos, this.type.pressurePlateClickOn(), SoundSource.BLOCKS);
            level.gameEvent(sourceEntity, GameEvent.BLOCK_ACTIVATE, pos);
        }
        if (isPressed) {
            level.scheduleTick(new BlockPos(pos), this, this.getPressedTime());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && this.getSignalForState(state) > 0) {
            this.updateNeighbours(level, pos);
        }
    }

    protected void updateNeighbours(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(pos.below(), this);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.UP) {
            return this.getSignalForState(state);
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return this.getSignalForState(state);
    }

    protected static int getEntityCount(Level level, AABB entityDetectionBox, Class<? extends Entity> entityClass) {
        return level.getEntitiesOfClass(entityClass, entityDetectionBox, EntitySelector.NO_SPECTATORS.and(e -> !e.isIgnoringBlockTriggers())).size();
    }

    protected abstract int getSignalStrength(Level var1, BlockPos var2);

    protected abstract int getSignalForState(BlockState var1);

    protected abstract BlockState setSignalForState(BlockState var1, int var2);
}

