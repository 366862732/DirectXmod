/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith {
    public static <T extends LivingEntity> BehaviorControl<LivingEntity> of(EntityType<? extends T> type, int interactionRange, MemoryModuleType<T> interactionTarget, float speedModifier, int stopDistance) {
        return InteractWith.of(type, interactionRange, mob -> true, mob -> true, interactionTarget, speedModifier, stopDistance);
    }

    public static <E extends LivingEntity, T extends LivingEntity> BehaviorControl<E> of(EntityType<? extends T> type, int interactionRange, Predicate<E> selfFilter, Predicate<T> targetFilter, MemoryModuleType<T> interactionTarget, float speedModifier, int stopDistance) {
        int interactionRangeSqr = interactionRange * interactionRange;
        Predicate<LivingEntity> isTargetValid = mob -> mob.is(type) && targetFilter.test(mob);
        return BehaviorBuilder.create(i -> i.group(i.registered(interactionTarget), i.registered(MemoryModuleType.LOOK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply((Applicative)i, (target, lookTarget, walkTarget, nearestEntities) -> (level, body, timestamp) -> {
            NearestVisibleLivingEntities entities = (NearestVisibleLivingEntities)i.get(nearestEntities);
            if (selfFilter.test(body) && entities.contains(isTargetValid)) {
                Optional<LivingEntity> closest = entities.findClosest(mob -> mob.distanceToSqr(body) <= (double)interactionRangeSqr && isTargetValid.test((LivingEntity)mob));
                closest.ifPresent(mob -> {
                    target.set(mob);
                    lookTarget.set(new EntityTracker((Entity)mob, true));
                    walkTarget.set(new WalkTarget(new EntityTracker((Entity)mob, false), speedModifier, stopDistance));
                });
                return true;
            }
            return false;
        }));
    }
}

