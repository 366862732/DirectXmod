/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class ShadowFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Shadow");
    private static final RenderType SHADOW_RENDER_TYPE = RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        VertexConsumer builder = this.getVertexBuilder(SHADOW_RENDER_TYPE);
        for (Submit submit : submits) {
            this.prepare(submit, builder);
        }
    }

    private void prepare(Submit submit, VertexConsumer builder) {
        for (EntityRenderState.ShadowPiece piece : submit.pieces()) {
            AABB aabb = piece.shapeBelow().bounds();
            float x01 = piece.relativeX() + (float)aabb.minX;
            float x11 = piece.relativeX() + (float)aabb.maxX;
            float y01 = piece.relativeY() + (float)aabb.minY;
            float z01 = piece.relativeZ() + (float)aabb.minZ;
            float z11 = piece.relativeZ() + (float)aabb.maxZ;
            float radius = submit.radius();
            float u0 = -x01 / 2.0f / radius + 0.5f;
            float u1 = -x11 / 2.0f / radius + 0.5f;
            float v0 = -z01 / 2.0f / radius + 0.5f;
            float v1 = -z11 / 2.0f / radius + 0.5f;
            int color = ARGB.white(piece.alpha());
            ShadowFeatureRenderer.shadowVertex(submit.pose(), builder, color, x01, y01, z01, u0, v0);
            ShadowFeatureRenderer.shadowVertex(submit.pose(), builder, color, x01, y01, z11, u0, v1);
            ShadowFeatureRenderer.shadowVertex(submit.pose(), builder, color, x11, y01, z11, u1, v1);
            ShadowFeatureRenderer.shadowVertex(submit.pose(), builder, color, x11, y01, z01, u1, v0);
        }
    }

    private static void shadowVertex(Matrix4fc pose, VertexConsumer buffer, int color, float x, float y, float z, float u, float v) {
        Vector3f position = pose.transformPosition(x, y, z, new Vector3f());
        buffer.addVertex(position.x(), position.y(), position.z(), color, u, v, OverlayTexture.NO_OVERLAY, 0xF000F0, 0.0f, 1.0f, 0.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(Matrix4fc pose, float radius, List<EntityRenderState.ShadowPiece> pieces) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

