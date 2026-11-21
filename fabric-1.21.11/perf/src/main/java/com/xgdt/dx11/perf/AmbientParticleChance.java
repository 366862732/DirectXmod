package com.xgdt.dx11.perf;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Highest ambient-particle probability seen in any loaded biome.
 *
 * <p>1.21.11 spawns biome particles through {@code EnvironmentAttributeSystem}:
 * {@code ClientLevel.doAnimateTick} turns the block position into a {@code Vec3},
 * looks up the blended {@code AMBIENT_PARTICLES} list and only then asks each
 * entry whether it should spawn.  The lookup allocates and runs on every
 * animated section tick even when no particle can possibly pass its chance
 * check.</p>
 *
 * <p>Tracking the maximum probability the same way Lithium does lets the caller
 * draw one random number and skip the whole lookup when it cannot beat the best
 * particle in any biome.  Probabilities are recorded at construction time, so
 * the value only ever grows: a datapack reload that removes the highest
 * probability leaves a too-high maximum, which makes the shortcut fire less
 * often but never suppresses a particle that could have spawned.</p>
 *
 * <p>Construction can happen off the client thread while datapacks load, hence
 * the atomic CAS loop.  A lost update would lower the maximum, so it must not
 * be a plain field.</p>
 */
public final class AmbientParticleChance {
    private static final AtomicInteger MAX_BITS = new AtomicInteger(0);

    private AmbientParticleChance() {
    }

    /** Records one {@code AmbientParticle} probability. */
    public static void track(float probability) {
        int current = MAX_BITS.get();
        while (probability > Float.intBitsToFloat(current)) {
            int observed = MAX_BITS.compareAndExchange(current, Float.floatToIntBits(probability));
            if (observed == current) {
                return;
            }
            current = observed;
        }
    }

    /** Highest probability recorded so far, or {@code 0} when none has loaded. */
    public static float max() {
        return Float.intBitsToFloat(MAX_BITS.get());
    }
}
