/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.lwjgl.opengl.GL33C
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.opengl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ShaderManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class GlProgram
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet((Object[])new String[]{"Projection", "Lighting", "Fog", "Globals"});
    public static final GlProgram INVALID_PROGRAM = new GlProgram(-1, "invalid");
    private final Map<String, Uniform> uniformsByName = new HashMap<String, Uniform>();
    private final int programId;
    private final String debugLabel;

    private GlProgram(int programId, String debugLabel) {
        this.programId = programId;
        this.debugLabel = debugLabel;
    }

    public static GlProgram link(GlShaderModule vertexShader, GlShaderModule fragmentShader, @Nullable VertexFormat[] vertexBindings, String debugLabel) throws ShaderManager.CompilationException {
        int programId = GlStateManager.glCreateProgram();
        if (programId <= 0) {
            throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + programId + ")");
        }
        int attributeLocation = 0;
        String previousName = null;
        for (VertexFormat vertexFormat : vertexBindings) {
            if (vertexFormat == null) continue;
            for (VertexFormatElement attribute : vertexFormat.getElements()) {
                String attributeName = attribute.name();
                if (!attributeName.equals(previousName)) {
                    GlStateManager._glBindAttribLocation(programId, attributeLocation, attributeName);
                }
                previousName = attributeName;
                ++attributeLocation;
            }
        }
        GlStateManager.glAttachShader(programId, vertexShader.getShaderId());
        GlStateManager.glAttachShader(programId, fragmentShader.getShaderId());
        GlStateManager.glLinkProgram(programId);
        int linkStatus = GlStateManager.glGetProgrami(programId, 35714);
        String linkMessage = GlStateManager.glGetProgramInfoLog(programId, 32768);
        if (linkStatus == 0 || linkMessage.contains("Failed for unknown reason")) {
            throw new ShaderManager.CompilationException("Error encountered when linking program containing VS " + String.valueOf(vertexShader.getId()) + " and FS " + String.valueOf(fragmentShader.getId()) + ". Log output: " + linkMessage);
        }
        if (!linkMessage.isEmpty()) {
            LOGGER.info("Info log when linking program containing VS {} and FS {}. Log output: {}", new Object[]{vertexShader.getId(), fragmentShader.getId(), linkMessage});
        }
        return new GlProgram(programId, debugLabel);
    }

    public void setupBindGroupLayouts(List<BindGroupLayout> bindGroupLayouts) {
        BindGroupLayout.ensureCompatible(bindGroupLayouts);
        List<BindGroupLayout.UniformDescription> uniforms = BindGroupLayout.flattenUniforms(bindGroupLayouts);
        List<String> samplers = BindGroupLayout.flattenSamplers(bindGroupLayouts);
        int nextUboBinding = 0;
        int nextSamplerIndex = 0;
        for (BindGroupLayout.UniformDescription uniformDescription : uniforms) {
            String uniformName = uniformDescription.name();
            Uniform.Utb uniform = switch (uniformDescription.type()) {
                default -> throw new MatchException(null, null);
                case UniformType.UNIFORM_BUFFER -> {
                    int index = GL33C.glGetUniformBlockIndex((int)this.programId, (CharSequence)uniformName);
                    if (index == -1) {
                        yield null;
                    }
                    int uboBinding = nextUboBinding++;
                    GL33C.glUniformBlockBinding((int)this.programId, (int)index, (int)uboBinding);
                    yield new Uniform.Ubo(uboBinding);
                }
                case UniformType.TEXEL_BUFFER -> {
                    int location = GlStateManager._glGetUniformLocation(this.programId, uniformName);
                    if (location == -1) {
                        LOGGER.warn("{} shader program does not use utb {} defined in the pipeline. This might be a bug.", (Object)this.debugLabel, (Object)uniformName);
                        yield null;
                    }
                    int samplerIndex = nextSamplerIndex++;
                    yield new Uniform.Utb(location, samplerIndex, Objects.requireNonNull(uniformDescription.gpuFormat()));
                }
            };
            if (uniform == null) continue;
            this.uniformsByName.put(uniformName, uniform);
        }
        for (String sampler : samplers) {
            int location = GlStateManager._glGetUniformLocation(this.programId, sampler);
            if (location == -1) {
                LOGGER.warn("{} shader program does not use sampler {} defined in the pipeline. This might be a bug.", (Object)this.debugLabel, (Object)sampler);
                continue;
            }
            int samplerIndex = nextSamplerIndex++;
            this.uniformsByName.put(sampler, new Uniform.Sampler(location, samplerIndex));
        }
        int totalDefinedBlocks = GlStateManager.glGetProgrami(this.programId, 35382);
        for (int i = 0; i < totalDefinedBlocks; ++i) {
            String name = GL33C.glGetActiveUniformBlockName((int)this.programId, (int)i);
            if (this.uniformsByName.containsKey(name)) continue;
            if (!samplers.contains(name) && BUILT_IN_UNIFORMS.contains(name)) {
                int uboBinding = nextUboBinding++;
                GL33C.glUniformBlockBinding((int)this.programId, (int)i, (int)uboBinding);
                this.uniformsByName.put(name, new Uniform.Ubo(uboBinding));
                continue;
            }
            LOGGER.warn("Found unknown and unsupported uniform {} in {}", (Object)name, (Object)this.debugLabel);
        }
    }

    @Override
    public void close() {
        this.uniformsByName.values().forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
    }

    public @Nullable Uniform getUniform(String name) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(name);
    }

    @VisibleForTesting
    public int getProgramId() {
        return this.programId;
    }

    public String toString() {
        return this.debugLabel;
    }

    public String getDebugLabel() {
        return this.debugLabel;
    }

    public Map<String, Uniform> getUniforms() {
        return this.uniformsByName;
    }
}

