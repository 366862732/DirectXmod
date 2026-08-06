/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class FenceGateBlock
extends HorizontalDirectionalBlock {
    public static final MapCodec<FenceGateBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)WoodType.CODEC.fieldOf("wood_type").forGetter(b -> b.type), FenceGateBlock.propertiesCodec()).apply((Applicative)i, FenceGateBlock::new));
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty IN_WALL = BlockStateProperties.IN_WALL;
    private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 4.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPES_WALL = Maps.newEnumMap(Util.mapValues(SHAPES, v -> Shapes.join(v, Block.column(16.0, 13.0, 16.0), BooleanOp.ONLY_FIRST)));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_COLLISION = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 0.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_SUPPORT = Shapes.rotateHorizontalAxis(Block.column(16.0, 4.0, 5.0, 24.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION = Shapes.rotateHorizontalAxis(Shapes.or(Block.box(0.0, 5.0, 7.0, 2.0, 16.0, 9.0), Block.box(14.0, 5.0, 7.0, 16.0, 16.0, 9.0)));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_OCCLUSION_WALL = Maps.newEnumMap(Util.mapValues(SHAPE_OCCLUSION, v -> v.move(0.0, -0.1875, 0.0).optimize()));
    private final WoodType type;

    public MapCodec<FenceGateBlock> codec() {
        return CODEC;
    }

    public FenceGateBlock(WoodType type, BlockBehaviour.Properties properties) {
        super(properties.sound(type.soundType()));
        this.type = type;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(OPEN, false)).setValue(POWERED, false)).setValue(IN_WALL, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = ((Direction)state.getValue(FACING)).getAxis();
        return (state.getValue(IN_WALL) != false ? SHAPES_WALL : SHAPES).get(axis);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        Direction.Axis axis = directionToNeighbour.getAxis();
        if (((Direction)state.getValue(FACING)).getClockWise().getAxis() == axis) {
            boolean inWall = this.isWall(neighbourState) || this.isWall(level.getBlockState(pos.relative(directionToNeighbour.getOpposite())));
            return (BlockState)state.setValue(IN_WALL, inWall);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction.Axis axis = ((Direction)state.getValue(FACING)).getAxis();
        return state.getValue(OPEN) != false ? Shapes.empty() : SHAPE_SUPPORT.get(axis);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction.Axis axis = ((Direction)state.getValue(FACING)).getAxis();
        return state.getValue(OPEN) != false ? Shapes.empty() : SHAPE_COLLISION.get(axis);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        Direction.Axis axis = ((Direction)state.getValue(FACING)).getAxis();
        return (state.getValue(IN_WALL) != false ? SHAPE_OCCLUSION_WALL : SHAPE_OCCLUSION).get(axis);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return switch (type) {
            case PathComputationType.LAND -> state.getValue(OPEN);
            case PathComputationType.WATER -> false;
            case PathComputationType.AIR -> state.getValue(OPEN);
            default -> false;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean isOpen = level.hasNeighborSignal(pos);
        Direction direction = context.getHorizontalDirection();
        Direction.Axis axis = direction.getAxis();
        boolean inWall = axis == Direction.Axis.Z && (this.isWall(level.getBlockState(pos.west())) || this.isWall(level.getBlockState(pos.east()))) || axis == Direction.Axis.X && (this.isWall(level.getBlockState(pos.north())) || this.isWall(level.getBlockState(pos.south())));
        return (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, direction)).setValue(OPEN, isOpen)).setValue(POWERED, isOpen)).setValue(IN_WALL, inWall);
    }

    private boolean isWall(BlockState state) {
        return state.is(BlockTags.WALLS);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(OPEN).booleanValue()) {
            state = (BlockState)state.setValue(OPEN, false);
            level.setBlock(pos, state, 10);
        } else {
            Direction direction = player.getDirection();
            if (state.getValue(FACING) == direction.getOpposite()) {
                state = (BlockState)state.setValue(FACING, direction);
            }
            state = (BlockState)state.setValue(OPEN, true);
            level.setBlock(pos, state, 10);
        }
        boolean opens = state.getValue(OPEN);
        level.playSound((Entity)player, pos, opens ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
        level.gameEvent((Entity)player, opens ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (explosion.canTriggerBlocks() && !state.getValue(POWERED).booleanValue()) {
            boolean open = state.getValue(OPEN);
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, !open));
            level.playSound(null, pos, open ? this.type.fenceGateClose() : this.type.fenceGateOpen(), SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
            level.gameEvent(open ? GameEvent.BLOCK_CLOSE : GameEvent.BLOCK_OPEN, pos, GameEvent.Context.of(state));
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean hasPower = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != hasPower) {
            level.setBlock(pos, (BlockState)((BlockState)state.setValue(POWERED, hasPower)).setValue(OPEN, hasPower), 2);
            if (state.getValue(OPEN) != hasPower) {
                level.playSound(null, pos, hasPower ? this.type.fenceGateOpen() : this.type.fenceGateClose(), SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
                level.gameEvent(null, hasPower ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, POWERED, IN_WALL);
    }

    public static boolean connectsToDirection(BlockState state, Direction direction) {
        return ((Direction)state.getValue(FACING)).getAxis() == direction.getClockWise().getAxis();
    }
}

