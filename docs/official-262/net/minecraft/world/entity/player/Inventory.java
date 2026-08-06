/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 */
package net.minecraft.world.entity.player;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class Inventory
implements Container,
Nameable {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    public static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int SLOT_BODY_ARMOR = 41;
    public static final int SLOT_SADDLE = 42;
    public static final int NOT_FOUND_INDEX = -1;
    public static final Int2ObjectMap<EquipmentSlot> EQUIPMENT_SLOT_MAPPING = new Int2ObjectArrayMap(Map.of(EquipmentSlot.FEET.getIndex(36), EquipmentSlot.FEET, EquipmentSlot.LEGS.getIndex(36), EquipmentSlot.LEGS, EquipmentSlot.CHEST.getIndex(36), EquipmentSlot.CHEST, EquipmentSlot.HEAD.getIndex(36), EquipmentSlot.HEAD, 40, EquipmentSlot.OFFHAND, 41, EquipmentSlot.BODY, 42, EquipmentSlot.SADDLE));
    private static final Component DEFAULT_NAME = Component.translatable("container.inventory");
    private final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    private int selected;
    public final Player player;
    private final EntityEquipment equipment;
    private int timesChanged;

    public Inventory(Player player, EntityEquipment equipment) {
        this.player = player;
        this.equipment = equipment;
    }

    public int getSelectedSlot() {
        return this.selected;
    }

    public void setSelectedSlot(int selected) {
        if (!Inventory.isHotbarSlot(selected)) {
            throw new IllegalArgumentException("Invalid selected slot");
        }
        this.selected = selected;
    }

    public ItemStack getSelectedItem() {
        return this.items.get(this.selected);
    }

    public ItemStack setSelectedItem(ItemStack itemStack) {
        return this.items.set(this.selected, itemStack);
    }

    public static int getSelectionSize() {
        return 9;
    }

    public NonNullList<ItemStack> getNonEquipmentItems() {
        return this.items;
    }

    private boolean hasRemainingSpaceForItem(ItemStack slotItemStack, ItemStack newItemStack) {
        return !slotItemStack.isEmpty() && ItemStack.isSameItemSameComponents(slotItemStack, newItemStack) && slotItemStack.isStackable() && slotItemStack.getCount() < this.getMaxStackSize(slotItemStack);
    }

    public int getFreeSlot() {
        for (int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public void addAndPickItem(ItemStack itemStack) {
        int freeSlot;
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        if (!this.items.get(this.selected).isEmpty() && (freeSlot = this.getFreeSlot()) != -1) {
            this.items.set(freeSlot, this.items.get(this.selected));
        }
        this.items.set(this.selected, itemStack);
    }

    public void pickSlot(int slot) {
        this.setSelectedSlot(this.getSuitableHotbarSlot());
        ItemStack tmp = this.items.get(this.selected);
        this.items.set(this.selected, this.items.get(slot));
        this.items.set(slot, tmp);
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    public int findSlotMatchingItem(ItemStack itemStack) {
        for (int i = 0; i < this.items.size(); ++i) {
            if (this.items.get(i).isEmpty() || !ItemStack.isSameItemSameComponents(itemStack, this.items.get(i))) continue;
            return i;
        }
        return -1;
    }

    public static boolean isUsableForCrafting(ItemStack item) {
        return !item.isDamaged() && !item.isEnchanted() && !item.has(DataComponents.CUSTOM_NAME);
    }

    public int findSlotMatchingCraftingIngredient(Holder<Item> item, ItemStack existingItem) {
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack inventoryItemStack = this.items.get(i);
            if (inventoryItemStack.isEmpty() || !inventoryItemStack.is(item) || !Inventory.isUsableForCrafting(inventoryItemStack) || !existingItem.isEmpty() && !ItemStack.isSameItemSameComponents(existingItem, inventoryItemStack)) continue;
            return i;
        }
        return -1;
    }

    public int getSuitableHotbarSlot() {
        int index;
        int slot;
        for (slot = 0; slot < 9; ++slot) {
            index = (this.selected + slot) % 9;
            if (!this.items.get(index).isEmpty()) continue;
            return index;
        }
        for (slot = 0; slot < 9; ++slot) {
            index = (this.selected + slot) % 9;
            if (this.items.get(index).isEnchanted()) continue;
            return index;
        }
        return this.selected;
    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> predicate, int amountToRemove, Container craftSlots) {
        int count = 0;
        boolean countingOnly = amountToRemove == 0;
        count += ContainerHelper.clearOrCountMatchingItems(this, predicate, amountToRemove - count, countingOnly);
        count += ContainerHelper.clearOrCountMatchingItems(craftSlots, predicate, amountToRemove - count, countingOnly);
        ItemStack carried = this.player.containerMenu.getCarried();
        count += ContainerHelper.clearOrCountMatchingItems(carried, predicate, amountToRemove - count, countingOnly);
        if (carried.isEmpty()) {
            this.player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        return count;
    }

    private int addResource(ItemStack itemStack) {
        int slot = this.getSlotWithRemainingSpace(itemStack);
        if (slot == -1) {
            slot = this.getFreeSlot();
        }
        if (slot == -1) {
            return itemStack.getCount();
        }
        return this.addResource(slot, itemStack);
    }

    private int addResource(int slot, ItemStack itemStack) {
        int maxToAdd;
        int toAdd;
        int count = itemStack.getCount();
        ItemStack itemStackInSlot = this.getItem(slot);
        if (itemStackInSlot.isEmpty()) {
            itemStackInSlot = itemStack.copyWithCount(0);
            this.setItem(slot, itemStackInSlot);
        }
        if ((toAdd = Math.min(count, maxToAdd = this.getMaxStackSize(itemStackInSlot) - itemStackInSlot.getCount())) == 0) {
            return count;
        }
        itemStackInSlot.grow(toAdd);
        itemStackInSlot.setPopTime(5);
        return count -= toAdd;
    }

    public int getSlotWithRemainingSpace(ItemStack newItemStack) {
        if (this.hasRemainingSpaceForItem(this.getItem(this.selected), newItemStack)) {
            return this.selected;
        }
        if (this.hasRemainingSpaceForItem(this.getItem(40), newItemStack)) {
            return 40;
        }
        for (int i = 0; i < this.items.size(); ++i) {
            if (!this.hasRemainingSpaceForItem(this.items.get(i), newItemStack)) continue;
            return i;
        }
        return -1;
    }

    public void tick() {
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) continue;
            itemStack.inventoryTick(this.player.level(), this.player, i == this.selected ? EquipmentSlot.MAINHAND : null);
        }
    }

    public boolean add(ItemStack itemStack) {
        return this.add(-1, itemStack);
    }

    public boolean add(int slot, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        try {
            if (!itemStack.isDamaged()) {
                int lastSize;
                do {
                    lastSize = itemStack.getCount();
                    if (slot == -1) {
                        itemStack.setCount(this.addResource(itemStack));
                        continue;
                    }
                    itemStack.setCount(this.addResource(slot, itemStack));
                } while (!itemStack.isEmpty() && itemStack.getCount() < lastSize);
                if (itemStack.getCount() == lastSize && this.player.hasInfiniteMaterials()) {
                    itemStack.setCount(0);
                    return true;
                }
                return itemStack.getCount() < lastSize;
            }
            if (slot == -1) {
                slot = this.getFreeSlot();
            }
            if (slot >= 0) {
                this.items.set(slot, itemStack.copyAndClear());
                this.items.get(slot).setPopTime(5);
                return true;
            }
            if (this.player.hasInfiniteMaterials()) {
                itemStack.setCount(0);
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Adding item to inventory");
            CrashReportCategory category = report.addCategory("Item being added");
            category.setDetail("Item ID", Item.getId(itemStack.getItem()));
            category.setDetail("Item data", itemStack.getDamageValue());
            category.setDetail("Item name", () -> itemStack.getHoverName().getString());
            throw new ReportedException(report);
        }
    }

    public void placeItemBackInInventory(ItemStack itemStack) {
        this.placeItemBackInInventory(itemStack, true);
    }

    public void placeItemBackInInventory(ItemStack itemStack, boolean shouldSendSetSlotPacket) {
        while (!itemStack.isEmpty()) {
            Player player;
            int slot = this.getSlotWithRemainingSpace(itemStack);
            if (slot == -1) {
                slot = this.getFreeSlot();
            }
            if (slot == -1) {
                this.player.drop(itemStack, false);
                break;
            }
            int slotHasSpaceFor = itemStack.getMaxStackSize() - this.getItem(slot).getCount();
            if (!this.add(slot, itemStack.split(slotHasSpaceFor)) || !shouldSendSetSlotPacket || !((player = this.player) instanceof ServerPlayer)) continue;
            ServerPlayer serverPlayer = (ServerPlayer)player;
            serverPlayer.connection.send(this.createInventoryUpdatePacket(slot));
        }
    }

    public ClientboundSetPlayerInventoryPacket createInventoryUpdatePacket(int slot) {
        return new ClientboundSetPlayerInventoryPacket(slot, this.getItem(slot).copy());
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack itemStack;
        if (slot < this.items.size()) {
            return ContainerHelper.removeItem(this.items, slot, count);
        }
        EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
        if (equipmentSlot != null && !(itemStack = this.equipment.get(equipmentSlot)).isEmpty()) {
            return itemStack.split(count);
        }
        return ItemStack.EMPTY;
    }

    public void removeItem(ItemStack itemStack) {
        for (int slot = 0; slot < this.items.size(); ++slot) {
            if (this.items.get(slot) != itemStack) continue;
            this.items.set(slot, ItemStack.EMPTY);
            return;
        }
        for (EquipmentSlot equipmentSlot : EQUIPMENT_SLOT_MAPPING.values()) {
            ItemStack stackInSlot = this.equipment.get(equipmentSlot);
            if (stackInSlot != itemStack) continue;
            this.equipment.set(equipmentSlot, ItemStack.EMPTY);
            return;
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < this.items.size()) {
            ItemStack itemStack = this.items.get(slot);
            this.items.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
        EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
        if (equipmentSlot != null) {
            return this.equipment.set(equipmentSlot, ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        EquipmentSlot equipmentSlot;
        if (slot < this.items.size()) {
            this.items.set(slot, itemStack);
        }
        if ((equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot)) != null) {
            this.equipment.set(equipmentSlot, itemStack);
        }
    }

    public void save(ValueOutput.TypedOutputList<ItemStackWithSlot> output) {
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack item = this.items.get(i);
            if (item.isEmpty()) continue;
            output.add(new ItemStackWithSlot(i, item));
        }
    }

    public void load(ValueInput.TypedInputList<ItemStackWithSlot> input) {
        this.items.clear();
        for (ItemStackWithSlot item : input) {
            if (!item.isValidInContainer(this.items.size())) continue;
            this.setItem(item.slot(), item.stack());
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size() + EQUIPMENT_SLOT_MAPPING.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        for (EquipmentSlot slot : EQUIPMENT_SLOT_MAPPING.values()) {
            if (this.equipment.get(slot).isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < this.items.size()) {
            return this.items.get(slot);
        }
        EquipmentSlot equipmentSlot = (EquipmentSlot)EQUIPMENT_SLOT_MAPPING.get(slot);
        if (equipmentSlot != null) {
            return this.equipment.get(equipmentSlot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getName() {
        return DEFAULT_NAME;
    }

    public void dropAll() {
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.items.get(i);
            if (itemStack.isEmpty()) continue;
            this.player.drop(itemStack, true, false);
            this.items.set(i, ItemStack.EMPTY);
        }
        this.equipment.dropAll(this.player);
    }

    @Override
    public void setChanged() {
        ++this.timesChanged;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public boolean contains(ItemStack searchStack) {
        for (ItemStack itemStack : this) {
            if (itemStack.isEmpty() || !ItemStack.isSameItemSameComponents(itemStack, searchStack)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(TagKey<Item> tag) {
        for (ItemStack itemStack : this) {
            if (itemStack.isEmpty() || !itemStack.is(tag)) continue;
            return true;
        }
        return false;
    }

    public boolean contains(Predicate<ItemStack> predicate) {
        for (ItemStack stack : this) {
            if (!predicate.test(stack)) continue;
            return true;
        }
        return false;
    }

    public void replaceWith(Inventory other) {
        for (int i = 0; i < this.getContainerSize(); ++i) {
            this.setItem(i, other.getItem(i));
        }
        this.setSelectedSlot(other.getSelectedSlot());
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.equipment.clear();
    }

    public void fillStackedContents(StackedItemContents contents) {
        for (ItemStack itemStack : this.items) {
            contents.accountSimpleStack(itemStack);
        }
    }

    public ItemStack removeFromSelected(boolean all) {
        ItemStack selectedItem = this.getSelectedItem();
        if (selectedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return this.removeItem(this.selected, all ? selectedItem.getCount() : 1);
    }
}

