/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Mount {
    private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;

    public static BehaviorControl<LivingEntity> create(float speedModifier) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.LOOK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.present(MemoryModuleType.RIDE_TARGET)).apply((Applicative)i, (lookTarget, walkTarget, rideTarget) -> (level, body, timestamp) -> {
            if (body.isPassenger()) {
                return false;
            }
            Entity ridableEntity = (Entity)i.get(rideTarget);
            if (ridableEntity.closerThan(body, 1.0)) {
                body.startRiding(ridableEntity);
            } else {
                lookTarget.set(new EntityTracker(ridableEntity, true));
                walkTarget.set(new WalkTarget(new EntityTracker(ridableEntity, false), speedModifier, 1));
            }
            return true;
        }));
    }
}

