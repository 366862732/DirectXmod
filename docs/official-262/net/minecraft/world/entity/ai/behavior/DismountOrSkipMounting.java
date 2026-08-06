/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.BiPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DismountOrSkipMounting {
    public static <E extends LivingEntity> BehaviorControl<E> create(int maxWalkDistToRideTarget, BiPredicate<E, Entity> dontRideIf) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.RIDE_TARGET)).apply((Applicative)i, rideTarget -> (level, body, timestamp) -> {
            Entity vehicle;
            Entity currentVehicle = body.getVehicle();
            Entity targetVehicle = i.tryGet(rideTarget).orElse(null);
            if (currentVehicle == null && targetVehicle == null) {
                return false;
            }
            Entity entity = vehicle = currentVehicle == null ? targetVehicle : currentVehicle;
            if (!DismountOrSkipMounting.isVehicleValid(body, vehicle, maxWalkDistToRideTarget) || dontRideIf.test(body, vehicle)) {
                body.stopRiding();
                rideTarget.erase();
                return true;
            }
            return false;
        }));
    }

    private static boolean isVehicleValid(LivingEntity body, Entity vehicle, int maxWalkDistToRideTarget) {
        return vehicle.isAlive() && vehicle.closerThan(body, maxWalkDistToRideTarget) && vehicle.level() == body.level();
    }
}

