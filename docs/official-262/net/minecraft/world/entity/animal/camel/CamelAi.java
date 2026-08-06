/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.animal.camel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.schedule.Activity;

public class CamelAi {
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 4.0f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 2.0f;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 2.5f;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 2.5f;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0f;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);

    protected static void initMemories(Camel body, RandomSource random) {
    }

    protected static List<ActivityData<Camel>> getActivities() {
        return List.of(CamelAi.initCoreActivity(), CamelAi.initIdleActivity());
    }

    private static ActivityData<Camel> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), (Object)new CamelPanic(4.0f), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), (Object)new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), (Object)new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS)));
    }

    private static ActivityData<Camel> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(30, 60))), (Object)Pair.of((Object)1, (Object)new AnimalMakeLove(EntityTypes.CAMEL)), (Object)Pair.of((Object)2, new RunOne(ImmutableList.of((Object)Pair.of((Object)new FollowTemptation(camel -> Float.valueOf(2.5f), camel -> camel.isBaby() ? 2.5 : 3.5), (Object)1), (Object)Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 2.5f)), (Object)1)))), (Object)Pair.of((Object)3, (Object)new RandomLookAround(UniformInt.of(150, 250), 30.0f, 0.0f, 0.0f)), (Object)Pair.of((Object)4, new RunOne((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), ImmutableList.of((Object)Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), RandomStroll.stroll(2.0f)), (Object)1), (Object)Pair.of(BehaviorBuilder.triggerIf(Predicate.not(Camel::refuseToMove), SetWalkTargetFromLookTarget.create(2.0f, 3)), (Object)1), (Object)Pair.of((Object)new RandomSitting(20), (Object)1), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1))))));
    }

    public static void updateActivity(Camel body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.IDLE));
    }

    public static class CamelPanic
    extends AnimalPanic<Camel> {
        public CamelPanic(float speedMultiplier) {
            super(speedMultiplier);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Camel body) {
            return super.checkExtraStartConditions(level, body) && !body.isMobControlled();
        }

        @Override
        protected void start(ServerLevel level, Camel camel, long timestamp) {
            camel.standUpInstantly();
            super.start(level, camel, timestamp);
        }
    }

    public static class RandomSitting
    extends Behavior<Camel> {
        private final int minimalPoseTicks;

        public RandomSitting(int minimalPoseTimeSec) {
            super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of());
            this.minimalPoseTicks = minimalPoseTimeSec * 20;
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Camel body) {
            return !body.isInWater() && body.getPoseTime() >= (long)this.minimalPoseTicks && !body.isLeashed() && body.onGround() && !body.hasControllingPassenger() && body.canCamelChangePose();
        }

        @Override
        protected void start(ServerLevel level, Camel body, long timestamp) {
            if (body.isCamelSitting()) {
                body.standUp();
            } else if (!body.isPanicking()) {
                body.sitDown();
            }
        }
    }
}

