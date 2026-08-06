/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class UploadStatus {
    private volatile long bytesWritten;
    private volatile long totalBytes;
    private long previousTimeSnapshot = Util.getMillis();
    private long previousBytesWritten;
    private long bytesPerSecond;

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public void restart() {
        this.bytesWritten = 0L;
        this.previousTimeSnapshot = Util.getMillis();
        this.previousBytesWritten = 0L;
        this.bytesPerSecond = 0L;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public long getBytesWritten() {
        return this.bytesWritten;
    }

    public void onWrite(long bytesWritten) {
        this.bytesWritten = bytesWritten;
    }

    public boolean uploadStarted() {
        return this.bytesWritten > 0L;
    }

    public boolean uploadCompleted() {
        return this.bytesWritten >= this.totalBytes;
    }

    public double getPercentage() {
        return Math.min((double)this.getBytesWritten() / (double)this.getTotalBytes(), 1.0);
    }

    public void refreshBytesPerSecond() {
        long currentMillis = Util.getMillis();
        long timeElapsed = currentMillis - this.previousTimeSnapshot;
        if (timeElapsed < 1000L) {
            return;
        }
        long bytesWritten = this.bytesWritten;
        this.bytesPerSecond = 1000L * (bytesWritten - this.previousBytesWritten) / timeElapsed;
        this.previousBytesWritten = bytesWritten;
        this.previousTimeSnapshot = currentMillis;
    }

    public long getBytesPerSecond() {
        return this.bytesPerSecond;
    }
}

