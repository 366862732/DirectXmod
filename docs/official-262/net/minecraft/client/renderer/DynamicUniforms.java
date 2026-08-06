/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public class DynamicUniforms
implements AutoCloseable {
    private static final Vector4fc WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3fc NO_OFFSET = new Vector3f();
    private static final Matrix4fc IDENTITY_TEXTURE_TRANSFORM = new Matrix4f();
    public static final int TRANSFORM_UBO_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec3().putMat4f().get();
    public static final int CHUNK_SECTION_UBO_SIZE = new Std140SizeCalculator().putMat4f().putFloat().putIVec2().putIVec3().get();
    private static final int INITIAL_CAPACITY = 2;
    private final DynamicUniformStorage<Transform> transforms = new DynamicUniformStorage("Dynamic Transforms UBO", TRANSFORM_UBO_SIZE, 2);
    private final DynamicUniformStorage<ChunkSectionInfo> chunkSections = new DynamicUniformStorage("Chunk Sections UBO", CHUNK_SECTION_UBO_SIZE, 2);

    public void reset() {
        this.transforms.endFrame();
        this.chunkSections.endFrame();
    }

    @Override
    public void close() {
        this.transforms.close();
        this.chunkSections.close();
    }

    public GpuBufferSlice writeTransform(Matrix4f modelView) {
        return this.writeTransform(new Transform((Matrix4fc)modelView, WHITE, NO_OFFSET, IDENTITY_TEXTURE_TRANSFORM));
    }

    public GpuBufferSlice writeTransform(Matrix4f modelView, Vector4f colorModulator) {
        return this.writeTransform(new Transform((Matrix4fc)modelView, (Vector4fc)colorModulator, NO_OFFSET, IDENTITY_TEXTURE_TRANSFORM));
    }

    public GpuBufferSlice writeTransform(Matrix4f modelView, Matrix4f textureMatrix) {
        return this.writeTransform(new Transform((Matrix4fc)modelView, WHITE, NO_OFFSET, (Matrix4fc)textureMatrix));
    }

    public GpuBufferSlice writeTransform(Matrix4f modelView, Vector4f colorModulator, Vector3f modelOffset, Matrix4f textureMatrix) {
        return this.writeTransform(new Transform((Matrix4fc)modelView, (Vector4fc)colorModulator, (Vector3fc)modelOffset, (Matrix4fc)textureMatrix));
    }

    public GpuBufferSlice writeTransform(Transform uniform) {
        return this.transforms.writeUniform(uniform);
    }

    public GpuBufferSlice[] writeTransforms(Transform ... transforms) {
        return this.transforms.writeUniforms(transforms);
    }

    public GpuBufferSlice[] writeChunkSections(ChunkSectionInfo ... infos) {
        return this.chunkSections.writeUniforms(infos);
    }

    @Environment(value=EnvType.CLIENT)
    public record Transform(Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix) implements DynamicUniformStorage.DynamicUniform
    {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putMat4f(this.modelView).putVec4(this.colorModulator).putVec3(this.modelOffset).putMat4f(this.textureMatrix);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record ChunkSectionInfo(Matrix4fc modelView, int x, int y, int z, float visibility, int textureAtlasWidth, int textureAtlasHeight) implements DynamicUniformStorage.DynamicUniform
    {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putMat4f(this.modelView).putFloat(this.visibility).putIVec2(this.textureAtlasWidth, this.textureAtlasHeight).putIVec3(this.x, this.y, this.z);
        }
    }
}

