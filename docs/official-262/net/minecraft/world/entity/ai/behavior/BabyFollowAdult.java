/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.Function;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class BabyFollowAdult {
    public static OneShot<LivingEntity> create(UniformInt followRange, float speedModifier) {
        return BabyFollowAdult.create(followRange, mob -> Float.valueOf(speedModifier), MemoryModuleType.NEAREST_VISIBLE_ADULT, false);
    }

    public static OneShot<LivingEntity> create(UniformInt followRange, Function<LivingEntity, Float> speedModifier, MemoryModuleType<? extends LivingEntity> nearestVisibleType, boolean targetEye) {
        return BehaviorBuilder.create(i -> i.group(i.present(nearestVisibleType), i.registered(MemoryModuleType.LOOK_TARGET), i.absent(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, (nearestAdult, lookTarget, walkTarget) -> (level, body, timestamp) -> {
            if (!body.isBaby()) {
                return false;
            }
            LivingEntity adult = (LivingEntity)i.get(nearestAdult);
            if (body.closerThan(adult, followRange.maxInclusive() + 1) && !body.closerThan(adult, followRange.minInclusive())) {
                WalkTarget target = new WalkTarget(new EntityTracker(adult, targetEye, targetEye), ((Float)speedModifier.apply(body)).floatValue(), followRange.minInclusive() - 1);
                lookTarget.set(new EntityTracker(adult, true, targetEye));
                walkTarget.set(target);
                return true;
            }
            return false;
        }));
    }
}

