package com.xgdt.dx11.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AmbientParticleChanceTest {
    @Test
    void tracksOnlyTheMaximumProbability() {
        float initial = AmbientParticleChance.max();

        AmbientParticleChance.track(0.1F);
        assertTrue(AmbientParticleChance.max() >= 0.1F);

        AmbientParticleChance.track(0.05F);
        assertTrue(AmbientParticleChance.max() >= 0.1F, "smaller values must not lower the maximum");

        AmbientParticleChance.track(0.7F);
        assertEquals(0.7F, AmbientParticleChance.max());

        AmbientParticleChance.track(Float.NaN);
        assertEquals(0.7F, AmbientParticleChance.max(), "NaN must be ignored");

        AmbientParticleChance.track(-1.0F);
        assertEquals(0.7F, AmbientParticleChance.max(), "negative probabilities must be ignored");

        assertTrue(AmbientParticleChance.max() >= initial);
    }
}
