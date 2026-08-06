/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public interface Hopper
extends Container {
    public static final AABB SUCK_AABB = Block.column(16.0, 11.0, 32.0).toAabbs().get(0);

    default public AABB getSuckAabb() {
        return SUCK_AABB;
    }

    public double getLevelX();

    public double getLevelY();

    public double getLevelZ();

    public boolean isGridAligned();
}

