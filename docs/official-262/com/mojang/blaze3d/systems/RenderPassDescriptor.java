/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector4fc
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RenderPassDescriptor {
    private final Supplier<String> label;
    public List<@Nullable Attachment<Optional<Vector4fc>>> colorAttachments = new ArrayList<Attachment<Optional<Vector4fc>>>();
    public @Nullable Attachment<OptionalDouble> depthAttachment;
    public  @Nullable RenderPass.RenderArea renderArea;

    public static RenderPassDescriptor create(Supplier<String> label) {
        return new RenderPassDescriptor(label);
    }

    private RenderPassDescriptor(Supplier<String> label) {
        this.label = label;
    }

    public RenderPassDescriptor withColorAttachment(GpuTextureView textureView) {
        this.colorAttachments.add(new Attachment(textureView, Optional.empty()));
        return this;
    }

    public RenderPassDescriptor withColorAttachment(GpuTextureView textureView, Optional<Vector4fc> clearValue) {
        this.colorAttachments.add(new Attachment<Optional<Vector4fc>>(textureView, clearValue));
        return this;
    }

    public RenderPassDescriptor withUnusedColorAttachment() {
        this.colorAttachments.add(null);
        return this;
    }

    public RenderPassDescriptor withDepthAttachment(GpuTextureView textureView) {
        this.depthAttachment = new Attachment<OptionalDouble>(textureView, OptionalDouble.empty());
        return this;
    }

    public RenderPassDescriptor withDepthAttachment(GpuTextureView textureView, OptionalDouble clearValue) {
        this.depthAttachment = new Attachment<OptionalDouble>(textureView, clearValue);
        return this;
    }

    public RenderPassDescriptor withRenderArea(RenderPass.RenderArea renderArea) {
        this.renderArea = renderArea;
        return this;
    }

    public Supplier<String> label() {
        return this.label;
    }

    public List<@Nullable Attachment<Optional<Vector4fc>>> colorAttachments() {
        return this.colorAttachments;
    }

    public @Nullable Attachment<OptionalDouble> depthAttachment() {
        return this.depthAttachment;
    }

    @Environment(value=EnvType.CLIENT)
    public record Attachment<T>(GpuTextureView textureView, T clearValue) {
    }
}

