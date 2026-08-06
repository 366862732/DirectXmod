/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BellBlock
extends BaseEntityBlock {
    public static final MapCodec<BellBlock> CODEC = BellBlock.simpleCodec(BellBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BellAttachType> ATTACHMENT = BlockStateProperties.BELL_ATTACHMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape BELL_SHAPE = Shapes.or(Block.column(6.0, 6.0, 13.0), Block.column(8.0, 4.0, 6.0));
    private static final VoxelShape SHAPE_CEILING = Shapes.or(BELL_SHAPE, Block.column(2.0, 13.0, 16.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_FLOOR = Shapes.rotateHorizontalAxis(Block.cube(16.0, 16.0, 8.0));
    private static final Map<Direction.Axis, VoxelShape> SHAPE_DOUBLE_WALL = Shapes.rotateHorizontalAxis(Shapes.or(BELL_SHAPE, Block.column(2.0, 16.0, 13.0, 15.0)));
    private static final Map<Direction, VoxelShape> SHAPE_SINGLE_WALL = Shapes.rotateHorizontal(Shapes.or(BELL_SHAPE, Block.boxZ(2.0, 13.0, 15.0, 0.0, 13.0)));
    public static final int EVENT_BELL_RING = 1;

    public MapCodec<BellBlock> codec() {
        return CODEC;
    }

    public BellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(ATTACHMENT, BellAttachType.FLOOR)).setValue(POWERED, false));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean signal = level.hasNeighborSignal(pos);
        if (signal != state.getValue(POWERED)) {
            if (signal) {
                this.attemptToRing(level, pos, null);
            }
            level.setBlock(pos, (BlockState)state.setValue(POWERED, signal), 3);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        Player player;
        Entity owner = projectile.getOwner();
        Player playerOwner = owner instanceof Player ? (player = (Player)owner) : null;
        this.onHit(level, state, hitResult, playerOwner, true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return this.onHit(level, state, hitResult, player, true) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public boolean onHit(Level level, BlockState state, BlockHitResult hitResult, @Nullable Player player, boolean requireHitFromCorrectSide) {
        boolean properHit;
        Direction direction = hitResult.getDirection();
        BlockPos blockPos = hitResult.getBlockPos();
        boolean bl = properHit = !requireHitFromCorrectSide || this.isProperHit(state, direction, hitResult.getLocation().y - (double)blockPos.getY());
        if (properHit) {
            boolean didRing = this.attemptToRing(player, level, blockPos, direction);
            if (didRing && player != null) {
                player.awardStat(Stats.BELL_RING);
            }
            return true;
        }
        return false;
    }

    private boolean isProperHit(BlockState state, Direction clickedDirection, double clickY) {
        if (clickedDirection.getAxis() == Direction.Axis.Y || clickY > (double)0.8124f) {
            return false;
        }
        Direction facing = state.getValue(FACING);
        BellAttachType attachType = state.getValue(ATTACHMENT);
        return switch (attachType) {
            case BellAttachType.FLOOR -> {
                if (facing.getAxis() == clickedDirection.getAxis()) {
                    yield true;
                }
                yield false;
            }
            case BellAttachType.SINGLE_WALL, BellAttachType.DOUBLE_WALL -> {
                if (facing.getAxis() != clickedDirection.getAxis()) {
                    yield true;
                }
                yield false;
            }
            case BellAttachType.CEILING -> true;
            default -> false;
        };
    }

    public boolean attemptToRing(Level level, BlockPos pos, @Nullable Direction direction) {
        return this.attemptToRing(null, level, pos, direction);
    }

    public boolean attemptToRing(@Nullable Entity ringingEntity, Level level, BlockPos pos, @Nullable Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide() && blockEntity instanceof BellBlockEntity) {
            BellBlockEntity bellBlockEntity = (BellBlockEntity)blockEntity;
            if (direction == null) {
                direction = level.getBlockState(pos).getValue(FACING);
            }
            bellBlockEntity.onHit(direction);
            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 1.0f);
            level.gameEvent(ringingEntity, GameEvent.BLOCK_CHANGE, pos);
            return true;
        }
        return false;
    }

    private VoxelShape getVoxelShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(ATTACHMENT)) {
            default -> throw new MatchException(null, null);
            case BellAttachType.FLOOR -> SHAPE_FLOOR.get(facing.getAxis());
            case BellAttachType.CEILING -> SHAPE_CEILING;
            case BellAttachType.SINGLE_WALL -> SHAPE_SINGLE_WALL.get(facing);
            case BellAttachType.DOUBLE_WALL -> SHAPE_DOUBLE_WALL.get(facing.getAxis());
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getVoxelShape(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getVoxelShape(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        Direction.Axis axis = clickedFace.getAxis();
        if (axis == Direction.Axis.Y) {
            BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(ATTACHMENT, clickedFace == Direction.DOWN ? BellAttachType.CEILING : BellAttachType.FLOOR)).setValue(FACING, context.getHorizontalDirection());
            if (state.canSurvive(context.getLevel(), pos)) {
                return state;
            }
        } else {
            boolean doubleAttached = axis == Direction.Axis.X && level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST) && level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST) || axis == Direction.Axis.Z && level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH) && level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH);
            BlockState state = (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, clickedFace.getOpposite())).setValue(ATTACHMENT, doubleAttached ? BellAttachType.DOUBLE_WALL : BellAttachType.SINGLE_WALL);
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
            boolean canAttachBelow = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
            if ((state = (BlockState)state.setValue(ATTACHMENT, canAttachBelow ? BellAttachType.FLOOR : BellAttachType.CEILING)).canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (explosion.canTriggerBlocks()) {
            this.attemptToRing(level, pos, null);
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BellAttachType attachment = state.getValue(ATTACHMENT);
        Direction connectedDirection = BellBlock.getConnectedDirection(state).getOpposite();
        if (connectedDirection == directionToNeighbour && !state.canSurvive(level, pos) && attachment != BellAttachType.DOUBLE_WALL) {
            return Blocks.AIR.defaultBlockState();
        }
        if (directionToNeighbour.getAxis() == state.getValue(FACING).getAxis()) {
            if (attachment == BellAttachType.DOUBLE_WALL && !neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour)) {
                return (BlockState)((BlockState)state.setValue(ATTACHMENT, BellAttachType.SINGLE_WALL)).setValue(FACING, directionToNeighbour.getOpposite());
            }
            if (attachment == BellAttachType.SINGLE_WALL && connectedDirection.getOpposite() == directionToNeighbour && neighbourState.isFaceSturdy(level, neighbourPos, state.getValue(FACING))) {
                return (BlockState)state.setValue(ATTACHMENT, BellAttachType.DOUBLE_WALL);
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction connectionDir = BellBlock.getConnectedDirection(state).getOpposite();
        if (connectionDir == Direction.UP) {
            return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
        }
        return FaceAttachedHorizontalDirectionalBlock.canAttach(level, pos, connectionDir);
    }

    private static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(ATTACHMENT)) {
            case BellAttachType.CEILING -> Direction.DOWN;
            case BellAttachType.FLOOR -> Direction.UP;
            default -> state.getValue(FACING).getOpposite();
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACHMENT, POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new BellBlockEntity(worldPosition, blockState);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
        return BellBlock.createTickerHelper(type, BlockEntityTypes.BELL, level.isClientSide() ? BellBlockEntity::clientTick : BellBlockEntity::serverTick);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}

