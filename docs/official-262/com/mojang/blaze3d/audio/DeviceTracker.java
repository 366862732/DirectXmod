/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.DeviceList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface DeviceTracker {
    public DeviceList currentDevices();

    public void tick();

    public void forceRefresh();
}

