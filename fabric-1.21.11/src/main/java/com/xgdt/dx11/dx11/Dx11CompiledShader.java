package com.xgdt.dx11.dx11;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * The result of translating one {@link RenderPipeline} to D3D11 HLSL.
 *
 * @param bindings         active resources in {@code b#} / {@code t#} order
 * @param vertexInputNames input variable names in vertex-format element order
 * @param semanticNames    HLSL semantics matching {@code vertexInputNames}
 * @param uniformSizes     initial size hint per constant-buffer slot
 */
@Environment(EnvType.CLIENT)
record Dx11CompiledShader(
    String vertexHlsl,
    String fragmentHlsl,
    List<Binding> bindings,
    List<String> vertexInputNames,
    List<String> semanticNames,
    int[] uniformSizes) {

    enum Type {
        UNIFORM_BUFFER,
        SAMPLED_IMAGE,
        TEXEL_BUFFER
    }

    record Binding(Type type, String name, int slot, @Nullable TextureFormat texelFormat) {
    }
}
