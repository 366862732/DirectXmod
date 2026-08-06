/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.server;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public interface PackLoadFeedback {
    public void reportUpdate(UUID var1, Update var2);

    public void reportFinalResult(UUID var1, FinalResult var2);

    @Environment(value=EnvType.CLIENT)
    public static enum FinalResult {
        DECLINED,
        APPLIED,
        DISCARDED,
        DOWNLOAD_FAILED,
        ACTIVATION_FAILED;

    }

    @Environment(value=EnvType.CLIENT)
    public static enum Update {
        ACCEPTED,
        DOWNLOADED;

    }
}

