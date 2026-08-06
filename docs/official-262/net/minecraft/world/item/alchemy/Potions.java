/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item.alchemy;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionIds;

public class Potions {
    public static final Holder<Potion> WATER = Potions.register(PotionIds.WATER, new Potion("water", new MobEffectInstance[0]));
    public static final Holder<Potion> MUNDANE = Potions.register(PotionIds.MUNDANE, new Potion("mundane", new MobEffectInstance[0]));
    public static final Holder<Potion> THICK = Potions.register(PotionIds.THICK, new Potion("thick", new MobEffectInstance[0]));
    public static final Holder<Potion> AWKWARD = Potions.register(PotionIds.AWKWARD, new Potion("awkward", new MobEffectInstance[0]));
    public static final Holder<Potion> NIGHT_VISION = Potions.register(PotionIds.NIGHT_VISION, new Potion("night_vision", new MobEffectInstance(MobEffects.NIGHT_VISION, 3600)));
    public static final Holder<Potion> LONG_NIGHT_VISION = Potions.register(PotionIds.LONG_NIGHT_VISION, new Potion("night_vision", new MobEffectInstance(MobEffects.NIGHT_VISION, 9600)));
    public static final Holder<Potion> INVISIBILITY = Potions.register(PotionIds.INVISIBILITY, new Potion("invisibility", new MobEffectInstance(MobEffects.INVISIBILITY, 3600)));
    public static final Holder<Potion> LONG_INVISIBILITY = Potions.register(PotionIds.LONG_INVISIBILITY, new Potion("invisibility", new MobEffectInstance(MobEffects.INVISIBILITY, 9600)));
    public static final Holder<Potion> LEAPING = Potions.register(PotionIds.LEAPING, new Potion("leaping", new MobEffectInstance(MobEffects.JUMP_BOOST, 3600)));
    public static final Holder<Potion> LONG_LEAPING = Potions.register(PotionIds.LONG_LEAPING, new Potion("leaping", new MobEffectInstance(MobEffects.JUMP_BOOST, 9600)));
    public static final Holder<Potion> STRONG_LEAPING = Potions.register(PotionIds.STRONG_LEAPING, new Potion("leaping", new MobEffectInstance(MobEffects.JUMP_BOOST, 1800, 1)));
    public static final Holder<Potion> FIRE_RESISTANCE = Potions.register(PotionIds.FIRE_RESISTANCE, new Potion("fire_resistance", new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600)));
    public static final Holder<Potion> LONG_FIRE_RESISTANCE = Potions.register(PotionIds.LONG_FIRE_RESISTANCE, new Potion("fire_resistance", new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 9600)));
    public static final Holder<Potion> SWIFTNESS = Potions.register(PotionIds.SWIFTNESS, new Potion("swiftness", new MobEffectInstance(MobEffects.SPEED, 3600)));
    public static final Holder<Potion> LONG_SWIFTNESS = Potions.register(PotionIds.LONG_SWIFTNESS, new Potion("swiftness", new MobEffectInstance(MobEffects.SPEED, 9600)));
    public static final Holder<Potion> STRONG_SWIFTNESS = Potions.register(PotionIds.STRONG_SWIFTNESS, new Potion("swiftness", new MobEffectInstance(MobEffects.SPEED, 1800, 1)));
    public static final Holder<Potion> SLOWNESS = Potions.register(PotionIds.SLOWNESS, new Potion("slowness", new MobEffectInstance(MobEffects.SLOWNESS, 1800)));
    public static final Holder<Potion> LONG_SLOWNESS = Potions.register(PotionIds.LONG_SLOWNESS, new Potion("slowness", new MobEffectInstance(MobEffects.SLOWNESS, 4800)));
    public static final Holder<Potion> STRONG_SLOWNESS = Potions.register(PotionIds.STRONG_SLOWNESS, new Potion("slowness", new MobEffectInstance(MobEffects.SLOWNESS, 400, 3)));
    public static final Holder<Potion> TURTLE_MASTER = Potions.register(PotionIds.TURTLE_MASTER, new Potion("turtle_master", new MobEffectInstance(MobEffects.SLOWNESS, 400, 3), new MobEffectInstance(MobEffects.RESISTANCE, 400, 2)));
    public static final Holder<Potion> LONG_TURTLE_MASTER = Potions.register(PotionIds.LONG_TURTLE_MASTER, new Potion("turtle_master", new MobEffectInstance(MobEffects.SLOWNESS, 800, 3), new MobEffectInstance(MobEffects.RESISTANCE, 800, 2)));
    public static final Holder<Potion> STRONG_TURTLE_MASTER = Potions.register(PotionIds.STRONG_TURTLE_MASTER, new Potion("turtle_master", new MobEffectInstance(MobEffects.SLOWNESS, 400, 5), new MobEffectInstance(MobEffects.RESISTANCE, 400, 3)));
    public static final Holder<Potion> WATER_BREATHING = Potions.register(PotionIds.WATER_BREATHING, new Potion("water_breathing", new MobEffectInstance(MobEffects.WATER_BREATHING, 3600)));
    public static final Holder<Potion> LONG_WATER_BREATHING = Potions.register(PotionIds.LONG_WATER_BREATHING, new Potion("water_breathing", new MobEffectInstance(MobEffects.WATER_BREATHING, 9600)));
    public static final Holder<Potion> HEALING = Potions.register(PotionIds.HEALING, new Potion("healing", new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1)));
    public static final Holder<Potion> STRONG_HEALING = Potions.register(PotionIds.STRONG_HEALING, new Potion("healing", new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1)));
    public static final Holder<Potion> HARMING = Potions.register(PotionIds.HARMING, new Potion("harming", new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1)));
    public static final Holder<Potion> STRONG_HARMING = Potions.register(PotionIds.STRONG_HARMING, new Potion("harming", new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1)));
    public static final Holder<Potion> POISON = Potions.register(PotionIds.POISON, new Potion("poison", new MobEffectInstance(MobEffects.POISON, 900)));
    public static final Holder<Potion> LONG_POISON = Potions.register(PotionIds.LONG_POISON, new Potion("poison", new MobEffectInstance(MobEffects.POISON, 1800)));
    public static final Holder<Potion> STRONG_POISON = Potions.register(PotionIds.STRONG_POISON, new Potion("poison", new MobEffectInstance(MobEffects.POISON, 432, 1)));
    public static final Holder<Potion> REGENERATION = Potions.register(PotionIds.REGENERATION, new Potion("regeneration", new MobEffectInstance(MobEffects.REGENERATION, 900)));
    public static final Holder<Potion> LONG_REGENERATION = Potions.register(PotionIds.LONG_REGENERATION, new Potion("regeneration", new MobEffectInstance(MobEffects.REGENERATION, 1800)));
    public static final Holder<Potion> STRONG_REGENERATION = Potions.register(PotionIds.STRONG_REGENERATION, new Potion("regeneration", new MobEffectInstance(MobEffects.REGENERATION, 450, 1)));
    public static final Holder<Potion> STRENGTH = Potions.register(PotionIds.STRENGTH, new Potion("strength", new MobEffectInstance(MobEffects.STRENGTH, 3600)));
    public static final Holder<Potion> LONG_STRENGTH = Potions.register(PotionIds.LONG_STRENGTH, new Potion("strength", new MobEffectInstance(MobEffects.STRENGTH, 9600)));
    public static final Holder<Potion> STRONG_STRENGTH = Potions.register(PotionIds.STRONG_STRENGTH, new Potion("strength", new MobEffectInstance(MobEffects.STRENGTH, 1800, 1)));
    public static final Holder<Potion> WEAKNESS = Potions.register(PotionIds.WEAKNESS, new Potion("weakness", new MobEffectInstance(MobEffects.WEAKNESS, 1800)));
    public static final Holder<Potion> LONG_WEAKNESS = Potions.register(PotionIds.LONG_WEAKNESS, new Potion("weakness", new MobEffectInstance(MobEffects.WEAKNESS, 4800)));
    public static final Holder<Potion> LUCK = Potions.register(PotionIds.LUCK, new Potion("luck", new MobEffectInstance(MobEffects.LUCK, 6000)));
    public static final Holder<Potion> SLOW_FALLING = Potions.register(PotionIds.SLOW_FALLING, new Potion("slow_falling", new MobEffectInstance(MobEffects.SLOW_FALLING, 1800)));
    public static final Holder<Potion> LONG_SLOW_FALLING = Potions.register(PotionIds.LONG_SLOW_FALLING, new Potion("slow_falling", new MobEffectInstance(MobEffects.SLOW_FALLING, 4800)));
    public static final Holder<Potion> WIND_CHARGED = Potions.register(PotionIds.WIND_CHARGED, new Potion("wind_charged", new MobEffectInstance(MobEffects.WIND_CHARGED, 3600)));
    public static final Holder<Potion> WEAVING = Potions.register(PotionIds.WEAVING, new Potion("weaving", new MobEffectInstance(MobEffects.WEAVING, 3600)));
    public static final Holder<Potion> OOZING = Potions.register(PotionIds.OOZING, new Potion("oozing", new MobEffectInstance(MobEffects.OOZING, 3600)));
    public static final Holder<Potion> INFESTED = Potions.register(PotionIds.INFESTED, new Potion("infested", new MobEffectInstance(MobEffects.INFESTED, 3600)));

    private static Holder<Potion> register(ResourceKey<Potion> key, Potion potion) {
        return Registry.registerForHolder(BuiltInRegistries.POTION, key, potion);
    }

    public static Holder<Potion> bootstrap(Registry<Potion> registry) {
        return WATER;
    }
}

