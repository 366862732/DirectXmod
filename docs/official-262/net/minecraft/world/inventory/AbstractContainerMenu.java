/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Supplier
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.HashBasedTable
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SLOT_CLICKED_OUTSIDE = -999;
    public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
    public static final int QUICKCRAFT_TYPE_GREEDY = 1;
    public static final int QUICKCRAFT_TYPE_CLONE = 2;
    public static final int QUICKCRAFT_HEADER_START = 0;
    public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
    public static final int QUICKCRAFT_HEADER_END = 2;
    public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
    public static final int SLOTS_PER_ROW = 9;
    public static final int SLOT_SIZE = 18;
    private final NonNullList<ItemStack> lastSlots = NonNullList.create();
    public final NonNullList<Slot> slots = NonNullList.create();
    private final List<DataSlot> dataSlots = Lists.newArrayList();
    private ItemStack carried = ItemStack.EMPTY;
    private final NonNullList<RemoteSlot> remoteSlots = NonNullList.create();
    private final IntList remoteDataSlots = new IntArrayList();
    private RemoteSlot remoteCarried = RemoteSlot.PLACEHOLDER;
    private int stateId;
    private final @Nullable MenuType<?> menuType;
    public final int containerId;
    private int quickcraftType = -1;
    private int quickcraftStatus;
    private final Set<Slot> quickcraftSlots = Sets.newHashSet();
    private final List<ContainerListener> containerListeners = Lists.newArrayList();
    private @Nullable ContainerSynchronizer synchronizer;
    private boolean suppressRemoteUpdates;

    protected AbstractContainerMenu(@Nullable MenuType<?> menuType, int containerId) {
        this.menuType = menuType;
        this.containerId = containerId;
    }

    protected void addInventoryHotbarSlots(Container inventory, int left, int top) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(inventory, x, left + x * 18, top));
        }
    }

    protected void addInventoryExtendedSlots(Container inventory, int left, int top) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(inventory, x + (y + 1) * 9, left + x * 18, top + y * 18));
            }
        }
    }

    protected void addStandardInventorySlots(Container container, int left, int top) {
        this.addInventoryExtendedSlots(container, left, top);
        int hotbarSeparator = 4;
        int topToHotbar = 58;
        this.addInventoryHotbarSlots(container, left, top + 58);
    }

    protected static boolean stillValid(ContainerLevelAccess access, Player player, Block block) {
        return access.evaluate((level, pos) -> {
            if (!level.getBlockState((BlockPos)pos).is(block)) {
                return false;
            }
            return player.isWithinBlockInteractionRange((BlockPos)pos, 4.0);
        }, true);
    }

    public MenuType<?> getType() {
        if (this.menuType == null) {
            throw new UnsupportedOperationException("Unable to construct this menu by type");
        }
        return this.menuType;
    }

    protected static void checkContainerSize(Container container, int expected) {
        int actual = container.getContainerSize();
        if (actual < expected) {
            throw new IllegalArgumentException("Container size " + actual + " is smaller than expected " + expected);
        }
    }

    protected static void checkContainerDataCount(ContainerData data, int expected) {
        int actual = data.getCount();
        if (actual < expected) {
            throw new IllegalArgumentException("Container data count " + actual + " is smaller than expected " + expected);
        }
    }

    public boolean isValidSlotIndex(int slotIndex) {
        return slotIndex == -1 || slotIndex == -999 || slotIndex < this.slots.size();
    }

    protected Slot addSlot(Slot slot) {
        slot.index = this.slots.size();
        this.slots.add(slot);
        this.lastSlots.add(ItemStack.EMPTY);
        this.remoteSlots.add(this.synchronizer != null ? this.synchronizer.createSlot() : RemoteSlot.PLACEHOLDER);
        return slot;
    }

    protected DataSlot addDataSlot(DataSlot dataSlot) {
        this.dataSlots.add(dataSlot);
        this.remoteDataSlots.add(0);
        return dataSlot;
    }

    protected void addDataSlots(ContainerData container) {
        for (int i = 0; i < container.getCount(); ++i) {
            this.addDataSlot(DataSlot.forContainer(container, i));
        }
    }

    public void addSlotListener(ContainerListener listener) {
        if (this.containerListeners.contains(listener)) {
            return;
        }
        this.containerListeners.add(listener);
        this.broadcastChanges();
    }

    public void setSynchronizer(ContainerSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
        this.remoteCarried = synchronizer.createSlot();
        this.remoteSlots.replaceAll(ignored -> synchronizer.createSlot());
        this.sendAllDataToRemote();
    }

    public void sendAllDataToRemote() {
        ArrayList<ItemStack> itemsToSend = new ArrayList<ItemStack>(this.slots.size());
        int slotsSize = this.slots.size();
        for (int i = 0; i < slotsSize; ++i) {
            ItemStack slotContents = this.slots.get(i).getItem();
            itemsToSend.add(slotContents.copy());
            this.remoteSlots.get(i).force(slotContents);
        }
        ItemStack carried = this.getCarried();
        this.remoteCarried.force(carried);
        int slotsSize2 = this.dataSlots.size();
        for (int i = 0; i < slotsSize2; ++i) {
            this.remoteDataSlots.set(i, this.dataSlots.get(i).get());
        }
        if (this.synchronizer != null) {
            this.synchronizer.sendInitialData(this, itemsToSend, carried.copy(), this.remoteDataSlots.toIntArray());
        }
    }

    public void removeSlotListener(ContainerListener listener) {
        this.containerListeners.remove(listener);
    }

    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> itemStacks = NonNullList.create();
        for (Slot slot : this.slots) {
            itemStacks.add(slot.getItem());
        }
        return itemStacks;
    }

    public void broadcastChanges() {
        Object current;
        int i;
        for (i = 0; i < this.slots.size(); ++i) {
            current = this.slots.get(i).getItem();
            com.google.common.base.Supplier currentCopy = Suppliers.memoize(((ItemStack)current)::copy);
            this.triggerSlotListeners(i, (ItemStack)current, (Supplier<ItemStack>)currentCopy);
            this.synchronizeSlotToRemote(i, (ItemStack)current, (Supplier<ItemStack>)currentCopy);
        }
        this.synchronizeCarriedToRemote();
        for (i = 0; i < this.dataSlots.size(); ++i) {
            current = this.dataSlots.get(i);
            int currentValue = ((DataSlot)current).get();
            if (((DataSlot)current).checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(i, currentValue);
            }
            this.synchronizeDataSlotToRemote(i, currentValue);
        }
    }

    public void broadcastFullState() {
        Object current;
        int i;
        for (i = 0; i < this.slots.size(); ++i) {
            current = this.slots.get(i).getItem();
            this.triggerSlotListeners(i, (ItemStack)current, ((ItemStack)current)::copy);
        }
        for (i = 0; i < this.dataSlots.size(); ++i) {
            current = this.dataSlots.get(i);
            if (!((DataSlot)current).checkAndClearUpdateFlag()) continue;
            this.updateDataSlotListeners(i, ((DataSlot)current).get());
        }
        this.sendAllDataToRemote();
    }

    private void updateDataSlotListeners(int id, int currentValue) {
        for (ContainerListener containerListener : this.containerListeners) {
            containerListener.dataChanged(this, id, currentValue);
        }
    }

    private void triggerSlotListeners(int i, ItemStack current, Supplier<ItemStack> currentCopy) {
        ItemStack localExpected = this.lastSlots.get(i);
        if (!ItemStack.matches(localExpected, current)) {
            ItemStack newItem = currentCopy.get();
            this.lastSlots.set(i, newItem);
            for (ContainerListener containerListener : this.containerListeners) {
                containerListener.slotChanged(this, i, newItem);
            }
        }
    }

    private void synchronizeSlotToRemote(int i, ItemStack current, Supplier<ItemStack> currentCopy) {
        if (this.suppressRemoteUpdates) {
            return;
        }
        RemoteSlot remoteExpected = this.remoteSlots.get(i);
        if (!remoteExpected.matches(current)) {
            remoteExpected.force(current);
            if (this.synchronizer != null) {
                this.synchronizer.sendSlotChange(this, i, currentCopy.get());
            }
        }
    }

    private void synchronizeDataSlotToRemote(int i, int current) {
        if (this.suppressRemoteUpdates) {
            return;
        }
        int remoteExpected = this.remoteDataSlots.getInt(i);
        if (remoteExpected != current) {
            this.remoteDataSlots.set(i, current);
            if (this.synchronizer != null) {
                this.synchronizer.sendDataChange(this, i, current);
            }
        }
    }

    private void synchronizeCarriedToRemote() {
        if (this.suppressRemoteUpdates) {
            return;
        }
        ItemStack carriedItem = this.getCarried();
        if (!this.remoteCarried.matches(carriedItem)) {
            this.remoteCarried.force(carriedItem);
            if (this.synchronizer != null) {
                this.synchronizer.sendCarriedChange(this, carriedItem.copy());
            }
        }
    }

    public void setRemoteSlot(int slot, ItemStack itemStack) {
        this.remoteSlots.get(slot).force(itemStack);
    }

    public void setRemoteSlotUnsafe(int slot, HashedStack itemStack) {
        if (slot < 0 || slot >= this.remoteSlots.size()) {
            LOGGER.debug("Incorrect slot index: {} available slots: {}", (Object)slot, (Object)this.remoteSlots.size());
            return;
        }
        this.remoteSlots.get(slot).receive(itemStack);
    }

    public void setRemoteCarried(HashedStack carriedItem) {
        this.remoteCarried.receive(carriedItem);
    }

    public boolean clickMenuButton(Player player, int buttonId) {
        return false;
    }

    public Slot getSlot(int index) {
        return this.slots.get(index);
    }

    public abstract ItemStack quickMoveStack(Player var1, int var2);

    public void setSelectedBundleItemIndex(int slotIndex, int selectedItemIndex) {
        if (slotIndex >= 0 && slotIndex < this.slots.size()) {
            ItemStack itemStack = this.slots.get(slotIndex).getItem();
            BundleItem.toggleSelectedItem(itemStack, selectedItemIndex);
        }
    }

    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        try {
            this.doClick(slotIndex, buttonNum, containerInput, player);
        }
        catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Container click");
            CrashReportCategory category = report.addCategory("Click info");
            category.setDetail("Menu Type", () -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>");
            category.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            category.setDetail("Slot Count", this.slots.size());
            category.setDetail("Slot", slotIndex);
            category.setDetail("Button", buttonNum);
            category.setDetail("Type", (Object)containerInput);
            throw new ReportedException(report);
        }
    }

    private void doClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        block40: {
            block52: {
                int amount;
                block51: {
                    block47: {
                        ItemStack targetItemStack;
                        Slot target;
                        ItemStack source;
                        Inventory inventory;
                        block50: {
                            block49: {
                                block48: {
                                    block45: {
                                        ClickAction clickAction;
                                        block46: {
                                            block44: {
                                                block38: {
                                                    block43: {
                                                        ItemStack carriedItemStack;
                                                        block42: {
                                                            block41: {
                                                                block39: {
                                                                    inventory = player.getInventory();
                                                                    if (containerInput != ContainerInput.QUICK_CRAFT) break block38;
                                                                    int expectedStatus = this.quickcraftStatus;
                                                                    this.quickcraftStatus = AbstractContainerMenu.getQuickcraftHeader(buttonNum);
                                                                    if (expectedStatus == 1 && this.quickcraftStatus == 2 || expectedStatus == this.quickcraftStatus) break block39;
                                                                    this.resetQuickCraft();
                                                                    break block40;
                                                                }
                                                                if (!this.getCarried().isEmpty()) break block41;
                                                                this.resetQuickCraft();
                                                                break block40;
                                                            }
                                                            if (this.quickcraftStatus != 0) break block42;
                                                            this.quickcraftType = AbstractContainerMenu.getQuickcraftType(buttonNum);
                                                            if (AbstractContainerMenu.isValidQuickcraftType(this.quickcraftType, player)) {
                                                                this.quickcraftStatus = 1;
                                                                this.quickcraftSlots.clear();
                                                            } else {
                                                                this.resetQuickCraft();
                                                            }
                                                            break block40;
                                                        }
                                                        if (this.quickcraftStatus != 1) break block43;
                                                        Slot slot = this.slots.get(slotIndex);
                                                        if (!AbstractContainerMenu.canItemQuickReplace(slot, carriedItemStack = this.getCarried(), true) || !slot.mayPlace(carriedItemStack) || this.quickcraftType != 2 && carriedItemStack.getCount() <= this.quickcraftSlots.size() || !this.canDragTo(slot)) break block40;
                                                        this.quickcraftSlots.add(slot);
                                                        break block40;
                                                    }
                                                    if (this.quickcraftStatus == 2) {
                                                        if (!this.quickcraftSlots.isEmpty()) {
                                                            if (this.quickcraftSlots.size() == 1) {
                                                                int slot = this.quickcraftSlots.iterator().next().index;
                                                                this.resetQuickCraft();
                                                                this.doClick(slot, this.quickcraftType, ContainerInput.PICKUP, player);
                                                                return;
                                                            }
                                                            ItemStack source2 = this.getCarried().copy();
                                                            if (source2.isEmpty()) {
                                                                this.resetQuickCraft();
                                                                return;
                                                            }
                                                            int remaining = this.getCarried().getCount();
                                                            for (Slot slot : this.quickcraftSlots) {
                                                                ItemStack carriedItemStack = this.getCarried();
                                                                if (slot == null || !AbstractContainerMenu.canItemQuickReplace(slot, carriedItemStack, true) || !slot.mayPlace(carriedItemStack) || this.quickcraftType != 2 && carriedItemStack.getCount() < this.quickcraftSlots.size() || !this.canDragTo(slot)) continue;
                                                                int carry = slot.hasItem() ? slot.getItem().getCount() : 0;
                                                                int maxSize = Math.min(source2.getMaxStackSize(), slot.getMaxStackSize(source2));
                                                                int newCount = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(this.quickcraftSlots.size(), this.quickcraftType, source2) + carry, maxSize);
                                                                remaining -= newCount - carry;
                                                                slot.setByPlayer(source2.copyWithCount(newCount));
                                                            }
                                                            source2.setCount(remaining);
                                                            this.setCarried(source2);
                                                        }
                                                        this.resetQuickCraft();
                                                    } else {
                                                        this.resetQuickCraft();
                                                    }
                                                    break block40;
                                                }
                                                if (this.quickcraftStatus == 0) break block44;
                                                this.resetQuickCraft();
                                                break block40;
                                            }
                                            if (containerInput != ContainerInput.PICKUP && containerInput != ContainerInput.QUICK_MOVE || buttonNum != 0 && buttonNum != 1) break block45;
                                            ClickAction clickAction2 = clickAction = buttonNum == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
                                            if (slotIndex != -999) break block46;
                                            if (this.getCarried().isEmpty()) break block40;
                                            if (clickAction == ClickAction.PRIMARY) {
                                                player.drop(this.getCarried(), true);
                                                this.setCarried(ItemStack.EMPTY);
                                            } else {
                                                player.drop(this.getCarried().split(1), true);
                                            }
                                            break block40;
                                        }
                                        if (containerInput == ContainerInput.QUICK_MOVE) {
                                            if (slotIndex < 0) {
                                                return;
                                            }
                                            Slot slot = this.slots.get(slotIndex);
                                            if (!slot.mayPickup(player)) {
                                                return;
                                            }
                                            ItemStack clicked = this.quickMoveStack(player, slotIndex);
                                            while (!clicked.isEmpty() && ItemStack.isSameItem(slot.getItem(), clicked)) {
                                                clicked = this.quickMoveStack(player, slotIndex);
                                            }
                                        } else {
                                            if (slotIndex < 0) {
                                                return;
                                            }
                                            Slot slot = this.slots.get(slotIndex);
                                            ItemStack clicked = slot.getItem();
                                            ItemStack carried = this.getCarried();
                                            player.updateTutorialInventoryAction(carried, slot.getItem(), clickAction);
                                            if (!this.tryItemClickBehaviourOverride(player, clickAction, slot, clicked, carried)) {
                                                if (clicked.isEmpty()) {
                                                    if (!carried.isEmpty()) {
                                                        int amount2 = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                                                        this.setCarried(slot.safeInsert(carried, amount2));
                                                    }
                                                } else if (slot.mayPickup(player)) {
                                                    if (carried.isEmpty()) {
                                                        int amount3 = clickAction == ClickAction.PRIMARY ? clicked.getCount() : (clicked.getCount() + 1) / 2;
                                                        Optional<ItemStack> newCarried = slot.tryRemove(amount3, Integer.MAX_VALUE, player);
                                                        newCarried.ifPresent(itemsTaken -> {
                                                            this.setCarried((ItemStack)itemsTaken);
                                                            slot.onTake(player, (ItemStack)itemsTaken);
                                                        });
                                                    } else if (slot.mayPlace(carried)) {
                                                        if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                                                            int amount4 = clickAction == ClickAction.PRIMARY ? carried.getCount() : 1;
                                                            this.setCarried(slot.safeInsert(carried, amount4));
                                                        } else if (carried.getCount() <= slot.getMaxStackSize(carried)) {
                                                            this.setCarried(clicked);
                                                            slot.setByPlayer(carried);
                                                        }
                                                    } else if (ItemStack.isSameItemSameComponents(clicked, carried)) {
                                                        Optional<ItemStack> newCarried = slot.tryRemove(clicked.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                                                        newCarried.ifPresent(itemsTaken -> {
                                                            carried.grow(itemsTaken.getCount());
                                                            slot.onTake(player, (ItemStack)itemsTaken);
                                                        });
                                                    }
                                                }
                                            }
                                            slot.setChanged();
                                        }
                                        break block40;
                                    }
                                    if (containerInput != ContainerInput.SWAP || (buttonNum < 0 || buttonNum >= 9) && buttonNum != 40) break block47;
                                    source = inventory.getItem(buttonNum);
                                    target = this.slots.get(slotIndex);
                                    targetItemStack = target.getItem();
                                    if (source.isEmpty() && targetItemStack.isEmpty()) break block40;
                                    if (!source.isEmpty()) break block48;
                                    if (!target.mayPickup(player)) break block40;
                                    inventory.setItem(buttonNum, targetItemStack);
                                    target.onSwapCraft(targetItemStack.getCount());
                                    target.setByPlayer(ItemStack.EMPTY);
                                    target.onTake(player, targetItemStack);
                                    break block40;
                                }
                                if (!targetItemStack.isEmpty()) break block49;
                                if (!target.mayPlace(source)) break block40;
                                int maxStackSize = target.getMaxStackSize(source);
                                if (source.getCount() > maxStackSize) {
                                    target.setByPlayer(source.split(maxStackSize));
                                } else {
                                    inventory.setItem(buttonNum, ItemStack.EMPTY);
                                    target.setByPlayer(source);
                                }
                                break block40;
                            }
                            if (!target.mayPickup(player) || !target.mayPlace(source)) break block40;
                            int maxStackSize = target.getMaxStackSize(source);
                            if (source.getCount() <= maxStackSize) break block50;
                            target.setByPlayer(source.split(maxStackSize));
                            target.onTake(player, targetItemStack);
                            if (inventory.add(targetItemStack)) break block40;
                            player.drop(targetItemStack, true);
                            break block40;
                        }
                        inventory.setItem(buttonNum, targetItemStack);
                        target.setByPlayer(source);
                        target.onTake(player, targetItemStack);
                        break block40;
                    }
                    if (containerInput != ContainerInput.CLONE || !player.hasInfiniteMaterials() || !this.getCarried().isEmpty() || slotIndex < 0) break block51;
                    Slot slot = this.slots.get(slotIndex);
                    if (!slot.hasItem()) break block40;
                    this.setCarried(slot.safeClone(player));
                    break block40;
                }
                if (containerInput != ContainerInput.THROW || !this.getCarried().isEmpty() || slotIndex < 0) break block52;
                Slot slot = this.slots.get(slotIndex);
                int n = amount = buttonNum == 0 ? 1 : slot.getItem().getCount();
                if (!player.canDropItems()) {
                    return;
                }
                ItemStack itemStack = slot.safeTake(amount, Integer.MAX_VALUE, player);
                player.drop(itemStack, true);
                player.handleCreativeModeItemDrop(itemStack);
                if (buttonNum != 1) break block40;
                while (!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
                    if (!player.canDropItems()) {
                        return;
                    }
                    itemStack = slot.safeTake(amount, Integer.MAX_VALUE, player);
                    player.drop(itemStack, true);
                    player.handleCreativeModeItemDrop(itemStack);
                }
                break block40;
            }
            if (containerInput == ContainerInput.PICKUP_ALL && slotIndex >= 0) {
                Slot slot = this.slots.get(slotIndex);
                ItemStack carried = this.getCarried();
                if (!(carried.isEmpty() || slot.hasItem() && slot.mayPickup(player))) {
                    int start = buttonNum == 0 ? 0 : this.slots.size() - 1;
                    int step = buttonNum == 0 ? 1 : -1;
                    for (int pass = 0; pass < 2; ++pass) {
                        for (int i = start; i >= 0 && i < this.slots.size() && carried.getCount() < carried.getMaxStackSize(); i += step) {
                            Slot target = this.slots.get(i);
                            if (!target.hasItem() || !AbstractContainerMenu.canItemQuickReplace(target, carried, true) || !target.mayPickup(player) || !this.canTakeItemForPickAll(carried, target)) continue;
                            ItemStack itemStack = target.getItem();
                            if (pass == 0 && itemStack.getCount() == itemStack.getMaxStackSize()) continue;
                            ItemStack removed = target.safeTake(itemStack.getCount(), carried.getMaxStackSize() - carried.getCount(), player);
                            carried.grow(removed.getCount());
                        }
                    }
                }
            }
        }
    }

    private boolean tryItemClickBehaviourOverride(Player player, ClickAction clickAction, Slot slot, ItemStack clicked, ItemStack carried) {
        FeatureFlagSet enabledFeatures = player.level().enabledFeatures();
        if (carried.isItemEnabled(enabledFeatures) && carried.overrideStackedOnOther(slot, clickAction, player)) {
            return true;
        }
        return clicked.isItemEnabled(enabledFeatures) && clicked.overrideOtherStackedOnMe(carried, slot, clickAction, player, this.createCarriedSlotAccess());
    }

    private SlotAccess createCarriedSlotAccess() {
        return new SlotAccess(this){
            final /* synthetic */ AbstractContainerMenu this$0;
            {
                AbstractContainerMenu abstractContainerMenu = this$0;
                Objects.requireNonNull(abstractContainerMenu);
                this.this$0 = abstractContainerMenu;
            }

            @Override
            public ItemStack get() {
                return this.this$0.getCarried();
            }

            @Override
            public boolean set(ItemStack itemStack) {
                this.this$0.setCarried(itemStack);
                return true;
            }
        };
    }

    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return true;
    }

    public void removed(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ItemStack carried = this.getCarried();
        if (!carried.isEmpty()) {
            AbstractContainerMenu.dropOrPlaceInInventory(player, carried);
            this.setCarried(ItemStack.EMPTY);
        }
    }

    private static void dropOrPlaceInInventory(Player player, ItemStack carried) {
        ServerPlayer serverPlayer;
        boolean serverPlayerHasDisconnected;
        boolean playerRemovedNotChangingDimension = player.isRemoved() && player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
        boolean bl = serverPlayerHasDisconnected = player instanceof ServerPlayer && (serverPlayer = (ServerPlayer)player).hasDisconnected();
        if (playerRemovedNotChangingDimension || serverPlayerHasDisconnected) {
            player.drop(carried, false);
        } else if (player instanceof ServerPlayer) {
            player.getInventory().placeItemBackInInventory(carried);
        }
    }

    protected void clearContainer(Player player, Container container) {
        for (int i = 0; i < container.getContainerSize(); ++i) {
            AbstractContainerMenu.dropOrPlaceInInventory(player, container.removeItemNoUpdate(i));
        }
    }

    public void slotsChanged(Container container) {
        this.broadcastChanges();
    }

    public void setItem(int slot, int stateId, ItemStack itemStack) {
        this.getSlot(slot).set(itemStack);
        this.stateId = stateId;
    }

    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        for (int i = 0; i < items.size(); ++i) {
            this.getSlot(i).set(items.get(i));
        }
        this.carried = carried;
        this.stateId = stateId;
    }

    public void setData(int id, int value) {
        this.dataSlots.get(id).set(value);
    }

    public abstract boolean stillValid(Player var1);

    protected boolean moveItemStackTo(ItemStack itemStack, int startSlot, int endSlot, boolean backwards) {
        ItemStack target;
        Slot slot;
        boolean anythingChanged = false;
        int destSlot = startSlot;
        if (backwards) {
            destSlot = endSlot - 1;
        }
        if (itemStack.isStackable()) {
            while (!itemStack.isEmpty() && (backwards ? destSlot >= startSlot : destSlot < endSlot)) {
                slot = this.slots.get(destSlot);
                target = slot.getItem();
                if (!target.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, target)) {
                    int maxStackSize;
                    int totalStack = target.getCount() + itemStack.getCount();
                    if (totalStack <= (maxStackSize = slot.getMaxStackSize(target))) {
                        itemStack.setCount(0);
                        target.setCount(totalStack);
                        slot.setChanged();
                        anythingChanged = true;
                    } else if (target.getCount() < maxStackSize) {
                        itemStack.shrink(maxStackSize - target.getCount());
                        target.setCount(maxStackSize);
                        slot.setChanged();
                        anythingChanged = true;
                    }
                }
                if (backwards) {
                    --destSlot;
                    continue;
                }
                ++destSlot;
            }
        }
        if (!itemStack.isEmpty()) {
            destSlot = backwards ? endSlot - 1 : startSlot;
            while (backwards ? destSlot >= startSlot : destSlot < endSlot) {
                slot = this.slots.get(destSlot);
                target = slot.getItem();
                if (target.isEmpty() && slot.mayPlace(itemStack)) {
                    int maxStackSize = slot.getMaxStackSize(itemStack);
                    slot.setByPlayer(itemStack.split(Math.min(itemStack.getCount(), maxStackSize)));
                    slot.setChanged();
                    anythingChanged = true;
                    break;
                }
                if (backwards) {
                    --destSlot;
                    continue;
                }
                ++destSlot;
            }
        }
        return anythingChanged;
    }

    public static int getQuickcraftType(int mask) {
        return mask >> 2 & 3;
    }

    public static int getQuickcraftHeader(int mask) {
        return mask & 3;
    }

    public static int getQuickcraftMask(int header, int type) {
        return header & 3 | (type & 3) << 2;
    }

    public static boolean isValidQuickcraftType(int type, Player player) {
        if (type == 0) {
            return true;
        }
        if (type == 1) {
            return true;
        }
        return type == 2 && player.hasInfiniteMaterials();
    }

    protected void resetQuickCraft() {
        this.quickcraftStatus = 0;
        this.quickcraftSlots.clear();
    }

    public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack itemStack, boolean ignoreSize) {
        boolean slotIsEmpty;
        boolean bl = slotIsEmpty = slot == null || !slot.hasItem();
        if (!slotIsEmpty && ItemStack.isSameItemSameComponents(itemStack, slot.getItem())) {
            return slot.getItem().getCount() + (ignoreSize ? 0 : itemStack.getCount()) <= itemStack.getMaxStackSize();
        }
        return slotIsEmpty;
    }

    public static int getQuickCraftPlaceCount(int quickCraftSlotsSize, int quickCraftingType, ItemStack itemStack) {
        return switch (quickCraftingType) {
            case 0 -> Mth.floor((float)itemStack.getCount() / (float)quickCraftSlotsSize);
            case 1 -> 1;
            case 2 -> itemStack.getMaxStackSize();
            default -> itemStack.getCount();
        };
    }

    public boolean canDragTo(Slot slot) {
        return true;
    }

    public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof Container) {
            Container container = (Container)((Object)blockEntity);
            return AbstractContainerMenu.getRedstoneSignalFromContainer(container);
        }
        return 0;
    }

    public static int getRedstoneSignalFromContainer(@Nullable Container container) {
        if (container == null) {
            return 0;
        }
        float totalPercent = 0.0f;
        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemStack = container.getItem(i);
            if (itemStack.isEmpty()) continue;
            totalPercent += (float)itemStack.getCount() / (float)container.getMaxStackSize(itemStack);
        }
        return Mth.lerpDiscrete(totalPercent /= (float)container.getContainerSize(), 0, 15);
    }

    public void setCarried(ItemStack carried) {
        this.carried = carried;
    }

    public ItemStack getCarried() {
        return this.carried;
    }

    public void suppressRemoteUpdates() {
        this.suppressRemoteUpdates = true;
    }

    public void resumeRemoteUpdates() {
        this.suppressRemoteUpdates = false;
    }

    public void transferState(AbstractContainerMenu otherContainer) {
        Slot slot;
        int slotIndex;
        HashBasedTable otherSlots = HashBasedTable.create();
        for (slotIndex = 0; slotIndex < otherContainer.slots.size(); ++slotIndex) {
            slot = otherContainer.slots.get(slotIndex);
            otherSlots.put((Object)slot.container, (Object)slot.getContainerSlot(), (Object)slotIndex);
        }
        for (slotIndex = 0; slotIndex < this.slots.size(); ++slotIndex) {
            slot = this.slots.get(slotIndex);
            Integer otherSlotIndex = (Integer)otherSlots.get((Object)slot.container, (Object)slot.getContainerSlot());
            if (otherSlotIndex == null) continue;
            this.lastSlots.set(slotIndex, otherContainer.lastSlots.get(otherSlotIndex));
            RemoteSlot sourceRemoteSlot = otherContainer.remoteSlots.get(otherSlotIndex);
            RemoteSlot targetRemoteSlot = this.remoteSlots.get(slotIndex);
            if (!(sourceRemoteSlot instanceof RemoteSlot.Synchronized)) continue;
            RemoteSlot.Synchronized synchronizedSource = (RemoteSlot.Synchronized)sourceRemoteSlot;
            if (!(targetRemoteSlot instanceof RemoteSlot.Synchronized)) continue;
            RemoteSlot.Synchronized synchronizedTarget = (RemoteSlot.Synchronized)targetRemoteSlot;
            synchronizedTarget.copyFrom(synchronizedSource);
        }
    }

    public OptionalInt findSlot(Container inventory, int slotIndex) {
        for (int i = 0; i < this.slots.size(); ++i) {
            Slot slot = this.slots.get(i);
            if (slot.container != inventory || slotIndex != slot.getContainerSlot()) continue;
            return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    public int getStateId() {
        return this.stateId;
    }

    public int incrementStateId() {
        this.stateId = this.stateId + 1 & Short.MAX_VALUE;
        return this.stateId;
    }
}

