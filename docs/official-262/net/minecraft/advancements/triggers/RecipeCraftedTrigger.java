/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.triggers;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public class RecipeCraftedTrigger
extends SimpleCriterionTrigger<TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceKey<Recipe<?>> id, List<ItemStack> usedIngredients) {
        this.trigger(player, t -> t.matches(id, usedIngredients));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceKey<Recipe<?>> recipeId, List<ItemPredicate> ingredients) implements SimpleCriterionTrigger.SimpleInstance
    {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group((App)EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player), (App)Recipe.KEY_CODEC.fieldOf("recipe_id").forGetter(TriggerInstance::recipeId), (App)ItemPredicate.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(TriggerInstance::ingredients)).apply((Applicative)i, TriggerInstance::new));

        public static Criterion<TriggerInstance> craftedItem(ResourceKey<Recipe<?>> recipeId, List<ItemPredicate.Builder> predicates) {
            return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new TriggerInstance(Optional.empty(), recipeId, predicates.stream().map(ItemPredicate.Builder::build).toList()));
        }

        public static Criterion<TriggerInstance> craftedItem(ResourceKey<Recipe<?>> recipeId) {
            return CriteriaTriggers.RECIPE_CRAFTED.createCriterion(new TriggerInstance(Optional.empty(), recipeId, List.of()));
        }

        public static Criterion<TriggerInstance> crafterCraftedItem(ResourceKey<Recipe<?>> recipeId) {
            return CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.createCriterion(new TriggerInstance(Optional.empty(), recipeId, List.of()));
        }

        private boolean matches(ResourceKey<Recipe<?>> id, List<ItemStack> usedIngredients) {
            if (id != this.recipeId) {
                return false;
            }
            ArrayList<ItemStack> remaining = new ArrayList<ItemStack>(usedIngredients);
            for (ItemPredicate predicate : this.ingredients) {
                boolean found = false;
                Iterator iterator = remaining.iterator();
                while (iterator.hasNext()) {
                    if (!predicate.test((ItemInstance)iterator.next())) continue;
                    iterator.remove();
                    found = true;
                    break;
                }
                if (found) continue;
                return false;
            }
            return true;
        }
    }
}

