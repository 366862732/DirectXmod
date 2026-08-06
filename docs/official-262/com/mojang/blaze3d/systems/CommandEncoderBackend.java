/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderPassDescriptor;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public interface CommandEncoderBackend {
    public void submit();

    public TransientMemory transientMemory();

    public RenderPassBackend createRenderPass(RenderPassDescriptor var1);

    public void submitRenderPass();

    public void clearColorTexture(GpuTexture var1, Vector4fc var2);

    public void clearColorAndDepthTextures(GpuTexture var1, Vector4fc var2, GpuTexture var3, double var4);

    public void clearColorAndDepthTextures(GpuTexture var1, Vector4fc var2, GpuTexture var3, double var4, int var6, int var7, int var8, int var9);

    public void clearDepthTexture(GpuTexture var1, double var2);

    public void writeToBuffer(GpuBufferSlice var1, ByteBuffer var2);

    public void copyToBuffer(GpuBufferSlice var1, GpuBufferSlice var2);

    public void writeToTexture(GpuTexture var1, ByteBuffer var2, int var3, int var4, int var5, int var6, int var7, int var8);

    public void copyBufferToTexture(GpuBufferSlice var1, int var2, int var3, int var4, int var5, GpuTexture var6, int var7, int var8, int var9, int var10, int var11, int var12);

    public void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6);

    public void copyTextureToBuffer(GpuTexture var1, GpuBuffer var2, long var3, Runnable var5, int var6, int var7, int var8, int var9, int var10);

    public void copyTextureToTexture(GpuTexture var1, GpuTexture var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9);

    public GpuFence createFence();

    public void writeTimestamp(GpuQueryPool var1, int var2);
}

