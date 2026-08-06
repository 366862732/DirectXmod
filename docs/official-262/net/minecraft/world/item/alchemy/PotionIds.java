/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.alchemy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.alchemy.Potion;

public class PotionIds {
    public static final ResourceKey<Potion> WATER = PotionIds.register("water");
    public static final ResourceKey<Potion> MUNDANE = PotionIds.register("mundane");
    public static final ResourceKey<Potion> THICK = PotionIds.register("thick");
    public static final ResourceKey<Potion> AWKWARD = PotionIds.register("awkward");
    public static final ResourceKey<Potion> NIGHT_VISION = PotionIds.register("night_vision");
    public static final ResourceKey<Potion> LONG_NIGHT_VISION = PotionIds.register("long_night_vision");
    public static final ResourceKey<Potion> INVISIBILITY = PotionIds.register("invisibility");
    public static final ResourceKey<Potion> LONG_INVISIBILITY = PotionIds.register("long_invisibility");
    public static final ResourceKey<Potion> LEAPING = PotionIds.register("leaping");
    public static final ResourceKey<Potion> LONG_LEAPING = PotionIds.register("long_leaping");
    public static final ResourceKey<Potion> STRONG_LEAPING = PotionIds.register("strong_leaping");
    public static final ResourceKey<Potion> FIRE_RESISTANCE = PotionIds.register("fire_resistance");
    public static final ResourceKey<Potion> LONG_FIRE_RESISTANCE = PotionIds.register("long_fire_resistance");
    public static final ResourceKey<Potion> SWIFTNESS = PotionIds.register("swiftness");
    public static final ResourceKey<Potion> LONG_SWIFTNESS = PotionIds.register("long_swiftness");
    public static final ResourceKey<Potion> STRONG_SWIFTNESS = PotionIds.register("strong_swiftness");
    public static final ResourceKey<Potion> SLOWNESS = PotionIds.register("slowness");
    public static final ResourceKey<Potion> LONG_SLOWNESS = PotionIds.register("long_slowness");
    public static final ResourceKey<Potion> STRONG_SLOWNESS = PotionIds.register("strong_slowness");
    public static final ResourceKey<Potion> TURTLE_MASTER = PotionIds.register("turtle_master");
    public static final ResourceKey<Potion> LONG_TURTLE_MASTER = PotionIds.register("long_turtle_master");
    public static final ResourceKey<Potion> STRONG_TURTLE_MASTER = PotionIds.register("strong_turtle_master");
    public static final ResourceKey<Potion> WATER_BREATHING = PotionIds.register("water_breathing");
    public static final ResourceKey<Potion> LONG_WATER_BREATHING = PotionIds.register("long_water_breathing");
    public static final ResourceKey<Potion> HEALING = PotionIds.register("healing");
    public static final ResourceKey<Potion> STRONG_HEALING = PotionIds.register("strong_healing");
    public static final ResourceKey<Potion> HARMING = PotionIds.register("harming");
    public static final ResourceKey<Potion> STRONG_HARMING = PotionIds.register("strong_harming");
    public static final ResourceKey<Potion> POISON = PotionIds.register("poison");
    public static final ResourceKey<Potion> LONG_POISON = PotionIds.register("long_poison");
    public static final ResourceKey<Potion> STRONG_POISON = PotionIds.register("strong_poison");
    public static final ResourceKey<Potion> REGENERATION = PotionIds.register("regeneration");
    public static final ResourceKey<Potion> LONG_REGENERATION = PotionIds.register("long_regeneration");
    public static final ResourceKey<Potion> STRONG_REGENERATION = PotionIds.register("strong_regeneration");
    public static final ResourceKey<Potion> STRENGTH = PotionIds.register("strength");
    public static final ResourceKey<Potion> LONG_STRENGTH = PotionIds.register("long_strength");
    public static final ResourceKey<Potion> STRONG_STRENGTH = PotionIds.register("strong_strength");
    public static final ResourceKey<Potion> WEAKNESS = PotionIds.register("weakness");
    public static final ResourceKey<Potion> LONG_WEAKNESS = PotionIds.register("long_weakness");
    public static final ResourceKey<Potion> LUCK = PotionIds.register("luck");
    public static final ResourceKey<Potion> SLOW_FALLING = PotionIds.register("slow_falling");
    public static final ResourceKey<Potion> LONG_SLOW_FALLING = PotionIds.register("long_slow_falling");
    public static final ResourceKey<Potion> WIND_CHARGED = PotionIds.register("wind_charged");
    public static final ResourceKey<Potion> WEAVING = PotionIds.register("weaving");
    public static final ResourceKey<Potion> OOZING = PotionIds.register("oozing");
    public static final ResourceKey<Potion> INFESTED = PotionIds.register("infested");

    private static ResourceKey<Potion> register(String name) {
        return ResourceKey.create(Registries.POTION, Identifier.withDefaultNamespace(name));
    }
}

