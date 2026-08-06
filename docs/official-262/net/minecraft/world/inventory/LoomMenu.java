/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 */
package net.minecraft.world.inventory;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class LoomMenu
extends AbstractContainerMenu {
    private static final int PATTERN_NOT_SET = -1;
    private static final int INV_SLOT_START = 4;
    private static final int INV_SLOT_END = 31;
    private static final int USE_ROW_SLOT_START = 31;
    private static final int USE_ROW_SLOT_END = 40;
    private final ContainerLevelAccess access;
    private final DataSlot selectedBannerPatternIndex = DataSlot.standalone();
    private List<Holder<BannerPattern>> selectablePatterns = List.of();
    private Runnable slotUpdateListener = () -> {};
    private final HolderGetter<BannerPattern> patternGetter;
    private final Slot bannerSlot;
    private final Slot dyeSlot;
    private final Slot patternSlot;
    private final Slot resultSlot;
    private long lastSoundTime;
    private final Container inputContainer = new SimpleContainer(this, 3){
        final /* synthetic */ LoomMenu this$0;
        {
            LoomMenu loomMenu = this$0;
            Objects.requireNonNull(loomMenu);
            this.this$0 = loomMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.this$0.slotsChanged(this);
            this.this$0.slotUpdateListener.run();
        }
    };
    private final Container outputContainer = new SimpleContainer(this, 1){
        final /* synthetic */ LoomMenu this$0;
        {
            LoomMenu loomMenu = this$0;
            Objects.requireNonNull(loomMenu);
            this.this$0 = loomMenu;
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.this$0.slotUpdateListener.run();
        }
    };

    public LoomMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public LoomMenu(int containerId, Inventory inventory, final ContainerLevelAccess access) {
        super(MenuType.LOOM, containerId);
        this.access = access;
        this.bannerSlot = this.addSlot(new Slot(this, this.inputContainer, 0, 13, 26){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.getItem() instanceof BannerItem;
            }
        });
        this.dyeSlot = this.addSlot(new Slot(this, this.inputContainer, 1, 33, 26){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return LoomMenu.isDyeItem(itemStack);
            }
        });
        this.patternSlot = this.addSlot(new Slot(this, this.inputContainer, 2, 23, 45){
            {
                Objects.requireNonNull(this$0);
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return LoomMenu.isPatternItem(itemStack);
            }
        });
        this.resultSlot = this.addSlot(new Slot(this, this.outputContainer, 0, 143, 57){
            final /* synthetic */ LoomMenu this$0;
            {
                LoomMenu loomMenu = this$0;
                Objects.requireNonNull(loomMenu);
                this.this$0 = loomMenu;
                super(container, slot, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack carried) {
                this.this$0.bannerSlot.remove(1);
                this.this$0.dyeSlot.remove(1);
                if (!this.this$0.bannerSlot.hasItem() || !this.this$0.dyeSlot.hasItem()) {
                    this.this$0.selectedBannerPatternIndex.set(-1);
                }
                access.execute((level, pos) -> {
                    long gameTime = level.getGameTime();
                    if (this.this$0.lastSoundTime != gameTime) {
                        level.playSound(null, (BlockPos)pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        this.this$0.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, carried);
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlot(this.selectedBannerPatternIndex);
        this.patternGetter = inventory.player.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
    }

    private static boolean isPatternItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.LOOM_PATTERNS) && itemStack.has(DataComponents.PROVIDES_BANNER_PATTERNS);
    }

    private static boolean isDyeItem(ItemStack itemStack) {
        return itemStack.is(ItemTags.LOOM_DYES) && itemStack.has(DataComponents.DYE);
    }

    @Override
    public boolean stillValid(Player player) {
        return LoomMenu.stillValid(this.access, player, Blocks.LOOM);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId >= 0 && buttonId < this.selectablePatterns.size()) {
            this.selectedBannerPatternIndex.set(buttonId);
            this.setupResultSlot(this.selectablePatterns.get(buttonId));
            return true;
        }
        return false;
    }

    private List<Holder<BannerPattern>> getSelectablePatterns(ItemStack patternStack) {
        if (patternStack.isEmpty()) {
            return (List)this.patternGetter.get(BannerPatternTags.NO_ITEM_REQUIRED).map(ImmutableList::copyOf).orElse(ImmutableList.of());
        }
        HolderSet<BannerPattern> itemPatterns = patternStack.get(DataComponents.PROVIDES_BANNER_PATTERNS);
        return itemPatterns != null ? ImmutableList.copyOf(itemPatterns) : ImmutableList.of();
    }

    private boolean isValidPatternIndex(int selectedPattern) {
        return selectedPattern >= 0 && selectedPattern < this.selectablePatterns.size();
    }

    @Override
    public void slotsChanged(Container container) {
        Holder<BannerPattern> patternToDisplay;
        ItemStack bannerStack = this.bannerSlot.getItem();
        ItemStack dyeStack = this.dyeSlot.getItem();
        ItemStack patternStack = this.patternSlot.getItem();
        if (bannerStack.isEmpty() || dyeStack.isEmpty()) {
            this.resultSlot.set(ItemStack.EMPTY);
            this.selectablePatterns = List.of();
            this.selectedBannerPatternIndex.set(-1);
            return;
        }
        int selectedPattern = this.selectedBannerPatternIndex.get();
        boolean validPatternIndex = this.isValidPatternIndex(selectedPattern);
        List<Holder<BannerPattern>> previousSelectablePatterns = this.selectablePatterns;
        this.selectablePatterns = this.getSelectablePatterns(patternStack);
        if (this.selectablePatterns.size() == 1) {
            this.selectedBannerPatternIndex.set(0);
            patternToDisplay = this.selectablePatterns.get(0);
        } else if (!validPatternIndex) {
            this.selectedBannerPatternIndex.set(-1);
            patternToDisplay = null;
        } else {
            Holder<BannerPattern> selectedValue = previousSelectablePatterns.get(selectedPattern);
            int newSelectedIndex = this.selectablePatterns.indexOf(selectedValue);
            if (newSelectedIndex != -1) {
                patternToDisplay = selectedValue;
                this.selectedBannerPatternIndex.set(newSelectedIndex);
            } else {
                patternToDisplay = null;
                this.selectedBannerPatternIndex.set(-1);
            }
        }
        if (patternToDisplay != null) {
            boolean hasMaxPatterns;
            BannerPatternLayers patterns = bannerStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
            boolean bl = hasMaxPatterns = patterns.layers().size() >= 6;
            if (hasMaxPatterns) {
                this.selectedBannerPatternIndex.set(-1);
                this.resultSlot.set(ItemStack.EMPTY);
            } else {
                this.setupResultSlot(patternToDisplay);
            }
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }
        this.broadcastChanges();
    }

    public List<Holder<BannerPattern>> getSelectablePatterns() {
        return this.selectablePatterns;
    }

    public int getSelectedBannerPatternIndex() {
        return this.selectedBannerPatternIndex.get();
    }

    public void registerUpdateListener(Runnable slotUpdateListener) {
        this.slotUpdateListener = slotUpdateListener;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == this.resultSlot.index) {
                if (!this.moveItemStackTo(stack, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex == this.dyeSlot.index || slotIndex == this.bannerSlot.index || slotIndex == this.patternSlot.index ? !this.moveItemStackTo(stack, 4, 40, false) : (stack.getItem() instanceof BannerItem ? !this.moveItemStackTo(stack, this.bannerSlot.index, this.bannerSlot.index + 1, false) : (LoomMenu.isDyeItem(stack) ? !this.moveItemStackTo(stack, this.dyeSlot.index, this.dyeSlot.index + 1, false) : (LoomMenu.isPatternItem(stack) ? !this.moveItemStackTo(stack, this.patternSlot.index, this.patternSlot.index + 1, false) : (slotIndex >= 4 && slotIndex < 31 ? !this.moveItemStackTo(stack, 31, 40, false) : slotIndex >= 31 && slotIndex < 40 && !this.moveItemStackTo(stack, 4, 31, false)))))) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputContainer));
    }

    private void setupResultSlot(Holder<BannerPattern> pattern) {
        DyeColor patternColor;
        ItemStack bannerStack = this.bannerSlot.getItem();
        ItemStack dyeStack = this.dyeSlot.getItem();
        ItemStack result = ItemStack.EMPTY;
        if (!bannerStack.isEmpty() && !dyeStack.isEmpty() && (patternColor = dyeStack.get(DataComponents.DYE)) != null) {
            result = bannerStack.copyWithCount(1);
            result.update(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY, layers -> new BannerPatternLayers.Builder().addAll((BannerPatternLayers)layers).add(pattern, patternColor).build());
        }
        if (!ItemStack.matches(result, this.resultSlot.getItem())) {
            this.resultSlot.set(result);
        }
    }

    public Slot getBannerSlot() {
        return this.bannerSlot;
    }

    public Slot getDyeSlot() {
        return this.dyeSlot;
    }

    public Slot getPatternSlot() {
        return this.patternSlot;
    }

    public Slot getResultSlot() {
        return this.resultSlot;
    }
}

