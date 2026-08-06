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
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.level.block.WeatheringCopper;

@Environment(value=EnvType.CLIENT)
public class CopperGolemRenderState
extends ArmedEntityRenderState {
    public WeatheringCopper.WeatherState weathering = WeatheringCopper.WeatherState.UNAFFECTED;
    public CopperGolemState copperGolemState = CopperGolemState.IDLE;
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState interactionGetItem = new AnimationState();
    public final AnimationState interactionGetNoItem = new AnimationState();
    public final AnimationState interactionDropItem = new AnimationState();
    public final AnimationState interactionDropNoItem = new AnimationState();
    public final BlockModelRenderState blockOnAntenna = new BlockModelRenderState();
}

