/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record GpuBufferSlice(GpuBuffer buffer, long offset, long length) {
    public GpuBufferSlice slice(long offset, long length) {
        if (offset < 0L || length < 0L || offset + length > this.length) {
            throw new IllegalArgumentException("Offset of " + offset + " and length " + length + " would put new slice outside existing slice's range (of " + this.offset + "," + this.length + ")");
        }
        return new GpuBufferSlice(this.buffer, this.offset + offset, length);
    }

    public MappedView map(boolean read, boolean write) {
        return this.buffer.map(this.offset, this.length, read, write);
    }

    @Environment(value=EnvType.CLIENT)
    public record MappedView(GpuBufferSlice slice, ByteBuffer data, Runnable onClose) implements AutoCloseable
    {
        @Override
        public void close() {
            this.onClose.run();
        }
    }
}

