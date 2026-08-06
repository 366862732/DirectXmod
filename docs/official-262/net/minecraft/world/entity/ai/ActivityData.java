/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

public record ActivityData<E extends LivingEntity>(Activity activityType, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions, Set<MemoryModuleType<?>> memoriesToEraseWhenStopped) {
    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, int priorityOfFirstBehavior, ImmutableList<? extends BehaviorControl<? super E>> behaviorList) {
        return ActivityData.create(activity, ActivityData.createPriorityPairs(priorityOfFirstBehavior, behaviorList));
    }

    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, int priorityOfFirstBehavior, ImmutableList<? extends BehaviorControl<? super E>> behaviorList, MemoryModuleType<?> memoryThatMustHaveValueAndWillBeErasedAfter) {
        ImmutableSet conditions = ImmutableSet.of((Object)Pair.of(memoryThatMustHaveValueAndWillBeErasedAfter, (Object)((Object)MemoryStatus.VALUE_PRESENT)));
        ImmutableSet memoriesToEraseWhenStopped = ImmutableSet.of(memoryThatMustHaveValueAndWillBeErasedAfter);
        return ActivityData.create(activity, ActivityData.createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions, memoriesToEraseWhenStopped);
    }

    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs) {
        return ActivityData.create(activity, behaviorPriorityPairs, ImmutableSet.of(), Sets.newHashSet());
    }

    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, int priorityOfFirstBehavior, ImmutableList<? extends BehaviorControl<? super E>> behaviorList, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions) {
        return ActivityData.create(activity, ActivityData.createPriorityPairs(priorityOfFirstBehavior, behaviorList), conditions);
    }

    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions) {
        return ActivityData.create(activity, behaviorPriorityPairs, conditions, Sets.newHashSet());
    }

    public static <E extends LivingEntity> ActivityData<E> create(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions, Set<MemoryModuleType<?>> memoriesToEraseWhenStopped) {
        return new ActivityData<E>(activity, behaviorPriorityPairs, conditions, memoriesToEraseWhenStopped);
    }

    public static <E extends LivingEntity> ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> createPriorityPairs(int priorityOfFirstBehavior, ImmutableList<? extends BehaviorControl<? super E>> behaviorList) {
        int nextPrio = priorityOfFirstBehavior;
        ImmutableList.Builder listBuilder = ImmutableList.builder();
        for (BehaviorControl behavior : behaviorList) {
            listBuilder.add((Object)Pair.of((Object)nextPrio++, (Object)behavior));
        }
        return listBuilder.build();
    }
}

