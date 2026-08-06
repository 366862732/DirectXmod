/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.waypoints;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface PartialTickSupplier {
    public float apply(Entity var1);
}

