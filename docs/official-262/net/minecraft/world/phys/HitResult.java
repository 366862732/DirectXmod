/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.phys;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class HitResult {
    protected final Vec3 location;

    protected HitResult(Vec3 location) {
        this.location = location;
    }

    public double distanceTo(Entity entity) {
        double xd = this.location.x - entity.getX();
        double yd = this.location.y - entity.getY();
        double zd = this.location.z - entity.getZ();
        return xd * xd + yd * yd + zd * zd;
    }

    public abstract Type getType();

    public Vec3 getLocation() {
        return this.location;
    }

    public static enum Type {
        MISS,
        BLOCK,
        ENTITY;

    }
}

