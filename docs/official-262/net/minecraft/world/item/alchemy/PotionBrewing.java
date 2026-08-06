/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class PotionBrewing {
    public static final int BREWING_TIME_SECONDS = 20;
    public static final PotionBrewing EMPTY = new PotionBrewing(List.of(), List.of(), List.of());
    private final List<Ingredient> containers;
    private final List<Mix<Potion>> potionMixes;
    private final List<Mix<Item>> containerMixes;

    private PotionBrewing(List<Ingredient> containers, List<Mix<Potion>> potionMixes, List<Mix<Item>> containerMixes) {
        this.containers = containers;
        this.potionMixes = potionMixes;
        this.containerMixes = containerMixes;
    }

    public boolean isIngredient(ItemStack ingredient) {
        return this.isContainerIngredient(ingredient) || this.isPotionIngredient(ingredient);
    }

    private boolean isContainer(ItemStack input) {
        for (Ingredient allowedContainer : this.containers) {
            if (!allowedContainer.test(input)) continue;
            return true;
        }
        return false;
    }

    public boolean isContainerIngredient(ItemStack ingredient) {
        for (Mix<Item> containerMix : this.containerMixes) {
            if (!containerMix.ingredient.test(ingredient)) continue;
            return true;
        }
        return false;
    }

    public boolean isPotionIngredient(ItemStack ingredient) {
        for (Mix<Potion> potionMix : this.potionMixes) {
            if (!potionMix.ingredient.test(ingredient)) continue;
            return true;
        }
        return false;
    }

    public boolean isBrewablePotion(Holder<Potion> potion) {
        for (Mix<Potion> mix : this.potionMixes) {
            if (!mix.to.is(potion)) continue;
            return true;
        }
        return false;
    }

    public boolean hasMix(ItemStack source, ItemStack ingredient) {
        if (!this.isContainer(source)) {
            return false;
        }
        return this.hasContainerMix(source, ingredient) || this.hasPotionMix(source, ingredient);
    }

    public boolean hasContainerMix(ItemStack source, ItemStack ingredient) {
        for (Mix<Item> mix : this.containerMixes) {
            if (!source.is(mix.from) || !mix.ingredient.test(ingredient)) continue;
            return true;
        }
        return false;
    }

    public boolean hasPotionMix(ItemStack source, ItemStack ingredient) {
        Optional<Holder<Potion>> potion = source.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        if (potion.isEmpty()) {
            return false;
        }
        for (Mix<Potion> mix : this.potionMixes) {
            if (!mix.from.is(potion.get()) || !mix.ingredient.test(ingredient)) continue;
            return true;
        }
        return false;
    }

    public ItemStack mix(ItemStack ingredient, ItemStack source) {
        if (source.isEmpty()) {
            return source;
        }
        Optional<Holder<Potion>> potion = source.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        if (potion.isEmpty()) {
            return source;
        }
        for (Mix<Item> mix : this.containerMixes) {
            if (!source.is(mix.from) || !mix.ingredient.test(ingredient)) continue;
            return PotionContents.createItemStack((Item)mix.to.value(), potion.get());
        }
        for (Mix<FeatureElement> mix : this.potionMixes) {
            if (!mix.from.is(potion.get()) || !mix.ingredient.test(ingredient)) continue;
            return PotionContents.createItemStack(source.getItem(), mix.to);
        }
        return source;
    }

    public static PotionBrewing bootstrap(FeatureFlagSet enabledFeatures) {
        Builder builder = new Builder(enabledFeatures);
        PotionBrewing.addVanillaMixes(builder);
        return builder.build();
    }

    public static void addVanillaMixes(Builder builder) {
        builder.addContainer(Items.POTION);
        builder.addContainer(Items.SPLASH_POTION);
        builder.addContainer(Items.LINGERING_POTION);
        builder.addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        builder.addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
        builder.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
        builder.addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
        builder.addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
        builder.addStartMix(Items.BREEZE_ROD, Potions.WIND_CHARGED);
        builder.addStartMix(Items.SLIME_BLOCK, Potions.OOZING);
        builder.addStartMix(Items.STONE, Potions.INFESTED);
        builder.addStartMix(Items.COBWEB, Potions.WEAVING);
        builder.addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
        builder.addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
        builder.addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
        builder.addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
        builder.addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
        builder.addStartMix(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
        builder.addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
        builder.addStartMix(Items.RABBIT_FOOT, Potions.LEAPING);
        builder.addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
        builder.addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
        builder.addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        builder.addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        builder.addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
        builder.addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
        builder.addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
        builder.addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
        builder.addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
        builder.addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
        builder.addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
        builder.addStartMix(Items.SUGAR, Potions.SWIFTNESS);
        builder.addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
        builder.addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
        builder.addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
        builder.addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
        builder.addStartMix(Items.GLISTERING_MELON_SLICE, Potions.HEALING);
        builder.addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
        builder.addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        builder.addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        builder.addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
        builder.addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        builder.addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
        builder.addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
        builder.addStartMix(Items.SPIDER_EYE, Potions.POISON);
        builder.addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
        builder.addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
        builder.addStartMix(Items.GHAST_TEAR, Potions.REGENERATION);
        builder.addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
        builder.addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
        builder.addStartMix(Items.BLAZE_POWDER, Potions.STRENGTH);
        builder.addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
        builder.addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
        builder.addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
        builder.addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
        builder.addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
        builder.addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
    }

    private record Mix<T>(Holder<T> from, Ingredient ingredient, Holder<T> to) {
    }

    public static class Builder {
        private final List<Ingredient> containers = new ArrayList<Ingredient>();
        private final List<Mix<Potion>> potionMixes = new ArrayList<Mix<Potion>>();
        private final List<Mix<Item>> containerMixes = new ArrayList<Mix<Item>>();
        private final FeatureFlagSet enabledFeatures;

        public Builder(FeatureFlagSet enabledFeatures) {
            this.enabledFeatures = enabledFeatures;
        }

        private static void expectPotion(Item from) {
            if (!(from instanceof PotionItem)) {
                throw new IllegalArgumentException("Expected a potion, got: " + String.valueOf(BuiltInRegistries.ITEM.getKey(from)));
            }
        }

        public void addContainerRecipe(Item from, Item ingredient, Item to) {
            if (!(from.isEnabled(this.enabledFeatures) && ingredient.isEnabled(this.enabledFeatures) && to.isEnabled(this.enabledFeatures))) {
                return;
            }
            Builder.expectPotion(from);
            Builder.expectPotion(to);
            this.containerMixes.add(new Mix<Item>(from.builtInRegistryHolder(), Ingredient.of((ItemLike)ingredient), to.builtInRegistryHolder()));
        }

        public void addContainer(Item item) {
            if (!item.isEnabled(this.enabledFeatures)) {
                return;
            }
            Builder.expectPotion(item);
            this.containers.add(Ingredient.of((ItemLike)item));
        }

        public void addMix(Holder<Potion> from, Item ingredient, Holder<Potion> to) {
            if (from.value().isEnabled(this.enabledFeatures) && ingredient.isEnabled(this.enabledFeatures) && to.value().isEnabled(this.enabledFeatures)) {
                this.potionMixes.add(new Mix<Potion>(from, Ingredient.of((ItemLike)ingredient), to));
            }
        }

        public void addStartMix(Item ingredient, Holder<Potion> potion) {
            if (potion.value().isEnabled(this.enabledFeatures)) {
                this.addMix(Potions.WATER, ingredient, Potions.MUNDANE);
                this.addMix(Potions.AWKWARD, ingredient, potion);
            }
        }

        public PotionBrewing build() {
            return new PotionBrewing(List.copyOf(this.containers), List.copyOf(this.potionMixes), List.copyOf(this.containerMixes));
        }
    }
}

