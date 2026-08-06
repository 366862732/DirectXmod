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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Crackiness;

@Environment(value=EnvType.CLIENT)
public class IronGolemRenderState
extends LivingEntityRenderState {
    public float attackTicksRemaining;
    public int offerFlowerTick;
    public final BlockModelRenderState flowerBlock = new BlockModelRenderState();
    public Crackiness.Level crackiness = Crackiness.Level.NONE;
}

