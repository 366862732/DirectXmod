/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class ShapeOutlineFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Shape Outline");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        Vector3f normal = new Vector3f();
        for (Submit submit : submits) {
            PoseStack.Pose pose = submit.pose();
            int color = submit.color();
            float width = submit.width();
            VertexConsumer builder = this.getVertexBuilder(submit.renderType());
            submit.shape().forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                normal.set((float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1)).normalize();
                builder.addVertex(pose, (float)x1, (float)y1, (float)z1).setColor(color).setNormal(pose, (Vector3fc)normal).setLineWidth(width);
                builder.addVertex(pose, (float)x2, (float)y2, (float)z2).setColor(color).setNormal(pose, (Vector3fc)normal).setLineWidth(width);
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(PoseStack.Pose pose, VoxelShape shape, RenderType renderType, int color, float width) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

