/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.data.recipes;

import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeUnlockAdvancementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.Nullable;

public class SpecialRecipeBuilder {
    private @Nullable RecipeUnlockAdvancementBuilder advancementBuilder;
    private final Supplier<Recipe<?>> factory;

    public SpecialRecipeBuilder(Supplier<Recipe<?>> factory) {
        this.factory = factory;
    }

    public static SpecialRecipeBuilder special(Supplier<Recipe<?>> factory) {
        return new SpecialRecipeBuilder(factory);
    }

    public SpecialRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        if (this.advancementBuilder == null) {
            this.advancementBuilder = new RecipeUnlockAdvancementBuilder();
        }
        this.advancementBuilder.unlockedBy(name, criterion);
        return this;
    }

    public void save(RecipeOutput output, String name) {
        this.save(output, ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
    }

    public void save(RecipeOutput output, ResourceKey<Recipe<?>> id) {
        AdvancementHolder advancement = this.advancementBuilder != null ? this.advancementBuilder.build(output, id, RecipeCategory.MISC) : null;
        output.accept(id, this.factory.get(), advancement);
    }
}

