/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SocializeAtBell {
    private static final float SPEED_MODIFIER = 0.3f;

    public static OneShot<LivingEntity> create() {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET), i.present(MemoryModuleType.MEETING_POINT), i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES), i.absent(MemoryModuleType.INTERACTION_TARGET)).apply((Applicative)i, (walkTarget, lookTarget, meetingPoint, nearestEntities, interactionTarget) -> (level, body, timestamp) -> {
            GlobalPos memory = (GlobalPos)i.get(meetingPoint);
            NearestVisibleLivingEntities visibleEntities = (NearestVisibleLivingEntities)i.get(nearestEntities);
            if (level.getRandom().nextInt(100) == 0 && level.dimension() == memory.dimension() && memory.pos().closerToCenterThan(body.position(), 4.0) && visibleEntities.contains(mob -> mob.is(EntityTypes.VILLAGER))) {
                visibleEntities.findClosest(mob -> mob.is(EntityTypes.VILLAGER) && mob.distanceToSqr(body) <= 32.0).ifPresent(mob -> {
                    interactionTarget.set(mob);
                    lookTarget.set(new EntityTracker((Entity)mob, true));
                    walkTarget.set(new WalkTarget(new EntityTracker((Entity)mob, false), 0.3f, 1));
                });
                return true;
            }
            return false;
        }));
    }
}

