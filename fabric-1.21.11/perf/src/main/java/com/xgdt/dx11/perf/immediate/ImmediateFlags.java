package com.xgdt.dx11.perf.immediate;

import com.xgdt.dx11.perf.PerfConfig;

/**
 * Cached ImmediatelyFast-style feature switches.
 *
 * <p>The hot render paths (text glyphs, GUI hit tests, per-buffer lookups) must
 * not walk the config object on every call.  {@link #apply} is called from
 * {@code PerfState.refresh()} after a config load or toggle, so each path only
 * reads one volatile boolean.</p>
 */
public final class ImmediateFlags {
    private static volatile boolean batching;
    private static volatile boolean fastTextLookup;
    private static volatile boolean skipTextSorting;
    private static volatile boolean fontAtlasResizing;
    private static volatile boolean guiLayerLookup;
    private static volatile boolean mapAtlas;
    private static volatile boolean signTextBuffering;

    private static volatile int fontAtlasSize = 256;
    private static volatile int mapAtlasSize = 2048;
    private static volatile int signAtlasSize = 4096;

    private ImmediateFlags() {
    }

    public static void apply(boolean perfActive, PerfConfig config) {
        boolean on = perfActive && config != null && config.immediateEnabled;
        batching = on && config.immediateBatching;
        fastTextLookup = on && config.fastTextLookup;
        skipTextSorting = on && config.skipTextSorting;
        fontAtlasResizing = on && config.fontAtlasResizing;
        guiLayerLookup = on && config.fastGuiLayerLookup;
        mapAtlas = on && config.mapAtlasGeneration;
        signTextBuffering = on && config.signTextBuffering;
        fontAtlasSize = on && config.fontAtlasResizing ? config.fontAtlasSize : 256;
        mapAtlasSize = on ? config.mapAtlasSize : 2048;
        signAtlasSize = on ? config.signAtlasSize : 4096;
    }

    public static boolean batching() {
        return batching;
    }

    public static boolean fastTextLookup() {
        return fastTextLookup;
    }

    public static boolean skipTextSorting() {
        return skipTextSorting;
    }

    public static boolean fontAtlasResizing() {
        return fontAtlasResizing;
    }

    public static int fontAtlasSize() {
        return fontAtlasSize;
    }

    public static boolean guiLayerLookup() {
        return guiLayerLookup;
    }

    public static boolean mapAtlas() {
        return mapAtlas;
    }

    public static int mapAtlasSize() {
        return mapAtlasSize;
    }

    public static boolean signTextBuffering() {
        return signTextBuffering;
    }

    public static int signAtlasSize() {
        return signAtlasSize;
    }
}
