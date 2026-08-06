/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.SlotsPredicate;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntitySlotsPredicate(SlotsPredicate slots) implements EntitySubPredicate
{
    public static final Codec<EntitySlotsPredicate> CODEC = SlotsPredicate.CODEC.xmap(EntitySlotsPredicate::new, EntitySlotsPredicate::slots);

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.slots.matches(entity);
    }
}

