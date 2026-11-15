package com.xgdt.dx11.perf;

import com.xgdt.dx11.config.Dx11Config;
import java.io.IOException;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime state for the experimental perf layer.
 *
 * <p>The perf feature is only active when all three conditions hold:</p>
 * <ol>
 *   <li>it is enabled in {@code config/gl4dx11-perf.json} (or via
 *       {@code -Dgl4dx11.perf.enabled=true}),</li>
 *   <li>the D3D11 backend probed successfully,</li>
 *   <li>the game is running on Windows (the D3D11 backend is Windows-only).</li>
 * </ol>
 *
 * <p>When inactive, every hook checks {@link #active()} and takes the vanilla
 * path.  Initialization is lazy so the command and debug-screen mixins can be
 * loaded before the client entrypoint runs.</p>
 */
public final class PerfState {
    private static final Logger LOGGER = LoggerFactory.getLogger("gl4dx11");
    public static final String DEBUG_PROPERTY = "gl4dx11.perf.debug";
    /**
     * Validation-only switch: runs the ImmediatelyFast-style layer even when
     * the D3D11 backend is unavailable (for example on a Linux GL smoke test).
     * The region renderer and other backend-gated features stay disabled.
     */
    public static final String FORCE_IMMEDIATE_PROPERTY = "gl4dx11.perf.forceImmediate";

    private static volatile PerfConfig config;
    private static volatile Path configPath;
    private static volatile boolean active;
    private static volatile boolean layerActive;
    private static volatile boolean initialized;

    private PerfState() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(PerfConfig.FILE_NAME);
        } catch (Throwable t) {
            LOGGER.warn("[dx11-perf] Could not resolve the config directory: {}", t.toString());
            configPath = null;
        }

        PerfConfig.LoadResult result = PerfConfig.load(configPath);
        config = result.config();
        for (String warning : result.warnings()) {
            LOGGER.warn("[dx11-perf] {}", warning);
        }
        if (result.created()) {
            LOGGER.info("[dx11-perf] Created {}", configPath);
        }
        LOGGER.info(
            "[dx11-perf] Config loaded: enabled={} regionSize={} (file: {})",
            config.enabled,
            config.regionSize,
            configPath);
        refresh();
    }

    public static PerfConfig config() {
        init();
        return config;
    }

    public static Path configPath() {
        init();
        return configPath;
    }

    /** True only when the perf layer is enabled and the D3D11 backend is live. */
    public static boolean active() {
        init();
        return active;
    }

    /**
     * True when the perf instrumentation is live, including the validation-only
     * GL override.  The overlay uses this so it can be inspected during GL
     * smoke tests where the D3D11 backend is absent.
     */
    public static boolean layerActive() {
        init();
        return layerActive;
    }

    /**
     * True when the ImmediatelyFast-style layer itself is live.  This adds the
     * {@code immediateEnabled} umbrella on top of {@link #layerActive()}, so a
     * single config flag can disable every batched-buffer, text, GUI, map and
     * sign feature without touching the region renderer.
     */
    public static boolean immediateActive() {
        init();
        PerfConfig current = config;
        return layerActive && current != null && current.immediateEnabled;
    }

    /**
     * True when the Sodium-style region renderer and its culling helpers may
     * run.  The {@code regionEnabled} umbrella disables all of them while the
     * ImmediatelyFast layer, overlay and counters stay live.
     */
    public static boolean regionActive() {
        init();
        PerfConfig current = config;
        return active && current != null && current.regionEnabled;
    }

    /**
     * Re-evaluates backend availability after a config/property change.  This
     * is cheap and safe to call from the render thread.
     */
    public static void refresh() {
        PerfConfig current = config;
        if (current == null) {
            return;
        }

        boolean backendActive;
        try {
            backendActive = Dx11Config.shouldUseDx11();
        } catch (Throwable t) {
            LOGGER.warn("[dx11-perf] D3D11 availability check failed: {}", t.toString());
            backendActive = false;
        }

        boolean wasActive = active;
        active = current.enabled() && backendActive;
        boolean forceImmediate = current.enabled()
            && !active
            && Boolean.getBoolean(FORCE_IMMEDIATE_PROPERTY);
        layerActive = active || forceImmediate;
        PerfCounters.setEnabled(layerActive);
        com.xgdt.dx11.perf.immediate.ImmediateFlags.apply(layerActive, current);
        if (forceImmediate) {
            LOGGER.info("[dx11-perf] Immediate feature layer forced on for validation");
        }
        if (active != wasActive) {
            if (active) {
                LOGGER.info(
                    "[dx11-perf] Enabled: regionSize={} upload={} draws={}",
                    current.regionSize,
                    current.regionUpload,
                    current.regionDraws);
            } else {
                LOGGER.info("[dx11-perf] Disabled");
            }
        }
    }

    public static boolean setEnabled(boolean enabled) {
        PerfConfig current = config();
        current.enabled = enabled;
        save();
        refresh();
        return active();
    }

    /** Reloads the JSON file from disk and re-applies the active state. */
    public static void reload() {
        PerfConfig.LoadResult result = PerfConfig.load(configPath);
        config = result.config();
        for (String warning : result.warnings()) {
            LOGGER.warn("[dx11-perf] {}", warning);
        }
        refresh();
    }

    public static void save() {
        PerfConfig current = config;
        if (current == null) {
            return;
        }
        try {
            current.save(configPath);
        } catch (IOException e) {
            LOGGER.warn("[dx11-perf] Could not save {}: {}", configPath, e.toString());
        }
    }
}
