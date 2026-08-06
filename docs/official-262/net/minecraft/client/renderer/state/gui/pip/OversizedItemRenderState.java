/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.gui.pip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record OversizedItemRenderState(GuiItemRenderState guiItemRenderState, int x0, int y0, int x1, int y1) implements PictureInPictureRenderState
{
    @Override
    public float scale() {
        return 16.0f;
    }

    @Override
    public Matrix3x2fc pose() {
        return this.guiItemRenderState.pose();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return this.guiItemRenderState.scissorArea();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return this.guiItemRenderState.bounds();
    }
}

