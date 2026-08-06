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
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BlockEntityWithBoundingBoxRenderState
extends BlockEntityRenderState {
    public boolean isVisible;
    public BoundingBoxRenderable.Mode mode;
    public BoundingBoxRenderable.RenderableBox box;
    public @Nullable InvisibleBlockType @Nullable [] invisibleBlocks;
    public boolean @Nullable [] structureVoids;

    @Environment(value=EnvType.CLIENT)
    public static enum InvisibleBlockType {
        AIR,
        BARRIER,
        LIGHT,
        STRUCTURE_VOID;

    }
}

