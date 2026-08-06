/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;

public class EnchantmentMenu
extends AbstractContainerMenu {
    private static final Identifier EMPTY_SLOT_LAPIS_LAZULI = Identifier.withDefaultNamespace("container/slot/lapis_lazuli");
    private final Container enchantSlots = new SimpleContainer(this, 2){
        final /* synthetic */ EnchantmentMenu this$0;
        {
            EnchantmentMenu enchantmentMenu = this$0;
            Objects.requireNonNull(enchantmentMenu);
            this.this$0 = enchantmentMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.this$0.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final RandomSource random = RandomSource.create();
    private final DataSlot enchantmentSeed = DataSlot.standalone();
    public final int[] costs = new int[3];
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};

    public EnchantmentMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public EnchantmentMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MenuType.ENCHANTMENT, containerId);
        this.access = access;
        this.addSlot(new Slot(this, this.enchantSlots, 0, 15, 47){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this, this.enchantSlots, 1, 35, 47){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.LAPIS_LAZULI);
            }

            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_SLOT_LAPIS_LAZULI;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlot(DataSlot.shared(this.costs, 0));
        this.addDataSlot(DataSlot.shared(this.costs, 1));
        this.addDataSlot(DataSlot.shared(this.costs, 2));
        this.addDataSlot(this.enchantmentSeed).set(inventory.player.getEnchantmentSeed());
        this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
        this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
        this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
        this.addDataSlot(DataSlot.shared(this.levelClue, 0));
        this.addDataSlot(DataSlot.shared(this.levelClue, 1));
        this.addDataSlot(DataSlot.shared(this.levelClue, 2));
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.enchantSlots) {
            ItemStack itemStack = container.getItem(0);
            if (itemStack.isEmpty() || !itemStack.isEnchantable()) {
                for (int i = 0; i < 3; ++i) {
                    this.costs[i] = 0;
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                }
            } else {
                this.access.execute((level, pos) -> {
                    int i;
                    IdMap<Holder<Holder<Enchantment>>> holders = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                    int bookcases = 0;
                    for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                        if (!EnchantingTableBlock.isValidBookShelf(level, pos, offset)) continue;
                        ++bookcases;
                    }
                    this.random.setSeed(this.enchantmentSeed.get());
                    for (i = 0; i < 3; ++i) {
                        this.costs[i] = EnchantmentHelper.getEnchantmentCost(this.random, i, bookcases, itemStack);
                        this.enchantClue[i] = -1;
                        this.levelClue[i] = -1;
                        if (this.costs[i] >= i + 1) continue;
                        this.costs[i] = 0;
                    }
                    for (i = 0; i < 3; ++i) {
                        List<EnchantmentInstance> list;
                        if (this.costs[i] <= 0 || (list = this.getEnchantmentList(level.registryAccess(), itemStack, i, this.costs[i])).isEmpty()) continue;
                        EnchantmentInstance ench = list.get(this.random.nextInt(list.size()));
                        this.enchantClue[i] = holders.getId(ench.enchantment());
                        this.levelClue[i] = ench.level();
                    }
                    this.broadcastChanges();
                });
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId < 0 || buttonId >= this.costs.length) {
            Util.logAndPauseIfInIde(player.getPlainTextName() + " pressed invalid button id: " + buttonId);
            return false;
        }
        ItemStack itemStack = this.enchantSlots.getItem(0);
        ItemStack currency = this.enchantSlots.getItem(1);
        int enchantmentCost = buttonId + 1;
        if ((currency.isEmpty() || currency.getCount() < enchantmentCost) && !player.hasInfiniteMaterials()) {
            return false;
        }
        if (this.costs[buttonId] > 0 && !itemStack.isEmpty() && (player.experienceLevel >= enchantmentCost && player.experienceLevel >= this.costs[buttonId] || player.hasInfiniteMaterials())) {
            this.access.execute((level, pos) -> {
                ItemStack enchantmentItem = itemStack;
                List<EnchantmentInstance> newEnchantment = this.getEnchantmentList(level.registryAccess(), enchantmentItem, buttonId, this.costs[buttonId]);
                if (!newEnchantment.isEmpty()) {
                    player.onEnchantmentPerformed(enchantmentItem, enchantmentCost);
                    if (enchantmentItem.is(Items.BOOK)) {
                        enchantmentItem = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
                        this.enchantSlots.setItem(0, enchantmentItem);
                    }
                    for (EnchantmentInstance enchantment : newEnchantment) {
                        enchantmentItem.enchant(enchantment.enchantment(), enchantment.level());
                    }
                    currency.consume(enchantmentCost, player);
                    if (currency.isEmpty()) {
                        this.enchantSlots.setItem(1, ItemStack.EMPTY);
                    }
                    player.awardStat(Stats.ENCHANT_ITEM);
                    if (player instanceof ServerPlayer) {
                        ServerPlayer serverPlayer = (ServerPlayer)player;
                        CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchantmentItem, enchantmentCost);
                    }
                    this.enchantSlots.setChanged();
                    this.enchantmentSeed.set(player.getEnchantmentSeed());
                    this.slotsChanged(this.enchantSlots);
                    level.playSound(null, (BlockPos)pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.1f + 0.9f);
                }
            });
            return true;
        }
        return false;
    }

    private List<EnchantmentInstance> getEnchantmentList(RegistryAccess access, ItemStack itemStack, int slot, int enchantmentCost) {
        this.random.setSeed(this.enchantmentSeed.get() + slot);
        Optional tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return List.of();
        }
        List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, itemStack, enchantmentCost, ((HolderSet.Named)tag.get()).stream());
        if (itemStack.is(Items.BOOK) && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }
        return list;
    }

    public int getGoldCount() {
        ItemStack goldStack = this.enchantSlots.getItem(1);
        if (goldStack.isEmpty()) {
            return 0;
        }
        return goldStack.getCount();
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed.get();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.enchantSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return EnchantmentMenu.stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex == 1) {
                if (!this.moveItemStackTo(stack, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.is(Items.LAPIS_LAZULI)) {
                if (!this.moveItemStackTo(stack, 1, 2, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!((Slot)this.slots.get(0)).hasItem() && ((Slot)this.slots.get(0)).mayPlace(stack)) {
                ItemStack singleItem = stack.copyWithCount(1);
                stack.shrink(1);
                ((Slot)this.slots.get(0)).setByPlayer(singleItem);
            } else {
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
            slot.onTake(player, stack);
        }
        return clicked;
    }
}

