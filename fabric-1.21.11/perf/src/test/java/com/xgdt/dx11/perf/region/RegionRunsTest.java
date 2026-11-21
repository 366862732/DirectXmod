package com.xgdt.dx11.perf.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegionRunsTest {
    @Test
    void coalescesContiguousSpansIntoOneRun() {
        List<RegionRuns.Span> spans = List.of(
            new RegionRuns.Span(0, 320, 32, 60),
            new RegionRuns.Span(320, 640, 32, 120),
            new RegionRuns.Span(960, 64, 32, 12));

        List<RegionRuns.Run> runs = RegionRuns.coalesce(0, spans);

        assertEquals(1, runs.size());
        RegionRuns.Run run = runs.get(0);
        assertEquals(0, run.startVertex());
        assertEquals(192, run.indexCount());
        assertEquals(3, run.sliceCount());
    }

    @Test
    void breaksRunsAtGapsAndStrideChanges() {
        List<RegionRuns.Span> spans = List.of(
            new RegionRuns.Span(0, 320, 32, 60),
            new RegionRuns.Span(384, 320, 32, 60),
            new RegionRuns.Span(704, 240, 16, 36),
            new RegionRuns.Span(944, 240, 16, 36));

        List<RegionRuns.Run> runs = RegionRuns.coalesce(0, spans);

        assertEquals(3, runs.size());
        assertEquals(0, runs.get(0).startVertex());
        assertEquals(60, runs.get(0).indexCount());
        assertEquals(12, runs.get(1).startVertex());
        assertEquals(60, runs.get(1).indexCount());
        assertEquals(704 / 16, runs.get(2).startVertex());
        assertEquals(72, runs.get(2).indexCount());
    }

    @Test
    void skipsZeroSizedAndUnalignedSpans() {
        List<RegionRuns.Span> spans = List.of(
            new RegionRuns.Span(0, 320, 32, 60),
            new RegionRuns.Span(320, 0, 32, 0),
            new RegionRuns.Span(330, 320, 32, 60),
            new RegionRuns.Span(672, 32, 32, 6));

        List<RegionRuns.Run> runs = RegionRuns.coalesce(0, spans);

        // The zero-length span is ignored; the unaligned 330 offset is ignored;
        // the final aligned span forms its own run.
        assertEquals(2, runs.size());
        assertEquals(0, runs.get(0).startVertex());
        assertEquals(60, runs.get(0).indexCount());
        assertEquals(672 / 32, runs.get(1).startVertex());
        assertEquals(6, runs.get(1).indexCount());
    }

    @Test
    void breaksRunsWhenCompactnessChanges() {
        List<RegionRuns.Span> spans = List.of(
            new RegionRuns.Span(0, 384, 24, 144, true),
            new RegionRuns.Span(384, 384, 24, 144, true),
            new RegionRuns.Span(768, 320, 32, 60),
            new RegionRuns.Span(1088, 320, 32, 60));

        List<RegionRuns.Run> runs = RegionRuns.coalesce(0, spans);

        assertEquals(2, runs.size());
        RegionRuns.Run compact = runs.get(0);
        assertTrue(compact.compact());
        assertEquals(0, compact.startVertex());
        assertEquals(288, compact.indexCount());
        assertEquals(2, compact.sliceCount());

        RegionRuns.Run vanilla = runs.get(1);
        assertEquals(false, vanilla.compact());
        assertEquals(768 / 32, vanilla.startVertex());
        assertEquals(120, vanilla.indexCount());
        assertEquals(2, vanilla.sliceCount());
    }

    @Test
    void spanWithoutCompactDefaultsToVanillaLayout() {
        RegionRuns.Span span = new RegionRuns.Span(0, 320, 32, 60);

        assertEquals(false, span.compact());
    }

    @Test
    void emptyInputProducesNoRuns() {
        assertTrue(RegionRuns.coalesce(0, List.of()).isEmpty());
    }
}
