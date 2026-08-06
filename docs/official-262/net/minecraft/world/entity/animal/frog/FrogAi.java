/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.Croak;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.LongJumpMidJump;
import net.minecraft.world.entity.ai.behavior.LongJumpToPreferredBlock;
import net.minecraft.world.entity.ai.behavior.LongJumpToRandomPos;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.TryFindLand;
import net.minecraft.world.entity.ai.behavior.TryFindLandNearWater;
import net.minecraft.world.entity.ai.behavior.TryLaySpawnOnFluidNearLand;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.ShootTongue;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class FrogAi {
    private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.0f;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0f;
    private static final float SPEED_MULTIPLIER_ON_LAND = 1.0f;
    private static final float SPEED_MULTIPLIER_IN_WATER = 0.75f;
    private static final UniformInt TIME_BETWEEN_LONG_JUMPS = UniformInt.of(100, 140);
    private static final int MAX_LONG_JUMP_HEIGHT = 2;
    private static final int MAX_LONG_JUMP_WIDTH = 4;
    private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 3.5714288f;
    private static final float SPEED_MULTIPLIER_WHEN_TEMPTED = 1.25f;

    protected static void initMemories(Frog body, RandomSource random) {
        body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, TIME_BETWEEN_LONG_JUMPS.sample(random));
    }

    protected static List<ActivityData<Frog>> getActivities() {
        return List.of(FrogAi.initCoreActivity(), FrogAi.initIdleActivity(), FrogAi.initSwimActivity(), FrogAi.initLaySpawnActivity(), FrogAi.initTongueActivity(), FrogAi.initJumpActivity());
    }

    private static ActivityData<Frog> initCoreActivity() {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(new AnimalPanic(2.0f), (Object)new LookAtTargetSink(45, 90), (Object)new MoveToTargetSink(), (Object)new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS), (Object)new CountDownCooldownTicks(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS)));
    }

    private static ActivityData<Frog> initIdleActivity() {
        return ActivityData.create(Activity.IDLE, ImmutableList.of((Object)Pair.of((Object)0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(30, 60))), (Object)Pair.of((Object)0, (Object)new AnimalMakeLove(EntityTypes.FROG)), (Object)Pair.of((Object)1, (Object)new FollowTemptation(s -> Float.valueOf(1.25f))), (Object)Pair.of((Object)2, StartAttacking.create((level, body) -> FrogAi.canAttack(body), (level, body) -> body.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))), (Object)Pair.of((Object)3, TryFindLand.create(6, 1.0f)), (Object)Pair.of((Object)4, new RunOne((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), ImmutableList.of((Object)Pair.of(RandomStroll.stroll(1.0f), (Object)1), (Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)1), (Object)Pair.of((Object)new Croak(), (Object)3), (Object)Pair.of(BehaviorBuilder.triggerIf(Entity::onGround), (Object)2))))), ImmutableSet.of((Object)Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.IS_IN_WATER, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    private static ActivityData<Frog> initSwimActivity() {
        return ActivityData.create(Activity.SWIM, ImmutableList.of((Object)Pair.of((Object)0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(30, 60))), (Object)Pair.of((Object)1, (Object)new FollowTemptation(s -> Float.valueOf(1.25f))), (Object)Pair.of((Object)2, StartAttacking.create((level, body) -> FrogAi.canAttack(body), (level, body) -> body.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))), (Object)Pair.of((Object)3, TryFindLand.create(8, 1.5f)), (Object)Pair.of((Object)5, new GateBehavior((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Set<MemoryModuleType<?>>)ImmutableSet.of(), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.TRY_ALL, ImmutableList.of((Object)Pair.of(RandomStroll.swim(0.75f), (Object)1), (Object)Pair.of(RandomStroll.stroll(1.0f, true), (Object)1), (Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)1), (Object)Pair.of(BehaviorBuilder.triggerIf(Entity::isInWater), (Object)5))))), ImmutableSet.of((Object)Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.IS_IN_WATER, (Object)((Object)MemoryStatus.VALUE_PRESENT))));
    }

    private static ActivityData<Frog> initLaySpawnActivity() {
        return ActivityData.create(Activity.LAY_SPAWN, ImmutableList.of((Object)Pair.of((Object)0, SetEntityLookTargetSometimes.create(EntityTypes.PLAYER, 6.0f, UniformInt.of(30, 60))), (Object)Pair.of((Object)1, StartAttacking.create((level, body) -> FrogAi.canAttack(body), (level, body) -> body.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))), (Object)Pair.of((Object)2, TryFindLandNearWater.create(8, 1.0f)), (Object)Pair.of((Object)3, TryLaySpawnOnFluidNearLand.create(Blocks.FROGSPAWN)), (Object)Pair.of((Object)4, new RunOne(ImmutableList.of((Object)Pair.of(RandomStroll.stroll(1.0f), (Object)2), (Object)Pair.of(SetWalkTargetFromLookTarget.create(1.0f, 3), (Object)1), (Object)Pair.of((Object)new Croak(), (Object)2), (Object)Pair.of(BehaviorBuilder.triggerIf(Entity::onGround), (Object)1))))), ImmutableSet.of((Object)Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.IS_PREGNANT, (Object)((Object)MemoryStatus.VALUE_PRESENT))));
    }

    private static ActivityData<Frog> initJumpActivity() {
        return ActivityData.create(Activity.LONG_JUMP, ImmutableList.of((Object)Pair.of((Object)0, (Object)new LongJumpMidJump(TIME_BETWEEN_LONG_JUMPS, SoundEvents.FROG_STEP)), (Object)Pair.of((Object)1, new LongJumpToPreferredBlock<Frog>(TIME_BETWEEN_LONG_JUMPS, 2, 4, 3.5714288f, frog -> SoundEvents.FROG_LONG_JUMP, BlockTags.FROG_PREFER_JUMP_TO, 0.5f, FrogAi::isAcceptableLandingSpot))), ImmutableSet.of((Object)Pair.of(MemoryModuleType.TEMPTING_PLAYER, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.BREED_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT)), (Object)Pair.of(MemoryModuleType.IS_IN_WATER, (Object)((Object)MemoryStatus.VALUE_ABSENT))));
    }

    private static ActivityData<Frog> initTongueActivity() {
        return ActivityData.create(Activity.TONGUE, 0, ImmutableList.of(StopAttackingIfTargetInvalid.create(), (Object)new ShootTongue(SoundEvents.FROG_TONGUE, SoundEvents.FROG_EAT)), MemoryModuleType.ATTACK_TARGET);
    }

    private static <E extends Mob> boolean isAcceptableLandingSpot(E body, BlockPos targetPos) {
        Level level = body.level();
        BlockPos below = targetPos.below();
        if (!(level.getFluidState(targetPos).isEmpty() && level.getFluidState(below).isEmpty() && level.getFluidState(targetPos.above()).isEmpty())) {
            return false;
        }
        BlockState bs = level.getBlockState(targetPos);
        BlockState bsBelow = level.getBlockState(below);
        if (bs.is(BlockTags.FROG_PREFER_JUMP_TO) || bsBelow.is(BlockTags.FROG_PREFER_JUMP_TO)) {
            return true;
        }
        PathfindingContext context = new PathfindingContext(body.level(), body);
        PathType pathType = WalkNodeEvaluator.getPathTypeStatic(context, targetPos.mutable());
        PathType pathTypeBelow = WalkNodeEvaluator.getPathTypeStatic(context, below.mutable());
        if (pathType == PathType.TRAPDOOR || bs.isAir() && pathTypeBelow == PathType.TRAPDOOR) {
            return true;
        }
        return LongJumpToRandomPos.defaultAcceptableLandingSpot(body, targetPos);
    }

    private static boolean canAttack(Mob mob) {
        return !BehaviorUtils.isBreeding(mob);
    }

    public static void updateActivity(Frog body) {
        body.getBrain().setActiveActivityToFirstValid((List<Activity>)ImmutableList.of((Object)Activity.TONGUE, (Object)Activity.LAY_SPAWN, (Object)Activity.LONG_JUMP, (Object)Activity.SWIM, (Object)Activity.IDLE));
    }

    public static Predicate<ItemStack> getTemptations() {
        return i -> i.is(ItemTags.FROG_FOOD);
    }
}

