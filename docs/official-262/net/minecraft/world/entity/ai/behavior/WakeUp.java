/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.schedule.Activity;

public class WakeUp {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
            if (body.getBrain().isActive(Activity.REST) || !body.isSleeping()) {
                return false;
            }
            body.stopSleeping();
            return true;
        }));
    }
}

