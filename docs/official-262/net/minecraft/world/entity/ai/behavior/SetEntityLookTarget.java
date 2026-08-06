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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetEntityLookTarget {
    public static BehaviorControl<LivingEntity> create(MobCategory category, float maxDist) {
        return SetEntityLookTarget.create((LivingEntity mob) -> category.equals(mob.getType().getCategory()), maxDist);
    }

    public static OneShot<LivingEntity> create(EntityType<?> type, float maxDist) {
        return SetEntityLookTarget.create((LivingEntity mob) -> mob.is(type), maxDist);
    }

    public static OneShot<LivingEntity> create(float maxDist) {
        return SetEntityLookTarget.create((LivingEntity mob) -> true, maxDist);
    }

    public static OneShot<LivingEntity> create(Predicate<LivingEntity> predicate, float maxDist) {
        float maxDistSqr = maxDist * maxDist;
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.LOOK_TARGET), i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply((Applicative)i, (lookTarget, nearestEntities) -> (level, body, timestamp) -> {
            Optional<LivingEntity> target = ((NearestVisibleLivingEntities)i.get(nearestEntities)).findClosest(predicate.and(mob -> mob.distanceToSqr(body) <= (double)maxDistSqr && !body.hasPassenger((Entity)mob)));
            if (target.isEmpty()) {
                return false;
            }
            lookTarget.set(new EntityTracker(target.get(), true));
            return true;
        }));
    }
}

