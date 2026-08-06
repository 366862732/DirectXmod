/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ListBackedContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity
extends BlockEntity
implements ListBackedContainer {
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_LAST_INTERACTED_SLOT = -1;
    private final NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public ChiseledBookShelfBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.CHISELED_BOOKSHELF, worldPosition, blockState);
    }

    private void updateState(int interactedSlot) {
        if (interactedSlot < 0 || interactedSlot >= 6) {
            LOGGER.error("Expected slot 0-5, got {}", (Object)interactedSlot);
            return;
        }
        this.lastInteractedSlot = interactedSlot;
        BlockState updatedState = this.getBlockState();
        for (int slot = 0; slot < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); ++slot) {
            boolean slotIsOccupied = !this.getItem(slot).isEmpty();
            BooleanProperty slotProperty = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot);
            updatedState = (BlockState)updatedState.setValue(slotProperty, slotIsOccupied);
        }
        Objects.requireNonNull(this.level).setBlock(this.worldPosition, updatedState, 3);
        this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(updatedState));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        this.lastInteractedSlot = input.getIntOr("last_interacted_slot", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items, true);
        output.putInt("last_interacted_slot", this.lastInteractedSlot);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean acceptsItemType(ItemStack itemStack) {
        return itemStack.is(ItemTags.BOOKSHELF_BOOKS);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack retrievedItem = Objects.requireNonNullElse(this.getItems().get(slot), ItemStack.EMPTY);
        this.getItems().set(slot, ItemStack.EMPTY);
        if (!retrievedItem.isEmpty()) {
            this.updateState(slot);
        }
        return retrievedItem;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (this.acceptsItemType(itemStack)) {
            this.getItems().set(slot, itemStack);
            this.updateState(slot);
        } else if (itemStack.isEmpty()) {
            this.removeItem(slot, this.getMaxStackSize());
        }
    }

    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack itemStack) {
        return into.hasAnyMatching(toItem -> {
            if (toItem.isEmpty()) {
                return true;
            }
            return ItemStack.isSameItemSameComponents(itemStack, toItem) && toItem.getCount() + itemStack.getCount() <= into.getMaxStackSize((ItemStack)toItem);
        });
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public int getLastInteractedSlot() {
        return this.lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("Items");
    }
}

