/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

public class CreativeModeTab {
    private static final Identifier DEFAULT_BACKGROUND = CreativeModeTab.createTextureLocation("items");
    private final Component displayName;
    private Identifier backgroundTexture = DEFAULT_BACKGROUND;
    private boolean canScroll = true;
    private boolean showTitle = true;
    private boolean alignedRight = false;
    private final Row row;
    private final int column;
    private final Type type;
    private @Nullable ItemStack iconItemStack;
    private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
    private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();
    private final Supplier<ItemStack> iconGenerator;
    private final DisplayItemsGenerator displayItemsGenerator;

    private CreativeModeTab(Row row, int column, Type type, Component displayName, Supplier<ItemStack> iconGenerator, DisplayItemsGenerator displayItemsGenerator) {
        this.row = row;
        this.column = column;
        this.displayName = displayName;
        this.iconGenerator = iconGenerator;
        this.displayItemsGenerator = displayItemsGenerator;
        this.type = type;
    }

    public static Identifier createTextureLocation(String name) {
        return Identifier.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + name + ".png");
    }

    public static Builder builder(Row row, int column) {
        return new Builder(row, column);
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public ItemStack getIconItem() {
        if (this.iconItemStack == null) {
            this.iconItemStack = this.iconGenerator.get();
        }
        return this.iconItemStack;
    }

    public Identifier getBackgroundTexture() {
        return this.backgroundTexture;
    }

    public boolean showTitle() {
        return this.showTitle;
    }

    public boolean canScroll() {
        return this.canScroll;
    }

    public int column() {
        return this.column;
    }

    public Row row() {
        return this.row;
    }

    public boolean hasAnyItems() {
        return !this.displayItems.isEmpty();
    }

    public boolean shouldDisplay() {
        return this.type != Type.CATEGORY || this.hasAnyItems();
    }

    public boolean isAlignedRight() {
        return this.alignedRight;
    }

    public Type getType() {
        return this.type;
    }

    public void buildContents(ItemDisplayParameters parameters) {
        ItemDisplayBuilder displayList = new ItemDisplayBuilder(this, parameters.enabledFeatures);
        this.displayItemsGenerator.accept(parameters, displayList);
        this.displayItems = displayList.tabContents;
        this.displayItemsSearchTab = displayList.searchTabContents;
    }

    public Collection<ItemStack> getDisplayItems() {
        return this.displayItems;
    }

    public Collection<ItemStack> getSearchTabDisplayItems() {
        return this.displayItemsSearchTab;
    }

    public boolean contains(ItemStack stack) {
        return this.displayItemsSearchTab.contains(stack);
    }

    public static enum Row {
        TOP,
        BOTTOM;

    }

    @FunctionalInterface
    public static interface DisplayItemsGenerator {
        public void accept(ItemDisplayParameters var1, Output var2);
    }

    public static enum Type {
        CATEGORY,
        INVENTORY,
        HOTBAR,
        SEARCH;

    }

    public static class Builder {
        private static final DisplayItemsGenerator EMPTY_GENERATOR = (parameters, output) -> {};
        private final Row row;
        private final int column;
        private Component displayName = Component.empty();
        private Supplier<ItemStack> iconGenerator = () -> ItemStack.EMPTY;
        private DisplayItemsGenerator displayItemsGenerator = EMPTY_GENERATOR;
        private boolean canScroll = true;
        private boolean showTitle = true;
        private boolean alignedRight = false;
        private Type type = Type.CATEGORY;
        private Identifier backgroundTexture = DEFAULT_BACKGROUND;

        public Builder(Row row, int column) {
            this.row = row;
            this.column = column;
        }

        public Builder title(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder icon(Supplier<ItemStack> iconGenerator) {
            this.iconGenerator = iconGenerator;
            return this;
        }

        public Builder displayItems(DisplayItemsGenerator displayItemsGenerator) {
            this.displayItemsGenerator = displayItemsGenerator;
            return this;
        }

        public Builder alignedRight() {
            this.alignedRight = true;
            return this;
        }

        public Builder hideTitle() {
            this.showTitle = false;
            return this;
        }

        public Builder noScrollBar() {
            this.canScroll = false;
            return this;
        }

        protected Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder backgroundTexture(Identifier backgroundTexture) {
            this.backgroundTexture = backgroundTexture;
            return this;
        }

        public CreativeModeTab build() {
            if ((this.type == Type.HOTBAR || this.type == Type.INVENTORY) && this.displayItemsGenerator != EMPTY_GENERATOR) {
                throw new IllegalStateException("Special tabs can't have display items");
            }
            CreativeModeTab tab = new CreativeModeTab(this.row, this.column, this.type, this.displayName, this.iconGenerator, this.displayItemsGenerator);
            tab.alignedRight = this.alignedRight;
            tab.showTitle = this.showTitle;
            tab.canScroll = this.canScroll;
            tab.backgroundTexture = this.backgroundTexture;
            return tab;
        }
    }

    private static class ItemDisplayBuilder
    implements Output {
        public final Collection<ItemStack> tabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
        public final Set<ItemStack> searchTabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
        private final CreativeModeTab tab;
        private final FeatureFlagSet featureFlagSet;

        public ItemDisplayBuilder(CreativeModeTab tab, FeatureFlagSet featureFlagSet) {
            this.tab = tab;
            this.featureFlagSet = featureFlagSet;
        }

        @Override
        public void accept(ItemStack stack, TabVisibility tabVisibility) {
            boolean foundDuplicateStack;
            if (stack.getCount() != 1) {
                throw new IllegalArgumentException("Stack size must be exactly 1");
            }
            boolean bl = foundDuplicateStack = this.tabContents.contains(stack) && tabVisibility != TabVisibility.SEARCH_TAB_ONLY;
            if (foundDuplicateStack) {
                throw new IllegalStateException("Accidentally adding the same item stack twice " + stack.getDisplayName().getString() + " to a Creative Mode Tab: " + this.tab.getDisplayName().getString());
            }
            if (stack.getItem().isEnabled(this.featureFlagSet)) {
                switch (tabVisibility.ordinal()) {
                    case 0: {
                        this.tabContents.add(stack);
                        this.searchTabContents.add(stack);
                        break;
                    }
                    case 1: {
                        this.tabContents.add(stack);
                        break;
                    }
                    case 2: {
                        this.searchTabContents.add(stack);
                    }
                }
            }
        }
    }

    public record ItemDisplayParameters(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
        public boolean needsUpdate(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
            return !this.enabledFeatures.equals(enabledFeatures) || this.hasPermissions != hasPermissions || this.holders != holders;
        }
    }

    protected static interface Output {
        public void accept(ItemStack var1, TabVisibility var2);

        default public void accept(ItemStack stack) {
            this.accept(stack, TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        default public void accept(ItemLike item, TabVisibility tabVisibility) {
            this.accept(new ItemStack(item), tabVisibility);
        }

        default public void accept(ItemLike item) {
            this.accept(new ItemStack(item), TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        default public void acceptAll(Collection<ItemStack> stacks, TabVisibility tabVisibility) {
            stacks.forEach(stack -> this.accept((ItemStack)stack, tabVisibility));
        }

        default public void acceptAll(Collection<ItemStack> stacks) {
            this.acceptAll(stacks, TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    protected static enum TabVisibility {
        PARENT_AND_SEARCH_TABS,
        PARENT_TAB_ONLY,
        SEARCH_TAB_ONLY;

    }
}

