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
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class NameTagFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Name Tag");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        GlyphRenderer glyphRenderer = new GlyphRenderer(this);
        for (Submit nameTag : submits) {
            Font.PreparedText preparedText = NameTagFeatureRenderer.prepareText(context.font(), nameTag);
            glyphRenderer.prepare(nameTag, nameTag.displayMode());
            preparedText.visit(glyphRenderer);
        }
    }

    private static Font.PreparedText prepareText(Font font, Submit nameTag) {
        return font.prepareText(nameTag.text().getVisualOrderText(), nameTag.x(), nameTag.y(), nameTag.color(), false, false, nameTag.backgroundColor());
    }

    @Environment(value=EnvType.CLIENT)
    private class GlyphRenderer
    implements Font.GlyphVisitor {
        private final Matrix4f pose;
        private int lightCoords;
        private Font.DisplayMode displayMode;
        final /* synthetic */ NameTagFeatureRenderer this$0;

        private GlyphRenderer(NameTagFeatureRenderer nameTagFeatureRenderer) {
            NameTagFeatureRenderer nameTagFeatureRenderer2 = nameTagFeatureRenderer;
            Objects.requireNonNull(nameTagFeatureRenderer2);
            this.this$0 = nameTagFeatureRenderer2;
            this.pose = new Matrix4f();
            this.lightCoords = 0xF000F0;
            this.displayMode = Font.DisplayMode.NORMAL;
        }

        public void prepare(Submit submit, Font.DisplayMode displayMode) {
            this.pose.set(submit.pose());
            this.lightCoords = submit.lightCoords();
            this.displayMode = displayMode;
        }

        @Override
        public void acceptRenderable(TextRenderable renderable) {
            VertexConsumer builder = this.this$0.getVertexBuilder(renderable.renderType(this.displayMode));
            renderable.render((Matrix4fc)this.pose, builder, this.lightCoords, false);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(Matrix4fc pose, float x, float y, Component text, int lightCoords, int color, int backgroundColor, Font.DisplayMode displayMode) implements TranslucentSubmit
    {
        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq(this.pose);
        }

        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

