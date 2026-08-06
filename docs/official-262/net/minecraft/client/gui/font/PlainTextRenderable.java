/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.font.TextRenderable;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public interface PlainTextRenderable
extends TextRenderable.Styled {
    public static final float DEFAULT_WIDTH = 8.0f;
    public static final float DEFAULT_HEIGHT = 8.0f;
    public static final float DEFUAULT_ASCENT = 8.0f;

    @Override
    default public void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
        float frontDepth = 0.0f;
        if (this.shadowColor() != 0) {
            this.renderSprite(pose, buffer, packedLightCoords, this.shadowOffset(), this.shadowOffset(), 0.0f, this.shadowColor());
            if (!flat) {
                frontDepth += 0.03f;
            }
        }
        this.renderSprite(pose, buffer, packedLightCoords, 0.0f, 0.0f, frontDepth, this.color());
    }

    public void renderSprite(Matrix4fc var1, VertexConsumer var2, int var3, float var4, float var5, float var6, int var7);

    public float x();

    public float y();

    public int color();

    public int shadowColor();

    public float shadowOffset();

    default public float width() {
        return 8.0f;
    }

    default public float height() {
        return 8.0f;
    }

    default public float ascent() {
        return 8.0f;
    }

    @Override
    default public float left() {
        return this.x();
    }

    @Override
    default public float right() {
        return this.left() + this.width();
    }

    @Override
    default public float top() {
        return this.y() + 7.0f - this.ascent();
    }

    @Override
    default public float bottom() {
        return this.activeTop() + this.height();
    }
}

