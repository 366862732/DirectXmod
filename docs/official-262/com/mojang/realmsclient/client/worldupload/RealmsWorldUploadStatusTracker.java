/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.client.UploadStatus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface RealmsWorldUploadStatusTracker {
    public UploadStatus getUploadStatus();

    public void setUploading();

    public static RealmsWorldUploadStatusTracker noOp() {
        return new RealmsWorldUploadStatusTracker(){
            private final UploadStatus uploadStatus = new UploadStatus();

            @Override
            public UploadStatus getUploadStatus() {
                return this.uploadStatus;
            }

            @Override
            public void setUploading() {
            }
        };
    }
}

