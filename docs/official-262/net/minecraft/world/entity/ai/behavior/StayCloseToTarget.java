/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StayCloseToTarget {
    public static BehaviorControl<LivingEntity> create(Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter, Predicate<LivingEntity> shouldRunPredicate, int closeEnough, int tooFar, float speedModifier) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.LOOK_TARGET), i.registered(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, (lookTarget, walkTarget) -> (level, body, timestamp) -> {
            Optional targetPosition = (Optional)targetPositionGetter.apply(body);
            if (targetPosition.isEmpty() || !shouldRunPredicate.test(body)) {
                return false;
            }
            PositionTracker positionTracker = (PositionTracker)targetPosition.get();
            if (body.position().closerThan(positionTracker.currentPosition(), tooFar)) {
                return false;
            }
            PositionTracker target = (PositionTracker)targetPosition.get();
            lookTarget.set(target);
            walkTarget.set(new WalkTarget(target, speedModifier, closeEnough));
            return true;
        }));
    }
}

