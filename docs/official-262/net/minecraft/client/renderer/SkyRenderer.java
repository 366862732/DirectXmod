/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.PrimitiveTopology;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EndFlashState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(value=EnvType.CLIENT)
public class SkyRenderer
implements AutoCloseable {
    private static final Identifier SUN_SPRITE = Identifier.withDefaultNamespace("sun");
    private static final Identifier END_FLASH_SPRITE = Identifier.withDefaultNamespace("end_flash");
    private static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
    private static final float SKY_DISC_RADIUS = 512.0f;
    private static final int SKY_VERTICES = 10;
    private static final int STAR_COUNT = 1500;
    private static final float SUN_SIZE = 30.0f;
    private static final float SUN_HEIGHT = 100.0f;
    private static final float MOON_SIZE = 20.0f;
    private static final float MOON_HEIGHT = 100.0f;
    private static final int SUNRISE_STEPS = 16;
    private static final int END_SKY_QUAD_COUNT = 6;
    private static final float END_FLASH_HEIGHT = 100.0f;
    private static final float END_FLASH_SCALE = 60.0f;
    private final TextureAtlas celestialsAtlas;
    private final RenderTarget renderTarget;
    private final GpuBuffer starBuffer;
    private final GpuBuffer topSkyBuffer;
    private final GpuBuffer bottomSkyBuffer;
    private final GpuBuffer endSkyBuffer;
    private final GpuBuffer sunBuffer;
    private final GpuBuffer moonBuffer;
    private final GpuBuffer sunriseBuffer;
    private final GpuBuffer endFlashBuffer;
    private final RenderSystem.AutoStorageIndexBuffer quadIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
    private final AbstractTexture endSkyTexture;
    private int starIndexCount;

    public SkyRenderer(TextureManager textureManager, AtlasManager atlasManager, RenderTarget renderTarget) {
        this.celestialsAtlas = atlasManager.getAtlasOrThrow(AtlasIds.CELESTIALS);
        this.renderTarget = renderTarget;
        this.starBuffer = this.buildStars();
        this.endSkyBuffer = SkyRenderer.buildEndSky();
        this.endSkyTexture = this.getTexture(textureManager, END_SKY_LOCATION);
        this.endFlashBuffer = SkyRenderer.buildEndFlashQuad(this.celestialsAtlas);
        this.sunBuffer = SkyRenderer.buildSunQuad(this.celestialsAtlas);
        this.moonBuffer = SkyRenderer.buildMoonPhases(this.celestialsAtlas);
        this.sunriseBuffer = this.buildSunriseFan();
        try (ByteBufferBuilder builder = ByteBufferBuilder.exactlySized(10 * DefaultVertexFormat.POSITION.getVertexSize());){
            BufferBuilder bufferBuilder = new BufferBuilder(builder, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            this.buildSkyDisc(bufferBuilder, 16.0f);
            try (MeshData meshData = bufferBuilder.buildOrThrow();){
                this.topSkyBuffer = RenderSystem.getDevice().createBuffer(() -> "Top sky vertex buffer", 32, meshData.vertexBuffer());
            }
            bufferBuilder = new BufferBuilder(builder, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            this.buildSkyDisc(bufferBuilder, -16.0f);
            meshData = bufferBuilder.buildOrThrow();
            try {
                this.bottomSkyBuffer = RenderSystem.getDevice().createBuffer(() -> "Bottom sky vertex buffer", 32, meshData.vertexBuffer());
            }
            finally {
                if (meshData != null) {
                    meshData.close();
                }
            }
        }
    }

    private AbstractTexture getTexture(TextureManager textureManager, Identifier location) {
        return textureManager.getTexture(location);
    }

    private GpuBuffer buildSunriseFan() {
        int vertices = 18;
        int vtxSize = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(18 * vtxSize);){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            int centerColor = ARGB.white(1.0f);
            int ringColor = ARGB.white(0.0f);
            bufferBuilder.addVertex(0.0f, 100.0f, 0.0f).setColor(centerColor);
            for (int i = 0; i <= 16; ++i) {
                float angle = (float)i * ((float)Math.PI * 2) / 16.0f;
                float sinAngle = Mth.sin(angle);
                float cosAngle = Mth.cos(angle);
                bufferBuilder.addVertex(sinAngle * 120.0f, cosAngle * 120.0f, -cosAngle * 40.0f).setColor(ringColor);
            }
            MeshData mesh = bufferBuilder.buildOrThrow();
            try {
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Sunrise/Sunset fan", 32, mesh.vertexBuffer());
                if (mesh != null) {
                    mesh.close();
                }
                return gpuBuffer;
            }
            catch (Throwable throwable) {
                if (mesh != null) {
                    try {
                        mesh.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }

    private static GpuBuffer buildSunQuad(TextureAtlas atlas) {
        return SkyRenderer.buildCelestialQuad("Sun quad", atlas.getSprite(SUN_SPRITE));
    }

    private static GpuBuffer buildEndFlashQuad(TextureAtlas atlas) {
        return SkyRenderer.buildCelestialQuad("End flash quad", atlas.getSprite(END_FLASH_SPRITE));
    }

    private static GpuBuffer buildCelestialQuad(String name, TextureAtlasSprite sprite) {
        VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(4 * format.getVertexSize());){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, format);
            bufferBuilder.addVertex(-1.0f, 0.0f, -1.0f).setUv(sprite.getU0(), sprite.getV0());
            bufferBuilder.addVertex(1.0f, 0.0f, -1.0f).setUv(sprite.getU1(), sprite.getV0());
            bufferBuilder.addVertex(1.0f, 0.0f, 1.0f).setUv(sprite.getU1(), sprite.getV1());
            bufferBuilder.addVertex(-1.0f, 0.0f, 1.0f).setUv(sprite.getU0(), sprite.getV1());
            MeshData mesh = bufferBuilder.buildOrThrow();
            try {
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> name, 32, mesh.vertexBuffer());
                if (mesh != null) {
                    mesh.close();
                }
                return gpuBuffer;
            }
            catch (Throwable throwable) {
                if (mesh != null) {
                    try {
                        mesh.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }

    private static GpuBuffer buildMoonPhases(TextureAtlas atlas) {
        MoonPhase[] phases = MoonPhase.values();
        VertexFormat format = DefaultVertexFormat.POSITION_TEX;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(phases.length * 4 * format.getVertexSize());){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, format);
            for (MoonPhase phase : phases) {
                TextureAtlasSprite sprite = atlas.getSprite(Identifier.withDefaultNamespace("moon/" + phase.getSerializedName()));
                bufferBuilder.addVertex(-1.0f, 0.0f, -1.0f).setUv(sprite.getU1(), sprite.getV1());
                bufferBuilder.addVertex(1.0f, 0.0f, -1.0f).setUv(sprite.getU0(), sprite.getV1());
                bufferBuilder.addVertex(1.0f, 0.0f, 1.0f).setUv(sprite.getU0(), sprite.getV0());
                bufferBuilder.addVertex(-1.0f, 0.0f, 1.0f).setUv(sprite.getU1(), sprite.getV0());
            }
            MeshData mesh = bufferBuilder.buildOrThrow();
            try {
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Moon phases", 32, mesh.vertexBuffer());
                if (mesh != null) {
                    mesh.close();
                }
                return gpuBuffer;
            }
            catch (Throwable throwable) {
                if (mesh != null) {
                    try {
                        mesh.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }

    private GpuBuffer buildStars() {
        RandomSource random = RandomSource.createThreadLocalInstance(10842L);
        float starDistance = 100.0f;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 1500 * 4);){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
            for (int i = 0; i < 1500; ++i) {
                float x = random.nextFloat() * 2.0f - 1.0f;
                float y = random.nextFloat() * 2.0f - 1.0f;
                float z = random.nextFloat() * 2.0f - 1.0f;
                float starSize = 0.15f + random.nextFloat() * 0.1f;
                float lengthSq = Mth.lengthSquared(x, y, z);
                if (lengthSq <= 0.010000001f || lengthSq >= 1.0f) continue;
                Vector3f starCenter = new Vector3f(x, y, z).normalize(100.0f);
                float zRot = (float)(random.nextDouble() * 3.1415927410125732 * 2.0);
                Matrix3f rotation = new Matrix3f().rotateTowards((Vector3fc)new Vector3f((Vector3fc)starCenter).negate(), (Vector3fc)new Vector3f(0.0f, 1.0f, 0.0f)).rotateZ(-zRot);
                bufferBuilder.addVertex((Vector3fc)new Vector3f(starSize, -starSize, 0.0f).mul((Matrix3fc)rotation).add((Vector3fc)starCenter));
                bufferBuilder.addVertex((Vector3fc)new Vector3f(starSize, starSize, 0.0f).mul((Matrix3fc)rotation).add((Vector3fc)starCenter));
                bufferBuilder.addVertex((Vector3fc)new Vector3f(-starSize, starSize, 0.0f).mul((Matrix3fc)rotation).add((Vector3fc)starCenter));
                bufferBuilder.addVertex((Vector3fc)new Vector3f(-starSize, -starSize, 0.0f).mul((Matrix3fc)rotation).add((Vector3fc)starCenter));
            }
            MeshData mesh = bufferBuilder.buildOrThrow();
            try {
                this.starIndexCount = mesh.drawState().indexCount();
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, mesh.vertexBuffer());
                if (mesh != null) {
                    mesh.close();
                }
                return gpuBuffer;
            }
            catch (Throwable throwable) {
                if (mesh != null) {
                    try {
                        mesh.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
        }
    }

    private void buildSkyDisc(VertexConsumer builder, float yy) {
        float x = Math.signum(yy) * 512.0f;
        builder.addVertex(0.0f, yy, 0.0f);
        for (int i = -180; i <= 180; i += 45) {
            builder.addVertex(x * Mth.cos((float)i * ((float)Math.PI / 180)), yy, 512.0f * Mth.sin((float)i * ((float)Math.PI / 180)));
        }
    }

    private static GpuBuffer buildEndSky() {
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(24 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize());){
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < 6; ++i) {
                Matrix4f pose = new Matrix4f();
                switch (i) {
                    case 1: {
                        pose.rotationX(1.5707964f);
                        break;
                    }
                    case 2: {
                        pose.rotationX(-1.5707964f);
                        break;
                    }
                    case 3: {
                        pose.rotationX((float)Math.PI);
                        break;
                    }
                    case 4: {
                        pose.rotationZ(1.5707964f);
                        break;
                    }
                    case 5: {
                        pose.rotationZ(-1.5707964f);
                    }
                }
                bufferBuilder.addVertex((Matrix4fc)pose, -100.0f, -100.0f, -100.0f).setUv(0.0f, 0.0f).setColor(-14145496);
                bufferBuilder.addVertex((Matrix4fc)pose, -100.0f, -100.0f, 100.0f).setUv(0.0f, 16.0f).setColor(-14145496);
                bufferBuilder.addVertex((Matrix4fc)pose, 100.0f, -100.0f, 100.0f).setUv(16.0f, 16.0f).setColor(-14145496);
                bufferBuilder.addVertex((Matrix4fc)pose, 100.0f, -100.0f, -100.0f).setUv(16.0f, 0.0f).setColor(-14145496);
            }
            MeshData meshData = bufferBuilder.buildOrThrow();
            try {
                GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "End sky vertex buffer", 40, meshData.vertexBuffer());
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

    public void renderSkyDisc(int skyColor) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), ARGB.vector4fFromARGB32(skyColor));
        GpuTextureView colorTexture = this.renderTarget.getColorTextureView();
        GpuTextureView depthTexture = this.renderTarget.getDepthTextureView();
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sky disc", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.SKY);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, this.topSkyBuffer.slice());
            renderPass.draw(10, 1, 0, 0);
        }
    }

    public void extractRenderState(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state) {
        state.skybox = level.dimensionType().skybox();
        if (state.skybox == DimensionType.Skybox.NONE) {
            return;
        }
        if (state.skybox == DimensionType.Skybox.END) {
            EndFlashState endFlashState = level.endFlashState();
            if (endFlashState == null) {
                return;
            }
            state.endFlashIntensity = endFlashState.getIntensity(partialTicks);
            state.endFlashXAngle = endFlashState.getXAngle();
            state.endFlashYAngle = endFlashState.getYAngle();
            return;
        }
        EnvironmentAttributeProbe attributeProbe = camera.attributeProbe();
        state.sunAngle = attributeProbe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTicks).floatValue() * ((float)Math.PI / 180);
        state.moonAngle = attributeProbe.getValue(EnvironmentAttributes.MOON_ANGLE, partialTicks).floatValue() * ((float)Math.PI / 180);
        state.starAngle = attributeProbe.getValue(EnvironmentAttributes.STAR_ANGLE, partialTicks).floatValue() * ((float)Math.PI / 180);
        state.rainBrightness = 1.0f - level.getRainLevel(partialTicks);
        state.starBrightness = attributeProbe.getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partialTicks).floatValue();
        state.sunriseAndSunsetColor = camera.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTicks);
        state.moonPhase = attributeProbe.getValue(EnvironmentAttributes.MOON_PHASE, partialTicks);
        state.skyColor = attributeProbe.getValue(EnvironmentAttributes.SKY_COLOR, partialTicks);
        state.shouldRenderDarkDisc = this.shouldRenderDarkDisc(partialTicks, level);
    }

    private boolean shouldRenderDarkDisc(float deltaPartialTick, ClientLevel level) {
        return Minecraft.getInstance().player.getEyePosition((float)deltaPartialTick).y - level.getLevelData().getHorizonHeight(level) < 0.0 && !Minecraft.getInstance().player.isUnderWater();
    }

    public void renderDarkDisc() {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.translate(0.0f, 12.0f, 0.0f);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
        GpuTextureView colorTexture = this.renderTarget.getColorTextureView();
        GpuTextureView depthTexture = this.renderTarget.getDepthTextureView();
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sky dark", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.SKY);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, this.bottomSkyBuffer.slice());
            renderPass.draw(10, 1, 0, 0);
        }
        modelViewStack.popMatrix();
    }

    public void renderSunMoonAndStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness) {
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(-90.0f));
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)Axis.XP.rotation(sunAngle));
        this.renderSun(rainBrightness, poseStack);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)Axis.XP.rotation(moonAngle));
        this.renderMoon(moonPhase, rainBrightness, poseStack);
        poseStack.popPose();
        if (starBrightness > 0.0f) {
            poseStack.pushPose();
            poseStack.mulPose((Quaternionfc)Axis.XP.rotation(starAngle));
            this.renderStars(starBrightness, poseStack);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private void renderSun(float rainBrightness, PoseStack poseStack) {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul((Matrix4fc)poseStack.last().pose());
        modelViewStack.translate(0.0f, 100.0f, 0.0f);
        modelViewStack.scale(30.0f, 1.0f, 30.0f);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), new Vector4f(1.0f, 1.0f, 1.0f, rainBrightness));
        GpuTextureView color = this.renderTarget.getColorTextureView();
        GpuTextureView depth = this.renderTarget.getDepthTextureView();
        GpuBuffer indexBuffer = this.quadIndices.getBuffer(6);
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sky sun", color, Optional.empty(), depth, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
            renderPass.setVertexBuffer(0, this.sunBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, this.quadIndices.type());
            renderPass.drawIndexed(6, 1, 0, 0, 0);
        }
        modelViewStack.popMatrix();
    }

    private void renderMoon(MoonPhase moonPhase, float rainBrightness, PoseStack poseStack) {
        int baseVertex = moonPhase.index() * 4;
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul((Matrix4fc)poseStack.last().pose());
        modelViewStack.translate(0.0f, 100.0f, 0.0f);
        modelViewStack.scale(20.0f, 1.0f, 20.0f);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), new Vector4f(1.0f, 1.0f, 1.0f, rainBrightness));
        GpuTextureView color = this.renderTarget.getColorTextureView();
        GpuTextureView depth = this.renderTarget.getDepthTextureView();
        GpuBuffer indexBuffer = this.quadIndices.getBuffer(6);
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sky moon", color, Optional.empty(), depth, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
            renderPass.setVertexBuffer(0, this.moonBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, this.quadIndices.type());
            renderPass.drawIndexed(6, 1, 0, baseVertex, 0);
        }
        modelViewStack.popMatrix();
    }

    private void renderStars(float starBrightness, PoseStack poseStack) {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul((Matrix4fc)poseStack.last().pose());
        RenderPipeline renderPipeline = RenderPipelines.STARS;
        GpuTextureView colorTexture = this.renderTarget.getColorTextureView();
        GpuTextureView depthTexture = this.renderTarget.getDepthTextureView();
        GpuBuffer indexBuffer = this.quadIndices.getBuffer(this.starIndexCount);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), new Vector4f(starBrightness, starBrightness, starBrightness, starBrightness));
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Stars", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(renderPipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, this.starBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, this.quadIndices.type());
            renderPass.drawIndexed(this.starIndexCount, 1, 0, 0, 0);
        }
        modelViewStack.popMatrix();
    }

    public void renderSunriseAndSunset(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor) {
        float alpha = ARGB.alphaFloat(sunriseAndSunsetColor);
        if (alpha <= 0.001f) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(90.0f));
        float angle = Mth.sin(sunAngle) < 0.0f ? 180.0f : 0.0f;
        poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(angle + 90.0f));
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul((Matrix4fc)poseStack.last().pose());
        modelViewStack.scale(1.0f, 1.0f, alpha);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), ARGB.vector4fFromARGB32(sunriseAndSunsetColor));
        GpuTextureView color = this.renderTarget.getColorTextureView();
        GpuTextureView depth = this.renderTarget.getDepthTextureView();
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sunrise sunset", color, Optional.empty(), depth, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.SUNRISE_SUNSET);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, this.sunriseBuffer.slice());
            renderPass.draw(18, 1, 0, 0);
        }
        modelViewStack.popMatrix();
        poseStack.popPose();
    }

    public void renderEndSky() {
        RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = autoIndices.getBuffer(36);
        GpuTextureView colorTexture = this.renderTarget.getColorTextureView();
        GpuTextureView depthTexture = this.renderTarget.getDepthTextureView();
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy());
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "End sky", colorTexture, Optional.empty(), depthTexture, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.END_SKY);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.bindTexture("Sampler0", this.endSkyTexture.getTextureView(), this.endSkyTexture.getSampler());
            renderPass.setVertexBuffer(0, this.endSkyBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, autoIndices.type());
            renderPass.drawIndexed(36, 1, 0, 0, 0);
        }
    }

    public void renderEndFlash(PoseStack poseStack, float intensity, float xAngle, float yAngle) {
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f - yAngle));
        poseStack.mulPose((Quaternionfc)Axis.XP.rotationDegrees(-90.0f - xAngle));
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul((Matrix4fc)poseStack.last().pose());
        modelViewStack.translate(0.0f, 100.0f, 0.0f);
        modelViewStack.scale(60.0f, 1.0f, 60.0f);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f((Matrix4fc)modelViewStack), new Vector4f(intensity, intensity, intensity, intensity));
        GpuTextureView color = this.renderTarget.getColorTextureView();
        GpuTextureView depth = this.renderTarget.getDepthTextureView();
        GpuBuffer indexBuffer = this.quadIndices.getBuffer(6);
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "End flash", color, Optional.empty(), depth, OptionalDouble.empty());){
            renderPass.setPipeline(RenderPipelines.CELESTIAL);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
            renderPass.setVertexBuffer(0, this.endFlashBuffer.slice());
            renderPass.setIndexBuffer(indexBuffer, this.quadIndices.type());
            renderPass.drawIndexed(6, 1, 0, 0, 0);
        }
        modelViewStack.popMatrix();
    }

    @Override
    public void close() {
        this.sunBuffer.close();
        this.moonBuffer.close();
        this.starBuffer.close();
        this.topSkyBuffer.close();
        this.bottomSkyBuffer.close();
        this.endSkyBuffer.close();
        this.sunriseBuffer.close();
        this.endFlashBuffer.close();
    }
}

