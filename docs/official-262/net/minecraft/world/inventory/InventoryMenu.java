/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InventoryMenu
extends AbstractCraftingMenu {
    public static final int CONTAINER_ID = 0;
    public static final int RESULT_SLOT = 0;
    private static final int CRAFTING_GRID_WIDTH = 2;
    private static final int CRAFTING_GRID_HEIGHT = 2;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_COUNT = 4;
    public static final int CRAFT_SLOT_END = 5;
    public static final int ARMOR_SLOT_START = 5;
    public static final int ARMOR_SLOT_COUNT = 4;
    public static final int ARMOR_SLOT_END = 9;
    public static final int INV_SLOT_START = 9;
    public static final int INV_SLOT_END = 36;
    public static final int USE_ROW_SLOT_START = 36;
    public static final int USE_ROW_SLOT_END = 45;
    public static final int SHIELD_SLOT = 45;
    public static final Identifier EMPTY_ARMOR_SLOT_HELMET = Identifier.withDefaultNamespace("container/slot/helmet");
    public static final Identifier EMPTY_ARMOR_SLOT_CHESTPLATE = Identifier.withDefaultNamespace("container/slot/chestplate");
    public static final Identifier EMPTY_ARMOR_SLOT_LEGGINGS = Identifier.withDefaultNamespace("container/slot/leggings");
    public static final Identifier EMPTY_ARMOR_SLOT_BOOTS = Identifier.withDefaultNamespace("container/slot/boots");
    public static final Identifier EMPTY_ARMOR_SLOT_SHIELD = Identifier.withDefaultNamespace("container/slot/shield");
    private static final Map<EquipmentSlot, Identifier> TEXTURE_EMPTY_SLOTS = Map.of(EquipmentSlot.FEET, EMPTY_ARMOR_SLOT_BOOTS, EquipmentSlot.LEGS, EMPTY_ARMOR_SLOT_LEGGINGS, EquipmentSlot.CHEST, EMPTY_ARMOR_SLOT_CHESTPLATE, EquipmentSlot.HEAD, EMPTY_ARMOR_SLOT_HELMET);
    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public final boolean active;
    private final Player owner;

    public InventoryMenu(Inventory inventory, boolean active, final Player owner) {
        super(null, 0, 2, 2);
        this.active = active;
        this.owner = owner;
        this.addResultSlot(owner, 154, 28);
        this.addCraftingGridSlots(98, 18);
        for (int i = 0; i < 4; ++i) {
            EquipmentSlot slot = SLOT_IDS[i];
            Identifier emptyIcon = TEXTURE_EMPTY_SLOTS.get(slot);
            this.addSlot(new ArmorSlot(inventory, owner, slot, 39 - i, 8, 8 + i * 18, emptyIcon));
        }
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addSlot(new Slot(this, inventory, 40, 77, 62){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public void setByPlayer(ItemStack itemStack, ItemStack previous) {
                owner.onEquipItem(EquipmentSlot.OFFHAND, previous, itemStack);
                super.setByPlayer(itemStack, previous);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_ARMOR_SLOT_SHIELD;
            }
        });
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 36 && slot < 45 || slot == 45;
    }

    @Override
    public void slotsChanged(Container container) {
        Level level = this.owner.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            CraftingMenu.slotChangedCraftingGrid(this, level2, this.owner, this.craftSlots, this.resultSlots, null);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.resultSlots.clearContent();
        if (player.level().isClientSide()) {
            return;
        }
        this.clearContainer(player, this.craftSlots);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot.hasItem()) {
            int pos;
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            EquipmentSlot eqSlot = player.getEquipmentSlotForItem(clicked);
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(stack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= 1 && slotIndex < 5 ? !this.moveItemStackTo(stack, 9, 45, false) : (slotIndex >= 5 && slotIndex < 9 ? !this.moveItemStackTo(stack, 9, 45, false) : (eqSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !((Slot)this.slots.get(8 - eqSlot.getIndex())).hasItem() ? !this.moveItemStackTo(stack, pos = 8 - eqSlot.getIndex(), pos + 1, false) : (eqSlot == EquipmentSlot.OFFHAND && !((Slot)this.slots.get(45)).hasItem() ? !this.moveItemStackTo(stack, 45, 46, false) : (slotIndex >= 9 && slotIndex < 36 ? !this.moveItemStackTo(stack, 36, 45, false) : (slotIndex >= 36 && slotIndex < 45 ? !this.moveItemStackTo(stack, 9, 36, false) : !this.moveItemStackTo(stack, 9, 45, false))))))) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, clicked);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            if (slotIndex == 0) {
                player.drop(stack, false);
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return (Slot)this.slots.get(0);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(1, 5);
    }

    public CraftingContainer getCraftSlots() {
        return this.craftSlots;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.owner;
    }
}

