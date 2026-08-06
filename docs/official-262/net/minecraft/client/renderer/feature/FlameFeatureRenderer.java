/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class FlameFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Flame");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS));
        TextureAtlasSprite fire1 = context.atlasManager().get(ModelBakery.FIRE_0);
        TextureAtlasSprite fire2 = context.atlasManager().get(ModelBakery.FIRE_1);
        for (Submit submit : submits) {
            this.prepare(submit, builder, fire1, fire2);
        }
    }

    private void prepare(Submit submit, VertexConsumer buffer, TextureAtlasSprite fire1, TextureAtlasSprite fire2) {
        PoseStack.Pose pose = submit.pose();
        EntityRenderState state = submit.entityRenderState();
        float s = state.boundingBoxWidth * 1.4f;
        pose.scale(s, s, s);
        float r = 0.5f;
        float xo = 0.0f;
        float h = state.boundingBoxHeight / s;
        float yo = 0.0f;
        pose.rotate((Quaternionfc)submit.rotation());
        pose.translate(0.0f, 0.0f, 0.3f - (float)((int)h) * 0.02f);
        float zo = 0.0f;
        int ss = 0;
        int lightCoords = LightCoordsUtil.withBlock(state.lightCoords, 15);
        while (h > 0.0f) {
            TextureAtlasSprite tex = ss % 2 == 0 ? fire1 : fire2;
            float u0 = tex.getU0();
            float v0 = tex.getV0();
            float u1 = tex.getU1();
            float v1 = tex.getV1();
            if (ss / 2 % 2 == 0) {
                float tmp = u1;
                u1 = u0;
                u0 = tmp;
            }
            FlameFeatureRenderer.fireVertex(pose, buffer, -r - 0.0f, 0.0f - yo, zo, u1, v1, lightCoords);
            FlameFeatureRenderer.fireVertex(pose, buffer, r - 0.0f, 0.0f - yo, zo, u0, v1, lightCoords);
            FlameFeatureRenderer.fireVertex(pose, buffer, r - 0.0f, 1.4f - yo, zo, u0, v0, lightCoords);
            FlameFeatureRenderer.fireVertex(pose, buffer, -r - 0.0f, 1.4f - yo, zo, u1, v0, lightCoords);
            h -= 0.45f;
            yo -= 0.45f;
            r *= 0.9f;
            zo -= 0.03f;
            ++ss;
        }
    }

    private static void fireVertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setUv1(0, 10).setLight(lightCoords).setNormal(pose, 0.0f, 1.0f, 0.0f);
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

