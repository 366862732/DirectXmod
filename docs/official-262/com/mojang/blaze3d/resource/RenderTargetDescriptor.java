/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 */
package com.mojang.blaze3d.resource;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public record RenderTargetDescriptor(int width, int height, boolean useDepth, Vector4fc clearColor, GpuFormat format) implements ResourceDescriptor<RenderTarget>
{
    @Override
    public RenderTarget allocate() {
        return new TextureTarget(null, this.width, this.height, this.useDepth, this.format);
    }

    @Override
    public void prepare(RenderTarget resource) {
        if (this.useDepth) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(resource.getColorTexture(), this.clearColor, resource.getDepthTexture(), 0.0);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(resource.getColorTexture(), this.clearColor);
        }
    }

    @Override
    public void free(RenderTarget resource) {
        resource.destroyBuffers();
    }

    @Override
    public boolean canUsePhysicalResource(ResourceDescriptor<?> other) {
        if (other instanceof RenderTargetDescriptor) {
            RenderTargetDescriptor descriptor = (RenderTargetDescriptor)other;
            return this.width == descriptor.width && this.height == descriptor.height && this.useDepth == descriptor.useDepth && this.format == descriptor.format;
        }
        return false;
    }
}

