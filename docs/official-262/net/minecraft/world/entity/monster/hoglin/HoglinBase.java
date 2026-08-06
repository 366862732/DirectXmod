/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
    public static final int ATTACK_ANIMATION_DURATION = 10;
    public static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2f;

    public int getAttackAnimationRemainingTicks();

    public static boolean hurtAndThrowTarget(ServerLevel level, LivingEntity body, LivingEntity target) {
        float attackDamage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float actualDamage = !body.isBaby() && (int)attackDamage > 0 ? attackDamage / 2.0f + (float)level.getRandom().nextInt((int)attackDamage) : attackDamage;
        DamageSource damageSource = body.damageSources().mobAttack(body);
        boolean wasHurt = target.hurtServer(level, damageSource, actualDamage);
        if (wasHurt) {
            EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
            if (!body.isBaby()) {
                HoglinBase.throwTarget(body, target);
            }
        }
        return wasHurt;
    }

    public static void throwTarget(LivingEntity body, LivingEntity target) {
        double knockbackResistance;
        double knockbackPower = body.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        double effectiveKnockbackPower = knockbackPower - (knockbackResistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (effectiveKnockbackPower <= 0.0) {
            return;
        }
        double xd = target.getX() - body.getX();
        double zd = target.getZ() - body.getZ();
        RandomSource random = body.level().getRandom();
        float horizontalPushAngle = random.nextInt(21) - 10;
        double horizontalScale = effectiveKnockbackPower * (double)(random.nextFloat() * 0.5f + 0.2f);
        Vec3 horizontalPushVector = new Vec3(xd, 0.0, zd).normalize().scale(horizontalScale).yRot(horizontalPushAngle);
        double verticalScale = effectiveKnockbackPower * (double)random.nextFloat() * 0.5;
        target.push(horizontalPushVector.x, verticalScale, horizontalPushVector.z);
        target.hurtMarked = true;
    }
}

