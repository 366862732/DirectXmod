/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityHitResult
extends HitResult {
    private final Entity entity;

    public EntityHitResult(Entity entity) {
        this(entity, entity.position());
    }

    public EntityHitResult(Entity entity, Vec3 location) {
        super(location);
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public HitResult.Type getType() {
        return HitResult.Type.ENTITY;
    }
}

