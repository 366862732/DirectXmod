package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.junit.jupiter.api.Test;

class PerfTerrainPipelinesTest {
    @Test
    void clonesCopyEverythingExceptTheVertexFormat() {
        RenderPipeline base = ChunkSectionLayer.SOLID.pipeline();
        RenderPipeline compact = PerfTerrainPipelines.compact(ChunkSectionLayer.SOLID);

        assertNotSame(base, compact);
        assertSame(CompactTerrainFormat.FORMAT, compact.getVertexFormat());
        assertEquals(base.getVertexFormatMode(), compact.getVertexFormatMode());
        assertEquals(base.getVertexShader(), compact.getVertexShader());
        assertEquals(base.getFragmentShader(), compact.getFragmentShader());
        assertEquals(base.getShaderDefines(), compact.getShaderDefines());
        assertEquals(base.getSamplers(), compact.getSamplers());
        assertEquals(base.getUniforms(), compact.getUniforms());
        assertEquals(base.getBlendFunction(), compact.getBlendFunction());
        assertEquals(base.getDepthTestFunction(), compact.getDepthTestFunction());
        assertEquals(base.getPolygonMode(), compact.getPolygonMode());
        assertEquals(base.isCull(), compact.isCull());
        assertEquals(base.isWriteColor(), compact.isWriteColor());
        assertEquals(base.isWriteAlpha(), compact.isWriteAlpha());
        assertEquals(base.isWriteDepth(), compact.isWriteDepth());
        assertEquals(base.getColorLogic(), compact.getColorLogic());
        assertEquals(base.getDepthBiasScaleFactor(), compact.getDepthBiasScaleFactor());
        assertEquals(base.getDepthBiasConstant(), compact.getDepthBiasConstant());
    }

    @Test
    void cachesOnePipelinePerLayer() {
        RenderPipeline first = PerfTerrainPipelines.compact(ChunkSectionLayer.CUTOUT);
        RenderPipeline second = PerfTerrainPipelines.compact(ChunkSectionLayer.CUTOUT);

        assertSame(first, second);
        assertSame(
            CompactTerrainFormat.FORMAT,
            PerfTerrainPipelines.compact(ChunkSectionLayer.TRANSLUCENT).getVertexFormat());
    }
}
