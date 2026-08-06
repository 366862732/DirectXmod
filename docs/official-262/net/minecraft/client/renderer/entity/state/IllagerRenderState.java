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
import net.minecraft.client.renderer.entity.state.UndeadRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

@Environment(value=EnvType.CLIENT)
public class IllagerRenderState
extends UndeadRenderState {
    public boolean isRiding;
    public boolean isAggressive;
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public AbstractIllager.IllagerArmPose armPose = AbstractIllager.IllagerArmPose.NEUTRAL;
    public int maxCrossbowChargeDuration;
    public float ticksUsingItem;
    public float attackAnim;
}

