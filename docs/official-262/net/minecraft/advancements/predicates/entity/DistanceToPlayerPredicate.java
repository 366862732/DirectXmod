/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record DistanceToPlayerPredicate(DistancePredicate distance) implements EntitySubPredicate
{
    public static final Codec<DistanceToPlayerPredicate> CODEC = DistancePredicate.CODEC.xmap(DistanceToPlayerPredicate::new, DistanceToPlayerPredicate::distance);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        if (position == null) {
            return false;
        }
        return this.distance.matches(position.x, position.y, position.z, entity.getX(), entity.getY(), entity.getZ());
    }
}

