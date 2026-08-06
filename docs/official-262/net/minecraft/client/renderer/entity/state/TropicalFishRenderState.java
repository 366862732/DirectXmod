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
import net.minecraft.world.entity.animal.fish.TropicalFish;

@Environment(value=EnvType.CLIENT)
public class TropicalFishRenderState
extends LivingEntityRenderState {
    public TropicalFish.Pattern pattern = TropicalFish.Pattern.FLOPPER;
    public int baseColor = -1;
    public int patternColor = -1;
}

