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
import net.minecraft.client.renderer.entity.state.SlimeRenderState;

@Environment(value=EnvType.CLIENT)
public class SulfurCubeRenderState
extends SlimeRenderState {
    public BlockModelRenderState containedBlock = new BlockModelRenderState();
    public float fuseRemainingTicks;
}

