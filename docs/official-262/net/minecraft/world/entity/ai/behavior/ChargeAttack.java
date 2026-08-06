/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

public class ChargeAttack
extends Behavior<Animal> {
    private final int timeBetweenAttacks;
    private final TargetingConditions chargeTargeting;
    private final float speed;
    private final float knockbackForce;
    private final double maxTargetDetectionDistance;
    private final double maxChargeDistance;
    private final SoundEvent chargeSound;
    private Vec3 chargeVelocityVector;
    private Vec3 startPosition;

    public ChargeAttack(int timeBetweenAttacks, TargetingConditions chargeTargeting, float speed, float knockbackForce, double maxChargeDistance, double maxTargetDetectionDistance, SoundEvent chargeSound) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)));
        this.timeBetweenAttacks = timeBetweenAttacks;
        this.chargeTargeting = chargeTargeting;
        this.speed = speed;
        this.knockbackForce = knockbackForce;
        this.maxChargeDistance = maxChargeDistance;
        this.maxTargetDetectionDistance = maxTargetDetectionDistance;
        this.chargeSound = chargeSound;
        this.chargeVelocityVector = Vec3.ZERO;
        this.startPosition = Vec3.ZERO;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Animal body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Animal body, long timestamp) {
        TamableAnimal tamedAnimal;
        Brain<? extends LivingEntity> brain = body.getBrain();
        Optional<LivingEntity> attackCandidate = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
        if (attackCandidate.isEmpty()) {
            return false;
        }
        LivingEntity attackTarget = attackCandidate.get();
        if (body instanceof TamableAnimal && (tamedAnimal = (TamableAnimal)body).isTame()) {
            return false;
        }
        if (body.position().subtract(this.startPosition).lengthSqr() >= this.maxChargeDistance * this.maxChargeDistance) {
            return false;
        }
        if (attackTarget.position().subtract(body.position()).lengthSqr() >= this.maxTargetDetectionDistance * this.maxTargetDetectionDistance) {
            return false;
        }
        if (!body.hasLineOfSight(attackTarget)) {
            return false;
        }
        return !brain.hasMemoryValue(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
    }

    @Override
    protected void start(ServerLevel level, Animal body, long timestamp) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        this.startPosition = body.position();
        LivingEntity attackCandidate = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
        Vec3 direction = attackCandidate.position().subtract(body.position()).normalize();
        this.chargeVelocityVector = direction.scale(this.speed);
        if (this.canStillUse(level, body, timestamp)) {
            body.playSound(this.chargeSound);
        }
    }

    @Override
    protected void tick(ServerLevel level, Animal body, long timestamp) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        LivingEntity attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
        body.lookAt(attackTarget, 360.0f, 360.0f);
        body.setDeltaMovement(this.chargeVelocityVector);
        ArrayList collidingEntities = new ArrayList(1);
        level.getEntities(EntityTypeTest.forClass(LivingEntity.class), body.getBoundingBox(), e -> this.chargeTargeting.test(level, body, (LivingEntity)e), collidingEntities, 1);
        if (!collidingEntities.isEmpty()) {
            LivingEntity closestAttackTarget = (LivingEntity)collidingEntities.get(0);
            if (body.hasPassenger(closestAttackTarget)) {
                return;
            }
            this.dealDamageToTarget(level, body, closestAttackTarget);
            this.dealKnockBack(level, body, closestAttackTarget);
            this.stop(level, body, timestamp);
        }
    }

    private void dealDamageToTarget(ServerLevel level, Animal body, LivingEntity target) {
        float damage;
        DamageSource damageSource = level.damageSources().mobAttack(body);
        if (target.hurtServer(level, damageSource, damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
        }
    }

    private void dealKnockBack(ServerLevel level, Animal body, LivingEntity target) {
        int movementSpeedLevel = body.hasEffect(MobEffects.SPEED) ? body.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
        int movementSlowdownLevel = body.hasEffect(MobEffects.SLOWNESS) ? body.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
        float speedBoostPower = 0.25f * (float)(movementSpeedLevel - movementSlowdownLevel);
        float speedFactor = Mth.clamp(this.speed * (float)body.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2f, 2.0f) + speedBoostPower;
        DamageSource damageSource = level.damageSources().mobAttack(body);
        float damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
        body.causeExtraKnockback(target, speedFactor * this.knockbackForce, body.getDeltaMovement(), damageSource, damage, false);
    }

    @Override
    protected void stop(ServerLevel level, Animal body, long timestamp) {
        body.getBrain().setMemory(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.timeBetweenAttacks);
        body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }
}

