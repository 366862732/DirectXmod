/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.PointerBuffer
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;

@Environment(value=EnvType.CLIENT)
public interface RenderPassBackend {
    public void pushDebugGroup(Supplier<String> var1);

    public void popDebugGroup();

    public void setPipeline(RenderPipeline var1);

    public void bindTexture(String var1, @Nullable GpuTextureView var2, @Nullable GpuSampler var3);

    public void setUniform(String var1, GpuBuffer var2);

    public void setUniform(String var1, GpuBufferSlice var2);

    public void enableScissor(int var1, int var2, int var3, int var4);

    public void disableScissor();

    public void setVertexBuffer(int var1, @Nullable GpuBufferSlice var2);

    public void setIndexBuffer(GpuBuffer var1, IndexType var2);

    public void drawIndexed(int var1, int var2, int var3, int var4, int var5);

    public void multiDrawIndexed(IntBuffer var1, int var2, int var3, int var4);

    public void multiDrawIndexed(PointerBuffer var1, IntBuffer var2, IntBuffer var3, int var4);

    public void drawIndexedIndirect(GpuBufferSlice var1, int var2);

    public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> var1, @Nullable GpuBuffer var2, @Nullable IndexType var3, Collection<String> var4, T var5);

    public void draw(int var1, int var2, int var3, int var4);

    public void multiDraw(IntBuffer var1, int var2, int var3, int var4);

    public void multiDraw(IntBuffer var1, IntBuffer var2, int var3);

    public void drawIndirect(GpuBufferSlice var1, int var2);

    public void writeTimestamp(GpuQueryPool var1, int var2);
}

