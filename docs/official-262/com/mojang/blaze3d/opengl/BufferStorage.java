/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GLCapabilities
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlDevice;
import java.nio.ByteBuffer;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GLCapabilities;

@Environment(value=EnvType.CLIENT)
public abstract class BufferStorage {
    public static BufferStorage create(GLCapabilities capabilities, Set<String> enabledExtensions) {
        if (capabilities.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage) {
            enabledExtensions.add("GL_ARB_buffer_storage");
            return new Immutable();
        }
        return new Mutable();
    }

    public abstract GlBuffer createBuffer(DirectStateAccess var1, @GpuBuffer.Usage int var2, long var3);

    public abstract GlBuffer createBuffer(DirectStateAccess var1, @GpuBuffer.Usage int var2, ByteBuffer var3);

    @Environment(value=EnvType.CLIENT)
    private static class Immutable
    extends BufferStorage {
        private Immutable() {
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, long size) {
            int buffer = dsa.createBuffer();
            dsa.bufferStorage(buffer, size, usage);
            return new GlBuffer.Direct(dsa, usage, size, buffer, true);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, ByteBuffer data) {
            int buffer = dsa.createBuffer();
            int size = data.remaining();
            dsa.bufferStorage(buffer, data, usage);
            return new GlBuffer.Direct(dsa, usage, size, buffer, true);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Mutable
    extends BufferStorage {
        private Mutable() {
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, long size) {
            int buffer = dsa.createBuffer();
            dsa.bufferData(buffer, size, usage);
            return new GlBuffer.Direct(dsa, usage, size, buffer, false);
        }

        @Override
        public GlBuffer createBuffer(DirectStateAccess dsa, @GpuBuffer.Usage int usage, ByteBuffer data) {
            int buffer = dsa.createBuffer();
            int size = data.remaining();
            dsa.bufferData(buffer, data, usage);
            return new GlBuffer.Direct(dsa, usage, size, buffer, false);
        }
    }
}

