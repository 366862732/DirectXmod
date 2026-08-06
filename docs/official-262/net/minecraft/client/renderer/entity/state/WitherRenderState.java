/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Environment(value=EnvType.CLIENT)
public class WitherRenderState
extends LivingEntityRenderState {
    public final float[] xHeadRots = new float[2];
    public final float[] yHeadRots = new float[2];
    public float invulnerableTicks;
    public boolean isPowered;
}

