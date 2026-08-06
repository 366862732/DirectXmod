/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 */
package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GrindstoneMenu
extends AbstractContainerMenu {
    public static final int MAX_NAME_LENGTH = 35;
    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final Container resultSlots = new ResultContainer();
    private final Container repairSlots = new SimpleContainer(this, 2){
        final /* synthetic */ GrindstoneMenu this$0;
        {
            GrindstoneMenu grindstoneMenu = this$0;
            Objects.requireNonNull(grindstoneMenu);
            this.this$0 = grindstoneMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.this$0.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;

    public GrindstoneMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public GrindstoneMenu(int containerId, Inventory inventory, final ContainerLevelAccess access) {
        super(MenuType.GRINDSTONE, containerId);
        this.access = access;
        this.addSlot(new Slot(this, this.repairSlots, 0, 49, 19){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack);
            }
        });
        this.addSlot(new Slot(this, this.repairSlots, 1, 49, 40){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack);
            }
        });
        this.addSlot(new Slot(this, this.resultSlots, 2, 129, 34){
            final /* synthetic */ GrindstoneMenu this$0;
            {
                GrindstoneMenu grindstoneMenu = this$0;
                Objects.requireNonNull(grindstoneMenu);
                this.this$0 = grindstoneMenu;
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                access.execute((level, pos) -> {
                    if (level instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel)level;
                        ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), this.getExperienceAmount((Level)level));
                    }
                    level.levelEvent(1042, (BlockPos)pos, 0);
                });
                this.this$0.repairSlots.setItem(0, ItemStack.EMPTY);
                this.this$0.repairSlots.setItem(1, ItemStack.EMPTY);
            }

            private int getExperienceAmount(Level level) {
                int amount = 0;
                amount += this.getExperienceFromItem(this.this$0.repairSlots.getItem(0));
                if ((amount += this.getExperienceFromItem(this.this$0.repairSlots.getItem(1))) > 0) {
                    int halfAmount = (int)Math.ceil((double)amount / 2.0);
                    return halfAmount + level.getRandom().nextInt(halfAmount);
                }
                return 0;
            }

            private int getExperienceFromItem(ItemStack item) {
                int amount = 0;
                ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                    Holder enchant = (Holder)entry.getKey();
                    int lvl = entry.getIntValue();
                    if (enchant.is(EnchantmentTags.CURSE)) continue;
                    amount += ((Enchantment)enchant.value()).getMinCost(lvl);
                }
                return amount;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.repairSlots) {
            this.createResult();
        }
    }

    private void createResult() {
        this.resultSlots.setItem(0, this.computeResult(this.repairSlots.getItem(0), this.repairSlots.getItem(1)));
        this.broadcastChanges();
    }

    private ItemStack computeResult(ItemStack input, ItemStack additional) {
        boolean hasBothItems;
        boolean hasAnItem;
        boolean bl = hasAnItem = !input.isEmpty() || !additional.isEmpty();
        if (!hasAnItem) {
            return ItemStack.EMPTY;
        }
        if (input.getCount() > 1 || additional.getCount() > 1) {
            return ItemStack.EMPTY;
        }
        boolean bl2 = hasBothItems = !input.isEmpty() && !additional.isEmpty();
        if (!hasBothItems) {
            ItemStack item;
            ItemStack itemStack = item = !input.isEmpty() ? input : additional;
            if (!EnchantmentHelper.hasAnyEnchantments(item)) {
                return ItemStack.EMPTY;
            }
            return this.removeNonCursesFrom(item.copy());
        }
        return this.mergeItems(input, additional);
    }

    private ItemStack mergeItems(ItemStack input, ItemStack additional) {
        ItemStack newItem;
        if (!input.is(additional.getItem())) {
            return ItemStack.EMPTY;
        }
        int durability = Math.max(input.getMaxDamage(), additional.getMaxDamage());
        int remaining1 = input.getMaxDamage() - input.getDamageValue();
        int remaining2 = additional.getMaxDamage() - additional.getDamageValue();
        int remaining = remaining1 + remaining2 + durability * 5 / 100;
        int count = 1;
        if (!input.isDamageableItem()) {
            if (input.getMaxStackSize() < 2 || !ItemStack.matches(input, additional)) {
                return ItemStack.EMPTY;
            }
            count = 2;
        }
        if ((newItem = input.copyWithCount(count)).isDamageableItem()) {
            newItem.set(DataComponents.MAX_DAMAGE, durability);
            newItem.setDamageValue(Math.max(durability - remaining, 0));
        }
        this.mergeEnchantsFrom(newItem, additional);
        return this.removeNonCursesFrom(newItem);
    }

    private void mergeEnchantsFrom(ItemStack target, ItemStack source) {
        EnchantmentHelper.updateEnchantments(target, newEnchantments -> {
            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(source);
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder enchant = (Holder)entry.getKey();
                if (enchant.is(EnchantmentTags.CURSE) && newEnchantments.getLevel(enchant) != 0) continue;
                newEnchantments.upgrade(enchant, entry.getIntValue());
            }
        });
    }

    private ItemStack removeNonCursesFrom(ItemStack item) {
        ItemEnchantments newEnchantments = EnchantmentHelper.updateEnchantments(item, enchantments -> enchantments.removeIf(enchantment -> !enchantment.is(EnchantmentTags.CURSE)));
        if (item.is(Items.ENCHANTED_BOOK) && newEnchantments.isEmpty()) {
            item = item.transmuteCopy(Items.BOOK);
        }
        int repairCost = 0;
        for (int i = 0; i < newEnchantments.size(); ++i) {
            repairCost = AnvilMenu.calculateIncreasedRepairCost(repairCost);
        }
        item.set(DataComponents.REPAIR_COST, repairCost);
        return item;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.repairSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return GrindstoneMenu.stillValid(this.access, player, Blocks.GRINDSTONE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack item = slot.getItem();
            clicked = item.copy();
            ItemStack input = this.repairSlots.getItem(0);
            ItemStack additional = this.repairSlots.getItem(1);
            if (slotIndex == 2) {
                if (!this.moveItemStackTo(item, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(item, clicked);
            } else if (slotIndex == 0 || slotIndex == 1 ? !this.moveItemStackTo(item, 3, 39, false) : (input.isEmpty() || additional.isEmpty() ? !this.moveItemStackTo(item, 0, 2, false) : (slotIndex >= 3 && slotIndex < 30 ? !this.moveItemStackTo(item, 30, 39, false) : slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(item, 3, 30, false)))) {
                return ItemStack.EMPTY;
            }
            if (item.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (item.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, item);
        }
        return clicked;
    }
}

