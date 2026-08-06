/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.rendertype;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public final class RenderSetup {
    final RenderPipeline pipeline;
    final Map<String, TextureBinding> textures;
    final TextureTransform textureTransform;
    final OutputTarget outputTarget;
    final OutlineProperty outlineProperty;
    final boolean useLightmap;
    final boolean useOverlay;
    final boolean affectsCrumbling;
    final boolean sortOnUpload;
    final LayeringTransform layeringTransform;

    private RenderSetup(RenderPipeline pipeline, Map<String, TextureBinding> textures, boolean useLightmap, boolean useOverlay, LayeringTransform layeringTransform, OutputTarget outputTarget, TextureTransform textureTransform, OutlineProperty outlineProperty, boolean affectsCrumbling, boolean sortOnUpload) {
        this.pipeline = pipeline;
        this.textures = textures;
        this.outputTarget = outputTarget;
        this.textureTransform = textureTransform;
        this.useLightmap = useLightmap;
        this.useOverlay = useOverlay;
        this.outlineProperty = outlineProperty;
        this.layeringTransform = layeringTransform;
        this.affectsCrumbling = affectsCrumbling;
        this.sortOnUpload = sortOnUpload;
    }

    public String toString() {
        return "RenderSetup[layeringTransform=" + String.valueOf(this.layeringTransform) + ", textureTransform=" + String.valueOf(this.textureTransform) + ", textures=" + String.valueOf(this.textures) + ", outlineProperty=" + String.valueOf((Object)this.outlineProperty) + ", useLightmap=" + this.useLightmap + ", useOverlay=" + this.useOverlay + "]";
    }

    public static RenderSetupBuilder builder(RenderPipeline pipeline) {
        return new RenderSetupBuilder(pipeline);
    }

    public List<PreparedRenderType.Texture> prepareTextures(TextureManager textureManager, SamplerCache samplerCache, GpuTextureView overlayTexture, GpuTextureView lightmapTexture) {
        if (this.textures.isEmpty() && !this.useOverlay && !this.useLightmap) {
            return List.of();
        }
        ImmutableList.Builder textures = ImmutableList.builderWithExpectedSize((int)(this.textures.size() + 2));
        if (this.useOverlay) {
            textures.add((Object)new PreparedRenderType.Texture("Sampler1", overlayTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));
        }
        if (this.useLightmap) {
            textures.add((Object)new PreparedRenderType.Texture("Sampler2", lightmapTexture, samplerCache.getClampToEdge(FilterMode.LINEAR)));
        }
        for (Map.Entry<String, TextureBinding> entry : this.textures.entrySet()) {
            AbstractTexture texture = textureManager.getTexture(entry.getValue().location);
            GpuSampler samplerOverride = entry.getValue().sampler().get();
            textures.add((Object)new PreparedRenderType.Texture(entry.getKey(), texture.getTextureView(), samplerOverride != null ? samplerOverride : texture.getSampler()));
        }
        return textures.build();
    }

    @Environment(value=EnvType.CLIENT)
    public static enum OutlineProperty {
        NONE("none"),
        IS_OUTLINE("is_outline"),
        AFFECTS_OUTLINE("affects_outline");

        private final String name;

        private OutlineProperty(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class RenderSetupBuilder {
        private final RenderPipeline pipeline;
        private boolean useLightmap = false;
        private boolean useOverlay = false;
        private LayeringTransform layeringTransform = LayeringTransform.NO_LAYERING;
        private OutputTarget outputTarget = OutputTarget.MAIN_TARGET;
        private TextureTransform textureTransform = TextureTransform.DEFAULT_TEXTURING;
        private boolean affectsCrumbling = false;
        private boolean sortOnUpload = false;
        private OutlineProperty outlineProperty = OutlineProperty.NONE;
        private final Map<String, TextureBinding> textures = new HashMap<String, TextureBinding>();

        private RenderSetupBuilder(RenderPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public RenderSetupBuilder withTexture(String name, Identifier texture) {
            this.textures.put(name, new TextureBinding(texture, () -> null));
            return this;
        }

        public RenderSetupBuilder withTexture(String name, Identifier texture, @Nullable Supplier<GpuSampler> sampler) {
            this.textures.put(name, new TextureBinding(texture, (Supplier<GpuSampler>)Suppliers.memoize(() -> sampler == null ? null : (GpuSampler)sampler.get())));
            return this;
        }

        public RenderSetupBuilder useLightmap() {
            this.useLightmap = true;
            return this;
        }

        public RenderSetupBuilder useOverlay() {
            this.useOverlay = true;
            return this;
        }

        public RenderSetupBuilder affectsCrumbling() {
            this.affectsCrumbling = true;
            return this;
        }

        public RenderSetupBuilder sortOnUpload() {
            this.sortOnUpload = true;
            return this;
        }

        public RenderSetupBuilder setLayeringTransform(LayeringTransform layeringTransform) {
            this.layeringTransform = layeringTransform;
            return this;
        }

        public RenderSetupBuilder setOutputTarget(OutputTarget outputTarget) {
            this.outputTarget = outputTarget;
            return this;
        }

        public RenderSetupBuilder setTextureTransform(TextureTransform textureTransform) {
            this.textureTransform = textureTransform;
            return this;
        }

        public RenderSetupBuilder setOutline(OutlineProperty outlineProperty) {
            this.outlineProperty = outlineProperty;
            return this;
        }

        public RenderSetup createRenderSetup() {
            return new RenderSetup(this.pipeline, this.textures, this.useLightmap, this.useOverlay, this.layeringTransform, this.outputTarget, this.textureTransform, this.outlineProperty, this.affectsCrumbling, this.sortOnUpload);
        }
    }

    @Environment(value=EnvType.CLIENT)
    record TextureBinding(Identifier location, Supplier<@Nullable GpuSampler> sampler) {
    }
}

