/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.block;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireBlock
extends Block {
    public static final MapCodec<TripWireBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BuiltInRegistries.BLOCK.byNameCodec().fieldOf("hook").forGetter(b -> b.hook), TripWireBlock.propertiesCodec()).apply((Applicative)i, TripWireBlock::new));
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    public static final BooleanProperty DISARMED = BlockStateProperties.DISARMED;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape SHAPE_ATTACHED = Block.column(16.0, 1.0, 2.5);
    private static final VoxelShape SHAPE_NOT_ATTACHED = Block.column(16.0, 0.0, 8.0);
    private static final int RECHECK_PERIOD = 10;
    private final Block hook;

    public MapCodec<TripWireBlock> codec() {
        return CODEC;
    }

    public TripWireBlock(Block hook, BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWERED, false)).setValue(ATTACHED, false)).setValue(DISARMED, false)).setValue(NORTH, false)).setValue(EAST, false)).setValue(SOUTH, false)).setValue(WEST, false));
        this.hook = hook;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ATTACHED) != false ? SHAPE_ATTACHED : SHAPE_NOT_ATTACHED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(NORTH, this.shouldConnectTo(level.getBlockState(pos.north()), Direction.NORTH))).setValue(EAST, this.shouldConnectTo(level.getBlockState(pos.east()), Direction.EAST))).setValue(SOUTH, this.shouldConnectTo(level.getBlockState(pos.south()), Direction.SOUTH))).setValue(WEST, this.shouldConnectTo(level.getBlockState(pos.west()), Direction.WEST));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour.getAxis().isHorizontal()) {
            return (BlockState)state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), this.shouldConnectTo(neighbourState, directionToNeighbour));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        this.updateSource(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston) {
            this.updateSource(level, pos, (BlockState)state.setValue(POWERED, true));
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getMainHandItem().isEmpty() && player.getMainHandItem().is(Items.SHEARS)) {
            level.setBlock(pos, (BlockState)state.setValue(DISARMED, true), 260);
            level.gameEvent((Entity)player, GameEvent.SHEAR, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void updateSource(Level level, BlockPos pos, BlockState state) {
        block0: for (Direction direction : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; ++i) {
                BlockPos testPos = pos.relative(direction, i);
                BlockState block = level.getBlockState(testPos);
                if (block.is(this.hook)) {
                    if (block.getValue(TripWireHookBlock.FACING) != direction.getOpposite()) continue block0;
                    TripWireHookBlock.calculateState(level, testPos, block, false, true, i, state);
                    continue block0;
                }
                if (!block.is(this)) continue block0;
            }
        }
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getShape(level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) {
            return;
        }
        if (state.getValue(POWERED).booleanValue() || level.getBlockTicks().hasScheduledTick(pos, this)) {
            return;
        }
        this.checkPressed(level, pos, List.of(entity));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos).getValue(POWERED).booleanValue()) {
            return;
        }
        this.checkPressed(level, pos);
    }

    private void checkPressed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<Entity> entities = level.getEntities(null, state.getShape(level, pos).bounds().move(pos));
        this.checkPressed(level, pos, entities);
    }

    private void checkPressed(Level level, BlockPos pos, List<? extends Entity> entities) {
        BlockState state = level.getBlockState(pos);
        boolean wasPressed = state.getValue(POWERED);
        boolean shouldBePressed = false;
        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity.isIgnoringBlockTriggers()) continue;
                shouldBePressed = true;
                break;
            }
        }
        if (shouldBePressed != wasPressed) {
            state = (BlockState)state.setValue(POWERED, shouldBePressed);
            level.setBlock(pos, state, 3);
            this.updateSource(level, pos, state);
        }
        if (shouldBePressed) {
            level.scheduleTick(new BlockPos(pos), this, 10);
        } else if (wasPressed) {
            level.scheduleTick(new BlockPos(pos), this, 0);
        }
    }

    public boolean shouldConnectTo(BlockState blockState, Direction direction) {
        if (blockState.is(this.hook)) {
            return blockState.getValue(TripWireHookBlock.FACING) == direction.getOpposite();
        }
        return blockState.is(this);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case Rotation.CLOCKWISE_180 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(SOUTH))).setValue(EAST, state.getValue(WEST))).setValue(SOUTH, state.getValue(NORTH))).setValue(WEST, state.getValue(EAST));
            case Rotation.COUNTERCLOCKWISE_90 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(EAST))).setValue(EAST, state.getValue(SOUTH))).setValue(SOUTH, state.getValue(WEST))).setValue(WEST, state.getValue(NORTH));
            case Rotation.CLOCKWISE_90 -> (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(NORTH, state.getValue(WEST))).setValue(EAST, state.getValue(NORTH))).setValue(SOUTH, state.getValue(EAST))).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT: {
                return (BlockState)((BlockState)state.setValue(NORTH, state.getValue(SOUTH))).setValue(SOUTH, state.getValue(NORTH));
            }
            case FRONT_BACK: {
                return (BlockState)((BlockState)state.setValue(EAST, state.getValue(WEST))).setValue(WEST, state.getValue(EAST));
            }
        }
        return super.mirror(state, mirror);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, ATTACHED, DISARMED, NORTH, EAST, WEST, SOUTH);
    }
}

