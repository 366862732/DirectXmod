/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PistonBaseBlock
extends DirectionalBlock {
    public static final MapCodec<PistonBaseBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.fieldOf("sticky").forGetter(b -> b.isSticky), PistonBaseBlock.propertiesCodec()).apply((Applicative)i, PistonBaseBlock::new));
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    public static final int TRIGGER_EXTEND = 0;
    public static final int TRIGGER_CONTRACT = 1;
    public static final int TRIGGER_DROP = 2;
    public static final int PLATFORM_THICKNESS = 4;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateAll(Block.boxZ(16.0, 4.0, 16.0));
    private final boolean isSticky;

    public MapCodec<PistonBaseBlock> codec() {
        return CODEC;
    }

    public PistonBaseBlock(boolean isSticky, BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(EXTENDED, false));
        this.isSticky = isSticky;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(EXTENDED).booleanValue()) {
            return SHAPES.get(state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (!level.isClientSide()) {
            this.checkIfExtend(level, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            this.checkIfExtend(level, pos, state);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) == null) {
            this.checkIfExtend(level, pos, state);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite())).setValue(EXTENDED, false);
    }

    private void checkIfExtend(Level level, BlockPos pos, BlockState state) {
        Direction direction = (Direction)state.getValue(FACING);
        boolean extend = this.getNeighborSignal(level, pos, direction);
        if (extend && !state.getValue(EXTENDED).booleanValue()) {
            if (new PistonStructureResolver(level, pos, direction, true).resolve()) {
                level.blockEvent(pos, this, 0, direction.get3DDataValue());
            }
        } else if (!extend && state.getValue(EXTENDED).booleanValue()) {
            PistonMovingBlockEntity pistonEntity;
            BlockEntity entity;
            BlockPos pushedPos = pos.relative(direction, 2);
            BlockState pushedState = level.getBlockState(pushedPos);
            int event = 1;
            if (pushedState.is(Blocks.MOVING_PISTON) && pushedState.getValue(FACING) == direction && (entity = level.getBlockEntity(pushedPos)) instanceof PistonMovingBlockEntity && (pistonEntity = (PistonMovingBlockEntity)entity).isExtending() && (pistonEntity.getProgress(0.0f) < 0.5f || level.getGameTime() == pistonEntity.getLastTicked() || ((ServerLevel)level).isHandlingTick())) {
                event = 2;
            }
            level.blockEvent(pos, this, event, direction.get3DDataValue());
        }
    }

    private boolean getNeighborSignal(SignalGetter level, BlockPos pos, Direction pushDirection) {
        for (Direction direction : Direction.values()) {
            if (direction == pushDirection || !level.hasSignal(pos.relative(direction), direction)) continue;
            return true;
        }
        if (level.hasSignal(pos, Direction.DOWN)) {
            return true;
        }
        BlockPos above = pos.above();
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || !level.hasSignal(above.relative(direction), direction)) continue;
            return true;
        }
        return false;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int b0, int b1) {
        Direction direction = (Direction)state.getValue(FACING);
        BlockState extendedState = (BlockState)state.setValue(EXTENDED, true);
        if (!level.isClientSide()) {
            boolean extend = this.getNeighborSignal(level, pos, direction);
            if (extend && (b0 == 1 || b0 == 2)) {
                level.setBlock(pos, extendedState, 2);
                return false;
            }
            if (!extend && b0 == 0) {
                return false;
            }
        }
        RandomSource random = level.getRandom();
        if (b0 == 0) {
            if (!this.moveBlocks(level, pos, direction, true)) return false;
            level.setBlock(pos, extendedState, 67);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, random.nextFloat() * 0.25f + 0.6f);
            level.gameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Context.of(extendedState));
            return true;
        } else {
            if (b0 != 1 && b0 != 2) return true;
            BlockEntity prevBlockEntity = level.getBlockEntity(pos.relative(direction));
            if (prevBlockEntity instanceof PistonMovingBlockEntity) {
                PistonMovingBlockEntity pistonMovingBlockEntity = (PistonMovingBlockEntity)prevBlockEntity;
                pistonMovingBlockEntity.finalTick();
            }
            BlockState movingPistonState = (BlockState)((BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction)).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            level.setBlock(pos, movingPistonState, 276);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, movingPistonState, (BlockState)this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(b1 & 7)), direction, false, true));
            level.updateNeighborsAt(pos, movingPistonState.getBlock());
            movingPistonState.updateNeighbourShapes(level, pos, 2);
            if (this.isSticky) {
                PistonMovingBlockEntity entity;
                BlockEntity blockEntity;
                BlockPos twoPos = pos.offset(direction.getStepX() * 2, direction.getStepY() * 2, direction.getStepZ() * 2);
                BlockState movingState = level.getBlockState(twoPos);
                boolean pistonPiece = false;
                if (movingState.is(Blocks.MOVING_PISTON) && (blockEntity = level.getBlockEntity(twoPos)) instanceof PistonMovingBlockEntity && (entity = (PistonMovingBlockEntity)blockEntity).getDirection() == direction && entity.isExtending()) {
                    entity.finalTick();
                    pistonPiece = true;
                }
                if (!pistonPiece) {
                    if (b0 == 1 && !movingState.isAir() && PistonBaseBlock.isPushable(movingState, level, twoPos, direction.getOpposite(), false, direction) && (movingState.getPistonPushReaction() == PushReaction.NORMAL || movingState.is(Blocks.PISTON) || movingState.is(Blocks.STICKY_PISTON))) {
                        this.moveBlocks(level, pos, direction, false);
                    } else {
                        level.removeBlock(pos.relative(direction), false);
                    }
                }
            } else {
                level.removeBlock(pos.relative(direction), false);
            }
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, random.nextFloat() * 0.15f + 0.6f);
            level.gameEvent(GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(movingPistonState));
        }
        return true;
    }

    public static boolean isPushable(BlockState state, Level level, BlockPos pos, Direction direction, boolean allowDestroyable, Direction connectionDirection) {
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (state.isAir()) {
            return true;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR) || state.is(Blocks.REINFORCED_DEEPSLATE)) {
            return false;
        }
        if (direction == Direction.DOWN && pos.getY() == level.getMinY()) {
            return false;
        }
        if (direction == Direction.UP && pos.getY() == level.getMaxY()) {
            return false;
        }
        if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)) {
            if (state.getValue(EXTENDED).booleanValue()) {
                return false;
            }
        } else {
            if (state.getDestroySpeed(level, pos) == -1.0f) {
                return false;
            }
            switch (state.getPistonPushReaction()) {
                case BLOCK: {
                    return false;
                }
                case DESTROY: {
                    return allowDestroyable;
                }
                case PUSH_ONLY: {
                    return direction == connectionDirection;
                }
            }
        }
        return !state.hasBlockEntity();
    }

    /*
     * WARNING - void declaration
     */
    private boolean moveBlocks(Level level, BlockPos pistonPos, Direction direction, boolean extending) {
        void var16_30;
        void var16_28;
        BlockPos pos;
        PistonStructureResolver resolver;
        BlockPos armPos = pistonPos.relative(direction);
        if (!extending && level.getBlockState(armPos).is(Blocks.PISTON_HEAD)) {
            level.setBlock(armPos, Blocks.AIR.defaultBlockState(), 276);
        }
        if (!(resolver = new PistonStructureResolver(level, pistonPos, direction, extending)).resolve()) {
            return false;
        }
        HashMap deleteAfterMove = Maps.newHashMap();
        List<BlockPos> toPush = resolver.getToPush();
        ArrayList toPushShapes = Lists.newArrayList();
        for (BlockPos pos2 : toPush) {
            BlockState state = level.getBlockState(pos2);
            toPushShapes.add(state);
            deleteAfterMove.put(pos2, state);
        }
        List<BlockPos> toDestroy = resolver.getToDestroy();
        BlockState[] toUpdate = new BlockState[toPush.size() + toDestroy.size()];
        Direction pushDirection = extending ? direction : direction.getOpposite();
        int updateIndex = 0;
        for (int i2 = toDestroy.size() - 1; i2 >= 0; --i2) {
            pos = toDestroy.get(i2);
            BlockState blockState = level.getBlockState(pos);
            BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            PistonBaseBlock.dropResources(blockState, level, pos, blockEntity);
            if (!blockState.is(BlockTags.FIRE) && level.isClientSide()) {
                level.levelEvent(2001, pos, PistonBaseBlock.getId(blockState));
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(blockState));
            toUpdate[updateIndex++] = blockState;
        }
        for (int i = toPush.size() - 1; i >= 0; --i) {
            pos = toPush.get(i);
            BlockState blockState = level.getBlockState(pos);
            pos = pos.relative(pushDirection);
            deleteAfterMove.remove(pos);
            BlockState actualState = (BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, direction);
            level.setBlock(pos, actualState, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, actualState, (BlockState)toPushShapes.get(i), direction, extending, false));
            toUpdate[updateIndex++] = blockState;
        }
        if (extending) {
            PistonType type = this.isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState state = (BlockState)((BlockState)Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, direction)).setValue(PistonHeadBlock.TYPE, type);
            BlockState blockState = (BlockState)((BlockState)Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction)).setValue(MovingPistonBlock.TYPE, this.isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            deleteAfterMove.remove(armPos);
            level.setBlock(armPos, blockState, 324);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(armPos, blockState, state, direction, true, true));
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos blockPos : deleteAfterMove.keySet()) {
            level.setBlock(blockPos, air, 82);
        }
        for (Map.Entry entry : deleteAfterMove.entrySet()) {
            BlockPos pos4 = (BlockPos)entry.getKey();
            BlockState oldState = (BlockState)entry.getValue();
            oldState.updateIndirectNeighbourShapes(level, pos4, 2);
            air.updateNeighbourShapes(level, pos4, 2);
            air.updateIndirectNeighbourShapes(level, pos4, 2);
        }
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, resolver.getPushDirection(), null);
        updateIndex = 0;
        int n = toDestroy.size() - 1;
        while (var16_28 >= 0) {
            BlockState state = toUpdate[updateIndex++];
            BlockPos pos5 = toDestroy.get((int)var16_28);
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                state.affectNeighborsAfterRemoval(serverLevel, pos5, false);
            }
            state.updateIndirectNeighbourShapes(level, pos5, 2);
            level.updateNeighborsAt(pos5, state.getBlock(), orientation);
            --var16_28;
        }
        int n2 = toPush.size() - 1;
        while (var16_30 >= 0) {
            level.updateNeighborsAt(toPush.get((int)var16_30), toUpdate[updateIndex++].getBlock(), orientation);
            --var16_30;
        }
        if (extending) {
            level.updateNeighborsAt(armPos, Blocks.PISTON_HEAD, orientation);
        }
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EXTENDED);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(EXTENDED);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

