package com.xgdt.dx11.perf.culling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SectionVisibilitySetTest {
    @Test
    void containsCapturedSections() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(0, 0, 0);
        set.add(1, 2, 3);
        set.freeze();

        assertEquals(2, set.size());
        assertTrue(set.captured());
        assertTrue(set.contains(0, 0, 0));
        assertTrue(set.contains(1, 2, 3));
        assertFalse(set.contains(0, 0, 1));
    }

    @Test
    void negativeCoordinatesRoundTrip() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(-1, -2, -3);
        set.add(-1024, 512, 2048);
        set.freeze();

        assertTrue(set.contains(-1, -2, -3));
        assertTrue(set.contains(-1024, 512, 2048));
        assertFalse(set.contains(-1, -2, -2));
    }

    @Test
    void worldBorderCoordinatesDoNotCollide() {
        // ±1,874,999 sections is the vanilla world border; the packing must
        // keep those distinct from their wrapped neighbourhood.
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(1_874_999, 0, -1_874_999);
        set.freeze();

        assertTrue(set.contains(1_874_999, 0, -1_874_999));
        assertFalse(set.contains(1_874_999, 0, 1_874_999));
        assertFalse(set.contains(-1_874_999, 0, -1_874_999));
    }

    @Test
    void intersectsFindsAnySectionInRange() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(-1, 0, 2);
        set.freeze();

        assertTrue(set.intersects(-2, -1, 1, 0, 0, 3));
        assertTrue(set.intersects(-1, 0, 2, -1, 0, 2));
        assertFalse(set.intersects(0, 0, 0, 3, 3, 3));
    }

    @Test
    void clearDisablesLookups() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(4, 5, 6);
        set.freeze();
        set.clear();

        assertFalse(set.captured());
        assertEquals(0, set.size());
        assertFalse(set.contains(4, 5, 6));
        assertFalse(set.intersects(4, 5, 6, 4, 5, 6));
    }

    @Test
    void emptyCapturedSetContainsNothing() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.freeze();

        assertTrue(set.captured());
        assertFalse(set.contains(0, 0, 0));
    }

    @Test
    void keysDistinguishAxes() {
        assertNotEquals(
            SectionVisibilitySet.key(1, 0, 0),
            SectionVisibilitySet.key(0, 1, 0));
        assertNotEquals(
            SectionVisibilitySet.key(0, 1, 0),
            SectionVisibilitySet.key(0, 0, 1));
        assertNotEquals(
            SectionVisibilitySet.key(-1, 0, 0),
            SectionVisibilitySet.key(0, 0, 0));
    }

    @Test
    void duplicateSectionsAreHarmless() {
        SectionVisibilitySet set = new SectionVisibilitySet();
        set.add(7, 8, 9);
        set.add(7, 8, 9);
        set.freeze();

        assertEquals(2, set.size());
        assertTrue(set.contains(7, 8, 9));
    }
}
