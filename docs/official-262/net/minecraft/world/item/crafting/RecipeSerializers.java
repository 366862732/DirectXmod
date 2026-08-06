/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.crafting;

import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.BannerDuplicateRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.BookCloningRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.DecoratedPotRecipe;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.FireworkStarFadeRecipe;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.ImbueRecipe;
import net.minecraft.world.item.crafting.MapExtendingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.ShieldDecorationRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;

public class RecipeSerializers {
    public static Object bootstrap(Registry<RecipeSerializer<?>> registry) {
        Registry.register(registry, "crafting_shaped", ShapedRecipe.SERIALIZER);
        Registry.register(registry, "crafting_shapeless", ShapelessRecipe.SERIALIZER);
        Registry.register(registry, "crafting_dye", DyeRecipe.SERIALIZER);
        Registry.register(registry, "crafting_imbue", ImbueRecipe.SERIALIZER);
        Registry.register(registry, "crafting_transmute", TransmuteRecipe.SERIALIZER);
        Registry.register(registry, "crafting_decorated_pot", DecoratedPotRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_bookcloning", BookCloningRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_mapextending", MapExtendingRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_firework_rocket", FireworkRocketRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_firework_star", FireworkStarRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_firework_star_fade", FireworkStarFadeRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_bannerduplicate", BannerDuplicateRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_shielddecoration", ShieldDecorationRecipe.SERIALIZER);
        Registry.register(registry, "crafting_special_repairitem", RepairItemRecipe.SERIALIZER);
        Registry.register(registry, "smelting", SmeltingRecipe.SERIALIZER);
        Registry.register(registry, "blasting", BlastingRecipe.SERIALIZER);
        Registry.register(registry, "smoking", SmokingRecipe.SERIALIZER);
        Registry.register(registry, "campfire_cooking", CampfireCookingRecipe.SERIALIZER);
        Registry.register(registry, "stonecutting", StonecutterRecipe.SERIALIZER);
        Registry.register(registry, "smithing_transform", SmithingTransformRecipe.SERIALIZER);
        return Registry.register(registry, "smithing_trim", SmithingTrimRecipe.SERIALIZER);
    }
}

