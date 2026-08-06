/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class MainTarget
extends RenderTarget {
    public static final int DEFAULT_WIDTH = 854;
    public static final int DEFAULT_HEIGHT = 480;
    private static final Dimension DEFAULT_DIMENSIONS = new Dimension(854, 480);

    public MainTarget(int desiredWidth, int desiredHeight) {
        super("Main", true, GpuFormat.RGBA8_UNORM);
        this.createFrameBuffer(desiredWidth, desiredHeight);
    }

    private void createFrameBuffer(int desiredWidth, int desiredHeight) {
        Dimension allocatedDimensions = this.allocateAttachments(desiredWidth, desiredHeight);
        if (this.colorTexture == null || this.depthTexture == null) {
            throw new IllegalStateException("Missing color and/or depth textures");
        }
        this.width = allocatedDimensions.width;
        this.height = allocatedDimensions.height;
    }

    private Dimension allocateAttachments(int width, int height) {
        RenderSystem.assertOnRenderThread();
        for (Dimension dimension : Dimension.listWithFallback(width, height)) {
            if (this.colorTexture != null) {
                this.colorTexture.close();
                this.colorTexture = null;
            }
            if (this.colorTextureView != null) {
                this.colorTextureView.close();
                this.colorTextureView = null;
            }
            if (this.depthTexture != null) {
                this.depthTexture.close();
                this.depthTexture = null;
            }
            if (this.depthTextureView != null) {
                this.depthTextureView.close();
                this.depthTextureView = null;
            }
            this.colorTexture = this.allocateColorAttachment(dimension);
            this.depthTexture = this.allocateDepthAttachment(dimension);
            if (this.colorTexture == null || this.depthTexture == null) continue;
            this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);
            this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
            return dimension;
        }
        throw new RuntimeException("Unrecoverable GL_OUT_OF_MEMORY (" + (this.colorTexture == null ? "missing color" : "have color") + ", " + (this.depthTexture == null ? "missing depth" : "have depth") + ")");
    }

    private @Nullable GpuTexture allocateColorAttachment(Dimension dimension) {
        try {
            return RenderSystem.getDevice().createTexture(() -> this.label + " / Color", 15, this.format, dimension.width, dimension.height, 1, 1);
        }
        catch (GpuOutOfMemoryException ignored) {
            return null;
        }
    }

    private @Nullable GpuTexture allocateDepthAttachment(Dimension dimension) {
        try {
            return RenderSystem.getDevice().createTexture(() -> this.label + " / Depth", 15, GpuFormat.D32_FLOAT, dimension.width, dimension.height, 1, 1);
        }
        catch (GpuOutOfMemoryException ignored) {
            return null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class Dimension {
        public final int width;
        public final int height;

        private Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        private static List<Dimension> listWithFallback(int width, int height) {
            RenderSystem.assertOnRenderThread();
            int maxTextureSize = RenderSystem.getDevice().getDeviceInfo().limits().maxTextureSize();
            if (width <= 0 || width > maxTextureSize || height <= 0 || height > maxTextureSize) {
                return ImmutableList.of((Object)DEFAULT_DIMENSIONS);
            }
            return ImmutableList.of((Object)new Dimension(width, height), (Object)DEFAULT_DIMENSIONS);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || this.getClass() != other.getClass()) {
                return false;
            }
            Dimension that = (Dimension)other;
            return this.width == that.width && this.height == that.height;
        }

        public int hashCode() {
            return Objects.hash(this.width, this.height);
        }

        public String toString() {
            return this.width + "x" + this.height;
        }
    }
}

