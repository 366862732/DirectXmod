/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class MovingBlockFeatureRenderer
extends RenderTypeFeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Moving Block");
    private final PoseStack poseStack = new PoseStack();

    @Override
    protected void buildGroup(FeatureFrameContext context, List<Submit> submits) {
        boolean ambientOcclusion = context.options().ambientOcclusion;
        boolean cutoutLeaves = context.options().cutoutLeaves;
        ModelBlockRenderer blockRenderer = new ModelBlockRenderer(ambientOcclusion, false, context.blockColors());
        for (Submit submit : submits) {
            MovingBlockRenderState movingBlockRenderState = submit.movingBlockRenderState();
            BlockState blockState = movingBlockRenderState.blockState;
            BlockStateModel model = context.blockStateModelSet().get(blockState);
            this.poseStack.setIdentity();
            this.poseStack.mulPose(submit.pose());
            BlockQuadOutput quadOutput = (x, y, z, quad, instance) -> this.putBakedQuad(this.poseStack, x, y, z, quad, instance, quad.materialInfo().layer(), submit.outlineColor());
            BlockQuadOutput solidQuadOutput = (x, y, z, quad, instance) -> this.putBakedQuad(this.poseStack, x, y, z, quad, instance, ChunkSectionLayer.SOLID, submit.outlineColor());
            BlockQuadOutput blockOutput = ModelBlockRenderer.forceOpaque(cutoutLeaves, blockState) ? solidQuadOutput : quadOutput;
            long blockSeed = blockState.getSeed(movingBlockRenderState.randomSeedPos);
            blockRenderer.tesselateBlock(blockOutput, 0.0f, 0.0f, 0.0f, movingBlockRenderState, movingBlockRenderState.blockPos, blockState, model, blockSeed);
        }
    }

    private void putBakedQuad(PoseStack poseStack, float x, float y, float z, BakedQuad quad, QuadInstance instance, ChunkSectionLayer layer, int outlineColor) {
        VertexConsumer buffer;
        RenderType renderType;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        switch (layer) {
            default: {
                throw new MatchException(null, null);
            }
            case SOLID: {
                RenderType renderType2 = RenderTypes.solidMovingBlock();
                break;
            }
            case CUTOUT: {
                RenderType renderType2 = RenderTypes.cutoutMovingBlock();
                break;
            }
            case TRANSLUCENT: {
                RenderType renderType2 = renderType = RenderTypes.translucentMovingBlock();
            }
        }
        if (outlineColor != 0 && renderType.outline().isPresent()) {
            instance.setColor(outlineColor);
            buffer = this.getVertexBuilder(renderType.outline().get());
        } else {
            buffer = this.getVertexBuilder(renderType);
        }
        buffer.putBakedQuad(poseStack.last(), quad, instance);
        poseStack.popPose();
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(Matrix4fc pose, MovingBlockRenderState movingBlockRenderState, int outlineColor) implements TranslucentSubmit
    {
        @Override
        public float distanceToCameraSq() {
            return TranslucentSubmit.computeDistanceToCameraSq(this.pose, 0.5f, 0.5f, 0.5f);
        }

        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }
}

