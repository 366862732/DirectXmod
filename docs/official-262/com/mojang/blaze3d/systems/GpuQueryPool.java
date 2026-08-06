/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.systems;

import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface GpuQueryPool
extends AutoCloseable {
    public int size();

    public OptionalLong getValue(int var1);

    public OptionalLong[] getValues(int var1, int var2);

    @Override
    public void close();
}

