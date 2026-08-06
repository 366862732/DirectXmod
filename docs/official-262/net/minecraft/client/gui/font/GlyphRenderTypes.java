/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset, RenderPipeline guiPipeline) {
    public static GlyphRenderTypes createForGrayscaleTexture(Identifier name) {
        return new GlyphRenderTypes(RenderTypes.textGrayscale(name), RenderTypes.textGrayscaleSeeThrough(name), RenderTypes.textGrayscalePolygonOffset(name), RenderPipelines.GUI_TEXT_GRAYSCALE);
    }

    public static GlyphRenderTypes createForColorTexture(Identifier name) {
        return new GlyphRenderTypes(RenderTypes.text(name), RenderTypes.textSeeThrough(name), RenderTypes.textPolygonOffset(name), RenderPipelines.GUI_TEXT);
    }

    public RenderType select(Font.DisplayMode mode) {
        return switch (mode) {
            default -> throw new MatchException(null, null);
            case Font.DisplayMode.NORMAL -> this.normal;
            case Font.DisplayMode.SEE_THROUGH -> this.seeThrough;
            case Font.DisplayMode.POLYGON_OFFSET -> this.polygonOffset;
        };
    }
}

