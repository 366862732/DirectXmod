/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.ticks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ticks.TickAccess;

public interface LevelTickAccess<T>
extends TickAccess<T> {
    public boolean willTickThisTick(BlockPos var1, T var2);
}

