/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.client.worldupload.RealmsUploadException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class RealmsUploadCanceledException
extends RealmsUploadException {
    private static final Component UPLOAD_CANCELED = Component.translatable("mco.upload.cancelled");

    @Override
    public Component getStatusMessage() {
        return UPLOAD_CANCELED;
    }
}

