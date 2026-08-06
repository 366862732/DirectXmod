/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.AbstractDeviceTracker;
import com.mojang.blaze3d.audio.DeviceList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class PollingDeviceTracker
extends AbstractDeviceTracker {
    private static final long DEFAULT_DEVICE_CHECK_INTERVAL_MS = 1000L;
    private long lastDeviceCheckTime;

    public PollingDeviceTracker(DeviceList deviceList) {
        super(deviceList);
    }

    @Override
    protected boolean isUpdateRequested() {
        return Util.getMillis() - this.lastDeviceCheckTime >= 1000L;
    }

    @Override
    protected void discardUpdateRequest() {
        this.lastDeviceCheckTime = Util.getMillis();
    }
}

