package com.xgdt.dx11.perf.immediate.sign;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx11.perf.immediate.ImmediateFlags;
import com.xgdt.dx11.perf.immediate.ImmediateStats;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide sign text atlas and slot cache.
 *
 * <p>Entries are kept for five seconds after their last use, matching the
 * reference implementation: signs in view refresh the timestamp every frame,
 * while signs that leave the frustum are evicted and their rectangle is
 * cleared.  The cache is keyed by {@link SignTextKey}, so identical text on
 * different signs shares one atlas slot.</p>
 *
 * <p>Everything here is render-thread state.  {@link #endFrame()} runs from the
 * frame flip and doubles as the point where the atlas is released when the
 * feature is toggled off or its configured size changes.</p>
 */
public final class SignTextCache implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    private static final long EXPIRE_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final int MAX_LOGGED_FAILURES = 4;

    private static SignTextCache instance;
    private static int nextAtlasId;
    private static int failureCount;

    private final int atlasSize;
    private final SignAtlas atlas;
    private final RenderType renderType;
    private final CachedOrthoProjectionMatrixBuffer projectionBuffer;
    private final GpuBufferSlice projectionMatrix;
    private final LinkedHashMap<SignTextKey, Entry> slots =
        new LinkedHashMap<>(256, 0.75F, true);

    private SignTextCache(int id, int atlasSize) {
        RenderSystem.assertOnRenderThread();
        this.atlasSize = atlasSize;
        this.atlas = new SignAtlas(id, atlasSize);
        this.renderType = RenderTypes.textPolygonOffset(this.atlas.identifier());
        this.projectionBuffer = new CachedOrthoProjectionMatrixBuffer(
            "gl4dx11 sign atlas " + id, -1000.0F, 1000.0F, true);
        this.projectionMatrix = this.projectionBuffer.getBuffer(atlasSize, atlasSize);
    }

    /**
     * Returns the cache, creating or resizing it if required.
     *
     * @throws RuntimeException when the GPU cannot create the render target;
     *         callers fall back to vanilla rendering
     */
    public static SignTextCache get() {
        int size = targetAtlasSize();
        SignTextCache cache = instance;
        if (cache != null && cache.atlasSize != size) {
            cache.close();
            instance = null;
            cache = null;
        }
        if (cache == null) {
            cache = new SignTextCache(nextAtlasId++, size);
            instance = cache;
        }
        return cache;
    }

    public SignAtlas atlas() {
        return this.atlas;
    }

    public RenderType renderType() {
        return this.renderType;
    }

    public GpuBufferSlice projectionMatrix() {
        return this.projectionMatrix;
    }

    /** Finds a resident slot and refreshes its five-second lease. */
    public SignAtlas.Slot lookup(SignTextKey key) {
        this.prune();
        Entry entry = this.slots.get(key);
        if (entry == null) {
            ImmediateStats.onSignLookup(false);
            return null;
        }
        entry.lastAccess = System.nanoTime();
        ImmediateStats.onSignLookup(true);
        return entry.slot;
    }

    public void store(SignTextKey key, SignAtlas.Slot slot) {
        this.slots.put(key, new Entry(slot, System.nanoTime()));
    }

    public void release(SignAtlas.Slot slot) {
        this.atlas.free(slot);
    }

    /**
     * Configured size, reduced to the device maximum when the driver reports
     * a smaller limit than the config allows.
     */
    private static int targetAtlasSize() {
        int size = ImmediateFlags.signAtlasSize();
        int max = RenderSystem.getDevice().getMaxTextureSize();
        if (max > 0 && size > max) {
            size = Integer.highestOneBit(max);
        }
        return size;
    }

    private void prune() {
        long cutoff = System.nanoTime() - EXPIRE_NANOS;
        Iterator<Map.Entry<SignTextKey, Entry>> iterator = this.slots.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (entry.lastAccess - cutoff < 0L) {
                this.atlas.free(entry.slot);
                iterator.remove();
                ImmediateStats.onSignEvict();
            }
        }
    }

    /**
     * Frame boundary.  Releases the atlas when the feature was disabled or
     * reconfigured and drops entries whose lease ran out.
     */
    public static void endFrame() {
        SignTextCache cache = instance;
        if (cache == null) {
            return;
        }
        if (!ImmediateFlags.signTextBuffering() || cache.atlasSize != targetAtlasSize()) {
            cache.close();
            instance = null;
            return;
        }
        cache.prune();
    }

    /** F3+T/language reload: cached pixels were rasterised with the old font. */
    public static void onFontReload() {
        reset();
    }

    public static void reset() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        failureCount = 0;
    }

    public static void reportFailure(Throwable failure) {
        failureCount++;
        if (failureCount <= MAX_LOGGED_FAILURES) {
            LOGGER.warn(
                "[dx11-perf] Sign text buffering failed ({} time(s)); using vanilla text rendering",
                failureCount,
                failure);
        }
    }

    public static int residentSlotCount() {
        return instance == null ? 0 : instance.slots.size();
    }

    @Override
    public void close() {
        if (instance == this) {
            instance = null;
        }
        this.slots.clear();
        this.projectionBuffer.close();
        this.atlas.close();
    }

    private static final class Entry {
        final SignAtlas.Slot slot;
        long lastAccess;

        Entry(SignAtlas.Slot slot, long lastAccess) {
            this.slot = slot;
            this.lastAccess = lastAccess;
        }
    }
}
