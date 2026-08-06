/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Map;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class BlockItem
extends Item {
    @Deprecated
    private final Block block;

    public BlockItem(Block block, Item.Properties properties) {
        super(properties);
        this.block = block;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult placeResult = this.place(new BlockPlaceContext(context));
        if (!placeResult.consumesAction() && context.getItemInHand().has(DataComponents.CONSUMABLE)) {
            return super.use(context.getLevel(), context.getPlayer(), context.getHand());
        }
        return placeResult;
    }

    public InteractionResult place(BlockPlaceContext placeContext) {
        if (!this.getBlock().isEnabled(placeContext.getLevel().enabledFeatures())) {
            return InteractionResult.FAIL;
        }
        if (!placeContext.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockPlaceContext updatedPlaceContext = this.updatePlacementContext(placeContext);
        if (updatedPlaceContext == null) {
            return InteractionResult.FAIL;
        }
        BlockState placementState = this.getPlacementState(updatedPlaceContext);
        if (placementState == null) {
            return InteractionResult.FAIL;
        }
        if (!this.placeBlock(updatedPlaceContext, placementState)) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = updatedPlaceContext.getClickedPos();
        Level level = updatedPlaceContext.getLevel();
        Player player = updatedPlaceContext.getPlayer();
        ItemStack itemStack = updatedPlaceContext.getItemInHand();
        BlockState placedState = level.getBlockState(pos);
        if (placedState.is(placementState.getBlock())) {
            placedState = this.updateBlockStateFromTag(pos, level, itemStack, placedState);
            this.updateCustomBlockEntityTag(pos, level, player, itemStack, placedState);
            BlockItem.updateBlockEntityComponents(level, pos, itemStack);
            placedState.getBlock().setPlacedBy(level, pos, placedState, player, itemStack);
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, itemStack);
            }
        }
        SoundType soundType = placedState.getSoundType();
        level.playSound((Entity)player, pos, this.getPlaceSound(placedState), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch() * 0.8f);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, placedState));
        itemStack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    protected SoundEvent getPlaceSound(BlockState blockState) {
        return blockState.getSoundType().getPlaceSound();
    }

    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        return context;
    }

    private static void updateBlockEntityComponents(Level level, BlockPos pos, ItemStack itemStack) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) {
            entity.applyComponentsFromItemStack(itemStack);
            entity.setChanged();
        }
    }

    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack itemStack, BlockState placedState) {
        return BlockItem.updateCustomBlockEntityTag(level, player, pos, itemStack);
    }

    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState stateForPlacement = this.getBlock().getStateForPlacement(context);
        return stateForPlacement != null && this.canPlace(context, stateForPlacement) ? stateForPlacement : null;
    }

    private BlockState updateBlockStateFromTag(BlockPos pos, Level level, ItemStack itemStack, BlockState placedState) {
        BlockItemStateProperties blockState = itemStack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (blockState.isEmpty()) {
            return placedState;
        }
        BlockState modifiedState = blockState.apply(placedState);
        if (modifiedState != placedState) {
            level.setBlock(pos, modifiedState, 2);
        }
        return modifiedState;
    }

    protected boolean canPlace(BlockPlaceContext context, BlockState stateForPlacement) {
        Player player = context.getPlayer();
        return (!this.mustSurvive() || stateForPlacement.canSurvive(context.getLevel(), context.getClickedPos())) && context.getLevel().isUnobstructed(stateForPlacement, context.getClickedPos(), CollisionContext.placementContext(player));
    }

    protected boolean mustSurvive() {
        return true;
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState placementState) {
        return context.getLevel().setBlock(context.getClickedPos(), placementState, 11);
    }

    public static boolean updateCustomBlockEntityTag(Level level, @Nullable Player player, BlockPos pos, ItemStack itemStack) {
        BlockEntity blockEntity;
        if (level.isClientSide()) {
            return false;
        }
        TypedEntityData<BlockEntityType<?>> customData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null && (blockEntity = level.getBlockEntity(pos)) != null) {
            BlockEntityType<?> type = blockEntity.getType();
            if (type != customData.type()) {
                return false;
            }
            if (type.onlyOpCanSetNbt() && (player == null || !player.canUseGameMasterBlocks())) {
                return false;
            }
            return customData.loadInto(blockEntity, level.registryAccess());
        }
        return false;
    }

    @Override
    public boolean shouldPrintOpWarning(ItemStack stack, @Nullable Player player) {
        TypedEntityData<BlockEntityType<?>> blockEntityData;
        if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && (blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA)) != null) {
            return blockEntityData.type().onlyOpCanSetNbt();
        }
        return false;
    }

    public Block getBlock() {
        return this.block;
    }

    public void registerBlocks(Map<Block, Item> map, Item item) {
        map.put(this.getBlock(), item);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return !(this.getBlock() instanceof ShulkerBoxBlock);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemContainerContents container = entity.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (container != null) {
            ItemUtils.onContainerDestroyed(entity, container.nonEmptyItemCopyStream());
        }
    }

    public static void setBlockEntityData(ItemStack stack, BlockEntityType<?> type, TagValueOutput output) {
        output.discard("id");
        if (output.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        } else {
            BlockEntity.addEntityType(output, type);
            stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(type, output.buildResult()));
        }
    }
}

