/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.damagesource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public class CombatRules {
    public static final float MAX_ARMOR = 20.0f;
    public static final float ARMOR_PROTECTION_DIVIDER = 25.0f;
    public static final float BASE_ARMOR_TOUGHNESS = 2.0f;
    public static final float MIN_ARMOR_RATIO = 0.2f;
    private static final int NUM_ARMOR_ITEMS = 4;

    public static float getDamageAfterAbsorb(LivingEntity victim, float damage, DamageSource source, float totalArmor, float armorToughness) {
        float modifiedArmorFraction;
        Level level;
        float toughness = 2.0f + armorToughness / 4.0f;
        float realArmor = Mth.clamp(totalArmor - damage / toughness, totalArmor * 0.2f, 20.0f);
        float armorFraction = realArmor / 25.0f;
        ItemStack weaponItem = source.getWeaponItem();
        if (weaponItem != null && (level = victim.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            modifiedArmorFraction = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness(level2, weaponItem, victim, source, armorFraction), 0.0f, 1.0f);
        } else {
            modifiedArmorFraction = armorFraction;
        }
        float damageMultiplier = 1.0f - modifiedArmorFraction;
        return damage * damageMultiplier;
    }

    public static float getDamageAfterMagicAbsorb(float damage, float totalMagicArmor) {
        float realArmor = Mth.clamp(totalMagicArmor, 0.0f, 20.0f);
        return damage * (1.0f - realArmor / 25.0f);
    }
}

