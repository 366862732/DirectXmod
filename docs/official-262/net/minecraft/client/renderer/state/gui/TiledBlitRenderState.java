/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3x2f
 *  org.joml.Matrix3x2fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.state.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record TiledBlitRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements GuiElementRenderState
{
    public TiledBlitRenderState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, @Nullable ScreenRectangle scissorArea) {
        this(pipeline, textureSetup, pose, tileWidth, tileHeight, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, TiledBlitRenderState.getBounds(x0, y0, x1, y1, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        int width = this.x1() - this.x0();
        int height = this.y1() - this.y0();
        for (int tileX = 0; tileX < width; tileX += this.tileWidth()) {
            float u1;
            int tileWidth;
            int remainingWidth = width - tileX;
            if (this.tileWidth() <= remainingWidth) {
                tileWidth = this.tileWidth();
                u1 = this.u1();
            } else {
                tileWidth = remainingWidth;
                u1 = Mth.lerp((float)remainingWidth / (float)this.tileWidth(), this.u0(), this.u1());
            }
            for (int tileY = 0; tileY < height; tileY += this.tileHeight()) {
                float v1;
                int tileHeight;
                int remainingHeight = height - tileY;
                if (this.tileHeight() <= remainingHeight) {
                    tileHeight = this.tileHeight();
                    v1 = this.v1();
                } else {
                    tileHeight = remainingHeight;
                    v1 = Mth.lerp((float)remainingHeight / (float)this.tileHeight(), this.v0(), this.v1());
                }
                int x0 = this.x0() + tileX;
                int x1 = this.x0() + tileX + tileWidth;
                int y0 = this.y0() + tileY;
                int y1 = this.y0() + tileY + tileHeight;
                vertexConsumer.addVertexWith2DPose((Matrix3x2fc)this.pose(), x0, y0).setUv(this.u0(), this.v0()).setColor(this.color());
                vertexConsumer.addVertexWith2DPose((Matrix3x2fc)this.pose(), x0, y1).setUv(this.u0(), v1).setColor(this.color());
                vertexConsumer.addVertexWith2DPose((Matrix3x2fc)this.pose(), x1, y1).setUv(u1, v1).setColor(this.color());
                vertexConsumer.addVertexWith2DPose((Matrix3x2fc)this.pose(), x1, y0).setUv(u1, this.v0()).setColor(this.color());
            }
        }
    }

    private static @Nullable ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds((Matrix3x2fc)pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}

