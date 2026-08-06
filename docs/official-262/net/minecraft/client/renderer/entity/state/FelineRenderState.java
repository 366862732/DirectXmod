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
public class FelineRenderState
extends LivingEntityRenderState {
    public boolean isCrouching;
    public boolean isSprinting;
    public boolean isSitting;
    public float lieDownAmount;
    public float lieDownAmountTail;
    public float relaxStateOneAmount;
}

