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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class LeashFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Leash");
    private static final int LEASH_RENDER_STEPS = 24;
    private static final float LEASH_WIDTH = 0.05f;

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        for (Submit submit : submits) {
            this.prepare(submit);
        }
    }

    private void prepare(Submit submit) {
        int k;
        VertexConsumer builder = this.getVertexBuilder(RenderTypes.leash());
        Matrix4f pose = submit.pose();
        EntityRenderState.LeashState leashState = submit.leashState();
        float dx = (float)(leashState.end.x - leashState.start.x);
        float dy = (float)(leashState.end.y - leashState.start.y);
        float dz = (float)(leashState.end.z - leashState.start.z);
        float offsetFactor = Mth.invSqrt(dx * dx + dz * dz) * 0.05f / 2.0f;
        float dxOff = dz * offsetFactor;
        float dzOff = dx * offsetFactor;
        pose.translate((float)leashState.offset.x, (float)leashState.offset.y, (float)leashState.offset.z);
        for (k = 0; k <= 24; ++k) {
            LeashFeatureRenderer.addVertexPair(builder, (Matrix4fc)pose, dx, dy, dz, 0.05f, dxOff, dzOff, k, false, leashState);
        }
        for (k = 24; k >= 0; --k) {
            LeashFeatureRenderer.addVertexPair(builder, (Matrix4fc)pose, dx, dy, dz, 0.0f, dxOff, dzOff, k, true, leashState);
        }
    }

    private static void addVertexPair(VertexConsumer builder, Matrix4fc pose, float dx, float dy, float dz, float fudge, float dxOff, float dzOff, int k, boolean backwards, EntityRenderState.LeashState state) {
        float progress = (float)k / 24.0f;
        int block = (int)Mth.lerp(progress, state.startBlockLight, state.endBlockLight);
        int sky = (int)Mth.lerp(progress, state.startSkyLight, state.endSkyLight);
        int lightCoords = LightCoordsUtil.pack(block, sky);
        float colorModifier = k % 2 == (backwards ? 1 : 0) ? 0.7f : 1.0f;
        float r = 0.5f * colorModifier;
        float g = 0.4f * colorModifier;
        float b = 0.3f * colorModifier;
        float x = dx * progress;
        float y = state.slack ? (dy > 0.0f ? dy * progress * progress : dy - dy * (1.0f - progress) * (1.0f - progress)) : dy * progress;
        float z = dz * progress;
        builder.addVertex(pose, x - dxOff, y + fudge, z + dzOff).setColor(r, g, b, 1.0f).setLight(lightCoords);
        builder.addVertex(pose, x + dxOff, y + 0.05f - fudge, z - dzOff).setColor(r, g, b, 1.0f).setLight(lightCoords);
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(Matrix4f pose, EntityRenderState.LeashState leashState) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

