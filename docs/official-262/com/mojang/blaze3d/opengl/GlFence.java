/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlCommandEncoder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GlFence
implements GpuFence {
    private final GlCommandEncoder encoder;
    private final long submitIndex;
    private boolean closedOrCompleted;

    GlFence(GlCommandEncoder encoder) {
        this.encoder = encoder;
        this.submitIndex = encoder.currentSubmitIndex();
    }

    @Override
    public void close() {
        this.closedOrCompleted = true;
    }

    @Override
    public boolean awaitCompletion(long timeoutNS) {
        if (this.closedOrCompleted) {
            return true;
        }
        this.closedOrCompleted = this.encoder.awaitSubmit(this.submitIndex, timeoutNS);
        return this.closedOrCompleted;
    }
}

