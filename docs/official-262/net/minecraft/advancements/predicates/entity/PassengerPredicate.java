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

public record PassengerPredicate(EntityPredicate passenger) implements EntitySubPredicate
{
    public static final Codec<PassengerPredicate> CODEC = EntityPredicate.CODEC.xmap(PassengerPredicate::new, PassengerPredicate::passenger);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        for (Entity passenger : entity.getPassengers()) {
            if (!this.passenger.matches(level, position, passenger)) continue;
            return true;
        }
        return false;
    }
}

