/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public abstract class GpuBuffer
implements AutoCloseable {
    public static final int USAGE_MAP_READ = 1;
    public static final int USAGE_MAP_WRITE = 2;
    public static final int USAGE_HINT_CLIENT_STORAGE = 4;
    public static final int USAGE_COPY_DST = 8;
    public static final int USAGE_COPY_SRC = 16;
    public static final int USAGE_VERTEX = 32;
    public static final int USAGE_INDEX = 64;
    public static final int USAGE_UNIFORM = 128;
    public static final int USAGE_UNIFORM_TEXEL_BUFFER = 256;
    public static final int USAGE_INDIRECT_PARAMETERS = 512;
    private final @Usage int usage;
    private final long size;
    private final GpuBufferSlice defaultSlice;

    public GpuBuffer(@Usage int usage, long size) {
        this.size = size;
        this.usage = usage;
        this.defaultSlice = new GpuBufferSlice(this, 0L, size);
    }

    public long size() {
        return this.size;
    }

    public @Usage int usage() {
        return this.usage;
    }

    public abstract boolean isClosed();

    @Override
    public abstract void close();

    public GpuBufferSlice slice(long offset, long length) {
        if (offset < 0L || length < 0L || offset + length > this.size) {
            throw new IllegalArgumentException("Offset of " + offset + " and length " + length + " would put new slice outside buffer's range (of 0," + length + ")");
        }
        return new GpuBufferSlice(this, offset, length);
    }

    public GpuBufferSlice slice() {
        return this.defaultSlice;
    }

    public GpuBufferSlice.MappedView map(boolean read, boolean write) {
        return this.map(0L, this.size, read, write);
    }

    public abstract GpuBufferSlice.MappedView map(long var1, long var3, boolean var5, boolean var6);

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    @Environment(value=EnvType.CLIENT)
    public static @interface Usage {
    }
}

