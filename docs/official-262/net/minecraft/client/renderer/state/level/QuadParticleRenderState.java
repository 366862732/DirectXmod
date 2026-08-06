/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package net.minecraft.client.renderer.state.level;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class QuadParticleRenderState
implements ParticleGroupRenderState {
    private static final int INITIAL_PARTICLE_CAPACITY = 1024;
    private static final int FLOATS_PER_PARTICLE = 12;
    private static final int INTS_PER_PARTICLE = 2;
    private final Map<SingleQuadParticle.Layer, Storage> particles = new HashMap<SingleQuadParticle.Layer, Storage>();
    private int particleCount;

    public void add(SingleQuadParticle.Layer layer, float x, float y, float z, float xRot, float yRot, float zRot, float wRot, float scale, float u0, float u1, float v0, float v1, int color, int lightCoords) {
        this.particles.computeIfAbsent(layer, ignored -> new Storage()).add(x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords);
        ++this.particleCount;
    }

    @Override
    public void clear() {
        this.particles.values().forEach(Storage::clear);
        this.particleCount = 0;
    }

    public boolean isEmpty() {
        return this.particleCount == 0;
    }

    public void buildLayer(SingleQuadParticle.Layer layer, VertexConsumer bufferBuilder) {
        Storage storage = this.particles.get(layer);
        if (storage == null) {
            return;
        }
        storage.forEachParticle((x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords) -> this.renderRotatedQuad(bufferBuilder, x, y, z, xRot, yRot, zRot, wRot, scale, u0, u1, v0, v1, color, lightCoords));
    }

    public Set<SingleQuadParticle.Layer> layers() {
        return this.particles.keySet();
    }

    protected void renderRotatedQuad(VertexConsumer builder, float x, float y, float z, float xRot, float yRot, float zRot, float wRot, float scale, float u0, float u1, float v0, float v1, int color, int lightCoords) {
        Quaternionf rotation = new Quaternionf(xRot, yRot, zRot, wRot);
        this.renderVertex(builder, rotation, x, y, z, 1.0f, -1.0f, scale, u1, v1, color, lightCoords);
        this.renderVertex(builder, rotation, x, y, z, 1.0f, 1.0f, scale, u1, v0, color, lightCoords);
        this.renderVertex(builder, rotation, x, y, z, -1.0f, 1.0f, scale, u0, v0, color, lightCoords);
        this.renderVertex(builder, rotation, x, y, z, -1.0f, -1.0f, scale, u0, v1, color, lightCoords);
    }

    private void renderVertex(VertexConsumer builder, Quaternionf rotation, float x, float y, float z, float nx, float ny, float scale, float u, float v, int color, int lightCoords) {
        Vector3f scratch = new Vector3f(nx, ny, 0.0f).rotate((Quaternionfc)rotation).mul(scale).add(x, y, z);
        builder.addVertex(scratch.x(), scratch.y(), scratch.z()).setUv(u, v).setColor(color).setLight(lightCoords);
    }

    @Override
    public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (this.particleCount > 0) {
            submitNodeCollector.submitQuadParticleGroup(this);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Storage {
        private int capacity = 1024;
        private float[] floatValues = new float[12288];
        private int[] intValues = new int[2048];
        private int currentParticleIndex;

        private Storage() {
        }

        public void add(float x, float y, float z, float xRot, float yRot, float zRot, float wRot, float scale, float u0, float u1, float v0, float v1, int color, int lightCoords) {
            if (this.currentParticleIndex >= this.capacity) {
                this.grow();
            }
            int index = this.currentParticleIndex * 12;
            this.floatValues[index++] = x;
            this.floatValues[index++] = y;
            this.floatValues[index++] = z;
            this.floatValues[index++] = xRot;
            this.floatValues[index++] = yRot;
            this.floatValues[index++] = zRot;
            this.floatValues[index++] = wRot;
            this.floatValues[index++] = scale;
            this.floatValues[index++] = u0;
            this.floatValues[index++] = u1;
            this.floatValues[index++] = v0;
            this.floatValues[index] = v1;
            index = this.currentParticleIndex * 2;
            this.intValues[index++] = color;
            this.intValues[index] = lightCoords;
            ++this.currentParticleIndex;
        }

        public void forEachParticle(ParticleConsumer consumer) {
            for (int particleIndex = 0; particleIndex < this.currentParticleIndex; ++particleIndex) {
                int floatIndex = particleIndex * 12;
                int intIndex = particleIndex * 2;
                consumer.consume(this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex++], this.floatValues[floatIndex], this.intValues[intIndex++], this.intValues[intIndex]);
            }
        }

        public void clear() {
            this.currentParticleIndex = 0;
        }

        private void grow() {
            this.capacity *= 2;
            this.floatValues = Arrays.copyOf(this.floatValues, this.capacity * 12);
            this.intValues = Arrays.copyOf(this.intValues, this.capacity * 2);
        }

        public int count() {
            return this.currentParticleIndex;
        }
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    public static interface ParticleConsumer {
        public void consume(float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, float var10, float var11, float var12, int var13, int var14);
    }
}

