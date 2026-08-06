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
import net.minecraft.world.entity.animal.rabbit.Rabbit;

@Environment(value=EnvType.CLIENT)
public class RabbitRenderState
extends LivingEntityRenderState {
    public float jumpCompletion;
    public boolean isToast;
    public Rabbit.Variant variant = Rabbit.Variant.DEFAULT;
    public final AnimationState hopAnimationState = new AnimationState();
    public final AnimationState idleHeadTiltAnimationState = new AnimationState();
}

