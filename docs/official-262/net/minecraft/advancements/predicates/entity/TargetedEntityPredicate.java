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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record TargetedEntityPredicate(EntityPredicate targetedEntity) implements EntitySubPredicate
{
    public static final Codec<TargetedEntityPredicate> CODEC = EntityPredicate.CODEC.xmap(TargetedEntityPredicate::new, TargetedEntityPredicate::targetedEntity);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        LivingEntity livingEntity;
        if (entity instanceof Mob) {
            Mob mob = (Mob)entity;
            livingEntity = mob.getTarget();
        } else {
            livingEntity = null;
        }
        return this.targetedEntity.matches(level, position, livingEntity);
    }
}

