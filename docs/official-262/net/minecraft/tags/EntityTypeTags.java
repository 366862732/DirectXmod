/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public interface EntityTypeTags {
    public static final TagKey<EntityType<?>> SKELETONS = EntityTypeTags.create("skeletons");
    public static final TagKey<EntityType<?>> ZOMBIES = EntityTypeTags.create("zombies");
    public static final TagKey<EntityType<?>> RAIDERS = EntityTypeTags.create("raiders");
    public static final TagKey<EntityType<?>> UNDEAD = EntityTypeTags.create("undead");
    public static final TagKey<EntityType<?>> BURN_IN_DAYLIGHT = EntityTypeTags.create("burn_in_daylight");
    public static final TagKey<EntityType<?>> BEEHIVE_INHABITORS = EntityTypeTags.create("beehive_inhabitors");
    public static final TagKey<EntityType<?>> ARROWS = EntityTypeTags.create("arrows");
    public static final TagKey<EntityType<?>> IMPACT_PROJECTILES = EntityTypeTags.create("impact_projectiles");
    public static final TagKey<EntityType<?>> POWDER_SNOW_WALKABLE_MOBS = EntityTypeTags.create("powder_snow_walkable_mobs");
    public static final TagKey<EntityType<?>> AXOLOTL_ALWAYS_HOSTILES = EntityTypeTags.create("axolotl_always_hostiles");
    public static final TagKey<EntityType<?>> AXOLOTL_HUNT_TARGETS = EntityTypeTags.create("axolotl_hunt_targets");
    public static final TagKey<EntityType<?>> FREEZE_IMMUNE_ENTITY_TYPES = EntityTypeTags.create("freeze_immune_entity_types");
    public static final TagKey<EntityType<?>> FREEZE_HURTS_EXTRA_TYPES = EntityTypeTags.create("freeze_hurts_extra_types");
    public static final TagKey<EntityType<?>> CAN_BREATHE_UNDER_WATER = EntityTypeTags.create("can_breathe_under_water");
    public static final TagKey<EntityType<?>> FROG_FOOD = EntityTypeTags.create("frog_food");
    public static final TagKey<EntityType<?>> FALL_DAMAGE_IMMUNE = EntityTypeTags.create("fall_damage_immune");
    public static final TagKey<EntityType<?>> DISMOUNTS_UNDERWATER = EntityTypeTags.create("dismounts_underwater");
    public static final TagKey<EntityType<?>> NON_CONTROLLING_RIDER = EntityTypeTags.create("non_controlling_rider");
    public static final TagKey<EntityType<?>> DEFLECTS_PROJECTILES = EntityTypeTags.create("deflects_projectiles");
    public static final TagKey<EntityType<?>> CAN_TURN_IN_BOATS = EntityTypeTags.create("can_turn_in_boats");
    public static final TagKey<EntityType<?>> ILLAGER = EntityTypeTags.create("illager");
    public static final TagKey<EntityType<?>> AQUATIC = EntityTypeTags.create("aquatic");
    public static final TagKey<EntityType<?>> ARTHROPOD = EntityTypeTags.create("arthropod");
    public static final TagKey<EntityType<?>> IGNORES_POISON_AND_REGEN = EntityTypeTags.create("ignores_poison_and_regen");
    public static final TagKey<EntityType<?>> INVERTED_HEALING_AND_HARM = EntityTypeTags.create("inverted_healing_and_harm");
    public static final TagKey<EntityType<?>> WITHER_FRIENDS = EntityTypeTags.create("wither_friends");
    public static final TagKey<EntityType<?>> ILLAGER_FRIENDS = EntityTypeTags.create("illager_friends");
    public static final TagKey<EntityType<?>> NOT_SCARY_FOR_PUFFERFISH = EntityTypeTags.create("not_scary_for_pufferfish");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_IMPALING = EntityTypeTags.create("sensitive_to_impaling");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_BANE_OF_ARTHROPODS = EntityTypeTags.create("sensitive_to_bane_of_arthropods");
    public static final TagKey<EntityType<?>> SENSITIVE_TO_SMITE = EntityTypeTags.create("sensitive_to_smite");
    public static final TagKey<EntityType<?>> NO_ANGER_FROM_WIND_CHARGE = EntityTypeTags.create("no_anger_from_wind_charge");
    public static final TagKey<EntityType<?>> IMMUNE_TO_OOZING = EntityTypeTags.create("immune_to_oozing");
    public static final TagKey<EntityType<?>> IMMUNE_TO_INFESTED = EntityTypeTags.create("immune_to_infested");
    public static final TagKey<EntityType<?>> REDIRECTABLE_PROJECTILE = EntityTypeTags.create("redirectable_projectile");
    public static final TagKey<EntityType<?>> BOAT = EntityTypeTags.create("boat");
    public static final TagKey<EntityType<?>> CAN_EQUIP_SADDLE = EntityTypeTags.create("can_equip_saddle");
    public static final TagKey<EntityType<?>> CAN_EQUIP_HARNESS = EntityTypeTags.create("can_equip_harness");
    public static final TagKey<EntityType<?>> CAN_WEAR_HORSE_ARMOR = EntityTypeTags.create("can_wear_horse_armor");
    public static final TagKey<EntityType<?>> CAN_WEAR_NAUTILUS_ARMOR = EntityTypeTags.create("can_wear_nautilus_armor");
    public static final TagKey<EntityType<?>> FOLLOWABLE_FRIENDLY_MOBS = EntityTypeTags.create("followable_friendly_mobs");
    public static final TagKey<EntityType<?>> CANNOT_BE_PUSHED_ONTO_BOATS = EntityTypeTags.create("cannot_be_pushed_onto_boats");
    public static final TagKey<EntityType<?>> ACCEPTS_IRON_GOLEM_GIFT = EntityTypeTags.create("accepts_iron_golem_gift");
    public static final TagKey<EntityType<?>> CANDIDATE_FOR_IRON_GOLEM_GIFT = EntityTypeTags.create("candidate_for_iron_golem_gift");
    public static final TagKey<EntityType<?>> NAUTILUS_HOSTILES = EntityTypeTags.create("nautilus_hostiles");
    public static final TagKey<EntityType<?>> CAN_FLOAT_WHILE_RIDDEN = EntityTypeTags.create("can_float_while_ridden");
    public static final TagKey<EntityType<?>> CANNOT_BE_AGE_LOCKED = EntityTypeTags.create("cannot_be_age_locked");
    public static final TagKey<EntityType<?>> NOT_AFFECTED_BY_GEYSERS = EntityTypeTags.create("not_affected_by_geysers");

    private static TagKey<EntityType<?>> create(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace(name));
    }
}

