package com.xgdt.dx11.perf;

/**
 * Counters for the Lithium-style client micro-optimizations.
 *
 * <p>These are diagnostics only, so they are plain {@code long}s: the counters
 * are incremented from the client thread (and occasionally the integrated
 * server thread for entity queries), where a lost update only makes a reported
 * number slightly smaller.  Reset from {@code /dx11perf reset}.</p>
 */
public final class MicroStats {
    private static long ambientParticleSkips;
    private static long entityLookupHits;
    private static long ambientSoundSkips;
    private static long entityQueryFastPaths;

    private MicroStats() {
    }

    public static void ambientParticleSkipped() {
        ambientParticleSkips++;
    }

    public static void entityLookupHit() {
        entityLookupHits++;
    }

    public static void ambientSoundSkipped() {
        ambientSoundSkips++;
    }

    public static void entityQueryFastPath() {
        entityQueryFastPaths++;
    }

    public static void reset() {
        ambientParticleSkips = 0;
        entityLookupHits = 0;
        ambientSoundSkips = 0;
        entityQueryFastPaths = 0;
    }

    public static String describe() {
        PerfConfig config = PerfState.config();
        return "micro | particles=" + config.fastAmbientParticles
            + " particleSkips=" + ambientParticleSkips
            + " entityLookup=" + config.fastEntityLookup
            + " entityLookupHits=" + entityLookupHits
            + " ambientSounds=" + config.skipClientAmbientSounds
            + " ambientSoundSkips=" + ambientSoundSkips
            + " entityQueries=" + config.fastEntityQueries
            + " entityQueryFastPaths=" + entityQueryFastPaths;
    }
}
