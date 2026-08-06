/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  org.apache.commons.lang3.math.Fraction
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.component;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

public final class BundleContents
implements TooltipComponent {
    public static final BundleContents EMPTY = new BundleContents(List.of());
    public static final Codec<BundleContents> CODEC = ItemStackTemplate.CODEC.listOf().xmap(BundleContents::new, contents -> contents.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BundleContents::new, contents -> contents.items);
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction((int)1, (int)16);
    private static final int NO_STACK_INDEX = -1;
    public static final int NO_SELECTED_ITEM_INDEX = -1;
    public static final DataResult<Fraction> BEEHIVE_WEIGHT = DataResult.success((Object)Fraction.ONE);
    private final List<ItemStackTemplate> items;
    private final int selectedItem;
    private final Supplier<DataResult<Fraction>> weight;

    private BundleContents(List<ItemStackTemplate> items, int selectedItem) {
        this.items = items;
        this.selectedItem = selectedItem;
        this.weight = Suppliers.memoize(() -> BundleContents.computeContentWeight(this.items));
    }

    public BundleContents(List<ItemStackTemplate> items) {
        this(items, -1);
    }

    private static DataResult<Fraction> computeContentWeight(List<? extends ItemInstance> items) {
        try {
            Fraction weight = Fraction.ZERO;
            for (ItemInstance itemInstance : items) {
                DataResult<Fraction> itemWeight = BundleContents.getWeight(itemInstance);
                if (itemWeight.isError()) {
                    return itemWeight;
                }
                weight = weight.add(((Fraction)itemWeight.getOrThrow()).multiplyBy(Fraction.getFraction((int)itemInstance.count(), (int)1)));
            }
            return DataResult.success((Object)weight);
        }
        catch (ArithmeticException exception) {
            return DataResult.error(() -> "Excessive total bundle weight");
        }
    }

    private static DataResult<Fraction> getWeight(ItemInstance item) {
        BundleContents bundle = item.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            return bundle.weight().map(nestedWeight -> nestedWeight.add(BUNDLE_IN_BUNDLE_WEIGHT));
        }
        List<BeehiveBlockEntity.Occupant> bees = item.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
        if (!bees.isEmpty()) {
            return BEEHIVE_WEIGHT;
        }
        return DataResult.success((Object)Fraction.getFraction((int)1, (int)item.getMaxStackSize()));
    }

    public static boolean canItemBeInBundle(ItemStack itemToAdd) {
        return !itemToAdd.isEmpty() && itemToAdd.getItem().canFitInsideContainerItems();
    }

    public int getNumberOfItemsToShow() {
        int numberOfItemStacks = this.size();
        int availableItemsToShow = numberOfItemStacks > 12 ? 11 : 12;
        int itemsOnNonFullRow = numberOfItemStacks % 4;
        int emptySpaceOnNonFullRow = itemsOnNonFullRow == 0 ? 0 : 4 - itemsOnNonFullRow;
        return Math.min(numberOfItemStacks, availableItemsToShow - emptySpaceOnNonFullRow);
    }

    public Stream<ItemStack> itemCopyStream() {
        return this.items.stream().map(ItemStackTemplate::create);
    }

    public List<ItemStackTemplate> items() {
        return this.items;
    }

    public int size() {
        return this.items.size();
    }

    public DataResult<Fraction> weight() {
        return this.weight.get();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public int getSelectedItemIndex() {
        return this.selectedItem;
    }

    public @Nullable ItemStackTemplate getSelectedItem() {
        if (this.selectedItem == -1) {
            return null;
        }
        return this.items.get(this.selectedItem);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BundleContents) {
            BundleContents contents = (BundleContents)obj;
            return this.items.equals(contents.items);
        }
        return false;
    }

    public int hashCode() {
        return this.items.hashCode();
    }

    public String toString() {
        return "BundleContents" + String.valueOf(this.items);
    }

    public static class Mutable {
        private final List<ItemStack> items;
        private Fraction weight;
        private int selectedItem;

        public Mutable(BundleContents contents) {
            DataResult<Fraction> currentWeight = contents.weight.get();
            if (currentWeight.isError()) {
                this.items = new ArrayList<ItemStack>();
                this.weight = Fraction.ZERO;
                this.selectedItem = -1;
            } else {
                this.items = new ArrayList<ItemStack>(contents.items.size());
                for (ItemStackTemplate item : contents.items) {
                    this.items.add(item.create());
                }
                this.weight = (Fraction)currentWeight.getOrThrow();
                this.selectedItem = contents.selectedItem;
            }
        }

        public Mutable clearItems() {
            this.items.clear();
            this.weight = Fraction.ZERO;
            this.selectedItem = -1;
            return this;
        }

        private int findStackIndex(ItemStack itemsToAdd) {
            if (!itemsToAdd.isStackable()) {
                return -1;
            }
            for (int i = 0; i < this.items.size(); ++i) {
                if (!ItemStack.isSameItemSameComponents(this.items.get(i), itemsToAdd)) continue;
                return i;
            }
            return -1;
        }

        private int getMaxAmountToAdd(Fraction itemWeight) {
            Fraction remainingWeight = Fraction.ONE.subtract(this.weight);
            return Math.max(remainingWeight.divideBy(itemWeight).intValue(), 0);
        }

        public int tryInsert(ItemStack itemsToAdd) {
            if (!BundleContents.canItemBeInBundle(itemsToAdd)) {
                return 0;
            }
            DataResult<Fraction> maybeItemWeight = BundleContents.getWeight(itemsToAdd);
            if (maybeItemWeight.isError()) {
                return 0;
            }
            Fraction itemWeight = (Fraction)maybeItemWeight.getOrThrow();
            int amountToAdd = Math.min(itemsToAdd.getCount(), this.getMaxAmountToAdd(itemWeight));
            if (amountToAdd == 0) {
                return 0;
            }
            this.weight = this.weight.add(itemWeight.multiplyBy(Fraction.getFraction((int)amountToAdd, (int)1)));
            int stackIndex = this.findStackIndex(itemsToAdd);
            if (stackIndex != -1) {
                ItemStack removedStack = this.items.remove(stackIndex);
                ItemStack mergedStack = removedStack.copyWithCount(removedStack.getCount() + amountToAdd);
                itemsToAdd.shrink(amountToAdd);
                this.items.add(0, mergedStack);
            } else {
                this.items.add(0, itemsToAdd.split(amountToAdd));
            }
            return amountToAdd;
        }

        public int tryTransfer(Slot slot, Player player) {
            ItemStack other = slot.getItem();
            DataResult<Fraction> itemWeight = BundleContents.getWeight(other);
            if (itemWeight.isError()) {
                return 0;
            }
            int maxAmount = this.getMaxAmountToAdd((Fraction)itemWeight.getOrThrow());
            return BundleContents.canItemBeInBundle(other) ? this.tryInsert(slot.safeTake(other.getCount(), maxAmount, player)) : 0;
        }

        public void toggleSelectedItem(int selectedItem) {
            this.selectedItem = this.selectedItem == selectedItem || this.indexIsOutsideAllowedBounds(selectedItem) ? -1 : selectedItem;
        }

        private boolean indexIsOutsideAllowedBounds(int selectedItem) {
            return selectedItem < 0 || selectedItem >= this.items.size();
        }

        public @Nullable ItemStack removeOne() {
            if (this.items.isEmpty()) {
                return null;
            }
            int removeIndex = this.indexIsOutsideAllowedBounds(this.selectedItem) ? 0 : this.selectedItem;
            ItemStack stack = this.items.remove(removeIndex).copy();
            this.weight = this.weight.subtract(((Fraction)BundleContents.getWeight(stack).getOrThrow()).multiplyBy(Fraction.getFraction((int)stack.getCount(), (int)1)));
            this.toggleSelectedItem(-1);
            return stack;
        }

        public Fraction weight() {
            return this.weight;
        }

        public BundleContents toImmutable() {
            ImmutableList.Builder builder = ImmutableList.builder();
            for (ItemStack item : this.items) {
                builder.add((Object)ItemStackTemplate.fromNonEmptyStack(item));
            }
            return new BundleContents((List<ItemStackTemplate>)builder.build(), this.selectedItem);
        }
    }
}

