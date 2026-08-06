/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SelectableSlotContainer;
import net.minecraft.world.level.block.SideChainPartBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SideChainPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShelfBlock
extends BaseEntityBlock
implements SelectableSlotContainer,
SideChainPartBlock,
SimpleWaterloggedBlock {
    public static final MapCodec<ShelfBlock> CODEC = ShelfBlock.simpleCodec(ShelfBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART = BlockStateProperties.SIDE_CHAIN_PART;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(Block.box(0.0, 12.0, 11.0, 16.0, 16.0, 13.0), Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0), Block.box(0.0, 0.0, 11.0, 16.0, 4.0, 13.0)));

    public MapCodec<ShelfBlock> codec() {
        return CODEC;
    }

    public ShelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false)).setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED)).setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type == PathComputationType.WATER && state.getFluidState().is(FluidTags.WATER);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
        return new ShelfBlockEntity(worldPosition, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SIDE_CHAIN_PART, WATERLOGGED);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
        this.updateNeighborsAfterPoweringDown(level, pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean signal = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != signal) {
            BlockState newState = (BlockState)state.setValue(POWERED, signal);
            if (!signal) {
                newState = (BlockState)newState.setValue(SIDE_CHAIN_PART, SideChainPart.UNCONNECTED);
            }
            level.setBlock(pos, newState, 3);
            this.playSound(level, pos, signal ? SoundEvents.SHELF_ACTIVATE : SoundEvents.SHELF_DEACTIVATE);
            level.gameEvent(signal ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos, GameEvent.Context.of(newState));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
        return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))).setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public int getRows() {
        return 1;
    }

    @Override
    public int getColumns() {
        return 3;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ShelfBlockEntity shelfBlockEntity;
        block13: {
            block12: {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof ShelfBlockEntity)) break block12;
                shelfBlockEntity = (ShelfBlockEntity)blockEntity;
                if (!hand.equals((Object)InteractionHand.OFF_HAND)) break block13;
            }
            return InteractionResult.PASS;
        }
        OptionalInt hitSlot = this.getHitSlot(hitResult, state.getValue(FACING));
        if (hitSlot.isEmpty()) {
            return InteractionResult.PASS;
        }
        Inventory inventory = player.getInventory();
        if (level.isClientSide()) {
            return inventory.getSelectedItem().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (!state.getValue(POWERED).booleanValue()) {
            boolean itemRemoved = ShelfBlock.swapSingleItem(itemStack, player, shelfBlockEntity, hitSlot.getAsInt(), inventory);
            if (itemRemoved) {
                this.playSound(level, pos, itemStack.isEmpty() ? SoundEvents.SHELF_TAKE_ITEM : SoundEvents.SHELF_SINGLE_SWAP);
            } else if (!itemStack.isEmpty()) {
                this.playSound(level, pos, SoundEvents.SHELF_PLACE_ITEM);
            } else {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
        }
        ItemStack previousItem = inventory.getSelectedItem();
        boolean anySwapped = this.swapHotbar(level, pos, inventory);
        if (!anySwapped) {
            return InteractionResult.CONSUME;
        }
        this.playSound(level, pos, SoundEvents.SHELF_MULTI_SWAP);
        if (previousItem == inventory.getSelectedItem()) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(inventory.getSelectedItem());
    }

    private static boolean swapSingleItem(ItemStack itemStack, Player player, ShelfBlockEntity shelfBlockEntity, int hitSlot, Inventory inventory) {
        ItemStack removedItem = shelfBlockEntity.swapItemNoUpdate(hitSlot, itemStack);
        ItemStack newInventoryItem = player.hasInfiniteMaterials() && removedItem.isEmpty() ? itemStack.copy() : removedItem;
        inventory.setItem(inventory.getSelectedSlot(), newInventoryItem);
        inventory.setChanged();
        shelfBlockEntity.setChanged((Holder.Reference<GameEvent>)(newInventoryItem.has(DataComponents.USE_EFFECTS) && !newInventoryItem.get(DataComponents.USE_EFFECTS).interactVibrations() ? null : GameEvent.ITEM_INTERACT_FINISH));
        return !removedItem.isEmpty();
    }

    private boolean swapHotbar(Level level, BlockPos pos, Inventory inventory) {
        List<BlockPos> connectedBlocks = this.getAllBlocksConnectedTo(level, pos);
        if (connectedBlocks.isEmpty()) {
            return false;
        }
        boolean anySwapped = false;
        for (int shelfPartIndex = 0; shelfPartIndex < connectedBlocks.size(); ++shelfPartIndex) {
            ShelfBlockEntity shelfPart = (ShelfBlockEntity)level.getBlockEntity(connectedBlocks.get(shelfPartIndex));
            if (shelfPart == null) continue;
            for (int slot = 0; slot < shelfPart.getContainerSize(); ++slot) {
                int inventorySlot = 9 - (connectedBlocks.size() - shelfPartIndex) * shelfPart.getContainerSize() + slot;
                if (inventorySlot < 0 || inventorySlot > inventory.getContainerSize()) continue;
                ItemStack placedInventoryItem = inventory.removeItemNoUpdate(inventorySlot);
                ItemStack removedShelfItem = shelfPart.swapItemNoUpdate(slot, placedInventoryItem);
                if (placedInventoryItem.isEmpty() && removedShelfItem.isEmpty()) continue;
                inventory.setItem(inventorySlot, removedShelfItem);
                anySwapped = true;
            }
            inventory.setChanged();
            shelfPart.setChanged(GameEvent.ENTITY_INTERACT);
        }
        return anySwapped;
    }

    @Override
    public SideChainPart getSideChainPart(BlockState state) {
        return state.getValue(SIDE_CHAIN_PART);
    }

    @Override
    public BlockState setSideChainPart(BlockState state, SideChainPart newPart) {
        return (BlockState)state.setValue(SIDE_CHAIN_PART, newPart);
    }

    @Override
    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public boolean isConnectable(BlockState state) {
        return state.is(BlockTags.WOODEN_SHELVES) && state.hasProperty(POWERED) && state.getValue(POWERED) != false;
    }

    @Override
    public int getMaxChainLength() {
        return 3;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (state.getValue(POWERED).booleanValue()) {
            this.updateSelfAndNeighborsOnPoweringUp(level, pos, state, oldState);
        } else {
            this.updateNeighborsAfterPoweringDown(level, pos, state);
        }
    }

    private void playSound(LevelAccessor level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED).booleanValue()) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.isClientSide()) {
            return 0;
        }
        if (direction != state.getValue(FACING).getOpposite()) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShelfBlockEntity) {
            ShelfBlockEntity blockEntity2 = (ShelfBlockEntity)blockEntity;
            int item1Bit = blockEntity2.getItem(0).isEmpty() ? 0 : 1;
            int item2Bit = blockEntity2.getItem(1).isEmpty() ? 0 : 1;
            int item3Bit = blockEntity2.getItem(2).isEmpty() ? 0 : 1;
            return item1Bit | item2Bit << 1 | item3Bit << 2;
        }
        return 0;
    }
}

