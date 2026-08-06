/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAi {
    private static final int ANGER_DURATION = 600;
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125;
    private static final int MAX_LOOK_DIST = 8;
    private static final int INTERACTION_RANGE = 8;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6f;
    private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int HOME_TOO_FAR_DISTANCE = 100;
    private static final int HOME_STROLL_AROUND_DISTANCE = 5;

    public static List<ActivityData<PiglinBrute>> getActivities(PiglinBrute piglin) {
        return List.of(PiglinBruteAi.initCoreActivity(), PiglinBruteAi.initIdleActivity(), PiglinBruteAi.initFightActivity(piglin));
    }

    protected static void initMemories(PiglinBrute body) {
        GlobalPos currentGlobalPos = GlobalPos.of(body.level().dimension(), body.blockPosition());
        body.getBrain().setMemory(MemoryModuleType.HOME, currentGlobalPos);
    }

    private static ActivityData<PiglinBrute> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of((Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), InteractWithDoor.create(), StopBeingAngryIfTargetDead.create()));
    }

    private static ActivityData<PiglinBrute> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, 10, ImmutableList.of(StartAttacking.create(PiglinBruteAi::findNearestValidAttackTarget), PiglinBruteAi.createIdleLookBehaviors(), PiglinBruteAi.createIdleMovementBehaviors(), SetLookAndInteract.create(EntityTypes.PLAYER, 4)));
    }

    private static ActivityData<PiglinBrute> initFightActivity(PiglinBrute body) {
        return ActivityData.create(Activity.FIGHT, 10, ImmutableList.of(StopAttackingIfTargetInvalid.create((level, target) -> !PiglinBruteAi.isNearestValidAttackTarget(level, body, target)), SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0f), MeleeAttack.create(20)), MemoryModuleType.ATTACK_TARGET);
    }

    private static RunOne<PiglinBrute> createIdleLookBehaviors() {
        return new RunOne<PiglinBrute>((List<Pair<BehaviorControl<PiglinBrute>, Integer>>)ImmutableList.of((Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0f), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0f), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PIGLIN_BRUTE, 8.0f), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(8.0f), (Object)1), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)));
    }

    private static RunOne<PiglinBrute> createIdleMovementBehaviors() {
        return new RunOne<PiglinBrute>((List<Pair<BehaviorControl<PiglinBrute>, Integer>>)ImmutableList.of((Object)Pair.of(RandomStroll.stroll(0.6f), (Object)2), (Object)Pair.of(InteractWith.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6f, 2), (Object)2), (Object)Pair.of(InteractWith.of(EntityTypes.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6f, 2), (Object)2), (Object)Pair.of(StrollToPoi.create(MemoryModuleType.HOME, 0.6f, 2, 100), (Object)2), (Object)Pair.of(StrollAroundPoi.create(MemoryModuleType.HOME, 0.6f, 5), (Object)2), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)));
    }

    protected static void updateActivity(PiglinBrute body) {
        Brain<PiglinBrute> brain = body.getBrain();
        Activity oldActivity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.FIGHT, (Object)Activity.IDLE));
        Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);
        if (oldActivity != newActivity) {
            PiglinBruteAi.playActivitySound(body);
        }
        body.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    private static boolean isNearestValidAttackTarget(ServerLevel level, AbstractPiglin body, LivingEntity target) {
        return PiglinBruteAi.findNearestValidAttackTarget(level, body).filter(nearestValidTarget -> nearestValidTarget == target).isPresent();
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel level, AbstractPiglin body) {
        Optional<LivingEntity> angryAt = BehaviorUtils.getLivingEntityFromUUIDMemory(body, MemoryModuleType.ANGRY_AT);
        if (angryAt.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(level, body, angryAt.get())) {
            return angryAt;
        }
        Optional<Player> player = body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
        if (player.isPresent()) {
            return player;
        }
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
    }

    protected static void wasHurtBy(ServerLevel level, PiglinBrute body, LivingEntity attacker) {
        if (attacker instanceof AbstractPiglin) {
            return;
        }
        PiglinAi.maybeRetaliate(level, body, attacker);
    }

    protected static void setAngerTarget(PiglinBrute body, LivingEntity target) {
        body.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
    }

    protected static void maybePlayActivitySound(PiglinBrute body) {
        if ((double)body.level().getRandom().nextFloat() < 0.0125) {
            PiglinBruteAi.playActivitySound(body);
        }
    }

    private static void playActivitySound(PiglinBrute body) {
        body.getBrain().getActiveNonCoreActivity().ifPresent(activity -> {
            if (activity == Activity.FIGHT) {
                body.playAngrySound();
            }
        });
    }
}

