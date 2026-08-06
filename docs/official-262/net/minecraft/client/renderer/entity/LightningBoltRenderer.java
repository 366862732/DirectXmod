/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class LightningBoltRenderer
extends EntityRenderer<LightningBolt, LightningBoltRenderState> {
    public LightningBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(LightningBoltRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        float[] xOffs = new float[8];
        float[] zOffs = new float[8];
        float xOff = 0.0f;
        float zOff = 0.0f;
        RandomSource random = RandomSource.createThreadLocalInstance(state.seed);
        for (int h = 7; h >= 0; --h) {
            xOffs[h] = xOff;
            zOffs[h] = zOff;
            xOff += (float)(random.nextInt(11) - 5);
            zOff += (float)(random.nextInt(11) - 5);
        }
        float finalXOff = xOff;
        float finalZOff = zOff;
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            Matrix4f poseMatrix = pose.pose();
            for (int r = 0; r < 4; ++r) {
                RandomSource random = RandomSource.createThreadLocalInstance(state.seed);
                for (int p = 0; p < 3; ++p) {
                    int hs = 7;
                    int ht = 0;
                    if (p > 0) {
                        hs = 7 - p;
                    }
                    if (p > 0) {
                        ht = hs - 2;
                    }
                    float xo0 = xOffs[hs] - finalXOff;
                    float zo0 = zOffs[hs] - finalZOff;
                    for (int h = hs; h >= ht; --h) {
                        float xo1 = xo0;
                        float zo1 = zo0;
                        if (p == 0) {
                            xo0 += (float)(random.nextInt(11) - 5);
                            zo0 += (float)(random.nextInt(11) - 5);
                        } else {
                            xo0 += (float)(random.nextInt(31) - 15);
                            zo0 += (float)(random.nextInt(31) - 15);
                        }
                        float br = 0.5f;
                        float boltRed = 0.45f;
                        float boltGreen = 0.45f;
                        float boltBlue = 0.5f;
                        float rr1 = 0.1f + (float)r * 0.2f;
                        if (p == 0) {
                            rr1 *= (float)h * 0.1f + 1.0f;
                        }
                        float rr2 = 0.1f + (float)r * 0.2f;
                        if (p == 0) {
                            rr2 *= ((float)h - 1.0f) * 0.1f + 1.0f;
                        }
                        LightningBoltRenderer.quad((Matrix4fc)poseMatrix, buffer, xo0, zo0, h, xo1, zo1, 0.45f, 0.45f, 0.5f, rr1, rr2, false, false, true, false);
                        LightningBoltRenderer.quad((Matrix4fc)poseMatrix, buffer, xo0, zo0, h, xo1, zo1, 0.45f, 0.45f, 0.5f, rr1, rr2, true, false, true, true);
                        LightningBoltRenderer.quad((Matrix4fc)poseMatrix, buffer, xo0, zo0, h, xo1, zo1, 0.45f, 0.45f, 0.5f, rr1, rr2, true, true, false, true);
                        LightningBoltRenderer.quad((Matrix4fc)poseMatrix, buffer, xo0, zo0, h, xo1, zo1, 0.45f, 0.45f, 0.5f, rr1, rr2, false, true, false, false);
                    }
                }
            }
        });
    }

    private static void quad(Matrix4fc pose, VertexConsumer buffer, float xo0, float zo0, int h, float xo1, float zo1, float boltRed, float boltGreen, float boltBlue, float rr1, float rr2, boolean px1, boolean pz1, boolean px2, boolean pz2) {
        buffer.addVertex(pose, xo0 + (px1 ? rr2 : -rr2), (float)(h * 16), zo0 + (pz1 ? rr2 : -rr2)).setColor(boltRed, boltGreen, boltBlue, 0.3f);
        buffer.addVertex(pose, xo1 + (px1 ? rr1 : -rr1), (float)((h + 1) * 16), zo1 + (pz1 ? rr1 : -rr1)).setColor(boltRed, boltGreen, boltBlue, 0.3f);
        buffer.addVertex(pose, xo1 + (px2 ? rr1 : -rr1), (float)((h + 1) * 16), zo1 + (pz2 ? rr1 : -rr1)).setColor(boltRed, boltGreen, boltBlue, 0.3f);
        buffer.addVertex(pose, xo0 + (px2 ? rr2 : -rr2), (float)(h * 16), zo0 + (pz2 ? rr2 : -rr2)).setColor(boltRed, boltGreen, boltBlue, 0.3f);
    }

    @Override
    public LightningBoltRenderState createRenderState() {
        return new LightningBoltRenderState();
    }

    @Override
    public void extractRenderState(LightningBolt entity, LightningBoltRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.seed = entity.seed;
    }

    @Override
    protected boolean affectedByCulling(LightningBolt entity) {
        return false;
    }
}

