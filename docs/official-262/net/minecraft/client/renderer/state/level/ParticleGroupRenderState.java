/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.state.level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Environment(value=EnvType.CLIENT)
public interface ParticleGroupRenderState {
    public void submit(SubmitNodeCollector var1, CameraRenderState var2);

    default public void clear() {
    }
}

