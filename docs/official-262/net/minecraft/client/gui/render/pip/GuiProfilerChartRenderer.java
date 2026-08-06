/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.GuiProfilerChartRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ResultField;

@Environment(value=EnvType.CLIENT)
public class GuiProfilerChartRenderer
extends PictureInPictureRenderer<GuiProfilerChartRenderState> {
    @Override
    public Class<GuiProfilerChartRenderState> getRenderStateClass() {
        return GuiProfilerChartRenderState.class;
    }

    @Override
    protected void renderToTexture(GuiProfilerChartRenderState chartState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        double totalPercentage = 0.0;
        poseStack.translate(0.0f, -5.0f, 0.0f);
        for (ResultField result : chartState.chartData()) {
            double slicePercentage = result.percentage;
            double currentPercentage = totalPercentage;
            totalPercentage += slicePercentage;
            int steps = Mth.floor(slicePercentage / 4.0) + 1;
            int color = ARGB.opaque(result.getColor());
            int shadeColor = ARGB.multiply(color, -8355712);
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugTriangleFan(), (pose, buffer) -> {
                buffer.addVertex(pose, 0.0f, 0.0f, 0.0f).setColor(color);
                for (int j = steps; j >= 0; --j) {
                    float dir = (float)((currentPercentage + slicePercentage * (double)j / (double)steps) * 6.2831854820251465 / 100.0);
                    float xx = Mth.sin(dir) * 105.0f;
                    float yy = Mth.cos(dir) * 105.0f * 0.5f;
                    buffer.addVertex(pose, xx, yy, 0.0f).setColor(color);
                }
            });
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, buffer) -> {
                for (int j = steps; j > 0; --j) {
                    float dir0 = (float)((currentPercentage + slicePercentage * (double)j / (double)steps) * 6.2831854820251465 / 100.0);
                    float x0 = Mth.sin(dir0) * 105.0f;
                    float y0 = Mth.cos(dir0) * 105.0f * 0.5f;
                    float dir1 = (float)((currentPercentage + slicePercentage * (double)(j - 1) / (double)steps) * 6.2831854820251465 / 100.0);
                    float x1 = Mth.sin(dir1) * 105.0f;
                    float y1 = Mth.cos(dir1) * 105.0f * 0.5f;
                    if ((y0 + y1) / 2.0f < 0.0f) continue;
                    buffer.addVertex(pose, x0, y0, 0.0f).setColor(shadeColor);
                    buffer.addVertex(pose, x0, y0 + 10.0f, 0.0f).setColor(shadeColor);
                    buffer.addVertex(pose, x1, y1 + 10.0f, 0.0f).setColor(shadeColor);
                    buffer.addVertex(pose, x1, y1, 0.0f).setColor(shadeColor);
                }
            });
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return (float)height / 2.0f;
    }

    @Override
    protected String getTextureLabel() {
        return "profiler chart";
    }
}

