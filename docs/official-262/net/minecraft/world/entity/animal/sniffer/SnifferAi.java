/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.entity.animal.sniffer;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.schedule.Activity;
import org.slf4j.Logger;

public class SnifferAi {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_LOOK_DISTANCE = 6;
    private static final int SNIFFING_COOLDOWN_TICKS = 9600;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0f;
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0f;
    private static final float SPEED_MULTIPLIER_WHEN_SNIFFING = 1.25f;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25f;

    public static List<ActivityData<Sniffer>> getActivities() {
        return List.of(SnifferAi.initCoreActivity(), SnifferAi.initIdleActivity(), SnifferAi.initSniffingActivity(), SnifferAi.initDigActivity());
    }

    private static Sniffer resetSniffing(Sniffer body) {
        body.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
        body.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        return body.transitionTo(Sniffer.State.IDLING);
    }

    private static ActivityData<Sniffer> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), (Object)new AnimalPanic<Sniffer>(2.0f){

            @Override
            protected void start(ServerLevel level, Sniffer body, long timestamp) {
                SnifferAi.resetSniffing(body);
                super.start(level, body, timestamp);
            }
        }, (Object)new MoveToTargetSink(500, 700), (Object)new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));
    }

    private static ActivityData<Sniffer> initSniffingActivity() {
        return ActivityData.create(Activity.SNIFF, ImmutableList.of((Object)Pair.of((Object)0, (Object)new Searching())), Set.of(Pair.of(MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT)), Pair.of(MemoryModuleType.SNIFFER_SNIFFING_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), Pair.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT))));
    }

    private static ActivityData<Sniffer> initDigActivity() {
        return ActivityData.create(Activity.DIG, ImmutableList.of((Object)Pair.of((Object)0, (Object)new Digging(160, 180)), (Object)Pair.of((Object)0, (Object)new FinishedDigging(40))), Set.of(Pair.of(MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT)), Pair.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), Pair.of(MemoryModuleType.SNIFFER_DIGGING, (Object)((Object)MemoryStatus.VALUE_PRESENT))));
    }

    private static ActivityData<Sniffer> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, (Object)new AnimalMakeLove(EntityTypes.SNIFFER){

            @Override
            protected void start(ServerLevel level, Animal body, long timestamp) {
                SnifferAi.resetSniffing((Sniffer)body);
                super.start(level, body, timestamp);
            }
        }), (Object)Pair.of((Object)1, (Object)new FollowTemptation(sniffer -> Float.valueOf(1.25f), sniffer -> sniffer.isBaby() ? 2.5 : 3.5){

            @Override
            protected void start(ServerLevel level, PathfinderMob body, long timestamp) {
                SnifferAi.resetSniffing((Sniffer)body);
                super.start(level, body, timestamp);
            }
        }), (Object)Pair.of((Object)2, (Object)new LookAtTargetSink(45, 90)), (Object)Pair.of((Object)3, (Object)new FeelingHappy(40, 100)), (Object)Pair.of((Object)4, new RunOne(ImmutableList.of((Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)2), (Object)Pair.of((Object)new Scenting(40, 80), (Object)1), (Object)Pair.of((Object)new Sniffing(40, 80), (Object)1), (Object)Pair.of(SetEntityLookTarget.create(EntityTypes.PLAYER, 6.0f), (Object)1), (Object)Pair.of(RandomStroll.stroll(1.0f), (Object)1), (Object)Pair.of((Object)new DoNothing(5, 20), (Object)2))))), Set.of(Pair.of(MemoryModuleType.SNIFFER_DIGGING, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    public static void updateActivity(Sniffer body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.DIG, (Object)Activity.SNIFF, (Object)Activity.IDLE));
    }

    private static class Searching
    extends Behavior<Sniffer> {
        private Searching() {
            super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_SNIFFING_TARGET, MemoryStatus.VALUE_PRESENT), 600);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Sniffer sniffer) {
            return sniffer.canSniff();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer sniffer, long timestamp) {
            if (!sniffer.canSniff()) {
                sniffer.transitionTo(Sniffer.State.IDLING);
                return false;
            }
            Optional<BlockPos> walkTarget = sniffer.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(WalkTarget::getTarget).map(PositionTracker::currentBlockPosition);
            Optional<BlockPos> sniffingTarget = sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
            if (walkTarget.isEmpty() || sniffingTarget.isEmpty()) {
                return false;
            }
            return sniffingTarget.get().equals(walkTarget.get());
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.SEARCHING);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            if (sniffer.canDig() && sniffer.canSniff()) {
                sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_DIGGING, true);
            }
            sniffer.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        }
    }

    private static class Digging
    extends Behavior<Sniffer> {
        private Digging(int min, int max) {
            super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_DIGGING, MemoryStatus.VALUE_PRESENT, MemoryModuleType.SNIFF_COOLDOWN, MemoryStatus.VALUE_ABSENT), min, max);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Sniffer sniffer) {
            return sniffer.canSniff();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer sniffer, long timestamp) {
            return sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_DIGGING).isPresent() && sniffer.canDig() && !sniffer.isInLove();
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.DIGGING);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            boolean finished = this.timedOut(timestamp);
            if (finished) {
                sniffer.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 9600L);
            } else {
                SnifferAi.resetSniffing(sniffer);
            }
        }
    }

    private static class FinishedDigging
    extends Behavior<Sniffer> {
        private FinishedDigging(int duration) {
            super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_DIGGING, MemoryStatus.VALUE_PRESENT, MemoryModuleType.SNIFF_COOLDOWN, MemoryStatus.VALUE_PRESENT), duration, duration);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Sniffer sniffer) {
            return true;
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer sniffer, long timestamp) {
            return sniffer.getBrain().getMemory(MemoryModuleType.SNIFFER_DIGGING).isPresent();
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.RISING);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            boolean finished = this.timedOut(timestamp);
            sniffer.transitionTo(Sniffer.State.IDLING).onDiggingComplete(finished);
            sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
            sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_HAPPY, true);
        }
    }

    private static class FeelingHappy
    extends Behavior<Sniffer> {
        private FeelingHappy(int min, int max) {
            super(Map.of(MemoryModuleType.SNIFFER_HAPPY, MemoryStatus.VALUE_PRESENT), min, max);
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer sniffer, long timestamp) {
            return true;
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.FEELING_HAPPY);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.IDLING);
            sniffer.getBrain().eraseMemory(MemoryModuleType.SNIFFER_HAPPY);
        }
    }

    private static class Scenting
    extends Behavior<Sniffer> {
        private Scenting(int min, int max) {
            super(Map.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_DIGGING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_SNIFFING_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_HAPPY, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT), min, max);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Sniffer sniffer) {
            return !sniffer.isTempted();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer sniffer, long timestamp) {
            return true;
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.SCENTING);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.IDLING);
        }
    }

    private static class Sniffing
    extends Behavior<Sniffer> {
        private Sniffing(int min, int max) {
            super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFFER_SNIFFING_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SNIFF_COOLDOWN, MemoryStatus.VALUE_ABSENT), min, max);
        }

        @Override
        protected boolean checkExtraStartConditions(ServerLevel level, Sniffer body) {
            return !body.isBaby() && body.canSniff();
        }

        @Override
        protected boolean canStillUse(ServerLevel level, Sniffer body, long timestamp) {
            return body.canSniff();
        }

        @Override
        protected void start(ServerLevel level, Sniffer sniffer, long timestamp) {
            sniffer.transitionTo(Sniffer.State.SNIFFING);
        }

        @Override
        protected void stop(ServerLevel level, Sniffer sniffer, long timestamp) {
            boolean finished = this.timedOut(timestamp);
            sniffer.transitionTo(Sniffer.State.IDLING);
            if (finished) {
                sniffer.calculateDigPosition().ifPresent(position -> {
                    sniffer.getBrain().setMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET, position);
                    sniffer.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget((BlockPos)position, 1.25f, 0));
                });
            }
        }
    }
}

