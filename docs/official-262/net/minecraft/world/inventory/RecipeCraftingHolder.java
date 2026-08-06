/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public interface RecipeCraftingHolder {
    public void setRecipeUsed(@Nullable RecipeHolder<?> var1);

    public @Nullable RecipeHolder<?> getRecipeUsed();

    default public void awardUsedRecipes(Player player, List<ItemStack> itemStacks) {
        RecipeHolder<?> recipeUsed = this.getRecipeUsed();
        if (recipeUsed != null) {
            player.triggerRecipeCrafted(recipeUsed, itemStacks);
            if (!recipeUsed.value().isSpecial()) {
                player.awardRecipes(Collections.singleton(recipeUsed));
                this.setRecipeUsed(null);
            }
        }
    }

    default public boolean setRecipeUsed(ServerPlayer player, RecipeHolder<?> recipe) {
        if (recipe.value().isSpecial() || !player.level().getGameRules().get(GameRules.LIMITED_CRAFTING).booleanValue() || player.getRecipeBook().contains(recipe.id())) {
            this.setRecipeUsed(recipe);
            return true;
        }
        return false;
    }
}

