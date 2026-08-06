/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface ScheduledTickAccess {
    public <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3, TickPriority var4);

    public <T> ScheduledTick<T> createTick(BlockPos var1, T var2, int var3);

    public LevelTickAccess<Block> getBlockTicks();

    default public void scheduleTick(BlockPos pos, Block type, int tickDelay, TickPriority priority) {
        this.getBlockTicks().schedule(this.createTick(pos, type, tickDelay, priority));
    }

    default public void scheduleTick(BlockPos pos, Block type, int tickDelay) {
        this.getBlockTicks().schedule(this.createTick(pos, type, tickDelay));
    }

    public LevelTickAccess<Fluid> getFluidTicks();

    default public void scheduleTick(BlockPos pos, Fluid type, int tickDelay, TickPriority priority) {
        this.getFluidTicks().schedule(this.createTick(pos, type, tickDelay, priority));
    }

    default public void scheduleTick(BlockPos pos, Fluid type, int tickDelay) {
        this.getFluidTicks().schedule(this.createTick(pos, type, tickDelay));
    }
}

