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
public class GoatRenderState
extends LivingEntityRenderState {
    public boolean hasLeftHorn = true;
    public boolean hasRightHorn = true;
    public float rammingXHeadRot;
}

