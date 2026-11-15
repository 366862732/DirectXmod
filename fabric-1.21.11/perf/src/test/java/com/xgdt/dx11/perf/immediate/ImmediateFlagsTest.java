package com.xgdt.dx11.perf.immediate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xgdt.dx11.perf.PerfConfig;
import org.junit.jupiter.api.Test;

class ImmediateFlagsTest {
    @Test
    void umbrellaDisablesEveryImmediateFeature() {
        PerfConfig config = new PerfConfig();
        config.immediateEnabled = false;
        config.immediateBatching = true;
        config.fastTextLookup = true;
        config.fontAtlasResizing = true;
        config.fastGuiLayerLookup = true;
        config.mapAtlasGeneration = true;
        config.signTextBuffering = true;

        ImmediateFlags.apply(true, config);

        assertFalse(ImmediateFlags.batching());
        assertFalse(ImmediateFlags.fastTextLookup());
        assertFalse(ImmediateFlags.skipTextSorting());
        assertFalse(ImmediateFlags.fontAtlasResizing());
        assertFalse(ImmediateFlags.guiLayerLookup());
        assertFalse(ImmediateFlags.mapAtlas());
        assertFalse(ImmediateFlags.signTextBuffering());
        assertTrue(ImmediateFlags.fontAtlasSize() == 256);
    }

    @Test
    void umbrellaOnKeepsIndividualSwitches() {
        PerfConfig config = new PerfConfig();
        config.immediateEnabled = true;
        config.immediateBatching = true;
        config.fastTextLookup = false;

        ImmediateFlags.apply(true, config);

        assertTrue(ImmediateFlags.batching());
        assertFalse(ImmediateFlags.fastTextLookup());
    }

    @Test
    void inactiveLayerDisablesEverything() {
        PerfConfig config = new PerfConfig();
        config.immediateEnabled = true;

        ImmediateFlags.apply(false, config);

        assertFalse(ImmediateFlags.batching());
        assertFalse(ImmediateFlags.mapAtlas());
    }
}
