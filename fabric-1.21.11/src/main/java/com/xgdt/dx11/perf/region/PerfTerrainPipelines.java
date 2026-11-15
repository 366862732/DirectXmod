package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compact-vertex clones of the vanilla terrain pipelines.
 *
 * <p>The shaders, uniforms and render state are copied from
 * {@link ChunkSectionLayer#pipeline()}; only the vertex format changes.  The
 * clones are created lazily on the render thread and compiled by the D3D11
 * backend on first use.  They are cached per layer because building a
 * {@link RenderPipeline} is a one-time setup cost.</p>
 */
public final class PerfTerrainPipelines {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final Map<ChunkSectionLayer, RenderPipeline> COMPACT =
        new EnumMap<>(ChunkSectionLayer.class);
    private static final Set<ChunkSectionLayer> UNAVAILABLE =
        EnumSet.noneOf(ChunkSectionLayer.class);

    private PerfTerrainPipelines() {
    }

    /**
     * Compiles (or fetches) the compact pipeline and reports whether the
     * backend accepted it.  Uploads only switch to compact data when this is
     * true, so a driver that rejects the packed layout can never leave the
     * arena holding vertices no pipeline can draw.
     */
    public static boolean isUsable(ChunkSectionLayer layer) {
        if (UNAVAILABLE.contains(layer)) {
            return false;
        }
        try {
            CompiledRenderPipeline compiled = RenderSystem.getDevice()
                .precompilePipeline(compact(layer), null);
            if (compiled.isValid()) {
                return true;
            }
        } catch (RuntimeException e) {
            LOGGER.warn(
                "[dx11-perf] Compact terrain pipeline {} is unavailable: {}",
                layer,
                e.toString());
        }
        // The backend caches failed pipelines until its pipeline cache is
        // cleared.  Remember the failure so uploads keep the vanilla layout
        // instead of retrying the same compile on every section.
        UNAVAILABLE.add(layer);
        return false;
    }

    /** Drops cached definitions and failures after a pipeline/shader reload. */
    public static void reset() {
        COMPACT.clear();
        UNAVAILABLE.clear();
    }

    /** @return the compact-format pipeline for a terrain layer */
    public static RenderPipeline compact(ChunkSectionLayer layer) {
        RenderPipeline cached = COMPACT.get(layer);
        if (cached != null) {
            return cached;
        }
        RenderPipeline created = create(layer);
        COMPACT.put(layer, created);
        return created;
    }

    private static RenderPipeline create(ChunkSectionLayer layer) {
        RenderPipeline base = layer.pipeline();
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                "gl4dx11", "pipeline/compact_" + layer.name().toLowerCase(Locale.ROOT)))
            .withVertexShader(base.getVertexShader())
            .withFragmentShader(base.getFragmentShader())
            .withVertexFormat(CompactTerrainFormat.FORMAT, base.getVertexFormatMode())
            .withDepthTestFunction(base.getDepthTestFunction())
            .withPolygonMode(base.getPolygonMode())
            .withCull(base.isCull())
            .withColorWrite(base.isWriteColor(), base.isWriteAlpha())
            .withDepthWrite(base.isWriteDepth())
            .withColorLogic(base.getColorLogic())
            .withDepthBias(base.getDepthBiasScaleFactor(), base.getDepthBiasConstant());

        ShaderDefines defines = base.getShaderDefines();
        for (String flag : defines.flags()) {
            builder.withShaderDefine(flag);
        }
        for (Map.Entry<String, String> define : defines.values().entrySet()) {
            applyDefine(builder, define.getKey(), define.getValue());
        }

        for (String sampler : base.getSamplers()) {
            builder.withSampler(sampler);
        }
        for (RenderPipeline.UniformDescription uniform : base.getUniforms()) {
            if (uniform.type() == null) {
                continue;
            }
            if (uniform.textureFormat() != null) {
                builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
            } else {
                builder.withUniform(uniform.name(), uniform.type());
            }
        }

        if (base.getBlendFunction().isPresent()) {
            builder.withBlend(base.getBlendFunction().get());
        } else {
            builder.withoutBlend();
        }
        return builder.build();
    }

    /**
     * {@link RenderPipeline.Builder} has integer and float define overloads but
     * no string overload.  ShaderDefines only stores values created by those
     * numeric overloads, so one of the two parses always succeeds.
     */
    private static void applyDefine(
        RenderPipeline.Builder builder, String name, String value) {
        try {
            builder.withShaderDefine(name, Integer.parseInt(value));
        } catch (NumberFormatException notAnInt) {
            try {
                builder.withShaderDefine(name, Float.parseFloat(value));
            } catch (NumberFormatException notAFloat) {
                throw new IllegalArgumentException("Unsupported shader define value: " + value);
            }
        }
    }
}
