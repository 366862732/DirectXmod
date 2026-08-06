/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.animal.armadillo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomLookAround;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.schedule.Activity;

public class ArmadilloAi {
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25f;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 1.25f;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0f;
    private static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.0;
    private static final double BABY_CLOSE_ENOUGH_DIST = 1.0;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
    private static final OneShot<Armadillo> ARMADILLO_ROLLING_OUT = BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.DANGER_DETECTED_RECENTLY)).apply((Applicative)i, location -> (level, body, timestamp) -> {
        if (body.isScared()) {
            body.rollOut();
            return true;
        }
        return false;
    }));

    protected static List<ActivityData<Armadillo>> getActivities() {
        return List.of(ArmadilloAi.initCoreActivity(), ArmadilloAi.initIdleActivity(), ArmadilloAi.initScaredActivity());
    }

    private static ActivityData<Armadillo> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), (Object)new ArmadilloPanic(2.0f), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(){

            @Override
            protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
                Armadillo armadillo;
                if (body instanceof Armadillo && (armadillo = (Armadillo)body).isScared()) {
                    return false;
                }
                return super.checkExtraStartConditions(level, body);
            }
        }, (Object)new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), (Object)new CountDownCooldownTicks(MemoryModuleType.GAZE_COOLDOWN_TICKS), ARMADILLO_ROLLING_OUT));
    }

    private static ActivityData<Armadillo> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(30, 60))), (Object)Pair.of((Object)1, (Object)new AnimalMakeLove(EntityTypes.ARMADILLO, 1.0f, 1)), (Object)Pair.of((Object)2, new RunOne(ImmutableList.of((Object)Pair.of((Object)new FollowTemptation(armadillo -> Float.valueOf(1.25f), armadillo -> armadillo.isBaby() ? 1.0 : 2.0), (Object)1), (Object)Pair.of(BabyFollowAdult.create(ADULT_FOLLOW_RANGE, 1.25f), (Object)1)))), (Object)Pair.of((Object)3, (Object)new RandomLookAround(UniformInt.of(150, 250), 30.0f, 0.0f, 0.0f)), (Object)Pair.of((Object)4, new RunOne((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), ImmutableList.of((Object)Pair.of(RandomStroll.stroll(1.0f), (Object)1), (Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)1), (Object)Pair.of((Object)new DoNothing(30, 60), (Object)1))))));
    }

    private static ActivityData<Armadillo> initScaredActivity() {
        return ActivityData.create(Activity.PANIC, ImmutableList.of((Object)Pair.of((Object)0, (Object)new ArmadilloBallUp())), Set.of(Pair.of(MemoryModuleType.DANGER_DETECTED_RECENTLY, (Object)((Object)MemoryStatus.VALUE_PRESENT)), Pair.of(MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    public static void updateActivity(Armadillo body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.PANIC, (Object)Activity.IDLE));
    }

    public static class ArmadilloPanic
    extends AnimalPanic<Armadillo> {
        public ArmadilloPanic(float speedMultiplier) {
            super(speedMultiplier, mob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES);
        }

        @Override
        protected void start(ServerLevel level, Armadillo armadillo, long timestamp) {
            armadillo.rollOut();
            super.start(level, armadillo, timestamp);
        }
    }

    public static class ArmadilloBallUp
    extends Behavior<Armadillo> {
        private static final int BALL_UP_STAY_IN_STATE = 5 * TimeUtil.SECONDS_PER_MINUTE * 20;
        private static final int TICKS_DELAY_TO_DETERMINE_IF_DANGER_IS_STILL_AROUND = 5;
        private static final int DANGER_DETECTED_RECENTLY_DANGER_THRESHOLD = 75;
        private int nextPeekTimer = 0;
        private boolean dangerWasAround;

        public ArmadilloBallUp() {
            super(Map.of(), BALL_UP_STAY_IN_STATE);
        }

        @Override
        protected void tick(ServerLevel level, Armadillo body, long timestamp) {
            boolean dangerIsAround;
            super.tick(level, body, timestamp);
            if (this.nextPeekTimer > 0) {
                --this.nextPeekTimer;
            }
            if (body.shouldSwitchToScaredState()) {
                body.switchToState(Armadillo.ArmadilloState.SCARED);
                if (body.onGround()) {
                    body.playSound(SoundEvents.ARMADILLO_LAND);
                }
                return;
            }
            Armadillo.ArmadilloState state = body.getState();
            long dangerTickCounter = body.getBrain().getTimeUntilExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY);
            boolean bl = dangerIsAround = dangerTickCounter > 75L;
            if (dangerIsAround != this.dangerWasAround) {
                this.nextPeekTimer = this.pickNextPeekTimer(body);
            }
            this.dangerWasAround = dangerIsAround;
            if (state == Armadillo.ArmadilloState.SCARED) {
                if (this.nextPeekTimer == 0 && body.onGround() && dangerIsAround) {
                    level.broadcastEntityEvent(body, (byte)64);
                    this.nextPeekTimer = this.pickNextPeekTimer(body);
                }
                if (dangerTickCounter < (long)Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
                    body.playSound(SoundEvents.ARMADILLO_UNROLL_START);
                    body.switchToState(Armadillo.ArmadilloState.UNROLLING);
                }
            } else if (state == Armadillo.ArmadilloState.UNROLLING && dangerTickCounter > (long)Armadillo.ArmadilloState.UNROLLING.animationDuration()) {
                body.switchToState(Armadillo.ArmadilloState.SCARED);
            }
        }

        private int pickNextPeekTimer(Armadillo body) {
            return Armadillo.ArmadilloState.SCARED.animationDuration() + body.getRandom().nextIntBetweenInclusive(100, 400);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Armadillo body) {
            return body.onGround();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Armadillo body, long timestamp) {
            return body.getState().isThreatened();
        }

        @Override
        protected void start(ServerLevel level, Armadillo body, long timestamp) {
            body.rollUp();
        }

        @Override
        protected void stop(ServerLevel level, Armadillo body, long timestamp) {
            if (!body.canStayRolledUp()) {
                body.rollOut();
            }
        }
    }
}

