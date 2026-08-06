/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface EntitySubPredicate {
    public static final EntitySubPredicate ALWAYS_TRUE = (entity, serverLevel, vec3) -> true;

    public boolean matches(Entity var1, ServerLevel var2, @Nullable Vec3 var3);

    default public EntitySubPredicate and(EntitySubPredicate other) {
        return (entity, level, position) -> this.matches(entity, level, position) && other.matches(entity, level, position);
    }
}

