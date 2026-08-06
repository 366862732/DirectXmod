/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.border;

import net.minecraft.world.level.border.WorldBorder;

public interface BorderChangeListener {
    public void onSetSize(WorldBorder var1, double var2);

    public void onLerpSize(WorldBorder var1, double var2, double var4, long var6, long var8);

    public void onSetCenter(WorldBorder var1, double var2, double var4);

    public void onSetWarningTime(WorldBorder var1, int var2);

    public void onSetWarningBlocks(WorldBorder var1, int var2);

    public void onSetDamagePerBlock(WorldBorder var1, double var2);

    public void onSetSafeZone(WorldBorder var1, double var2);
}

