/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;

@Environment(value=EnvType.CLIENT)
public class SkullBlockRenderState
extends BlockEntityRenderState {
    public float animationProgress;
    public Transformation transformation = Transformation.IDENTITY;
    public SkullBlock.Type skullType = SkullBlock.Types.ZOMBIE;
    public RenderType renderType;
}

