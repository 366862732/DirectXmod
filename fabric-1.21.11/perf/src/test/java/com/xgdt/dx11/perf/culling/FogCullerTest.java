package com.xgdt.dx11.perf.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FogCullerTest {
    @Test
    void sectionContainingCameraIsNeverCulled() {
        FogCuller culler = new FogCuller(1.0, 1.0, 8.0, 8.0, 8.0);

        assertFalse(culler.isFullyFogged(0, 0, 0));
    }

    @Test
    void sectionBeyondBothEndsIsCulled() {
        FogCuller culler = new FogCuller(16.0, 24.0, 0.0, 0.0, 0.0);

        assertTrue(culler.isFullyFogged(32, 0, 0));
    }

    @Test
    void sectionBeyondRenderEndIsCulledEvenInsideEnvironmentalFog() {
        // Render-distance fog alone already saturates the shader's max().
        FogCuller culler = new FogCuller(64.0, 24.0, 0.0, 0.0, 0.0);

        assertTrue(culler.isFullyFogged(32, 0, 0));
    }

    @Test
    void sectionBeyondEnvironmentalEndIsCulledEvenInsideRenderFog() {
        // Directly above the camera: spherical distance passes the
        // environmental end, so the environmental term alone is opaque.
        FogCuller culler = new FogCuller(16.0, 128.0, 0.0, 0.0, 0.0);

        assertTrue(culler.isFullyFogged(0, 64, 0));
    }

    @Test
    void sectionInsideBothEndsIsKept() {
        FogCuller culler = new FogCuller(64.0, 64.0, 0.0, 0.0, 0.0);

        assertFalse(culler.isFullyFogged(16, 0, 0));
    }

    @Test
    void sectionBehindCameraUsesAbsoluteDistance() {
        FogCuller culler = new FogCuller(32.0, 32.0, 100.0, 64.0, 100.0);

        assertTrue(culler.isFullyFogged(0, 64, 0));
    }

    @Test
    void nearestPointEqualToFogEndCountsAsFullyFogged() {
        FogCuller culler = new FogCuller(35.0, 31.0, 0.0, 0.0, 0.0);

        // Nearest point is (16, 32, 0): spherical ≈ 35.78, cylindrical = 32.
        assertTrue(culler.isFullyFogged(16, 32, 0));
    }

    @Test
    void nearestToZeroClampsToTheBox() {
        assertEquals(5.0, FogCuller.nearestToZero(5.0, 21.0));
        assertEquals(5.0, FogCuller.nearestToZero(-21.0, -5.0));
        assertEquals(0.0, FogCuller.nearestToZero(-5.0, 5.0));
        assertEquals(0.0, FogCuller.nearestToZero(0.0, 16.0));
    }
}
