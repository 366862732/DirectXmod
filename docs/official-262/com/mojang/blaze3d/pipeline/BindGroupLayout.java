/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BindGroupLayout {
    private final List<String> samplers;
    private final List<UniformDescription> uniforms;

    private BindGroupLayout(List<String> samplers, List<UniformDescription> uniforms) {
        this.samplers = samplers;
        this.uniforms = uniforms;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<UniformDescription> getUniforms() {
        return this.uniforms;
    }

    public static List<String> flattenSamplers(List<BindGroupLayout> bindGroupLayouts) {
        ArrayList<String> flattened = new ArrayList<String>();
        for (BindGroupLayout bindGroupLayout : bindGroupLayouts) {
            flattened.addAll(bindGroupLayout.getSamplers());
        }
        return flattened;
    }

    public static List<UniformDescription> flattenUniforms(List<BindGroupLayout> bindGroupLayouts) {
        ArrayList<UniformDescription> flattened = new ArrayList<UniformDescription>();
        for (BindGroupLayout bindGroupLayout : bindGroupLayouts) {
            flattened.addAll(bindGroupLayout.getUniforms());
        }
        return flattened;
    }

    public static void ensureCompatible(List<BindGroupLayout> bindGroupLayouts) {
        HashSet<String> names = new HashSet<String>();
        for (int layoutIndex = 0; layoutIndex < bindGroupLayouts.size(); ++layoutIndex) {
            BindGroupLayout bindGroupLayout = bindGroupLayouts.get(layoutIndex);
            for (UniformDescription uniform : bindGroupLayout.getUniforms()) {
                if (names.add(uniform.name())) continue;
                throw new IllegalArgumentException("Duplicate bind name '" + uniform.name() + "' in bind group layout " + layoutIndex);
            }
            for (String sampler : bindGroupLayout.getSamplers()) {
                if (names.add(sampler)) continue;
                throw new IllegalArgumentException("Duplicate bind name '" + sampler + "' in bind group layout " + layoutIndex);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final List<String> samplers = new ArrayList<String>();
        private final List<UniformDescription> uniforms = new ArrayList<UniformDescription>();

        private Builder() {
        }

        public Builder withSampler(String sampler) {
            this.samplers.add(sampler);
            return this;
        }

        public Builder withUniform(String name, UniformType type) {
            if (type == UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
            }
            this.uniforms.add(new UniformDescription(name, type));
            return this;
        }

        public Builder withUniform(String name, UniformType type, GpuFormat format) {
            if (type != UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Only texel buffer can specify texture format");
            }
            this.uniforms.add(new UniformDescription(name, format));
            return this;
        }

        public BindGroupLayout build() {
            return new BindGroupLayout(List.copyOf(this.samplers), List.copyOf(this.uniforms));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record UniformDescription(String name, UniformType type, @Nullable GpuFormat gpuFormat) {
        public UniformDescription(String name, UniformType type) {
            this(name, type, null);
            if (type == UniformType.TEXEL_BUFFER) {
                throw new IllegalArgumentException("Texel buffer needs a texture format");
            }
        }

        public UniformDescription(String name, GpuFormat gpuFormat) {
            this(name, UniformType.TEXEL_BUFFER, gpuFormat);
        }
    }
}

