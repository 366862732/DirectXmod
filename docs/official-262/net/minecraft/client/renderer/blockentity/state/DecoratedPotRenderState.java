/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DecoratedPotRenderState
extends BlockEntityRenderState {
    public float yRot;
    public  @Nullable DecoratedPotBlockEntity.WobbleStyle wobbleStyle;
    public float wobbleProgress;
    public PotDecorations decorations = PotDecorations.EMPTY;
    public Direction direction = Direction.NORTH;
}

