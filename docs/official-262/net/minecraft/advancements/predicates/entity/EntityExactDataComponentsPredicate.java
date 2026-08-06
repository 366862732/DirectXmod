/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityExactDataComponentsPredicate(DataComponentExactPredicate predicate) implements EntitySubPredicate
{
    public static final Codec<EntityExactDataComponentsPredicate> CODEC = DataComponentExactPredicate.CODEC.xmap(EntityExactDataComponentsPredicate::new, EntityExactDataComponentsPredicate::predicate);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.predicate.test(entity);
    }
}

