/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.WindowRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;

@Environment(value=EnvType.CLIENT)
public class CubeMap
implements AutoCloseable {
    private static final int SIDES = 6;
    private static final float PROJECTION_Z_NEAR = 0.05f;
    private static final float PROJECTION_Z_FAR = 10.0f;
    private static final float PROJECTION_FOV = 85.0f;
    private final GpuBuffer vertexBuffer;
    private final Projection projection;
    private final ProjectionMatrixBuffer projectionMatrixUbo;
    private final Identifier location;

    public CubeMap(Identifier base) {
        this.location = base;
        this.projection = new Projection();
        this.projectionMatrixUbo = new ProjectionMatrixBuffer("cubemap");
        this.vertexBuffer = CubeMap.initializeVertices();
    }

    public void render(float rotXInDegrees, float rotYInDegrees) {
        Minecraft minecraft = Minecraft.getInstance();
        WindowRenderState windowState = minecraft.gameRenderer.gameRenderState().windowRenderState;
        this.projection.setupPerspective(0.05f, 10.0f, 85.0f, windowState.width, windowState.height);
        RenderSystem.setProjectionMatrix(this.projectionMatrixUbo.getBuffer(this.projection), ProjectionType.PERSPECTIVE);
        RenderPipeline renderPipeline = RenderPipelines.PANORAMA;
        RenderTarget mainRenderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainRenderTarget.getColorTextureView();
        GpuTextureView depthTexture = mainRenderTarget.getDepthTextureView();
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(36);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.rotationX((float)Math.PI);
        modelViewStack.rotateX(rotXInDegrees * ((float)Math.PI / 180));
        modelViewStack.rotateY(rotYInDegrees * ((float)Math.PI / 180));
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack));
        modelViewStack.popMatrix();
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Cubemap", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setVertexBuffer(0, this.vertexBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, indices.type());
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            AbstractTexture texture = minecraft.getTextureManager().getTexture(this.location);
            renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            renderPass.drawIndexed(36, 1, 0, 0, 0);
        }
    }

    private static GpuBuffer initializeVertices() {
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4 * 6);){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
            bufferBuilder.addVertex(-1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, 1.0f);
            bufferBuilder.addVertex(-1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(-1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, 1.0f);
            bufferBuilder.addVertex(1.0f, -1.0f, -1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, 1.0f);
            bufferBuilder.addVertex(-1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, -1.0f);
            bufferBuilder.addVertex(1.0f, 1.0f, 1.0f);
            MeshData meshData = bufferBuilder.buildOrThrow();
            try {
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Cube map vertex buffer", 32, meshData.vertexBuffer());
                if (meshData != null) {
                    meshData.close();
                }
                return gpuBuffer;
            }
            catch (Throwable throwable) {
                if (meshData != null) {
                    try {
                        meshData.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }

    public void registerTextures(TextureManager textureManager) {
        textureManager.register(this.location, new CubeMapTexture(this.location));
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        this.projectionMatrixUbo.close();
    }
}

