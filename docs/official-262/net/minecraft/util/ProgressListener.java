/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.network.chat.Component;

public interface ProgressListener {
    public void progressStartNoAbort(Component var1);

    public void progressStart(Component var1);

    public void progressStage(Component var1);

    public void progressStagePercentage(int var1);

    public void stop();
}

