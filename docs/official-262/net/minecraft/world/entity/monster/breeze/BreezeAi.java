/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.LongJump;
import net.minecraft.world.entity.monster.breeze.Shoot;
import net.minecraft.world.entity.monster.breeze.ShootWhenStuck;
import net.minecraft.world.entity.monster.breeze.Slide;
import net.minecraft.world.entity.schedule.Activity;

public class BreezeAi {
    public static final float SPEED_MULTIPLIER_WHEN_SLIDING = 0.6f;
    public static final float JUMP_CIRCLE_INNER_RADIUS = 4.0f;
    public static final float JUMP_CIRCLE_MIDDLE_RADIUS = 8.0f;
    public static final float JUMP_CIRCLE_OUTER_RADIUS = 24.0f;
    private static final int TICKS_TO_REMEMBER_SEEN_TARGET = 100;

    protected static List<ActivityData<Breeze>> getActivities(Breeze breeze) {
        return List.of(BreezeAi.initCoreActivity(), BreezeAi.initIdleActivity(), BreezeAi.initFightActivity(breeze));
    }

    private static ActivityData<Breeze> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new Swim(0.8f), (Object)new LookAtTargetSink(45, 90)));
    }

    private static ActivityData<Breeze> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, StartAttacking.create((serverLevel, breeze) -> breeze.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))), (Object)Pair.of((Object)1, StartAttacking.create((serverLevel, breeze) -> breeze.getBrain().getMemory(MemoryModuleType.HURT_BY).map(DamageSource::getEntity).filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity)entity))), (Object)Pair.of((Object)2, (Object)new SlideToTargetSink(20, 40)), (Object)Pair.of((Object)3, new RunOne(ImmutableList.of((Object)Pair.of((Object)new DoNothing(20, 100), (Object)1), (Object)Pair.of(RandomStroll.stroll(0.6f), (Object)2))))));
    }

    private static ActivityData<Breeze> initFightActivity(Breeze body) {
        return ActivityData.create(Activity.FIGHT, ImmutableList.of((Object)Pair.of((Object)0, StopAttackingIfTargetInvalid.create(Sensor.wasEntityAttackableLastNTicks(body, 100).negate()::test)), (Object)Pair.of((Object)1, (Object)new Shoot()), (Object)Pair.of((Object)2, (Object)new LongJump()), (Object)Pair.of((Object)3, (Object)new ShootWhenStuck()), (Object)Pair.of((Object)4, (Object)new Slide())), ImmutableSet.of((Object)Pair.of(MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), (Object)Pair.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    public static void updateActivity(Breeze body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.FIGHT, (Object)Activity.IDLE));
    }

    public static class SlideToTargetSink
    extends MoveToTargetSink {
        @VisibleForTesting
        public SlideToTargetSink(int minTimeout, int maxTimeout) {
            super(minTimeout, maxTimeout);
        }

        @Override
        protected void start(ServerLevel level, Mob body, long timestamp) {
            super.start(level, body, timestamp);
            body.playSound(SoundEvents.BREEZE_SLIDE);
            body.setPose(Pose.SLIDING);
        }

        @Override
        protected void stop(ServerLevel level, Mob body, long timestamp) {
            super.stop(level, body, timestamp);
            body.setPose(Pose.STANDING);
            if (body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                body.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 60L);
            }
        }
    }
}

