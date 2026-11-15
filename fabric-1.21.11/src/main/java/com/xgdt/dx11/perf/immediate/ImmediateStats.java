package com.xgdt.dx11.perf.immediate;

/**
 * Render-thread counters for the ImmediatelyFast-style features.
 *
 * <p>These are deliberately separate from {@code PerfCounters}: the region
 * renderer's snapshot format is used by existing validation scripts, and the
 * new counters are only meaningful when the corresponding feature is on.
 * Every increment happens on the render thread, so plain longs are enough.</p>
 */
public final class ImmediateStats {
    private static long bufferBorrows;
    private static long bufferCreates;
    private static long bufferReleases;
    private static long bufferCloses;
    private static long batchFlushes;
    private static long batchDraws;
    private static long activeBatchPeak;

    private static long textGlyphLookups;
    private static long textGlyphHits;

    private static long guiQueries;
    private static long guiIndexedAnswers;
    private static long guiFallbacks;

    private static long mapAtlasLookups;
    private static long mapAtlasUploads;
    private static long mapAtlasFallbacks;

    private static long signCacheHits;
    private static long signCacheMisses;
    private static long signCacheEvictions;
    private static long signRenders;

    private ImmediateStats() {
    }

    public static void onBorrow() {
        bufferBorrows++;
    }

    public static void onCreate() {
        bufferCreates++;
    }

    public static void onRelease() {
        bufferReleases++;
    }

    public static void onClose() {
        bufferCloses++;
    }

    public static void onBatchFlush() {
        batchFlushes++;
    }

    public static void onBatchDraw() {
        batchDraws++;
    }

    public static void onActiveBatches(int active) {
        if (active > activeBatchPeak) {
            activeBatchPeak = active;
        }
    }

    public static void onTextLookup(boolean hit) {
        textGlyphLookups++;
        if (hit) {
            textGlyphHits++;
        }
    }

    public static void onGuiQuery(boolean indexed) {
        guiQueries++;
        if (indexed) {
            guiIndexedAnswers++;
        } else {
            guiFallbacks++;
        }
    }

    public static void onMapLookup(boolean found) {
        mapAtlasLookups++;
        if (!found) {
            mapAtlasFallbacks++;
        }
    }

    public static void onMapUpload() {
        mapAtlasUploads++;
    }

    public static void onSignLookup(boolean hit) {
        if (hit) {
            signCacheHits++;
        } else {
            signCacheMisses++;
        }
    }

    public static void onSignEvict() {
        signCacheEvictions++;
    }

    public static void onSignRender() {
        signRenders++;
    }

    public static void reset() {
        bufferBorrows = 0;
        bufferCreates = 0;
        bufferReleases = 0;
        bufferCloses = 0;
        batchFlushes = 0;
        batchDraws = 0;
        activeBatchPeak = 0;
        textGlyphLookups = 0;
        textGlyphHits = 0;
        guiQueries = 0;
        guiIndexedAnswers = 0;
        guiFallbacks = 0;
        mapAtlasLookups = 0;
        mapAtlasUploads = 0;
        mapAtlasFallbacks = 0;
        signCacheHits = 0;
        signCacheMisses = 0;
        signCacheEvictions = 0;
        signRenders = 0;
    }

    public static String describe() {
        return "IF-like: buffers borrow=" + bufferBorrows
            + " create=" + bufferCreates
            + " release=" + bufferReleases
            + " close=" + bufferCloses
            + " pool=" + BufferPool.freeCount()
            + " peakBatches=" + activeBatchPeak
            + " flushes=" + batchFlushes
            + " batchDraws=" + batchDraws
            + " | text lookup=" + textGlyphLookups
            + " hit=" + textGlyphHits
            + " | gui query=" + guiQueries
            + " indexed=" + guiIndexedAnswers
            + " fallback=" + guiFallbacks
            + " | map lookup=" + mapAtlasLookups
            + " upload=" + mapAtlasUploads
            + " fallback=" + mapAtlasFallbacks
            + " | sign hit=" + signCacheHits
            + " miss=" + signCacheMisses
            + " evict=" + signCacheEvictions
            + " draws=" + signRenders
            + " slots=" + com.xgdt.dx11.perf.immediate.sign.SignTextCache.residentSlotCount();
    }
}
