package com.xgdt.dx11.perf.region;

import java.util.ArrayList;
import java.util.List;

/**
 * Coalesces sorted region-arena slices into contiguous draw runs.
 *
 * <p>Region uploads pack whole sections back-to-back, so two visible sections
 * whose allocations touch can be drawn with a single indexed draw that starts
 * at the first section's base vertex.  Slices must be supplied in ascending
 * arena-offset order; this class is deliberately free of GPU state so the
 * coalescing rules can be unit-tested.</p>
 */
public final class RegionRuns {
    private RegionRuns() {
    }

    /** One visible slice, in arena byte order. */
    public record Span(
        long offset, long byteSize, int vertexStride, int indexCount, boolean compact) {
        public Span(long offset, long byteSize, int vertexStride, int indexCount) {
            this(offset, byteSize, vertexStride, indexCount, false);
        }
    }

    /**
     * A contiguous run that can be drawn with one call.
     *
     * <p>{@code startVertex} is relative to the start of the arena buffer and
     * {@code indexCount} is the sum of the slices' index counts.</p>
     */
    public record Run(
        int slot,
        long startOffset,
        int startVertex,
        int indexCount,
        int sliceCount,
        boolean compact) {
    }

    /**
     * Builds runs from spans that are already sorted by {@link Span#offset()}.
     * Span {@code slot} is not part of the span because one call only ever
     * coalesces draws that share a vertex-buffer slot; the caller passes it in.
     */
    public static List<Run> coalesce(int slot, List<Span> sortedSpans) {
        List<Run> runs = new ArrayList<>();
        if (sortedSpans.isEmpty()) {
            return runs;
        }

        long startOffset = 0L;
        long endOffset = 0L;
        int stride = 0;
        int indexCount = 0;
        int sliceCount = 0;
        boolean compact = false;

        for (Span span : sortedSpans) {
            if (span.byteSize() <= 0 || span.indexCount() <= 0
                || span.offset() < 0 || span.vertexStride() <= 0
                || span.offset() % span.vertexStride() != 0) {
                // An unusable span still separates the spans around it; do not
                // let a later span join across the hole.
                addRun(runs, slot, startOffset, stride, indexCount, sliceCount, compact);
                sliceCount = 0;
                continue;
            }

            boolean joins = sliceCount > 0
                && span.offset() == endOffset
                && span.vertexStride() == stride
                && span.compact() == compact;
            if (joins) {
                endOffset += span.byteSize();
                indexCount += span.indexCount();
                sliceCount++;
                continue;
            }

            addRun(runs, slot, startOffset, stride, indexCount, sliceCount, compact);
            startOffset = span.offset();
            endOffset = span.offset() + span.byteSize();
            stride = span.vertexStride();
            indexCount = span.indexCount();
            sliceCount = 1;
            compact = span.compact();
        }
        addRun(runs, slot, startOffset, stride, indexCount, sliceCount, compact);
        return runs;
    }

    private static void addRun(
        List<Run> runs,
        int slot,
        long startOffset,
        int stride,
        int indexCount,
        int sliceCount,
        boolean compact) {
        if (sliceCount <= 0 || indexCount <= 0 || stride <= 0) {
            return;
        }
        long startVertex = startOffset / stride;
        if (startVertex > Integer.MAX_VALUE) {
            return;
        }
        runs.add(new Run(slot, startOffset, (int) startVertex, indexCount, sliceCount, compact));
    }
}
