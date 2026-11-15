package com.xgdt.dx11.perf.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UploadOrderTest {
    @Test
    void bookkeepingUploadsRunBeforeSectionUploads() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 0, 64, 0, 2, 100),
            new UploadOrder.Entry(false, 0, 0, 0, 1, 10),
            new UploadOrder.Entry(true, 0, 0, 0, 3, 100),
            new UploadOrder.Entry(false, 0, 0, 0, 4, 10));

        int[] ranked = UploadOrder.rank(entries, 0, 64, 0);

        assertArrayEquals(new int[] {1, 3, 0, 2}, ranked);
    }

    @Test
    void nearestSectionUploadsComeFirst() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 512, 0, 0, 0, 100),
            new UploadOrder.Entry(true, 0, 0, 0, 1, 100),
            new UploadOrder.Entry(true, 256, 0, 0, 2, 100));

        int[] ranked = UploadOrder.rank(entries, 8, 8, 8);

        assertArrayEquals(new int[] {1, 2, 0}, ranked);
    }

    @Test
    void equalDistanceKeepsInsertionOrder() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 32, 0, 0, 9, 100),
            new UploadOrder.Entry(true, 0, 0, 32, 4, 100));

        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        assertArrayEquals(new int[] {1, 0}, ranked);
    }

    @Test
    void visibleSectionUploadsComeBeforeOffscreenOnes() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, false, 0, 0, 0, 0, 100),
            new UploadOrder.Entry(true, true, 256, 0, 0, 1, 100),
            new UploadOrder.Entry(true, true, 0, 0, 0, 2, 100),
            new UploadOrder.Entry(true, false, 0, 0, 0, 3, 100));

        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        assertArrayEquals(new int[] {2, 1, 0, 3}, ranked);
    }

    @Test
    void bookkeepingStillRunsBeforeVisibleSectionUploads() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, true, 0, 0, 0, 0, 100),
            new UploadOrder.Entry(false, true, 0, 0, 0, 1, 10));

        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        assertArrayEquals(new int[] {1, 0}, ranked);
    }

    @Test
    void selectionHonoursSectionAndByteCaps() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 0, 0, 0, 0, 100),
            new UploadOrder.Entry(true, 16, 0, 0, 1, 100),
            new UploadOrder.Entry(true, 32, 0, 0, 2, 100),
            new UploadOrder.Entry(true, 48, 0, 0, 3, 100));
        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        UploadOrder.Selection bySections = UploadOrder.select(entries, ranked, 2, 0L);
        assertEquals(2, bySections.count());
        assertEquals(200L, bySections.bytes());
        assertTrue(bySections.capped());

        UploadOrder.Selection byBytes = UploadOrder.select(entries, ranked, 0, 150L);
        assertEquals(2, byBytes.count());
        assertEquals(200L, byBytes.bytes());
        assertTrue(byBytes.capped());
    }

    @Test
    void selectionRunsAtLeastOneOversizedUpload() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 0, 0, 0, 0, 8 * 1024 * 1024),
            new UploadOrder.Entry(true, 16, 0, 0, 1, 100));
        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        UploadOrder.Selection selection = UploadOrder.select(entries, ranked, 1, 1L);

        assertEquals(1, selection.count());
        assertEquals(8 * 1024 * 1024L, selection.bytes());
        assertTrue(selection.capped());
    }

    @Test
    void zeroCapsMeanUnlimited() {
        List<UploadOrder.Entry> entries = List.of(
            new UploadOrder.Entry(true, 0, 0, 0, 0, 100),
            new UploadOrder.Entry(true, 16, 0, 0, 1, 100));
        int[] ranked = UploadOrder.rank(entries, 0, 0, 0);

        UploadOrder.Selection selection = UploadOrder.select(entries, ranked, 0, 0L);

        assertEquals(2, selection.count());
        assertFalse(selection.capped());
    }

    @Test
    void emptyListSelectsNothing() {
        UploadOrder.Selection selection = UploadOrder.select(List.of(), new int[0], 4, 1024L);
        assertEquals(0, selection.count());
        assertFalse(selection.capped());
    }
}
