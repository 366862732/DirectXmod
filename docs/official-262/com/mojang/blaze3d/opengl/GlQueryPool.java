/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.lwjgl.opengl.GL33C
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.systems.GpuQueryPool;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL33C;

@Environment(value=EnvType.CLIENT)
public class GlQueryPool
implements GpuQueryPool {
    private final int[] handles;
    private boolean closed;

    public GlQueryPool(int size) {
        this.handles = new int[size];
        GL33C.glGenQueries((int[])this.handles);
    }

    @Override
    public int size() {
        return this.handles.length;
    }

    @Override
    public OptionalLong getValue(int index) {
        int handle = this.handles[index];
        if (GL33C.glGetQueryObjecti((int)handle, (int)34919) == 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(GL33C.glGetQueryObjectui64((int)handle, (int)34918));
    }

    @Override
    public OptionalLong[] getValues(int index, int count) {
        if (index + count > this.handles.length) {
            throw new IndexOutOfBoundsException("getValues would read out-of-bounds for an array of " + count + " starting at " + index + ", when total size is " + this.handles.length);
        }
        OptionalLong[] result = new OptionalLong[count];
        for (int i = 0; i < count; ++i) {
            result[i] = this.getValue(index + i);
        }
        return result;
    }

    protected void writeTimestamp(int index) {
        GL33C.glQueryCounter((int)this.handles[index], (int)36392);
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            GL33C.glDeleteQueries((int[])this.handles);
        }
    }
}

