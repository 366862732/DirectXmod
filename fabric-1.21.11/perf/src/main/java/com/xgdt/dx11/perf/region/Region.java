package com.xgdt.dx11.perf.region;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * One region in the world: a {@link RegionSize} box of chunk sections that
 * owns one vertex arena per render layer.
 *
 * <p>Regions are reference-counted by the section vertex slices they hand out.
 * A region leaves {@link RegionManager} when the last slice is closed, which
 * also closes its arenas.  All methods run on the render thread.</p>
 */
public final class Region {
    private final RegionPos pos;
    private final RegionSize size;
    private final int originBlockX;
    private final int originBlockY;
    private final int originBlockZ;
    private final Map<ChunkSectionLayer, RegionBuffer> buffers =
        new EnumMap<>(ChunkSectionLayer.class);
    private final Map<ChunkSectionLayer, RegionTranslucentIndex> translucentIndices =
        new EnumMap<>(ChunkSectionLayer.class);

    private int liveSlices;
    private boolean detached;

    Region(RegionPos pos, RegionSize size) {
        this.pos = pos;
        this.size = size;
        this.originBlockX = pos.originBlockX(size);
        this.originBlockY = pos.originBlockY(size);
        this.originBlockZ = pos.originBlockZ(size);
    }

    public RegionPos pos() {
        return this.pos;
    }

    public RegionSize size() {
        return this.size;
    }

    public int originBlockX() {
        return this.originBlockX;
    }

    public int originBlockY() {
        return this.originBlockY;
    }

    public int originBlockZ() {
        return this.originBlockZ;
    }

    /**
     * Returns the layer arena, creating it when missing.
     *
     * @return the arena, or {@code null} when the global memory budget would be
     *         exceeded
     */
    public RegionBuffer getBuffer(ChunkSectionLayer layer) {
        RegionBuffer existing = this.buffers.get(layer);
        if (existing != null) {
            return existing.isClosed() ? null : existing;
        }

        long initial = initialCapacity(layer);
        if (!RegionManager.tryReserve(initial)) {
            return null;
        }
        try {
            RegionBuffer created = new RegionBuffer(this, layer, initial);
            this.buffers.put(layer, created);
            return created;
        } catch (RuntimeException e) {
            RegionManager.release(initial);
            return null;
        }
    }

    public RegionBuffer buffer(ChunkSectionLayer layer) {
        return this.buffers.get(layer);
    }

    /**
     * Returns the translucent sorted-index storage for this region, creating
     * it on first use.  The GPU buffer itself is allocated lazily on the first
     * sort.
     */
    public RegionTranslucentIndex getOrCreateTranslucentIndex(ChunkSectionLayer layer) {
        RegionTranslucentIndex existing = this.translucentIndices.get(layer);
        if (existing != null) {
            return existing;
        }
        RegionTranslucentIndex created = new RegionTranslucentIndex(this, layer);
        this.translucentIndices.put(layer, created);
        return created;
    }

    void retainSlice() {
        this.liveSlices++;
    }

    void releaseSlice() {
        if (this.liveSlices == 0) {
            return;
        }
        this.liveSlices--;
        if (this.liveSlices == 0) {
            RegionManager.remove(this);
            closeBuffers();
        }
    }

    /** Detaches from the manager; arenas close when the last slice is freed. */
    void detach() {
        this.detached = true;
        if (this.liveSlices == 0) {
            closeBuffers();
        }
    }

    /** Force-closes this region and its arenas regardless of live slices. */
    void closeNow() {
        this.detached = true;
        closeBuffers();
    }

    public boolean isDetached() {
        return this.detached;
    }

    public int liveSlices() {
        return this.liveSlices;
    }

    /** True when no section currently references this region's arenas. */
    public boolean isEmpty() {
        return this.liveSlices == 0;
    }

    public long arenaBytes() {
        long total = 0;
        for (RegionBuffer buffer : this.buffers.values()) {
            total += buffer.capacity();
        }
        return total;
    }

    public int liveAllocations() {
        int total = 0;
        for (RegionBuffer buffer : this.buffers.values()) {
            total += buffer.liveAllocations();
        }
        return total;
    }

    /** Capacity of the sorted translucent index buffers, in bytes. */
    public long translucentIndexBytes() {
        long total = 0;
        for (RegionTranslucentIndex index : this.translucentIndices.values()) {
            total += index.capacityBytes();
        }
        return total;
    }

    private void closeBuffers() {
        for (RegionBuffer buffer : this.buffers.values()) {
            try {
                buffer.close();
            } catch (RuntimeException ignored) {
                // The arena is already marked closed and its budget released;
                // a failed native release only waits for device teardown.
            }
        }
        this.buffers.clear();
        for (RegionTranslucentIndex index : this.translucentIndices.values()) {
            try {
                index.close();
            } catch (RuntimeException ignored) {
                // Device teardown already released it.
            }
        }
        this.translucentIndices.clear();
    }

    private static long initialCapacity(ChunkSectionLayer layer) {
        return switch (layer) {
            case SOLID -> 128L * 1024L;
            case CUTOUT -> 64L * 1024L;
            default -> 32L * 1024L;
        };
    }
}
