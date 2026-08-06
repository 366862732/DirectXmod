/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryStack
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Projection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

@Environment(value=EnvType.CLIENT)
public class ProjectionMatrixBuffer
implements AutoCloseable {
    private final GpuBuffer buffer;
    private final GpuBufferSlice bufferSlice;
    private @Nullable Projection lastUploadedProjection = null;
    private long projectionMatrixVersion = -1L;
    private final Matrix4f tempMatrix = new Matrix4f();

    public ProjectionMatrixBuffer(String name) {
        GpuDevice device = RenderSystem.getDevice();
        this.buffer = device.createBuffer(() -> "Camera projection matrix UBO " + name, 136, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        this.bufferSlice = this.buffer.slice(0L, RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
    }

    public GpuBufferSlice getBuffer(Projection projection) {
        assert (projection.getMatrixVersion() != -1L);
        if (this.lastUploadedProjection != projection || projection.getMatrixVersion() != this.projectionMatrixVersion) {
            this.lastUploadedProjection = projection;
            this.projectionMatrixVersion = projection.getMatrixVersion();
            return this.writeBuffer(projection.getMatrix(this.tempMatrix));
        }
        return this.bufferSlice;
    }

    public GpuBufferSlice getBuffer(Matrix4f projectionMatrix) {
        this.lastUploadedProjection = null;
        this.projectionMatrixVersion = -1L;
        return this.writeBuffer(projectionMatrix);
    }

    private GpuBufferSlice writeBuffer(Matrix4f projectionMatrix) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = Std140Builder.onStack(stack, RenderSystem.PROJECTION_MATRIX_UBO_SIZE).putMat4f((Matrix4fc)projectionMatrix).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), byteBuffer);
        }
        return this.bufferSlice;
    }

    @Override
    public void close() {
        this.buffer.close();
    }
}

