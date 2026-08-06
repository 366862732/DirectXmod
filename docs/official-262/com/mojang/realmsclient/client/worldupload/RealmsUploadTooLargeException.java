/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.worldupload.RealmsUploadException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public class RealmsUploadTooLargeException
extends RealmsUploadException {
    private final long sizeLimit;

    public RealmsUploadTooLargeException(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Override
    public Component[] getErrorMessages() {
        return new Component[]{Component.translatable("mco.upload.failed.too_big.title"), Component.translatable("mco.upload.failed.too_big.description", Unit.humanReadable(this.sizeLimit, Unit.getLargest(this.sizeLimit)))};
    }
}

