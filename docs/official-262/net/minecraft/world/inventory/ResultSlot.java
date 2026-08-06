/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class ResultSlot
extends Slot {
    private final CraftingContainer craftSlots;
    private final Player player;
    private int removeCount;

    public ResultSlot(Player player, CraftingContainer craftSlots, Container container, int id, int x, int y) {
        super(container, id, x, y);
        this.player = player;
        this.craftSlots = craftSlots;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    protected void onQuickCraft(ItemStack picked, int count) {
        this.removeCount += count;
        this.checkTakeAchievements(picked);
    }

    @Override
    protected void onSwapCraft(int count) {
        this.removeCount += count;
    }

    @Override
    public ItemStack safeClone(Player player) {
        ItemStack result = super.safeClone(player);
        result.getItem().onCraftedBy(result, player);
        return result;
    }

    @Override
    protected void checkTakeAchievements(ItemStack carried) {
        Container container;
        if (this.removeCount > 0) {
            carried.onCraftedBy(this.player, this.removeCount);
        }
        if ((container = this.container) instanceof RecipeCraftingHolder) {
            RecipeCraftingHolder recipeCraftingHolder = (RecipeCraftingHolder)((Object)container);
            recipeCraftingHolder.awardUsedRecipes(this.player, this.craftSlots.getItems());
        }
        this.removeCount = 0;
    }

    private static NonNullList<ItemStack> copyAllInputItems(CraftingInput input) {
        NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < result.size(); ++slot) {
            result.set(slot, input.getItem(slot));
        }
        return result;
    }

    private NonNullList<ItemStack> getRemainingItems(CraftingInput input, Level level) {
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            return serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverLevel).map(recipe -> ((CraftingRecipe)recipe.value()).getRemainingItems(input)).orElseGet(() -> ResultSlot.copyAllInputItems(input));
        }
        return CraftingRecipe.defaultCraftingReminder(input);
    }

    @Override
    public void onTake(Player player, ItemStack carried) {
        this.checkTakeAchievements(carried);
        CraftingInput.Positioned positionedRecipe = this.craftSlots.asPositionedCraftInput();
        CraftingInput input = positionedRecipe.input();
        int recipeLeft = positionedRecipe.left();
        int recipeTop = positionedRecipe.top();
        NonNullList<ItemStack> remaining = this.getRemainingItems(input, player.level());
        for (int y = 0; y < input.height(); ++y) {
            for (int x = 0; x < input.width(); ++x) {
                int slot = x + recipeLeft + (y + recipeTop) * this.craftSlots.getWidth();
                ItemStack itemStack = this.craftSlots.getItem(slot);
                ItemStack replacement = remaining.get(x + y * input.width());
                if (!itemStack.isEmpty()) {
                    this.craftSlots.removeItem(slot, 1);
                    itemStack = this.craftSlots.getItem(slot);
                }
                if (replacement.isEmpty()) continue;
                if (itemStack.isEmpty()) {
                    this.craftSlots.setItem(slot, replacement);
                    continue;
                }
                if (ItemStack.isSameItemSameComponents(itemStack, replacement)) {
                    replacement.grow(itemStack.getCount());
                    this.craftSlots.setItem(slot, replacement);
                    continue;
                }
                if (this.player.getInventory().add(replacement)) continue;
                this.player.drop(replacement, false);
            }
        }
    }

    @Override
    public boolean isFake() {
        return true;
    }
}

