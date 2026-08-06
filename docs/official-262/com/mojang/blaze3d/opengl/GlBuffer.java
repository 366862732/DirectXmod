/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.jtracy.MemoryPool
 *  com.mojang.jtracy.TracyClient
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.system.MemoryUtil
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@Environment(value=EnvType.CLIENT)
public abstract class GlBuffer
extends GpuBuffer {
    protected static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool((String)"GPU Buffers");
    private final int handle;
    protected final boolean canPersistentMap;
    protected int mappingRefCount = 0;

    protected GlBuffer(@GpuBuffer.Usage int usage, long size, int handle, boolean canPersistentMap) {
        super(usage, size);
        this.handle = handle;
        this.canPersistentMap = canPersistentMap;
    }

    public int handle() {
        return this.handle;
    }

    protected void checkCanBeUsed() {
        if (this.canPersistentMap) {
            return;
        }
        if (this.mappingRefCount != 0) {
            throw new IllegalStateException("Attempt to use buffer while mapped without persistent mapping capability");
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Direct
    extends GlBuffer {
        private boolean closed;
        private final DirectStateAccess dsa;
        protected final int mappingFlags;
        protected @Nullable ByteBuffer mappedBuffer;

        protected Direct(DirectStateAccess dsa, @GpuBuffer.Usage int usage, long size, int handle, boolean canPersistentMap) {
            this.dsa = dsa;
            int clampedSize = (int)Math.min(size, Integer.MAX_VALUE);
            MEMORY_POOL.malloc((long)handle, clampedSize);
            int mappingFlags = 0;
            if ((usage & 1) != 0) {
                mappingFlags |= 1;
            }
            if ((usage & 2) != 0) {
                mappingFlags |= 0x32;
            }
            if (canPersistentMap) {
                mappingFlags |= 0x40;
            }
            this.mappingFlags = mappingFlags;
            super(usage, size, handle, canPersistentMap);
            if (canPersistentMap && (usage & 3) != 0) {
                this.map(0L, size, (usage & 1) != 0, (usage & 2) != 0);
            }
        }

        @Override
        public boolean isClosed() {
            return this.closed;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.canPersistentMap && (this.usage() & 3) != 0) {
                this.unmap();
            }
            if (this.mappingRefCount != 0) {
                throw new IllegalStateException("Attempt to close a mapped buffer");
            }
            GlStateManager._glDeleteBuffers(this.handle());
            MEMORY_POOL.free((long)this.handle());
        }

        @Override
        public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
            if (this.isClosed()) {
                throw new IllegalStateException("Buffer already closed");
            }
            if (!read && !write) {
                throw new IllegalArgumentException("At least read or write must be true");
            }
            if (read && (this.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            }
            if (write && (this.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            }
            if (offset + length > this.size()) {
                throw new IllegalArgumentException("Cannot map more data than this buffer can hold (attempting to map " + length + " bytes at offset " + offset + " from " + this.size() + " size buffer)");
            }
            if (offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Mapping buffers larger than 2GB is not supported");
            }
            if (offset < 0L || length < 0L) {
                throw new IllegalArgumentException("Offset or length must be positive integer values");
            }
            ++this.mappingRefCount;
            if (this.mappedBuffer == null) {
                GlStateManager.clearGlErrors();
                this.mappedBuffer = this.dsa.mapBufferRange(this.handle(), 0L, this.size(), this.mappingFlags, this.usage());
                if (this.mappedBuffer == null) {
                    throw new IllegalStateException("Failed to map buffer");
                }
            }
            return new GpuBufferSlice.MappedView(this.slice(offset, length), MemoryUtil.memSlice((ByteBuffer)this.mappedBuffer, (int)((int)offset), (int)((int)length)), new Runnable(this){
                private boolean closed;
                final /* synthetic */ Direct this$0;
                {
                    Direct direct = this$0;
                    Objects.requireNonNull(direct);
                    this.this$0 = direct;
                    this.closed = false;
                }

                @Override
                public void run() {
                    if (this.closed) {
                        return;
                    }
                    this.closed = true;
                    if ((this.this$0.mappingFlags & 0x10) != 0) {
                        this.this$0.dsa.flushMappedBufferRange(this.this$0.handle(), this.this$0.slice().offset(), this.this$0.slice().length(), this.this$0.usage());
                    }
                    this.this$0.unmap();
                }
            });
        }

        private void unmap() {
            --this.mappingRefCount;
            if (this.mappingRefCount == 0) {
                this.dsa.unmapBuffer(this.handle(), this.usage());
                this.mappedBuffer = null;
            }
        }
    }
}

