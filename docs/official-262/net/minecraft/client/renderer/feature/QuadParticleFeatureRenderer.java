/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderer;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class QuadParticleFeatureRenderer
implements FeatureRenderer<Submit> {
    public static final FeatureRendererType<Submit> TYPE = FeatureRendererType.create("Particle");
    private final List<PreparedGroup> groups = new ArrayList<PreparedGroup>();
    private @Nullable GpuBufferSlice dynamicTransforms;

    @Override
    public void prepareGroup(FeatureFrameContext context, List<Submit> submits, boolean strictlyOrdered) {
        if (submits.isEmpty()) {
            return;
        }
        StagedVertexBuffer stagedVertexBuffer = context.stagedVertexBuffer();
        IdentityHashMap<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> drawByLayer = new IdentityHashMap<SingleQuadParticle.Layer, StagedVertexBuffer.Draw>();
        for (Submit submit : submits) {
            QuadParticleRenderState particles = submit.particles();
            if (particles.isEmpty()) continue;
            for (SingleQuadParticle.Layer layer2 : particles.layers()) {
                if (layer2.translucent() != submit.translucent()) continue;
                StagedVertexBuffer.Draw draw = drawByLayer.computeIfAbsent(layer2, layer -> stagedVertexBuffer.appendDraw(DefaultVertexFormat.PARTICLE, PrimitiveTopology.QUADS, null));
                particles.buildLayer(layer2, stagedVertexBuffer.getVertexBuilder(draw));
            }
        }
        boolean translucent = submits.getFirst().translucent();
        this.groups.add(new PreparedGroup(drawByLayer, translucent));
    }

    @Override
    public void finishPrepare(FeatureFrameContext context) {
        this.dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
    }

    @Override
    public void executeGroup(FeatureFrameContext context, int groupIndex, List<Submit> submits, boolean strictlyOrdered) {
        PreparedGroup group = this.groups.get(groupIndex);
        GpuDevice device = RenderSystem.getDevice();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        RenderTarget particleTarget = minecraft.levelRenderer.particlesTarget();
        boolean useParticleTarget = particleTarget != null && group.translucent;
        GpuTextureView colorTextureView = useParticleTarget ? particleTarget.getColorTextureView() : mainTarget.getColorTextureView();
        GpuTextureView depthTextureView = useParticleTarget ? particleTarget.getDepthTextureView() : mainTarget.getDepthTextureView();
        try (RenderPass renderPass = device.createCommandEncoder().createRenderPass(() -> "Particles - " + (group.translucent ? "Translucent" : "Solid"), colorTextureView, Optional.empty(), depthTextureView, OptionalDouble.empty());){
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", Objects.requireNonNull(this.dynamicTransforms));
            renderPass.bindTexture("Sampler2", context.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            QuadParticleFeatureRenderer.drawLayers(context.stagedVertexBuffer(), group.layers, renderPass, context.textureManager());
        }
    }

    private static void drawLayers(StagedVertexBuffer stagedBuffer, Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> layers, RenderPass renderPass, TextureManager textureManager) {
        for (Map.Entry<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> entry : layers.entrySet()) {
            StagedVertexBuffer.ExecuteInfo executeInfo = stagedBuffer.getExecuteInfo(entry.getValue());
            if (executeInfo == null) continue;
            renderPass.setPipeline(entry.getKey().pipeline());
            renderPass.setVertexBuffer(0, executeInfo.vertexBuffer().slice());
            renderPass.setIndexBuffer(executeInfo.indexBuffer(), executeInfo.indexType());
            AbstractTexture texture = textureManager.getTexture(entry.getKey().textureAtlasLocation());
            renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            renderPass.drawIndexed(executeInfo.indexCount(), 1, executeInfo.firstIndex(), executeInfo.baseVertex(), 0);
        }
    }

    @Override
    public void finishExecute(FeatureFrameContext context) {
        this.groups.clear();
        this.dynamicTransforms = null;
    }

    @Environment(value=EnvType.CLIENT)
    public record Submit(QuadParticleRenderState particles, boolean translucent) implements SubmitNode
    {
        public FeatureRendererType<Submit> featureType() {
            return TYPE;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record PreparedGroup(Map<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> layers, boolean translucent) {
    }
}

