package com.xgdt.dx11.perf.culling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeafFaceCullingTest {
    @Test
    void nonLeafNeighbourAlwaysKeepsTheFace() {
        assertFalse(LeafFaceCulling.shouldCull(2, false, 0b1));
        assertFalse(LeafFaceCulling.shouldCull(4, false, 0b111));
    }

    @Test
    void depthOneCullsEveryLeafToLeafFace() {
        assertTrue(LeafFaceCulling.shouldCull(1, true, 0));
    }

    @Test
    void depthTwoRequiresOneLeafBehindTheNeighbour() {
        assertFalse(LeafFaceCulling.shouldCull(2, true, 0));
        assertTrue(LeafFaceCulling.shouldCull(2, true, 0b1));
    }

    @Test
    void deeperDepthsRequireEveryLayer() {
        assertFalse(LeafFaceCulling.shouldCull(3, true, 0b01));
        assertFalse(LeafFaceCulling.shouldCull(3, true, 0b10));
        assertTrue(LeafFaceCulling.shouldCull(3, true, 0b11));
        assertFalse(LeafFaceCulling.shouldCull(4, true, 0b011));
        assertTrue(LeafFaceCulling.shouldCull(4, true, 0b111));
    }

    @Test
    void unknownOrInvalidDepthKeepsTheFace() {
        assertFalse(LeafFaceCulling.shouldCull(0, true, 0b111));
        assertFalse(LeafFaceCulling.shouldCull(-1, true, 0b111));
    }

    @Test
    void sectionWindowMatchesTheRenderRegion() {
        // Same section, and one section either way, are in the 3x3x3 region.
        assertTrue(LeafFaceCulling.withinOneSection(0, 0));
        assertTrue(LeafFaceCulling.withinOneSection(0, 15));
        assertTrue(LeafFaceCulling.withinOneSection(0, 16));
        assertTrue(LeafFaceCulling.withinOneSection(15, -1));
        assertTrue(LeafFaceCulling.withinOneSection(-1, -16));
        // Two sections away is outside the region, in both directions.
        assertFalse(LeafFaceCulling.withinOneSection(15, 32));
        assertFalse(LeafFaceCulling.withinOneSection(-16, -33));
        assertFalse(LeafFaceCulling.withinOneSection(0, -17));
    }
}
