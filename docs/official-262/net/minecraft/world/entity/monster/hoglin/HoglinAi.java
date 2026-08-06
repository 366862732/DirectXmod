/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.BecomePassiveIfMemoryPresent;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.schedule.Activity;

public class HoglinAi {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final int ATTACK_DURATION = 200;
    private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_IDLING = 8;
    private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_RETREATING = 15;
    private static final int ATTACK_INTERVAL = 40;
    private static final int BABY_ATTACK_INTERVAL = 15;
    private static final int REPELLENT_PACIFY_TIME = 200;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING_REPELLENT = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.3f;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 0.6f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.4f;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 0.6f;

    protected static List<ActivityData<Hoglin>> getActivities() {
        return List.of(HoglinAi.initCoreActivity(), HoglinAi.initIdleActivity(), HoglinAi.initFightActivity(), HoglinAi.initRetreatActivity());
    }

    private static ActivityData<Hoglin> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of((Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink()));
    }

    private static ActivityData<Hoglin> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, 10, ImmutableList.of(BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200), (Object)new AnimalMakeLove(EntityTypes.HOGLIN, 0.6f, 2), SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0f, 8, true), StartAttacking.create(HoglinAi::findNearestValidAttackTarget), BehaviorBuilder.triggerIf(Hoglin::isAdult, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4f, 8, false)), SetEntityLookTargetSometimes.create(8.0f, UniformInt.of(30, 60)), BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 0.6f), HoglinAi.createIdleMovementBehaviors()));
    }

    private static ActivityData<Hoglin> initFightActivity() {
        return ActivityData.create(Activity.FIGHT, 10, ImmutableList.of(BecomePassiveIfMemoryPresent.create(MemoryModuleType.NEAREST_REPELLENT, 200), (Object)new AnimalMakeLove(EntityTypes.HOGLIN, 0.6f, 2), SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0f), BehaviorBuilder.triggerIf(Hoglin::isAdult, MeleeAttack.create(40)), BehaviorBuilder.triggerIf(AgeableMob::isBaby, MeleeAttack.create(15)), StopAttackingIfTargetInvalid.create(), EraseMemoryIf.create(HoglinAi::isBreeding, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static ActivityData<Hoglin> initRetreatActivity() {
        return ActivityData.create(Activity.AVOID, 10, ImmutableList.of(SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.3f, 15, false), HoglinAi.createIdleMovementBehaviors(), SetEntityLookTargetSometimes.create(8.0f, UniformInt.of(30, 60)), EraseMemoryIf.create(HoglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static RunOne<Hoglin> createIdleMovementBehaviors() {
        return new RunOne<Hoglin>((List<Pair<BehaviorControl<Hoglin>, Integer>>)ImmutableList.of((Object)Pair.of(RandomStroll.stroll(0.4f), (Object)2), (Object)Pair.of(SetWalkTargetFromLookTarget.create(0.4f, 3), (Object)2), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)));
    }

    protected static void updateActivity(Hoglin body) {
        Brain<Hoglin> brain = body.getBrain();
        Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.FIGHT, (Object)Activity.AVOID, (Object)Activity.IDLE));
        Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
        if (oldActivity != newActivity) {
            HoglinAi.getSoundForCurrentActivity(body).ifPresent(body::makeSound);
        }
        body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    protected static void onHitTarget(Hoglin attackerBody, LivingEntity target) {
        if (attackerBody.isBaby()) {
            return;
        }
        if (target.is(EntityTypes.PIGLIN) && HoglinAi.piglinsOutnumberHoglins(attackerBody)) {
            HoglinAi.setAvoidTarget(attackerBody, target);
            HoglinAi.broadcastRetreat(attackerBody, target);
            return;
        }
        HoglinAi.broadcastAttackTarget(attackerBody, target);
    }

    private static void broadcastRetreat(Hoglin body, LivingEntity target) {
        HoglinAi.getVisibleAdultHoglins(body).forEach(hoglin -> HoglinAi.retreatFromNearestTarget(hoglin, target));
    }

    private static void retreatFromNearestTarget(Hoglin body, LivingEntity newAvoidTarget) {
        LivingEntity nearest = newAvoidTarget;
        Brain<Hoglin> brain = body.getBrain();
        nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.AVOID_TARGET), nearest);
        nearest = BehaviorUtils.getNearestTarget(body, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
        HoglinAi.setAvoidTarget(body, nearest);
    }

    private static void setAvoidTarget(Hoglin body, LivingEntity avoidTarget) {
        body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, avoidTarget, RETREAT_DURATION.sample(body.level().getRandom()));
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel level, Hoglin body) {
        if (HoglinAi.isPacified(body) || HoglinAi.isBreeding(body)) {
            return Optional.empty();
        }
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
    }

    static boolean isPosNearNearestRepellent(Hoglin body, BlockPos pos) {
        Optional<BlockPos> repellentPos = body.getBrain().getMemory(MemoryModuleType.NEAREST_REPELLENT);
        return repellentPos.isPresent() && repellentPos.get().closerThan(pos, 8.0);
    }

    private static boolean wantsToStopFleeing(Hoglin body) {
        return body.isAdult() && !HoglinAi.piglinsOutnumberHoglins(body);
    }

    private static boolean piglinsOutnumberHoglins(Hoglin body) {
        int hoglinCount;
        if (body.isBaby()) {
            return false;
        }
        int piglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0);
        return piglinCount > (hoglinCount = body.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0) + 1);
    }

    protected static void wasHurtBy(ServerLevel level, Hoglin body, LivingEntity attacker) {
        Brain<Hoglin> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.PACIFIED);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        if (body.isBaby()) {
            HoglinAi.retreatFromNearestTarget(body, attacker);
            return;
        }
        HoglinAi.maybeRetaliate(level, body, attacker);
    }

    private static void maybeRetaliate(ServerLevel level, Hoglin body, LivingEntity attacker) {
        if (body.getBrain().isActive(Activity.AVOID) && attacker.is(EntityTypes.PIGLIN)) {
            return;
        }
        if (attacker.is(EntityTypes.HOGLIN)) {
            return;
        }
        if (BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(body, attacker, 4.0)) {
            return;
        }
        if (!Sensor.isEntityAttackable(level, body, attacker)) {
            return;
        }
        HoglinAi.setAttackTarget(body, attacker);
        HoglinAi.broadcastAttackTarget(body, attacker);
    }

    private static void setAttackTarget(Hoglin body, LivingEntity target) {
        Brain<Hoglin> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.eraseMemory(MemoryModuleType.BREED_TARGET);
        brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
    }

    private static void broadcastAttackTarget(Hoglin body, LivingEntity target) {
        HoglinAi.getVisibleAdultHoglins(body).forEach(hoglin -> HoglinAi.setAttackTargetIfCloserThanCurrent(hoglin, target));
    }

    private static void setAttackTargetIfCloserThanCurrent(Hoglin body, LivingEntity newTarget) {
        if (HoglinAi.isPacified(body)) {
            return;
        }
        Optional<LivingEntity> currentTarget = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        LivingEntity nearest = BehaviorUtils.getNearestTarget(body, currentTarget, newTarget);
        HoglinAi.setAttackTarget(body, nearest);
    }

    public static Optional<SoundEvent> getSoundForCurrentActivity(Hoglin body) {
        return body.getBrain().getActiveNonCoreActivity().map(activity -> HoglinAi.getSoundForActivity(body, activity));
    }

    private static SoundEvent getSoundForActivity(Hoglin body, Activity activity) {
        if (activity == Activity.AVOID || body.isConverting()) {
            return SoundEvents.HOGLIN_RETREAT;
        }
        if (activity == Activity.FIGHT) {
            return SoundEvents.HOGLIN_ANGRY;
        }
        if (HoglinAi.isNearRepellent(body)) {
            return SoundEvents.HOGLIN_RETREAT;
        }
        return SoundEvents.HOGLIN_AMBIENT;
    }

    private static List<Hoglin> getVisibleAdultHoglins(Hoglin body) {
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS).orElse((List<Hoglin>)ImmutableList.of());
    }

    private static boolean isNearRepellent(Hoglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean isBreeding(Hoglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
    }

    protected static boolean isPacified(Hoglin body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.PACIFIED);
    }
}

