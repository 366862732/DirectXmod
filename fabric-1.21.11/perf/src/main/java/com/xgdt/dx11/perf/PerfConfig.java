package com.xgdt.dx11.perf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xgdt.dx11.perf.region.RegionSize;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent configuration for the experimental D3D11 region renderer.
 *
 * <p>The file is deliberately separate from the early system-property
 * configuration in {@code Dx11Config}: the backend is selected automatically
 * on Windows when the native probe succeeds, while this file controls the perf
 * layer and can be edited while the game is closed.  A missing file is created
 * with conservative defaults; an unreadable/corrupt file falls back to defaults
 * and leaves the original file untouched so it can be inspected.</p>
 */
public final class PerfConfig {
    public static final String FILE_NAME = "gl4dx11-perf.json";
    /** Development override: enables the perf layer regardless of the file. */
    public static final String ENABLE_PROPERTY = "gl4dx11.perf.enabled";

    /**
     * Enabled by default.  The perf layer still requires a successful D3D11
     * probe, so the vanilla OpenGL fallback is never affected.  Use
     * {@code /dx11perf toggle} to disable the whole layer or
     * {@code /dx11perf performancemode off} to keep the overlay and counters
     * live while every optimization is turned off.
     */
    public boolean enabled = true;

    /**
     * Umbrella switch for the Sodium-style region renderer and the culling
     * features that feed it.  Turning this off leaves the ImmediatelyFast
     * layer and the debug tooling untouched.
     */
    public boolean regionEnabled = true;

    /** {@code REGION_4} (4×4×4) or {@code REGION_8} (8×4×8). */
    public String regionSize = "REGION_8";

    /** Replace per-section vertex buffers with region arenas. */
    public boolean regionUpload = true;
    /** Coalesce adjacent visible sections into region draws. */
    public boolean regionDraws = true;

    /** Apply the per-frame upload section/byte budget. */
    public boolean uploadBudget = true;
    public int uploadMaxSectionsPerFrame = 6;
    public int uploadMaxBytesPerFrame = 4 * 1024 * 1024;
    /** Soft cap on queued deferred upload bytes before the section cap is relaxed. */
    public int deferredUploadLimitMb = 32;
    /**
     * Total GPU capacity for region vertex arenas.  This is separate from the
     * per-frame deferred upload budget: arenas live for the whole world, while
     * deferred uploads are transient.  When the budget is exhausted, affected
     * sections fall back to vanilla per-section buffers.
     */
    public int regionArenaLimitMb = 256;

    public boolean compactVertices = true;
    /**
     * Attempt compact vertices even when the vanilla terrain interface looked
     * incompatible.  The compiled shader is still validated: a pack that
     * actually reads normal/other dropped inputs keeps 32-byte vertices.
     */
    public boolean compactVerticesForce = false;
    public int compactPositionBits = 16;

    public boolean translucentRegion = true;
    public int translucentSortBudget = 4;

    public boolean fogOcclusion = true;
    public boolean deferredOffscreenUpdates = true;
    public boolean entityCulling = true;
    public boolean visibleTextureAnimation = true;

    // Lithium-style client tick micro-optimizations.  Independent of the
    // region renderer and the ImmediatelyFast layer, but still part of the
    // perf layer's optimization set.
    /** Fastutil reference map plus a resolved-type fast path for entity queries. */
    public boolean fastEntityLookup = true;
    /** Skip ambient sound attempts for client-side mobs (Breeze excepted). */
    public boolean skipClientAmbientSounds = true;
    /** Skip the biome-particle attribute lookup when no particle can spawn. */
    public boolean fastAmbientParticles = true;
    /** Visit only the entity sections inside small entity-query boxes. */
    public boolean fastEntityQueries = true;

    // ImmediatelyFast-style non-terrain optimizations.  Each one is
    // independent so a regression can be isolated without rebuilding.
    /**
     * Umbrella switch for the whole ImmediatelyFast-style layer.  Turning this
     * off leaves the region renderer and the debug tooling untouched.
     */
    public boolean immediateEnabled = true;
    /** Reusable/ pooled entity, block-entity, particle and text draw buffers. */
    public boolean immediateBatching = true;
    /** Cache the last RenderType -> VertexConsumer lookup inside a text run. */
    public boolean fastTextLookup = true;
    /** Do not depth-sort translucent text quads. */
    public boolean skipTextSorting = true;
    /** Use a larger font atlas so high-resolution fonts do not thrash it. */
    public boolean fontAtlasResizing = true;
    public int fontAtlasSize = 1024;
    /** Spatial index for GUI layer/scissor hit tests. */
    public boolean fastGuiLayerLookup = true;
    /** Pack every loaded map into a shared atlas texture. */
    public boolean mapAtlasGeneration = true;
    public int mapAtlasSize = 2048;
    /** Cache sign text in a render-target atlas. */
    public boolean signTextBuffering = true;
    public int signAtlasSize = 4096;

