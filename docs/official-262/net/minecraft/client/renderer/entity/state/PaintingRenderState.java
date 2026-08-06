/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class PaintingRenderState
extends EntityRenderState {
    public Direction direction = Direction.NORTH;
    public @Nullable PaintingVariant variant;
    public int[] lightCoordsPerBlock = new int[0];
}

