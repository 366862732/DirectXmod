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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;

@Environment(value=EnvType.CLIENT)
public class PiglinRenderState
extends HumanoidRenderState {
    public boolean isBrute;
    public boolean isConverting;
    public float maxCrossbowChageDuration;
    public PiglinArmPose armPose = PiglinArmPose.DEFAULT;
}

