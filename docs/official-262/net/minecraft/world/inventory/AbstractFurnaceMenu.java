/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.List;
import java.util.Objects;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public abstract class AbstractFurnaceMenu
extends RecipeBookMenu {
    public static final int INGREDIENT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 4;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final Container container;
    private final ContainerData data;
    protected final Level level;
    private final RecipePropertySet acceptedInputs;
    private final RecipeBookType recipeBookType;

    protected AbstractFurnaceMenu(MenuType<?> menuType, ResourceKey<RecipePropertySet> allowedInputs, RecipeBookType recipeBookType, int containerId, Inventory inventory) {
        this(menuType, allowedInputs, recipeBookType, containerId, inventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    protected AbstractFurnaceMenu(MenuType<?> menuType, ResourceKey<RecipePropertySet> allowedInputs, RecipeBookType recipeBookType, int containerId, Inventory inventory, Container container, ContainerData data) {
        super(menuType, containerId);
        this.recipeBookType = recipeBookType;
        AbstractFurnaceMenu.checkContainerSize(container, 3);
        AbstractFurnaceMenu.checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        this.acceptedInputs = this.level.recipeAccess().propertySet(allowedInputs);
        this.addSlot(new Slot(container, 0, 56, 17));
        this.addSlot(new FurnaceFuelSlot(this, container, 1, 56, 53));
        this.addSlot(new FurnaceResultSlot(inventory.player, container, 2, 116, 35));
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(data);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        Container container = this.container;
        if (container instanceof StackedContentsCompatible) {
            StackedContentsCompatible stackedContentsCompatible = (StackedContentsCompatible)((Object)container);
            stackedContentsCompatible.fillStackedContents(stackedContents);
        }
    }

    public Slot getResultSlot() {
        return (Slot)this.slots.get(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == 2) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex == 1 || slotIndex == 0 ? !this.moveItemStackTo(stack, 3, 39, false) : (this.canSmelt(stack) ? !this.moveItemStackTo(stack, 0, 1, false) : (this.isFuel(stack) ? !this.moveItemStackTo(stack, 1, 2, false) : (slotIndex >= 3 && slotIndex < 30 ? !this.moveItemStackTo(stack, 30, 39, false) : slotIndex >= 30 && slotIndex < 39 && !this.moveItemStackTo(stack, 3, 30, false))))) {
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

    protected boolean canSmelt(ItemStack itemStack) {
        return this.acceptedInputs.test(itemStack);
    }

    protected boolean isFuel(ItemStack itemStack) {
        return this.level.fuelValues().isFuel(itemStack);
    }

    public float getBurnProgress() {
        int current = this.data.get(2);
        int total = this.data.get(3);
        if (total == 0 || current == 0) {
            return 0.0f;
        }
        return Mth.clamp((float)current / (float)total, 0.0f, 1.0f);
    }

    public float getLitProgress() {
        int litDuration = this.data.get(1);
        if (litDuration == 0) {
            litDuration = 200;
        }
        return Mth.clamp((float)this.data.get(0) / (float)litDuration, 0.0f, 1.0f);
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return this.recipeBookType;
    }

    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear, RecipeHolder<?> recipe, final ServerLevel level, Inventory inventory) {
        final List<Slot> slotsToClear = List.of(this.getSlot(0), this.getSlot(2));
        RecipeHolder<?> typedRecipe = recipe;
        return ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<AbstractCookingRecipe>(this){
            final /* synthetic */ AbstractFurnaceMenu this$0;
            {
                AbstractFurnaceMenu abstractFurnaceMenu = this$0;
                Objects.requireNonNull(abstractFurnaceMenu);
                this.this$0 = abstractFurnaceMenu;
            }

            @Override
            public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
                this.this$0.fillCraftSlotsStackedContents(stackedContents);
            }

            @Override
            public void clearCraftingContent() {
                slotsToClear.forEach(s -> s.set(ItemStack.EMPTY));
            }

            @Override
            public boolean recipeMatches(RecipeHolder<AbstractCookingRecipe> recipe) {
                return recipe.value().matches(new SingleRecipeInput(this.this$0.container.getItem(0)), (Level)level);
            }
        }, 1, 1, List.of(this.getSlot(0)), slotsToClear, inventory, typedRecipe, useMaxItems, allowDroppingItemsToClear);
    }
}

