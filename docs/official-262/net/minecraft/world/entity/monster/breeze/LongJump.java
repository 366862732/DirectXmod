/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.BreezeUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LongJump
extends Behavior<Breeze> {
    private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    private static final int JUMP_COOLDOWN_TICKS = 10;
    private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    private static final int INHALING_DURATION_TICKS = Math.round(10.0f);
    private static final float DEFAULT_FOLLOW_RANGE = 24.0f;
    private static final float DEFAULT_MAX_JUMP_VELOCITY = 1.4f;
    private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334f;
    private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList((Collection)Lists.newArrayList((Object[])new Integer[]{40, 55, 60, 75, 80}));

    @VisibleForTesting
    public LongJump() {
        super(Map.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.BREEZE_JUMP_COOLDOWN, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.REGISTERED, MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.BREEZE_SHOOT, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.REGISTERED), 200);
    }

    public static boolean canRun(ServerLevel level, Breeze breeze) {
        if (!breeze.onGround() && !breeze.isInWater()) {
            return false;
        }
        if (Swim.shouldSwim(breeze)) {
            return false;
        }
        if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return true;
        }
        LivingEntity attackTarget = breeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (attackTarget == null) {
            return false;
        }
        if (LongJump.outOfAggroRange(breeze, attackTarget)) {
            breeze.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            return false;
        }
        if (LongJump.tooCloseForJump(breeze, attackTarget)) {
            return false;
        }
        if (!LongJump.canJumpFromCurrentPosition(level, breeze)) {
            return false;
        }
        BlockPos targetPos = LongJump.snapToSurface(breeze, BreezeUtil.randomPointBehindTarget(attackTarget, breeze.getRandom()));
        if (targetPos == null) {
            return false;
        }
        BlockState bs = level.getBlockState(targetPos.below());
        if (breeze.getType().isBlockDangerous(bs)) {
            return false;
        }
        if (!BreezeUtil.hasLineOfSight(breeze, Vec3.atCenterOf(targetPos)) && !BreezeUtil.hasLineOfSight(breeze, Vec3.atCenterOf(targetPos.above(4)))) {
            return false;
        }
        breeze.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, targetPos);
        return true;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Breeze breeze) {
        return LongJump.canRun(level, breeze);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Breeze breeze, long timestamp) {
        return breeze.getPose() != Pose.STANDING && !breeze.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
    }

    @Override
    protected void start(ServerLevel level, Breeze breeze, long timestamp) {
        if (breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, INHALING_DURATION_TICKS);
        }
        breeze.setPose(Pose.INHALING);
        level.playSound(null, breeze, SoundEvents.BREEZE_CHARGE, SoundSource.HOSTILE, 1.0f, 1.0f);
        breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET).ifPresent(targetPos -> breeze.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(targetPos)));
    }

    @Override
    protected void tick(ServerLevel level, Breeze breeze, long timestamp) {
        boolean inWater = breeze.isInWater();
        if (!inWater && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_PRESENT)) {
            breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
        }
        if (LongJump.isFinishedInhaling(breeze)) {
            Vec3 velocityVector = breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET).flatMap(targetPos -> LongJump.calculateOptimalJumpVector(breeze, breeze.getRandom(), Vec3.atBottomCenterOf(targetPos))).orElse(null);
            if (velocityVector == null) {
                breeze.setPose(Pose.STANDING);
                return;
            }
            if (inWater) {
                breeze.getBrain().setMemory(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
            }
            breeze.playSound(SoundEvents.BREEZE_JUMP, 1.0f, 1.0f);
            breeze.setPose(Pose.LONG_JUMPING);
            breeze.setYRot(breeze.yBodyRot);
            breeze.setDiscardFriction(true);
            breeze.setDeltaMovement(velocityVector);
        } else if (LongJump.isFinishedJumping(breeze)) {
            breeze.playSound(SoundEvents.BREEZE_LAND, 1.0f, 1.0f);
            breeze.setPose(Pose.STANDING);
            breeze.setDiscardFriction(false);
            boolean wasHurt = breeze.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, wasHurt ? 2L : 10L);
            breeze.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
        }
    }

    @Override
    protected void stop(ServerLevel level, Breeze breeze, long timestamp) {
        if (breeze.getPose() == Pose.LONG_JUMPING || breeze.getPose() == Pose.INHALING) {
            breeze.setPose(Pose.STANDING);
        }
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
        breeze.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
    }

    private static boolean isFinishedInhaling(Breeze breeze) {
        return breeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && breeze.getPose() == Pose.INHALING;
    }

    private static boolean isFinishedJumping(Breeze breeze) {
        boolean isJumping = breeze.getPose() == Pose.LONG_JUMPING;
        boolean landedOnGround = breeze.onGround();
        boolean landedInWater = breeze.isInWater() && breeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_ABSENT);
        return isJumping && (landedOnGround || landedInWater);
    }

    private static @Nullable BlockPos snapToSurface(LivingEntity entity, Vec3 target) {
        ClipContext collisionBelow = new ClipContext(target, target.relative(Direction.DOWN, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult surfaceBelow = entity.level().clip(collisionBelow);
        if (((HitResult)surfaceBelow).getType() == HitResult.Type.BLOCK) {
            return BlockPos.containing(surfaceBelow.getLocation()).above();
        }
        ClipContext collisionAbove = new ClipContext(target, target.relative(Direction.UP, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult surfaceAbove = entity.level().clip(collisionAbove);
        if (((HitResult)surfaceAbove).getType() == HitResult.Type.BLOCK) {
            return BlockPos.containing(surfaceAbove.getLocation()).above();
        }
        return null;
    }

    private static boolean outOfAggroRange(Breeze breeze, LivingEntity attackTarget) {
        return !attackTarget.closerThan(breeze, breeze.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    private static boolean tooCloseForJump(Breeze breeze, LivingEntity attackTarget) {
        return attackTarget.distanceTo(breeze) - 4.0f <= 0.0f;
    }

    private static boolean canJumpFromCurrentPosition(ServerLevel level, Breeze breeze) {
        BlockPos currentPos = breeze.blockPosition();
        if (level.getBlockState(currentPos).is(Blocks.HONEY_BLOCK)) {
            return false;
        }
        for (int i = 1; i <= 4; ++i) {
            BlockPos offsetPos = currentPos.relative(Direction.UP, i);
            if (level.getBlockState(offsetPos).isAir() || level.getFluidState(offsetPos).is(FluidTags.WATER)) continue;
            return false;
        }
        return true;
    }

    private static Optional<Vec3> calculateOptimalJumpVector(Breeze body, RandomSource random, Vec3 targetPos) {
        List<Integer> allowedAngles = Util.shuffledCopy(ALLOWED_ANGLES, random);
        for (int angle : allowedAngles) {
            float maxJumpVelocity = 0.058333334f * (float)body.getAttributeValue(Attributes.FOLLOW_RANGE);
            Optional<Vec3> velocityVector = LongJumpUtil.calculateJumpVectorForAngle(body, targetPos, maxJumpVelocity, angle, false);
            if (!velocityVector.isPresent()) continue;
            if (body.hasEffect(MobEffects.JUMP_BOOST)) {
                double jumpEffectAmplifier = velocityVector.get().normalize().y * (double)body.getJumpBoostPower();
                return velocityVector.map(v -> v.add(0.0, jumpEffectAmplifier, 0.0));
            }
            return velocityVector;
        }
        return Optional.empty();
    }
}

