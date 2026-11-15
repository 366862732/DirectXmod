package com.xgdt.dx11.perf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerfCommandTest {
    @Test
    void geometryFlagsRequireARebuild() {
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle regionUpload"));
        assertTrue(PerfCommand.requiresChunkRebuild("dx11perf toggle regionDraws"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle compact"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle compactVerticesForce"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle translucentRegion"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle regionEnabled"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf toggle sodium"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf performancemode on"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf performancemode off"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf perfmode toggle"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf regionSize REGION_4"));
        assertTrue(PerfCommand.requiresChunkRebuild("/dx11perf reload"));
    }

    @Test
    void diagnosticsDoNotRequireARebuild() {
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf stats"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf leaks"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf toggle hud"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf toggle debugLog"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf toggle immediateEnabled"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf toggle immediate"));
        assertFalse(PerfCommand.requiresChunkRebuild("/dx11perf toggle uploadBudget"));
        assertFalse(PerfCommand.requiresChunkRebuild("/say hello"));
        assertFalse(PerfCommand.requiresChunkRebuild(null));
    }

    @Test
    void performanceModeSwitchesEveryOptimization() {
        PerfConfig config = new PerfConfig();
        assertTrue(PerfCommand.allPerformanceFeaturesEnabled(config));

        PerfCommand.applyPerformanceMode(config, false);

        assertTrue(config.enabled, "instrumentation stays available for A/B testing");
        assertFalse(config.regionEnabled);
        assertFalse(config.regionUpload);
        assertFalse(config.regionDraws);
        assertFalse(config.uploadBudget);
        assertFalse(config.compactVertices);
        assertFalse(config.translucentRegion);
        assertFalse(config.fogOcclusion);
        assertFalse(config.deferredOffscreenUpdates);
        assertFalse(config.entityCulling);
        assertFalse(config.visibleTextureAnimation);
        assertFalse(config.immediateEnabled);
        assertFalse(config.immediateBatching);
        assertFalse(config.fastTextLookup);
        assertFalse(config.skipTextSorting);
        assertFalse(config.fontAtlasResizing);
        assertFalse(config.fastGuiLayerLookup);
        assertFalse(config.mapAtlasGeneration);
        assertFalse(config.signTextBuffering);
        assertFalse(PerfCommand.allPerformanceFeaturesEnabled(config));

        PerfCommand.applyPerformanceMode(config, true);

        assertTrue(config.enabled);
        assertTrue(config.regionEnabled);
        assertTrue(config.regionDraws);
        assertTrue(config.compactVertices);
        assertTrue(config.translucentRegion);
        assertTrue(config.immediateEnabled);
        assertTrue(config.signTextBuffering);
        assertTrue(PerfCommand.allPerformanceFeaturesEnabled(config));
    }
}
