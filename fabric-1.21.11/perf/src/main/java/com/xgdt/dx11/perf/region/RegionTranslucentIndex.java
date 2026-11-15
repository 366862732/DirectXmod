package com.xgdt.dx11.perf.region;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One 32-bit, region-relative sorted index buffer for a translucent layer.
 *
 * <p>The buffer covers the currently visible section slices of one region.  It
 * is rebuilt only when the visible slice set changes or when the camera has
 * moved far enough to justify a resort.  Region submission ignores vanilla's
 * per-section translucent index buffers and binds this one instead.</p>
 *
 * <p>Render-thread only, like every other region resource.</p>
 */
public final class RegionTranslucentIndex implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final int USAGE = GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST;
    private static final int INITIAL_INDICES = 6 * 1024;
    private static final int MAX_INDICES = 8 * 1024 * 1024;
    private static final RegionVertexSlice[] NO_SLICES = new RegionVertexSlice[0];

    private final Region region;
    private final ChunkSectionLayer layer;

    private GpuBuffer buffer;
    private int capacity;
    private int indexCount;
    private boolean sorted;
    private int skippedFrames;
    private RegionVertexSlice[] lastSlices = NO_SLICES;
    private double sortX;
    private double sortY;
    private double sortZ;
    private float centerX;
    private float centerY;
    private float centerZ;

    RegionTranslucentIndex(Region region, ChunkSectionLayer layer) {
        this.region = region;
        this.layer = layer;
    }

    public Region region() {
        return this.region;
    }

    public ChunkSectionLayer layer() {
        return this.layer;
    }

    public GpuBuffer gpuBuffer() {
        return this.buffer;
    }

    public int indexCount() {
        return this.indexCount;
    }

    public boolean sorted() {
        return this.sorted;
    }

    public long capacityBytes() {
        return (long) this.capacity * Integer.BYTES;
    }

    /**
     * @return true when {@code slices} are the same live slices as the last
     *         sort, in the same arena order
     */
    public boolean matches(List<RegionVertexSlice> slices) {
        if (!this.sorted || this.lastSlices.length != slices.size()) {
            return false;
        }
        for (int i = 0; i < this.lastSlices.length; i++) {
            RegionVertexSlice previous = this.lastSlices[i];
            RegionVertexSlice current = slices.get(i);
            if (previous != current
                || previous.isClosed()
                || previous.startVertex() != current.startVertex()) {
                return false;
            }
        }
        return true;
    }

    /** Called after a successful CPU sort and GPU upload. */
    public void markSorted(
        List<RegionVertexSlice> slices,
        double cameraX,
        double cameraY,
        double cameraZ,
        float centerX,
        float centerY,
        float centerZ,
        int indexCount) {
        this.lastSlices = slices.toArray(new RegionVertexSlice[0]);
        this.sortX = cameraX;
        this.sortY = cameraY;
        this.sortZ = cameraZ;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.indexCount = indexCount;
        this.skippedFrames = 0;
        this.sorted = true;
    }

    /** Frames since the last rebuild, capped to avoid unbounded growth. */
    public int skippedFrames() {
        return this.skippedFrames;
    }

    /** Records that a camera-move rebuild was deferred by the sort budget. */
    public void noteSkippedFrame() {
        if (this.skippedFrames < 1_000) {
            this.skippedFrames++;
        }
    }

    /** Squared distance from the camera to the last sorted quad centroid. */
    public double centerDistanceSquared(double cameraX, double cameraY, double cameraZ) {
        double dx = this.region.originBlockX() + this.centerX - cameraX;
        double dy = this.region.originBlockY() + this.centerY - cameraY;
        double dz = this.region.originBlockZ() + this.centerZ - cameraZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public double lastSortX() {
        return this.sortX;
    }

    public double lastSortY() {
        return this.sortY;
    }

    public double lastSortZ() {
        return this.sortZ;
    }

    /**
     * Uploads a completed index list.  The buffer grows geometrically and
     * never shrinks, so the steady state allocates nothing.
     *
     * @return false when the GPU buffer could not be created or written
     */
    public boolean upload(ByteBuffer data, int count) {
        if (count <= 0) {
            this.indexCount = 0;
            this.sorted = true;
            return true;
        }
        if (this.capacity < count) {
            if (!grow(count)) {
                return false;
            }
        }
        try {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToBuffer(this.buffer.slice(0L, (long) count * Integer.BYTES), data);
            this.indexCount = count;
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("[dx11-perf] Could not upload translucent region indices: {}",
                e.toString());
            this.indexCount = 0;
            return false;
        }
    }

    private boolean grow(int required) {
        long target = Math.max(required, Math.max(INITIAL_INDICES, (long) this.capacity * 2L));
        target = Math.min(target, MAX_INDICES);
        if (target < required) {
            return false;
        }

        GpuBuffer grown;
        try {
            grown = RenderSystem.getDevice().createBuffer(
                () -> "dx11 translucent region indices",
                USAGE,
                target * Integer.BYTES);
        } catch (RuntimeException e) {
            LOGGER.warn("[dx11-perf] Could not create translucent region indices: {}",
                e.toString());
            return false;
        }

        GpuBuffer old = this.buffer;
        this.buffer = grown;
        this.capacity = (int) target;
        if (old != null) {
            try {
                old.close();
            } catch (RuntimeException ignored) {
                // The replacement is already live.
            }
        }
        return true;
    }

    @Override
    public void close() {
        this.sorted = false;
        this.indexCount = 0;
        this.skippedFrames = 0;
        this.lastSlices = NO_SLICES;
        if (this.buffer != null) {
            try {
                this.buffer.close();
            } catch (RuntimeException ignored) {
                // Device teardown already released it.
            }
            this.buffer = null;
        }
        this.capacity = 0;
    }
}
