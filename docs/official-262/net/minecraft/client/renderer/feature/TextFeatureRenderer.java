/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class TextFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Text");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        Font font = context.font();
        GlyphRenderer glyphRenderer = new GlyphRenderer(this);
        for (Submit submit : submits) {
            glyphRenderer.pose.set(submit.pose());
            glyphRenderer.lightCoords = submit.lightCoords();
            glyphRenderer.displayMode = submit.displayMode();
            if (submit.outlineColor() == 0) {
                Font.PreparedText text = font.prepareText(submit.string(), submit.x(), submit.y(), submit.color(), submit.dropShadow(), false, submit.backgroundColor());
                text.visit(glyphRenderer);
                continue;
            }
            Font.PreparedText outline = font.prepare8xTextOutline(submit.string(), submit.x(), submit.y(), submit.outlineColor());
            Font.PreparedText text = font.prepareText(submit.string(), submit.x(), submit.y(), submit.color(), false, false, 0);
            glyphRenderer.displayMode = Font.DisplayMode.NORMAL;
            outline.visit(glyphRenderer);
            glyphRenderer.displayMode = Font.DisplayMode.POLYGON_OFFSET;
            text.visit(glyphRenderer);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class GlyphRenderer
    implements Font.GlyphVisitor {
        private final Matrix4f pose;
        private int lightCoords;
        private Font.DisplayMode displayMode;
        final /* synthetic */ TextFeatureRenderer this$0;

        private GlyphRenderer(TextFeatureRenderer textFeatureRenderer) {
            TextFeatureRenderer textFeatureRenderer2 = textFeatureRenderer;
            Objects.requireNonNull(textFeatureRenderer2);
            this.this$0 = textFeatureRenderer2;
            this.pose = new Matrix4f();
            this.lightCoords = 0xF000F0;
            this.displayMode = Font.DisplayMode.NORMAL;
        }

        @Override
        public void acceptRenderable(TextRenderable renderable) {
            VertexConsumer builder = this.this$0.getVertexBuilder(renderable.renderType(this.displayMode));
            renderable.render((Matrix4fc)this.pose, builder, this.lightCoords, false);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(Matrix4fc pose, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

