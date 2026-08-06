/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem
extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public KnowledgeBookItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        List recipeIds = itemStack.getOrDefault(DataComponents.RECIPES, List.of());
        itemStack.consume(1, player);
        if (recipeIds.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            RecipeManager recipeManager = level.getServer().getRecipeManager();
            ArrayList recipes = new ArrayList(recipeIds.size());
            for (ResourceKey recipeId : recipeIds) {
                Optional<RecipeHolder<?>> recipe = recipeManager.byKey(recipeId);
                if (recipe.isPresent()) {
                    recipes.add(recipe.get());
                    continue;
                }
                LOGGER.error("Invalid recipe: {}", (Object)recipeId);
                return InteractionResult.FAIL;
            }
            player.awardRecipes(recipes);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }
}

