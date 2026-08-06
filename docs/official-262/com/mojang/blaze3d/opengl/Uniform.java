/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public sealed interface Uniform
extends AutoCloseable {
    @Override
    default public void close() {
    }

    @Environment(value=EnvType.CLIENT)
    public record Sampler(int location, int samplerIndex) implements Uniform
    {
    }

    @Environment(value=EnvType.CLIENT)
    public record Utb(int location, int samplerIndex, GpuFormat format, int texture) implements Uniform
    {
        public Utb(int location, int samplerIndex, GpuFormat format) {
            this(location, samplerIndex, format, GlStateManager._genTexture());
        }

        @Override
        public void close() {
            GlStateManager._deleteTexture(this.texture);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Ubo(int blockBinding) implements Uniform
    {
    }
}

