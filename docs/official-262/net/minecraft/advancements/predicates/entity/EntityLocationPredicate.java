/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityLocationPredicate(LocationPredicate predicate) implements EntitySubPredicate
{
    public static final Codec<EntityLocationPredicate> CODEC = LocationPredicate.CODEC.xmap(EntityLocationPredicate::new, EntityLocationPredicate::predicate);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.predicate.matches(level, entity.getX(), entity.getY(), entity.getZ());
    }
}

