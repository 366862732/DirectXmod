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
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

@Environment(value=EnvType.CLIENT)
public class AxolotlRenderState
extends LivingEntityRenderState {
    public Axolotl.Variant variant = Axolotl.Variant.DEFAULT;
    public float playingDeadFactor;
    public float movingFactor;
    public float inWaterFactor = 1.0f;
    public float onGroundFactor;
    public final AnimationState swimAnimation = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    public final AnimationState walkUnderWaterAnimationState = new AnimationState();
    public final AnimationState idleUnderWaterAnimationState = new AnimationState();
    public final AnimationState idleUnderWaterOnGroundAnimationState = new AnimationState();
    public final AnimationState idleOnGroundAnimationState = new AnimationState();
    public final AnimationState playDeadAnimationState = new AnimationState();
}

