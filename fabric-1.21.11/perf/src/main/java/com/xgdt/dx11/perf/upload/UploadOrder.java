package com.xgdt.dx11.perf.upload;

import java.util.Arrays;
import java.util.List;

/**
 * Pure ordering and budgeting rules for the deferred section-upload queue.
 *
 * <p>Vanilla drains every queued upload in one frame.  On a burst (teleport,
 * render-distance change, world load) that can submit dozens of vertex/index
 * writes in a single frame.  The region renderer keeps those uploads in the
 * dispatcher queue and runs only a bounded prefix per frame; this class decides
 * the prefix.  It deliberately has no GPU/Minecraft dependencies so the rules
 * can be unit-tested on the Linux host.</p>
 */
public final class UploadOrder {
    private UploadOrder() {
    }

    /**
     * One pending upload.
     *
     * @param prioritized true for a section upload (ranked by distance); false
     *                    for small bookkeeping uploads that should run first
     * @param visible     true when the section was in the frame's visible set;
     *                    visible uploads are ranked before off-screen ones
     * @param centerX     section-origin block coordinate
     * @param sequence    insertion sequence, used as a stable tie-breaker
     * @param estimatedBytes estimated vertex+index bytes of the upload
     */
    public record Entry(
        boolean prioritized,
        boolean visible,
        double centerX,
        double centerY,
        double centerZ,
        long sequence,
        long estimatedBytes) {
        /** Convenience constructor for uploads with no visibility snapshot. */
        public Entry(
            boolean prioritized,
            double centerX,
            double centerY,
            double centerZ,
            long sequence,
            long estimatedBytes) {
            this(prioritized, true, centerX, centerY, centerZ, sequence, estimatedBytes);
        }
    }

    /** Result of prefix selection over a ranked entry list. */
    public record Selection(int[] indexes, int count, long bytes, boolean capped) {
        public boolean isSelected(int rank) {
            return rank < this.count;
        }
    }

    /**
     * Ranks entries for one frame: non-prioritized bookkeeping first (queue
     * order), then visible section uploads from nearest to farthest, then
     * off-screen section uploads from nearest to farthest.  Insertion order is
     * the stable tie-breaker.
     */
    public static int[] rank(
        List<Entry> entries, double cameraX, double cameraY, double cameraZ) {
        Integer[] order = new Integer[entries.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (left, right) -> {
            Entry a = entries.get(left);
            Entry b = entries.get(right);
            if (a.prioritized() != b.prioritized()) {
                return a.prioritized() ? 1 : -1;
            }
            if (a.prioritized()) {
                if (a.visible() != b.visible()) {
                    return a.visible() ? -1 : 1;
                }
                double distanceA = distanceSquared(a, cameraX, cameraY, cameraZ);
                double distanceB = distanceSquared(b, cameraX, cameraY, cameraZ);
                int byDistance = Double.compare(distanceA, distanceB);
                if (byDistance != 0) {
                    return byDistance;
                }
            }
            return Long.compare(a.sequence(), b.sequence());
        });

        int[] ranked = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            ranked[i] = order[i];
        }
        return ranked;
    }

    /**
     * Selects the longest prefix of {@code ranked} that stays inside the
     * per-frame caps.  Zero means "no cap".  At least one entry is always
     * selected when the list is non-empty, so a single oversized upload can
     * never stall the queue forever.
     */
    public static Selection select(
        List<Entry> entries, int[] ranked, int maxSections, long maxBytes) {
        int count = 0;
        long bytes = 0L;
        boolean capped = false;

        for (int rank = 0; rank < ranked.length; rank++) {
            Entry entry = entries.get(ranked[rank]);
            if (count > 0
                && ((maxSections > 0 && count >= maxSections)
                    || (maxBytes > 0 && bytes >= maxBytes))) {
                capped = true;
                break;
            }
            count++;
            bytes += Math.max(0L, entry.estimatedBytes());
        }

        return new Selection(Arrays.copyOf(ranked, count), count, bytes, capped);
    }

    private static double distanceSquared(
        Entry entry, double cameraX, double cameraY, double cameraZ) {
        // Compare section centers (the entry stores the section origin).
        double dx = entry.centerX() + 8.0 - cameraX;
        double dy = entry.centerY() + 8.0 - cameraY;
        double dz = entry.centerZ() + 8.0 - cameraZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
