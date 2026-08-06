/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public interface DamageTypeTags {
    public static final TagKey<DamageType> DAMAGES_HELMET = DamageTypeTags.create("damages_helmet");
    public static final TagKey<DamageType> BYPASSES_ARMOR = DamageTypeTags.create("bypasses_armor");
    public static final TagKey<DamageType> BYPASSES_SHIELD = DamageTypeTags.create("bypasses_shield");
    public static final TagKey<DamageType> BYPASSES_INVULNERABILITY = DamageTypeTags.create("bypasses_invulnerability");
    public static final TagKey<DamageType> BYPASSES_COOLDOWN = DamageTypeTags.create("bypasses_cooldown");
    public static final TagKey<DamageType> BYPASSES_EFFECTS = DamageTypeTags.create("bypasses_effects");
    public static final TagKey<DamageType> BYPASSES_RESISTANCE = DamageTypeTags.create("bypasses_resistance");
    public static final TagKey<DamageType> BYPASSES_ENCHANTMENTS = DamageTypeTags.create("bypasses_enchantments");
    public static final TagKey<DamageType> IS_FIRE = DamageTypeTags.create("is_fire");
    public static final TagKey<DamageType> IS_PROJECTILE = DamageTypeTags.create("is_projectile");
    public static final TagKey<DamageType> WITCH_RESISTANT_TO = DamageTypeTags.create("witch_resistant_to");
    public static final TagKey<DamageType> IS_EXPLOSION = DamageTypeTags.create("is_explosion");
    public static final TagKey<DamageType> IS_FALL = DamageTypeTags.create("is_fall");
    public static final TagKey<DamageType> IS_DROWNING = DamageTypeTags.create("is_drowning");
    public static final TagKey<DamageType> IS_FREEZING = DamageTypeTags.create("is_freezing");
    public static final TagKey<DamageType> IS_LIGHTNING = DamageTypeTags.create("is_lightning");
    public static final TagKey<DamageType> NO_ANGER = DamageTypeTags.create("no_anger");
    public static final TagKey<DamageType> NO_IMPACT = DamageTypeTags.create("no_impact");
    public static final TagKey<DamageType> ALWAYS_MOST_SIGNIFICANT_FALL = DamageTypeTags.create("always_most_significant_fall");
    public static final TagKey<DamageType> WITHER_IMMUNE_TO = DamageTypeTags.create("wither_immune_to");
    public static final TagKey<DamageType> SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO = DamageTypeTags.create("sulfur_cube_with_block_immune_to");
    public static final TagKey<DamageType> IGNITES_ARMOR_STANDS = DamageTypeTags.create("ignites_armor_stands");
    public static final TagKey<DamageType> BURNS_ARMOR_STANDS = DamageTypeTags.create("burns_armor_stands");
    public static final TagKey<DamageType> AVOIDS_GUARDIAN_THORNS = DamageTypeTags.create("avoids_guardian_thorns");
    public static final TagKey<DamageType> ALWAYS_TRIGGERS_SILVERFISH = DamageTypeTags.create("always_triggers_silverfish");
    public static final TagKey<DamageType> ALWAYS_HURTS_ENDER_DRAGONS = DamageTypeTags.create("always_hurts_ender_dragons");
    public static final TagKey<DamageType> NO_KNOCKBACK = DamageTypeTags.create("no_knockback");
    public static final TagKey<DamageType> ALWAYS_KILLS_ARMOR_STANDS = DamageTypeTags.create("always_kills_armor_stands");
    public static final TagKey<DamageType> CAN_BREAK_ARMOR_STAND = DamageTypeTags.create("can_break_armor_stand");
    public static final TagKey<DamageType> BYPASSES_WOLF_ARMOR = DamageTypeTags.create("bypasses_wolf_armor");
    public static final TagKey<DamageType> IS_PLAYER_ATTACK = DamageTypeTags.create("is_player_attack");
    public static final TagKey<DamageType> BURN_FROM_STEPPING = DamageTypeTags.create("burn_from_stepping");
    public static final TagKey<DamageType> PANIC_CAUSES = DamageTypeTags.create("panic_causes");
    public static final TagKey<DamageType> PANIC_ENVIRONMENTAL_CAUSES = DamageTypeTags.create("panic_environmental_causes");
    public static final TagKey<DamageType> IS_MACE_SMASH = DamageTypeTags.create("mace_smash");

    private static TagKey<DamageType> create(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, Identifier.withDefaultNamespace(name));
    }
}