    public boolean debugHud = false;
    public boolean debugOverlay = false;
    public boolean debugLog = false;

    public PerfConfig() {
    }

    public RegionSize regionSize() {
        return RegionSize.fromName(this.regionSize);
    }

    public boolean enabledByProperty() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    /** True when the perf layer should run, ignoring backend availability. */
    public boolean enabled() {
        return this.enabled || enabledByProperty();
    }

    /**
     * Loads the config, creating it when missing and validating/clamping every
     * value.  The returned warnings are intended for the mod logger.
     */
    public static LoadResult load(Path path) {
        List<String> warnings = new ArrayList<>();
        Gson gson = gson();

        if (path == null) {
            PerfConfig defaults = new PerfConfig();
            defaults.normalize(warnings);
            return new LoadResult(defaults, warnings, false);
        }

        if (!Files.isRegularFile(path)) {
            PerfConfig config = new PerfConfig();
            config.normalize(warnings);
            try {
                config.save(path);
            } catch (IOException e) {
                warnings.add("Could not create " + path + ": " + e.getMessage());
            }
            return new LoadResult(config, warnings, true);
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            PerfConfig config = gson.fromJson(json, PerfConfig.class);
            if (config == null) {
                warnings.add("Config file is empty; using defaults: " + path);
                config = new PerfConfig();
            }
            config.normalize(warnings);
            return new LoadResult(config, warnings, false);
        } catch (IOException | RuntimeException e) {
            warnings.add("Could not read " + path + " (" + e + "); using defaults");
            PerfConfig config = new PerfConfig();
            config.normalize(warnings);
            return new LoadResult(config, warnings, false);
        }
    }

    public void save(Path path) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String json = gson().toJson(this) + System.lineSeparator();
        Path temp = Files.createTempFile(parent, FILE_NAME, ".tmp");
        try {
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(
                    temp,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Normalizes enum names and clamps numeric values to safe ranges.  Returns
     * the same list it was passed, for convenient call sites.
     */
    public List<String> normalize(List<String> warnings) {
        if (!RegionSize.isKnown(this.regionSize)) {
            warnings.add("Unknown perf.regionSize '" + this.regionSize
                + "'; using REGION_8");
            this.regionSize = "REGION_8";
        } else {
            this.regionSize = this.regionSize().name();
        }

        this.uploadMaxSectionsPerFrame =
            clamp("perf.uploadMaxSectionsPerFrame", this.uploadMaxSectionsPerFrame, 0, 64, warnings);
        this.uploadMaxBytesPerFrame =
            clamp("perf.uploadMaxBytesPerFrame", this.uploadMaxBytesPerFrame, 0, 512 * 1024 * 1024, warnings);
        this.deferredUploadLimitMb =
            clamp("perf.deferredUploadLimitMb", this.deferredUploadLimitMb, 0, 4096, warnings);
        this.regionArenaLimitMb =
            clamp("perf.regionArenaLimitMb", this.regionArenaLimitMb, 0, 4096, warnings);
        this.compactPositionBits =
            clamp("perf.compactPositionBits", this.compactPositionBits, 16, 20, warnings);
        this.translucentSortBudget =
            clamp("perf.translucentSortBudget", this.translucentSortBudget, 0, 64, warnings);
        this.fontAtlasSize =
            clampAtlasSize("perf.fontAtlasSize", this.fontAtlasSize, 256, 8192, warnings);
        this.mapAtlasSize =
            clampAtlasSize("perf.mapAtlasSize", this.mapAtlasSize, 256, 8192, warnings);
        this.signAtlasSize =
            clampAtlasSize("perf.signAtlasSize", this.signAtlasSize, 512, 8192, warnings);
        return warnings;
    }

    /** Atlas sizes must be powers of two for mip/quadtree assumptions. */
    private static int clampAtlasSize(
        String name, int value, int min, int max, List<String> warnings) {
        int clamped = Math.max(min, Math.min(max, value));
        int power = Integer.highestOneBit(clamped);
        if (power != clamped) {
            power = power << 1;
        }
        if (power > max) {
            power = Integer.highestOneBit(max);
        }
        if (power != value) {
            warnings.add(name + "=" + value + " is not a supported power of two [" + min
                + ", " + max + "]; using " + power);
        }
        return power;
    }

    private static int clamp(String name, int value, int min, int max, List<String> warnings) {
        if (value < min || value > max) {
            int clamped = Math.max(min, Math.min(max, value));
            warnings.add(name + "=" + value + " is out of range [" + min + ", " + max
                + "]; using " + clamped);
            return clamped;
        }
        return value;
    }

    private static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /** Result of a config load, including warnings for the logger. */
    public record LoadResult(PerfConfig config, List<String> warnings, boolean created) {
    }
}
