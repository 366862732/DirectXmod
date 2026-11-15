package com.xgdt.dx11.perf.region;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Copies a compiled section mesh into a region-relative vertex buffer.
 *
 * <p>Vanilla terrain vertices store positions relative to the section origin.
 * A region draw supplies one region-origin uniform, so each vertex position
 * must be shifted by {@code sectionOrigin - regionOrigin} before it is packed
 * into the shared arena.  Positions are three native-order floats; the rest of
 * the vertex is copied verbatim.  This is deliberately a small, pure helper so
 * the arithmetic can be tested without a GPU.</p>
 */
public final class RegionVertexRebase {
    private RegionVertexRebase() {
    }

    /**
     * Copies {@code byteSize} bytes from {@code source} (starting at its
     * current position) into {@code target} and adds {@code delta} to every
     * vertex position.
     *
     * <p>{@code target} must be a direct native-order buffer of at least
     * {@code byteSize} bytes.  On return its position is 0 and its limit is
     * {@code byteSize}, ready for an upload.</p>
     */
    public static void rebase(
        ByteBuffer source,
        ByteBuffer target,
        int byteSize,
        int stride,
        int positionOffset,
        float dx,
        float dy,
        float dz) {
        if (byteSize < 0 || stride <= 0 || positionOffset < 0
            || positionOffset + 12 > stride) {
            throw new IllegalArgumentException("Invalid vertex layout for rebase");
        }
        if (stride % 4 != 0 || byteSize % stride != 0) {
            throw new IllegalArgumentException("Vertex data is not stride-aligned");
        }
        if (target.capacity() < byteSize) {
            throw new IllegalArgumentException("Rebase target is too small");
        }

        ByteBuffer src = source.duplicate();
        int start = src.position();
        if (src.remaining() < byteSize) {
            throw new IllegalArgumentException("Source has fewer than " + byteSize + " bytes");
        }
        src.limit(start + byteSize);

        target.clear();
        target.order(ByteOrder.nativeOrder());
        target.put(src);
        for (int base = 0; base < byteSize; base += stride) {
            int p = base + positionOffset;
            target.putFloat(p, target.getFloat(p) + dx);
            target.putFloat(p + 4, target.getFloat(p + 4) + dy);
            target.putFloat(p + 8, target.getFloat(p + 8) + dz);
        }
        target.position(0);
        target.limit(byteSize);
    }
}
