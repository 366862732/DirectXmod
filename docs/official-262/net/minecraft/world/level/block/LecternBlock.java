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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class LecternBlock
extends BaseEntityBlock {
    public static final MapCodec<LecternBlock> CODEC = LecternBlock.simpleCodec(LecternBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
    private static final VoxelShape SHAPE_COLLISION = Shapes.or(Block.column(16.0, 0.0, 2.0), Block.column(8.0, 2.0, 14.0));
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(Block.boxZ(16.0, 10.0, 14.0, 1.0, 5.333333), Block.boxZ(16.0, 12.0, 16.0, 5.333333, 9.666667), Block.boxZ(16.0, 14.0, 18.0, 9.666667, 14.0), SHAPE_COLLISION));
    private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

    public MapCodec<LecternBlock> codec() {
        return CODEC;
    }

    protected LecternBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false)).setValue(HAS_BOOK, false));
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return SHAPE_COLLISION;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        TypedEntityData<BlockEntityType<?>> blockEntityData;
        Level level = context.getLevel();
        ItemStack itemStack = context.getItemInHand();
        Player player = context.getPlayer();
        boolean hasBook = false;
        if (!level.isClientSide() && player != null && player.canUseGameMasterBlocks() && (blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA)) != null && blockEntityData.contains("Book")) {
            hasBook = true;
        }
        return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(HAS_BOOK, hasBook);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_COLLISION;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, HAS_BOOK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new LecternBlockEntity(worldPosition, blockState);
    }

    public static boolean tryPlaceBook(@Nullable LivingEntity sourceEntity, Level level, BlockPos pos, BlockState state, ItemStack item) {
        if (!state.getValue(HAS_BOOK).booleanValue()) {
            if (!level.isClientSide()) {
                LecternBlock.placeBook(sourceEntity, level, pos, state, item);
            }
            return true;
        }
        return false;
    }

    private static void placeBook(@Nullable LivingEntity sourceEntity, Level level, BlockPos pos, BlockState state, ItemStack book) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof LecternBlockEntity) {
            LecternBlockEntity lectern = (LecternBlockEntity)entity;
            lectern.setBook(book.consumeAndReturn(1, sourceEntity));
            LecternBlock.resetBookState(sourceEntity, level, pos, state, true);
            level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    public static void resetBookState(@Nullable Entity sourceEntity, Level level, BlockPos pos, BlockState state, boolean hasBook) {
        BlockState newState = (BlockState)((BlockState)state.setValue(POWERED, false)).setValue(HAS_BOOK, hasBook);
        level.setBlock(pos, newState, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
        LecternBlock.updateBelow(level, pos, state);
    }

    public static void signalPageChange(Level level, BlockPos pos, BlockState state) {
        LecternBlock.changePowered(level, pos, state, true);
        level.scheduleTick(pos, state.getBlock(), 2);
        level.levelEvent(1043, pos, 0);
    }

    private static void changePowered(Level level, BlockPos pos, BlockState state, boolean isPowered) {
        level.setBlock(pos, (BlockState)state.setValue(POWERED, isPowered), 3);
        LecternBlock.updateBelow(level, pos, state);
    }

    private static void updateBelow(Level level, BlockPos pos, BlockState state) {
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, state.getValue(FACING).getOpposite(), Direction.UP);
        level.updateNeighborsAt(pos.below(), state.getBlock(), orientation);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        LecternBlock.changePowered(level, pos, state, false);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(POWERED).booleanValue()) {
            LecternBlock.updateBelow(level, pos, state);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWERED) != false ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction == Direction.UP && state.getValue(POWERED) != false ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity;
        if (state.getValue(HAS_BOOK).booleanValue() && (blockEntity = level.getBlockEntity(pos)) instanceof LecternBlockEntity) {
            LecternBlockEntity lecternBlockEntity = (LecternBlockEntity)blockEntity;
            return lecternBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(HAS_BOOK).booleanValue()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (itemStack.is(ItemTags.LECTERN_BOOKS)) {
            return LecternBlock.tryPlaceBook(player, level, pos, state, itemStack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (itemStack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(HAS_BOOK).booleanValue()) {
            if (!level.isClientSide()) {
                this.openScreen(level, pos, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(HAS_BOOK).booleanValue()) {
            return null;
        }
        return super.getMenuProvider(state, level, pos);
    }

    private void openScreen(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity) {
            LecternBlockEntity lecternBlockEntity = (LecternBlockEntity)blockEntity;
            player.openMenu(lecternBlockEntity);
            player.awardStat(Stats.INTERACT_WITH_LECTERN);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}

