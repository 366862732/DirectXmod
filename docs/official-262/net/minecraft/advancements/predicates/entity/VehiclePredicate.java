/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record VehiclePredicate(EntityPredicate vehicle) implements EntitySubPredicate
{
    public static final Codec<VehiclePredicate> CODEC = EntityPredicate.CODEC.xmap(VehiclePredicate::new, VehiclePredicate::vehicle);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.vehicle.matches(level, position, entity.getVehicle());
    }
}

