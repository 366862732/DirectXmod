/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import java.nio.ByteBuffer;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public abstract class StagingBuffer
implements AutoCloseable {
    private int nextWriteOffset;
    private int usedBufferCount;

    public static StagingBuffer create(String name, GpuDevice gpuDevice, int bufferSize) {
        if (gpuDevice.getDeviceInfo().hintsAndWorkarounds().writeToBufferIsSlow() && gpuDevice.getDeviceInfo().features().persistentMapping()) {
            return new PersistentlyMapped(name, bufferSize);
        }
        return new Cpu(bufferSize);
    }

    public @Nullable BufferHandle tryAppend(ByteBuffer buffer) {
        ByteBuffer writeBuffer;
        int writeOffset = this.nextWriteOffset;
        int bufferSize = buffer.remaining();
        if (bufferSize > (writeBuffer = this.getWriteBuffer()).capacity()) {
            throw new IllegalArgumentException("Cannot fit allocation of size " + bufferSize + " into staging buffer of size " + writeBuffer.capacity());
        }
        if (bufferSize > writeBuffer.capacity() - writeOffset) {
            return null;
        }
        MemoryUtil.memCopy((ByteBuffer)buffer, (ByteBuffer)writeBuffer.position(writeOffset));
        this.nextWriteOffset += bufferSize;
        ++this.usedBufferCount;
        return new BufferHandle(this, writeOffset, bufferSize);
    }

    protected abstract ByteBuffer getWriteBuffer();

    protected abstract void copyTo(CommandEncoder var1, GpuBuffer var2, long var3, long var5, long var7);

    protected void rotateBuffer() {
    }

    public Uploader startUploading(CommandEncoder encoder) {
        return new Uploader(this, encoder);
    }

    private void tryClearAndRotate() {
        if (this.nextWriteOffset > 0 && this.usedBufferCount == 0) {
            this.rotateBuffer();
            this.nextWriteOffset = 0;
        }
    }

    @Override
    public abstract void close();

    @Environment(value=EnvType.CLIENT)
    private static class PersistentlyMapped
    extends StagingBuffer {
        private final MappableRingBuffer mappableRingBuffer;
        private GpuBufferSlice.MappedView currentMappedView;
        private GpuBuffer currentGPUBuffer;
        private ByteBuffer currentBuffer;

        private PersistentlyMapped(String name, int bufferSize) {
            this.mappableRingBuffer = new MappableRingBuffer(() -> name + " staging buffer", 18, bufferSize / 2);
            this.currentGPUBuffer = this.mappableRingBuffer.currentBuffer();
            this.currentMappedView = this.currentGPUBuffer.map(false, true);
            this.currentBuffer = this.currentMappedView.data();
        }

        @Override
        protected ByteBuffer getWriteBuffer() {
            return this.currentBuffer;
        }

        @Override
        protected void copyTo(CommandEncoder encoder, GpuBuffer dstBuffer, long dstOffset, long stagingBufferOffset, long copySize) {
            encoder.copyToBuffer(this.currentGPUBuffer.slice(stagingBufferOffset, copySize), dstBuffer.slice(dstOffset, copySize));
        }

        @Override
        protected void rotateBuffer() {
            this.currentMappedView.close();
            this.mappableRingBuffer.rotate();
            this.currentGPUBuffer = this.mappableRingBuffer.currentBuffer();
            this.currentMappedView = this.currentGPUBuffer.map(false, true);
            this.currentBuffer = this.currentMappedView.data();
        }

        @Override
        public void close() {
            this.currentMappedView.close();
            this.mappableRingBuffer.close();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Cpu
    extends StagingBuffer {
        private final ByteBuffer stagingBuffer;

        private Cpu(int bufferSize) {
            this.stagingBuffer = MemoryUtil.memAlloc((int)bufferSize);
        }

        @Override
        protected ByteBuffer getWriteBuffer() {
            return this.stagingBuffer;
        }

        @Override
        protected void copyTo(CommandEncoder encoder, GpuBuffer dstBuffer, long dstOffset, long stagingBufferOffset, long copySize) {
            encoder.writeToBuffer(dstBuffer.slice(dstOffset, copySize), this.stagingBuffer.slice((int)stagingBufferOffset, (int)copySize));
        }

        @Override
        public void close() {
            MemoryUtil.memFree((ByteBuffer)this.stagingBuffer);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class BufferHandle
    implements AutoCloseable {
        private final int offset;
        private final int size;
        private boolean closed;
        final /* synthetic */ StagingBuffer this$0;

        public BufferHandle(StagingBuffer this$0, int offset, int size) {
            StagingBuffer stagingBuffer = this$0;
            Objects.requireNonNull(stagingBuffer);
            this.this$0 = stagingBuffer;
            this.offset = offset;
            this.size = size;
        }

        private void checkValidFor(StagingBuffer stagingBuffer) {
            if (this.closed) {
                throw new IllegalStateException("Buffer has already been closed");
            }
            if (stagingBuffer != this.this$0) {
                throw new IllegalArgumentException("Buffer is not valid for " + String.valueOf(stagingBuffer));
            }
        }

        public long size() {
            return this.size;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            --this.this$0.usedBufferCount;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class Uploader
    implements AutoCloseable {
        private final CommandEncoder encoder;
        final /* synthetic */ StagingBuffer this$0;

        public Uploader(StagingBuffer this$0, CommandEncoder encoder) {
            StagingBuffer stagingBuffer = this$0;
            Objects.requireNonNull(stagingBuffer);
            this.this$0 = stagingBuffer;
            this.encoder = encoder;
        }

        public void copyTo(BufferHandle srcBuffer, GpuBuffer dstBuffer, long dstOffset) {
            srcBuffer.checkValidFor(this.this$0);
            this.this$0.copyTo(this.encoder, dstBuffer, dstOffset, srcBuffer.offset, srcBuffer.size);
        }

        @Override
        public void close() {
            this.this$0.tryClearAndRotate();
        }

        public void checkValidFor(StagingBuffer stagingBuffer) {
            if (stagingBuffer != this.this$0) {
                throw new IllegalArgumentException("Uploader is not valid for " + String.valueOf(stagingBuffer));
            }
        }
    }
}

