/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.audio;

import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.DeviceTracker;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractDeviceTracker
implements DeviceTracker {
    private volatile DeviceList deviceList;
    private final AtomicBoolean updatePending = new AtomicBoolean();

    public AbstractDeviceTracker(DeviceList deviceList) {
        this.deviceList = deviceList;
    }

    protected abstract boolean isUpdateRequested();

    protected abstract void discardUpdateRequest();

    @Override
    public DeviceList currentDevices() {
        return this.deviceList;
    }

    @Override
    public void forceRefresh() {
        this.discardUpdateRequest();
        this.deviceList = DeviceList.query();
    }

    @Override
    public void tick() {
        if (this.isUpdateRequested()) {
            this.discardUpdateRequest();
            if (this.updatePending.compareAndSet(false, true)) {
                Util.ioPool().execute(() -> {
                    this.deviceList = DeviceList.query();
                    this.updatePending.set(false);
                });
            }
        }
    }
}

