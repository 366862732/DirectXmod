/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public class RamTarget
extends Behavior<Goat> {
    public static final int TIME_OUT_DURATION = 200;
    public static final float RAM_SPEED_FORCE_FACTOR = 1.65f;
    private final Function<Goat, UniformInt> getTimeBetweenRams;
    private final TargetingConditions ramTargeting;
    private final float speed;
    private final ToDoubleFunction<Goat> getKnockbackForce;
    private Vec3 ramDirection;
    private final Function<Goat, SoundEvent> getImpactSound;
    private final Function<Goat, SoundEvent> getHornBreakSound;

    public RamTarget(Function<Goat, UniformInt> getTimeBetweenRams, TargetingConditions ramTargeting, float speed, ToDoubleFunction<Goat> getKnockbackForce, Function<Goat, SoundEvent> getImpactSound, Function<Goat, SoundEvent> getHornBreakSound) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.RAM_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), 200);
        this.getTimeBetweenRams = getTimeBetweenRams;
        this.ramTargeting = ramTargeting;
        this.speed = speed;
        this.getKnockbackForce = getKnockbackForce;
        this.getImpactSound = getImpactSound;
        this.getHornBreakSound = getHornBreakSound;
        this.ramDirection = Vec3.ZERO;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Goat body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Goat body, long timestamp) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
    }

    @Override
    protected void start(ServerLevel level, Goat body, long timestamp) {
        BlockPos curPos = body.blockPosition();
        Brain<Goat> brain = body.getBrain();
        Vec3 ramTargetPos = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
        this.ramDirection = new Vec3((double)curPos.getX() - ramTargetPos.x(), 0.0, (double)curPos.getZ() - ramTargetPos.z()).normalize();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(ramTargetPos, this.speed, 0));
    }

    @Override
    protected void tick(ServerLevel level, Goat body, long timestamp) {
        List<LivingEntity> nearbyEntities = level.getNearbyEntities(LivingEntity.class, this.ramTargeting, body, body.getBoundingBox());
        Brain<Goat> brain = body.getBrain();
        if (!nearbyEntities.isEmpty()) {
            float damage;
            DamageSource damageSource;
            LivingEntity ramTarget = nearbyEntities.get(0);
            if (ramTarget.hurtServer(level, damageSource = level.damageSources().noAggroMobAttack(body), damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                EnchantmentHelper.doPostAttackEffects(level, ramTarget, damageSource);
            }
            int movementSpeedLevel = body.hasEffect(MobEffects.SPEED) ? body.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
            int movementSlowdownLevel = body.hasEffect(MobEffects.SLOWNESS) ? body.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
            float speedBoostPower = 0.25f * (float)(movementSpeedLevel - movementSlowdownLevel);
            float speedFactor = Mth.clamp(body.getSpeed() * 1.65f, 0.2f, 3.0f) + speedBoostPower;
            DamageSource source = level.damageSources().mobAttack(body);
            float blockedDamage = ramTarget.applyItemBlocking(level, source, damage);
            float blockingFactor = blockedDamage > 0.0f ? 0.5f : 1.0f;
            ramTarget.knockback((double)(blockingFactor * speedFactor) * this.getKnockbackForce.applyAsDouble(body), this.ramDirection.x(), this.ramDirection.z(), source, damage);
            this.finishRam(level, body);
            level.playSound(null, body, this.getImpactSound.apply(body), SoundSource.NEUTRAL, 1.0f, 1.0f);
        } else if (this.hasRammedHornBreakingBlock(level, body)) {
            level.playSound(null, body, this.getImpactSound.apply(body), SoundSource.NEUTRAL, 1.0f, 1.0f);
            boolean dropped = body.dropHorn();
            if (dropped) {
                level.playSound(null, body, this.getHornBreakSound.apply(body), SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            this.finishRam(level, body);
        } else {
            boolean lostOrReachedTarget;
            Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
            Optional<Vec3> ramTarget = brain.getMemory(MemoryModuleType.RAM_TARGET);
            boolean bl = lostOrReachedTarget = walkTarget.isEmpty() || ramTarget.isEmpty() || walkTarget.get().getTarget().currentPosition().closerThan(ramTarget.get(), 0.25);
            if (lostOrReachedTarget) {
                this.finishRam(level, body);
            }
        }
    }

    private boolean hasRammedHornBreakingBlock(ServerLevel level, Goat body) {
        Vec3 horizontalMovementNormalized = body.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
        BlockPos facingBlockPosition = BlockPos.containing(body.position().add(horizontalMovementNormalized));
        return level.getBlockState(facingBlockPosition).is(BlockTags.SNAPS_GOAT_HORN) || level.getBlockState(facingBlockPosition.above()).is(BlockTags.SNAPS_GOAT_HORN);
    }

    protected void finishRam(ServerLevel level, Goat body) {
        level.broadcastEntityEvent(body, (byte)59);
        body.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(body).sample(level.getRandom()));
        body.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
    }
}

