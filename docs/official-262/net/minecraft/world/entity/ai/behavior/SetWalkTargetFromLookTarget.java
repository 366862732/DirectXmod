/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget {
    public static OneShot<LivingEntity> create(float speedModifier, int closeEnoughDistance) {
        return SetWalkTargetFromLookTarget.create(mob -> true, mob -> Float.valueOf(speedModifier), closeEnoughDistance);
    }

    public static OneShot<LivingEntity> create(Predicate<LivingEntity> canSetWalkTargetPredicate, Function<LivingEntity, Float> speedModifier, int closeEnoughDistance) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET), i.present(MemoryModuleType.LOOK_TARGET)).apply((Applicative)i, (walkTarget, lookTarget) -> (level, body, timestamp) -> {
            if (!canSetWalkTargetPredicate.test(body)) {
                return false;
            }
            walkTarget.set(new WalkTarget((PositionTracker)i.get(lookTarget), ((Float)speedModifier.apply(body)).floatValue(), closeEnoughDistance));
            return true;
        }));
    }
}

