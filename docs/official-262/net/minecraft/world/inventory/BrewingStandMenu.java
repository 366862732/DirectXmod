/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;

public class BrewingStandMenu
extends AbstractContainerMenu {
    private static final Identifier EMPTY_SLOT_FUEL = Identifier.withDefaultNamespace("container/slot/brewing_fuel");
    private static final Identifier EMPTY_SLOT_POTION = Identifier.withDefaultNamespace("container/slot/potion");
    private static final int BOTTLE_SLOT_START = 0;
    private static final int BOTTLE_SLOT_END = 2;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int SLOT_COUNT = 5;
    private static final int DATA_COUNT = 2;
    private static final int INV_SLOT_START = 5;
    private static final int INV_SLOT_END = 32;
    private static final int USE_ROW_SLOT_START = 32;
    private static final int USE_ROW_SLOT_END = 41;
    private final Container brewingStand;
    private final ContainerData brewingStandData;
    private final Slot ingredientSlot;

    public BrewingStandMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(2));
    }

    public BrewingStandMenu(int containerId, Inventory inventory, Container brewingStand, ContainerData brewingStandData) {
        super(MenuType.BREWING_STAND, containerId);
        BrewingStandMenu.checkContainerSize(brewingStand, 5);
        BrewingStandMenu.checkContainerDataCount(brewingStandData, 2);
        this.brewingStand = brewingStand;
        this.brewingStandData = brewingStandData;
        PotionBrewing potionBrewing = inventory.player.level().potionBrewing();
        this.addSlot(new PotionSlot(brewingStand, 0, 56, 51));
        this.addSlot(new PotionSlot(brewingStand, 1, 79, 58));
        this.addSlot(new PotionSlot(brewingStand, 2, 102, 51));
        this.ingredientSlot = this.addSlot(new IngredientsSlot(potionBrewing, brewingStand, 3, 79, 17));
        this.addSlot(new FuelSlot(brewingStand, 4, 17, 17));
        this.addDataSlots(brewingStandData);
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.brewingStand.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex >= 0 && slotIndex <= 2 || slotIndex == 3 || slotIndex == 4) {
                if (!this.moveItemStackTo(stack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (FuelSlot.mayPlaceItem(clicked) ? this.moveItemStackTo(stack, 4, 5, false) || this.ingredientSlot.mayPlace(stack) && !this.moveItemStackTo(stack, 3, 4, false) : (this.ingredientSlot.mayPlace(stack) ? !this.moveItemStackTo(stack, 3, 4, false) : (PotionSlot.mayPlaceItem(clicked) ? !this.moveItemStackTo(stack, 0, 3, false) : (slotIndex >= 5 && slotIndex < 32 ? !this.moveItemStackTo(stack, 32, 41, false) : (slotIndex >= 32 && slotIndex < 41 ? !this.moveItemStackTo(stack, 5, 32, false) : !this.moveItemStackTo(stack, 5, 41, false)))))) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, clicked);
        }
        return clicked;
    }

    public int getFuel() {
        return this.brewingStandData.get(1);
    }

    public int getBrewingTicks() {
        return this.brewingStandData.get(0);
    }

    private static class PotionSlot
    extends Slot {
        public PotionSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return PotionSlot.mayPlaceItem(itemStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            Optional<Holder<Potion>> potion = carried.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
            if (potion.isPresent() && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.BREWED_POTION.trigger(serverPlayer, potion.get());
            }
            super.onTake(player, carried);
        }

        public static boolean mayPlaceItem(ItemStack itemStack) {
            return itemStack.is(Items.POTION) || itemStack.is(Items.SPLASH_POTION) || itemStack.is(Items.LINGERING_POTION) || itemStack.is(Items.GLASS_BOTTLE);
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_SLOT_POTION;
        }
    }

    private static class IngredientsSlot
    extends Slot {
        private final PotionBrewing potionBrewing;

        public IngredientsSlot(PotionBrewing potionBrewing, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.potionBrewing = potionBrewing;
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return this.potionBrewing.isIngredient(itemStack);
        }
    }

    private static class FuelSlot
    extends Slot {
        public FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return FuelSlot.mayPlaceItem(itemStack);
        }

        public static boolean mayPlaceItem(ItemStack itemStack) {
            return itemStack.is(ItemTags.BREWING_FUEL);
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_SLOT_FUEL;
        }
    }
}

