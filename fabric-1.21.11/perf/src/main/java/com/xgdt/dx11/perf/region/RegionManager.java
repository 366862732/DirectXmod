package com.xgdt.dx11.perf.region;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Owns the world's {@link Region} instances and the global arena budget.
 *
 * <p>The budget is the config's {@code regionArenaLimitMb}.  When it would
 * be exceeded, uploads fall back to vanilla per-section buffers instead of
 * growing an arena without bound.  All methods are render-thread only.</p>
 */
public final class RegionManager {
    private static final Map<RegionPos, Region> REGIONS = new HashMap<>();
    private static long arenaBytes;

    private RegionManager() {
    }

    public static @Nullable Region getOrCreate(RegionPos pos, RegionSize size) {
        Region region = REGIONS.get(pos);
        if (region != null) {
            return region;
        }
        Region created = new Region(pos, size);
        REGIONS.put(pos, created);
        return created;
    }

    public static int liveRegions() {
        return REGIONS.size();
    }

    public static long arenaBytes() {
        return arenaBytes;
    }

    public static long budgetBytes() {
        PerfConfig config = PerfState.config();
        return Math.max(0L, config.regionArenaLimitMb) * 1024L * 1024L;
    }

    public static long remainingBudgetBytes() {
        return Math.max(0L, budgetBytes() - arenaBytes);
    }

    /** Reserves arena capacity; returns false when the budget is exhausted. */
    public static boolean tryReserve(long bytes) {
        if (bytes <= 0) {
            return true;
        }
        long budget = budgetBytes();
        if (arenaBytes > budget - bytes) {
            return false;
        }
        arenaBytes += bytes;
        return true;
    }

    public static void release(long bytes) {
        if (bytes <= 0) {
            return;
        }
        arenaBytes = Math.max(0L, arenaBytes - bytes);
    }

    static void remove(Region region) {
        REGIONS.remove(region.pos(), region);
    }

    /**
     * Drops a region that ended up with no slices, for example because every
     * upload for a new region fell back to vanilla after the budget was
     * exhausted.  Its arenas (if any) are closed immediately.
     */
    public static void discardIfEmpty(Region region) {
        if (region == null || !region.isEmpty()) {
            return;
        }
        if (REGIONS.remove(region.pos(), region)) {
            region.closeNow();
        }
    }

    /**
     * Detaches every region without forcing its arenas closed.  Used when the
     * world changes and old section meshes may still be drawn for a frame; the
     * arenas close as soon as their slices are freed.
     */
    public static void clear() {
        RegionFrame.reset();
        List<Region> copy = new ArrayList<>(REGIONS.values());
        REGIONS.clear();
        for (Region region : copy) {
            region.detach();
        }
    }

    /** Force-closes every region.  Used on renderer disposal. */
    public static void closeAll() {
        RegionFrame.reset();
        List<Region> copy = new ArrayList<>(REGIONS.values());
        REGIONS.clear();
        for (Region region : copy) {
            region.closeNow();
        }
    }

    public static int liveSlices() {
        int total = 0;
        for (Region region : REGIONS.values()) {
            total += region.liveSlices();
        }
        return total;
    }

    public static int liveAllocations() {
        int total = 0;
        for (Region region : REGIONS.values()) {
            total += region.liveAllocations();
        }
        return total;
    }

    public static long translucentIndexBytes() {
        long total = 0;
        for (Region region : REGIONS.values()) {
            total += region.translucentIndexBytes();
        }
        return total;
    }

    public static Collection<Region> regions() {
        return List.copyOf(REGIONS.values());
    }

    public static String describe() {
        return "regions=" + liveRegions()
            + " slices=" + liveSlices()
            + " allocations=" + liveAllocations()
            + " arena=" + String.format(java.util.Locale.ROOT, "%.2f", arenaBytes / (1024.0 * 1024.0))
            + "/" + String.format(java.util.Locale.ROOT, "%.2f", budgetBytes() / (1024.0 * 1024.0))
            + " MiB"
            + " tindex=" + String.format(
                java.util.Locale.ROOT, "%.2f", translucentIndexBytes() / (1024.0 * 1024.0))
            + " MiB";
    }
}
