/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.monster.warden;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.warden.Digging;
import net.minecraft.world.entity.ai.behavior.warden.Emerging;
import net.minecraft.world.entity.ai.behavior.warden.ForceUnmount;
import net.minecraft.world.entity.ai.behavior.warden.Roar;
import net.minecraft.world.entity.ai.behavior.warden.SetRoarTarget;
import net.minecraft.world.entity.ai.behavior.warden.SetWardenLookTarget;
import net.minecraft.world.entity.ai.behavior.warden.Sniffing;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.ai.behavior.warden.TryToSniff;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.schedule.Activity;

public class WardenAi {
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.5f;
    private static final float SPEED_MULTIPLIER_WHEN_INVESTIGATING = 0.7f;
    private static final float SPEED_MULTIPLIER_WHEN_FIGHTING = 1.2f;
    private static final int MELEE_ATTACK_COOLDOWN = 18;
    private static final int DIGGING_DURATION = Mth.ceil(100.0f);
    public static final int EMERGE_DURATION = Mth.ceil(133.59999f);
    public static final int ROAR_DURATION = Mth.ceil(84.0f);
    private static final int SNIFFING_DURATION = Mth.ceil(83.2f);
    public static final int DIGGING_COOLDOWN = 1200;
    private static final int DISTURBANCE_LOCATION_EXPIRY_TIME = 100;
    private static final BehaviorControl<Warden> DIG_COOLDOWN_SETTER = BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.DIG_COOLDOWN)).apply((Applicative)i, cooldown -> (level, body, timestamp) -> {
        if (i.tryGet(cooldown).isPresent()) {
            cooldown.setWithExpiry(Unit.INSTANCE, 1200L);
        }
        return true;
    }));

    protected static List<ActivityData<Warden>> getActivities(Warden body) {
        return List.of(WardenAi.initCoreActivity(), WardenAi.initEmergeActivity(), WardenAi.initDiggingActivity(), WardenAi.initIdleActivity(), WardenAi.initRoarActivity(), WardenAi.initFightActivity(body), WardenAi.initInvestigateActivity(), WardenAi.initSniffingActivity());
    }

    public static void updateActivity(Brain<Warden> brain) {
        brain.setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.EMERGE, (Object)Activity.DIG, (Object)Activity.ROAR, (Object)Activity.FIGHT, (Object)Activity.INVESTIGATE, (Object)Activity.SNIFF, (Object)Activity.IDLE));
    }

    private static ActivityData<Warden> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), SetWardenLookTarget.create(), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink()));
    }

    private static ActivityData<Warden> initEmergeActivity() {
        return ActivityData.create(Activity.EMERGE, 5, ImmutableList.of(new Emerging(EMERGE_DURATION)), MemoryModuleType.IS_EMERGING);
    }

    private static ActivityData<Warden> initDiggingActivity() {
        return ActivityData.create(Activity.DIG, ImmutableList.of((Object)Pair.of((Object)0, (Object)new ForceUnmount()), (Object)Pair.of((Object)1, new Digging(DIGGING_DURATION))), ImmutableSet.of((Object)Pair.of(MemoryModuleType.ROAR_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.DIG_COOLDOWN, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    private static ActivityData<Warden> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, 10, ImmutableList.of(SetRoarTarget.create(Warden::getEntityAngryAt), TryToSniff.create(), new RunOne((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.IS_SNIFFING, (Object)((Object)MemoryStatus.VALUE_ABSENT)), ImmutableList.of((Object)Pair.of(RandomStroll.stroll(0.5f), (Object)2), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1)))));
    }

    private static ActivityData<Warden> initInvestigateActivity() {
        return ActivityData.create(Activity.INVESTIGATE, 5, ImmutableList.of(SetRoarTarget.create(Warden::getEntityAngryAt), GoToTargetLocation.create(MemoryModuleType.DISTURBANCE_LOCATION, 2, 0.7f)), MemoryModuleType.DISTURBANCE_LOCATION);
    }

    private static ActivityData<Warden> initSniffingActivity() {
        return ActivityData.create(Activity.SNIFF, 5, ImmutableList.of(SetRoarTarget.create(Warden::getEntityAngryAt), new Sniffing(SNIFFING_DURATION)), MemoryModuleType.IS_SNIFFING);
    }

    private static ActivityData<Warden> initRoarActivity() {
        return ActivityData.create(Activity.ROAR, 10, ImmutableList.of((Object)new Roar()), MemoryModuleType.ROAR_TARGET);
    }

    private static ActivityData<Warden> initFightActivity(Warden body) {
        return ActivityData.create(Activity.FIGHT, 10, ImmutableList.of(DIG_COOLDOWN_SETTER, StopAttackingIfTargetInvalid.create((level, target) -> !body.getAngerLevel().isAngry() || !body.canTargetEntity(target), WardenAi::onTargetInvalid, false), SetEntityLookTarget.create(entity -> WardenAi.isTarget(body, entity), (float)body.getAttributeValue(Attributes.FOLLOW_RANGE)), SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2f), (Object)new SonicBoom(), MeleeAttack.create(18)), MemoryModuleType.ATTACK_TARGET);
    }

    private static boolean isTarget(Warden body, LivingEntity living) {
        return body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(e -> e == living).isPresent();
    }

    private static void onTargetInvalid(ServerLevel level, Warden body, LivingEntity attackTarget) {
        if (!body.canTargetEntity(attackTarget)) {
            body.clearAnger(attackTarget);
        }
        WardenAi.setDigCooldown(body);
    }

    public static void setDigCooldown(LivingEntity body) {
        if (body.getBrain().hasMemoryValue(MemoryModuleType.DIG_COOLDOWN)) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
        }
    }

    public static void setDisturbanceLocation(Warden body, BlockPos position) {
        if (!body.level().getWorldBorder().isWithinBounds(position) || body.getEntityAngryAt().isPresent() || body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            return;
        }
        WardenAi.setDigCooldown(body);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 100L);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(position), 100L);
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.DISTURBANCE_LOCATION, position, 100L);
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }
}

