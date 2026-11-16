package com.xgdt.dx11.perf;

import com.xgdt.dx11.metrics.Dx11Metrics;

import com.xgdt.dx11.perf.overlay.PerfOverlayStats;
import com.xgdt.dx11.perf.region.RegionManager;
import com.xgdt.dx11.perf.region.RegionSize;
import java.util.Locale;

/**
 * Local {@code /dx11perf} command implementation.
 *
 * <p>The command is intercepted before it reaches the server.  It deliberately
 * returns plain text so it can be unit-tested and so the mixin owns the chat
 * formatting.</p>
 */
public final class PerfCommand {
    public static final String NAME = "dx11perf";

    private PerfCommand() {
    }

    public static boolean isPerfCommand(String rawCommand) {
        if (rawCommand == null) {
            return false;
        }
        String command = rawCommand.strip();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        return command.equals(NAME) || command.startsWith(NAME + " ");
    }

    /** Executes a command and returns newline-separated chat feedback. */
    public static String execute(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.strip();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String[] tokens = command.isEmpty() ? new String[0] : command.split("\\s+");
        if (tokens.length == 0 || !tokens[0].equals(NAME)) {
            return help();
        }
        if (tokens.length == 1) {
            return help();
        }

        String action = tokens[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "stats", "status" -> stats();
            case "reset" -> reset();
            case "leaks" -> leaks();
            case "debugoverlay", "overlay" -> debugOverlay(tokens);
            case "performancemode", "perfmode", "performance" -> performanceMode(tokens);
            case "toggle" -> tokens.length >= 3 ? toggle(tokens[2]) : toggleEnabled();
            case "reload" -> reload();
            case "save" -> save();
            case "regionsize", "region" -> tokens.length >= 3 ? regionSize(tokens[2]) : help();
            case "help", "?" -> help();
            default -> "Unknown /" + NAME + " subcommand: " + action + "\n" + help();
        };
    }

    /** True when a command changes vertex/region state and needs a full rebuild. */
    public static boolean requiresChunkRebuild(String rawCommand) {
        String command = rawCommand == null ? "" : rawCommand.strip();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        String[] tokens = command.isEmpty() ? new String[0] : command.split("\\s+");
        if (tokens.length < 2 || !tokens[0].equals(NAME)) {
            return false;
        }
        String action = tokens[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "performancemode", "perfmode", "performance" -> true;
            case "regionsize", "region", "reload" -> true;
            case "toggle" -> {
                if (tokens.length < 3) {
                    yield true;
                }
                String flag = tokens[2].toLowerCase(Locale.ROOT);
                yield !(flag.equals("debughud")
                    || flag.equals("hud")
                    || flag.equals("debugoverlay")
                    || flag.equals("overlay")
                    || flag.equals("debuglog")
                    || flag.equals("log")
                    || flag.equals("immediateenabled")
                    || flag.equals("immediate")
                    || flag.equals("if")
                    || flag.equals("uploadbudget")
                    || flag.equals("budget")
                    || flag.equals("fogocclusion")
                    || flag.equals("fog")
                    || flag.equals("deferredoffscreenupdates")
                    || flag.equals("deferred")
                    || flag.equals("entityculling")
                    || flag.equals("entities")
                    || flag.equals("fastentitylookup")
                    || flag.equals("fastentities")
                    || flag.equals("skipclientambientsounds")
                    || flag.equals("ambientsounds")
                    || flag.equals("fastambientparticles")
                    || flag.equals("particles")
                    || flag.equals("fastentityqueries")
                    || flag.equals("entityqueries")
                    || flag.equals("faststitcher")
                    || flag.equals("stitcher"));
            }
            default -> false;
        };
    }

    private static String stats() {
        PerfConfig config = PerfState.config();
        Dx11Metrics.Snapshot snapshot = Dx11Metrics.snapshot();
        return "DX11 perf | active=" + PerfState.active()
            + " layer=" + PerfState.layerActive()
            + " immediate=" + PerfState.immediateActive()
            + " region=" + PerfState.regionActive()
            + " enabled=" + config.enabled
            + " property=" + config.enabledByProperty()
            + " immediateEnabled=" + config.immediateEnabled
            + " regionEnabled=" + config.regionEnabled
            + " performanceMode=" + allPerformanceFeaturesEnabled(config)
            + " regionSize=" + config.regionSize
            + " upload=" + config.regionUpload
            + " regionDraws=" + config.regionDraws
            + " compact=" + config.compactVertices
            + " translucentRegion=" + config.translucentRegion
            + " fogOcclusion=" + config.fogOcclusion
            + " deferredOffscreen=" + config.deferredOffscreenUpdates
            + " entityCulling=" + config.entityCulling
            + " fastEntityLookup=" + config.fastEntityLookup
            + " skipClientAmbientSounds=" + config.skipClientAmbientSounds
            + " fastAmbientParticles=" + config.fastAmbientParticles
            + " fastEntityQueries=" + config.fastEntityQueries
            + " fastStitcher=" + config.fastStitcher
            + "\n" + snapshot.format()
            + "\n" + RegionManager.describe()
            + "\n" + com.xgdt.dx11.terrain.CompactTerrainFormat.describe()
            + "\n" + com.xgdt.dx11.perf.region.RegionTranslucentSorter.describe()
            + "\n" + com.xgdt.dx11.perf.upload.UploadScheduler.describe()
            + "\n" + com.xgdt.dx11.perf.culling.FogOcclusion.describe()
            + "\n" + com.xgdt.dx11.perf.culling.EntityCulling.describe()
            + "\n" + MicroStats.describe()
            + "\n" + com.xgdt.dx11.perf.texture.StitcherStats.describe()
            + "\n" + com.xgdt.dx11.perf.immediate.ImmediateStats.describe()
            + "\nconfig=" + PerfState.configPath();
    }

    private static String reset() {
        Dx11Metrics.reset();
        com.xgdt.dx11.terrain.CompactTerrainFormat.resetStats();
        com.xgdt.dx11.perf.culling.FogOcclusion.resetStats();
        com.xgdt.dx11.perf.culling.EntityCulling.resetStats();
        MicroStats.reset();
        com.xgdt.dx11.perf.texture.StitcherStats.reset();
        com.xgdt.dx11.perf.upload.UploadScheduler.resetStats();
        com.xgdt.dx11.perf.immediate.ImmediateStats.reset();
        return "DX11 perf counters reset.";
    }

    private static String leaks() {
        return "DX11 perf " + RegionManager.describe()
            + (PerfState.active() ? "" : " (perf layer inactive)");
    }

    private static String debugOverlay(String[] tokens) {
        PerfConfig config = PerfState.config();
        Boolean requested;
        if (tokens.length < 3) {
            requested = Boolean.valueOf(!config.debugOverlay);
        } else {
            String raw = tokens[2].toLowerCase(Locale.ROOT);
            requested = raw.equals("toggle") ? Boolean.valueOf(!config.debugOverlay) : parseBoolean(raw);
        }
        if (requested == null) {
            return "DX11 perf debugOverlay is " + config.debugOverlay
                + ". Usage: /dx11perf debugOverlay [on|off|toggle]";
        }
        config.debugOverlay = requested;
        PerfState.save();
        PerfState.refresh();
        PerfOverlayStats.resetGpuQuery();
        return "DX11 perf debugOverlay=" + config.debugOverlay + " (saved)"
            + (PerfState.layerActive() ? "" : " - perf layer inactive")
            + (config.immediateEnabled ? "" : " (immediate layer off)");
    }

    private static String performanceMode(String[] tokens) {
        PerfConfig config = PerfState.config();
        boolean current = allPerformanceFeaturesEnabled(config);
        Boolean requested;
        if (tokens.length < 3) {
            requested = Boolean.valueOf(!current);
        } else {
            String raw = tokens[2].toLowerCase(Locale.ROOT);
            requested = raw.equals("toggle") ? Boolean.valueOf(!current) : parseBoolean(raw);
        }
        if (requested == null) {
            return "DX11 performancemode is " + current
                + ". Usage: /dx11perf performancemode [on|off|toggle]";
        }
        String result = applyPerformanceMode(config, requested);
        PerfState.save();
        PerfState.refresh();
        PerfOverlayStats.resetGpuQuery();
        return result + " (saved). Region changes require a world reload/full chunk rebuild.";
    }

    /**
     * Switches every optimization feature in one operation.  Disabling the
     * performance mode deliberately leaves {@code enabled} alone so the
     * overlay and counters stay live for vanilla-vs-performance comparisons.
     */
    static String applyPerformanceMode(PerfConfig config, boolean enabled) {
        if (enabled) {
            config.enabled = true;
        }
        config.regionEnabled = enabled;
        config.regionUpload = enabled;
        config.regionDraws = enabled;
        config.uploadBudget = enabled;
        config.compactVertices = enabled;
        config.translucentRegion = enabled;
        config.fogOcclusion = enabled;
        config.deferredOffscreenUpdates = enabled;
        config.entityCulling = enabled;
        config.fastEntityLookup = enabled;
        config.skipClientAmbientSounds = enabled;
        config.fastAmbientParticles = enabled;
        config.fastEntityQueries = enabled;
        config.fastStitcher = enabled;
        config.visibleTextureAnimation = enabled;
        config.immediateEnabled = enabled;
        config.immediateBatching = enabled;
        config.fastTextLookup = enabled;
        config.skipTextSorting = enabled;
        config.fontAtlasResizing = enabled;
        config.fastGuiLayerLookup = enabled;
        config.mapAtlasGeneration = enabled;
        config.signTextBuffering = enabled;
        return "DX11 performancemode=" + enabled
            + (enabled
                ? " - all region and immediate features enabled"
                : " - all optimization features disabled; instrumentation unchanged");
    }

    /** True when the perf layer and every optimization feature are enabled. */
    static boolean allPerformanceFeaturesEnabled(PerfConfig config) {
        return config.enabled
            && config.regionEnabled
            && config.regionUpload
            && config.regionDraws
            && config.uploadBudget
            && config.compactVertices
            && config.translucentRegion
            && config.fogOcclusion
            && config.deferredOffscreenUpdates
            && config.entityCulling
            && config.fastEntityLookup
            && config.skipClientAmbientSounds
            && config.fastAmbientParticles
            && config.fastEntityQueries
            && config.fastStitcher
            && config.visibleTextureAnimation
            && config.immediateEnabled
            && config.immediateBatching
            && config.fastTextLookup
            && config.skipTextSorting
            && config.fontAtlasResizing
            && config.fastGuiLayerLookup
            && config.mapAtlasGeneration
            && config.signTextBuffering;
    }

    private static Boolean parseBoolean(String raw) {
        return switch (raw) {
            case "on", "true", "yes", "enable", "enabled" -> Boolean.TRUE;
            case "off", "false", "no", "disable", "disabled" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static String toggleEnabled() {
        PerfConfig config = PerfState.config();
        boolean target = !config.enabled;
        boolean active = PerfState.setEnabled(target);
        String note = config.enabledByProperty()
            ? " (-Dgl4dx11.perf.enabled=true is still forcing it on)"
            : "";
        return "DX11 perf enabled=" + target + " active=" + active + note
            + ". Region rendering changes require a world reload/full chunk rebuild.";
    }

    private static String toggle(String rawFlag) {
        PerfConfig config = PerfState.config();
        String flag = rawFlag.toLowerCase(Locale.ROOT);
        Boolean value = switch (flag) {
            case "debughud", "hud" -> {
                config.debugHud = !config.debugHud;
                yield config.debugHud;
            }
            case "debugoverlay", "overlay" -> {
                config.debugOverlay = !config.debugOverlay;
                yield config.debugOverlay;
            }
            case "debuglog", "log" -> {
                config.debugLog = !config.debugLog;
                yield config.debugLog;
            }
            case "regionenabled", "sodium", "renderer" -> {
                config.regionEnabled = !config.regionEnabled;
                yield config.regionEnabled;
            }
            case "regionupload", "upload" -> {
                config.regionUpload = !config.regionUpload;
                yield config.regionUpload;
            }
            case "regiondraws", "draws" -> {
                config.regionDraws = !config.regionDraws;
                yield config.regionDraws;
            }
            case "uploadbudget", "budget" -> {
                config.uploadBudget = !config.uploadBudget;
                yield config.uploadBudget;
            }
            case "compactvertices", "compact" -> {
                config.compactVertices = !config.compactVertices;
                yield config.compactVertices;
            }
            case "compactverticesforce", "forcecompact" -> {
                config.compactVerticesForce = !config.compactVerticesForce;
                yield config.compactVerticesForce;
            }
            case "translucentregion", "translucent" -> {
                config.translucentRegion = !config.translucentRegion;
                yield config.translucentRegion;
            }
            case "fogocclusion", "fog" -> {
                config.fogOcclusion = !config.fogOcclusion;
                yield config.fogOcclusion;
            }
            case "deferredoffscreenupdates", "deferred" -> {
                config.deferredOffscreenUpdates = !config.deferredOffscreenUpdates;
                yield config.deferredOffscreenUpdates;
            }
            case "entityculling", "entities" -> {
                config.entityCulling = !config.entityCulling;
                yield config.entityCulling;
            }
            case "fastentitylookup", "fastentities" -> {
                config.fastEntityLookup = !config.fastEntityLookup;
                yield config.fastEntityLookup;
            }
            case "skipclientambientsounds", "ambientsounds" -> {
                config.skipClientAmbientSounds = !config.skipClientAmbientSounds;
                yield config.skipClientAmbientSounds;
            }
            case "fastambientparticles", "particles" -> {
                config.fastAmbientParticles = !config.fastAmbientParticles;
                yield config.fastAmbientParticles;
            }
            case "fastentityqueries", "entityqueries" -> {
                config.fastEntityQueries = !config.fastEntityQueries;
                yield config.fastEntityQueries;
            }
            case "faststitcher", "stitcher" -> {
                config.fastStitcher = !config.fastStitcher;
                yield config.fastStitcher;
            }
            case "visibletextureanimation", "textures" -> {
                config.visibleTextureAnimation = !config.visibleTextureAnimation;
                yield config.visibleTextureAnimation;
            }
            case "immediateenabled", "immediate", "if" -> {
                config.immediateEnabled = !config.immediateEnabled;
                yield config.immediateEnabled;
            }
            case "immediatebatching", "batching" -> {
                config.immediateBatching = !config.immediateBatching;
                yield config.immediateBatching;
            }
            case "fasttextlookup", "textlookup" -> {
                config.fastTextLookup = !config.fastTextLookup;
                yield config.fastTextLookup;
            }
            case "skiptextsorting", "textsort" -> {
                config.skipTextSorting = !config.skipTextSorting;
                yield config.skipTextSorting;
            }
            case "fontatlasresizing", "fontatlas" -> {
                config.fontAtlasResizing = !config.fontAtlasResizing;
                yield config.fontAtlasResizing;
            }
            case "fastguilayerlookup", "guilookup" -> {
                config.fastGuiLayerLookup = !config.fastGuiLayerLookup;
                yield config.fastGuiLayerLookup;
            }
            case "mapatlasgeneration", "mapatlas" -> {
                config.mapAtlasGeneration = !config.mapAtlasGeneration;
                yield config.mapAtlasGeneration;
            }
            case "signtextbuffering", "signcache" -> {
                config.signTextBuffering = !config.signTextBuffering;
                yield config.signTextBuffering;
            }
            default -> null;
        };
        if (value == null) {
            return "Unknown perf flag: " + rawFlag + "\n" + help();
        }
        PerfState.save();
        PerfState.refresh();
        if (flag.equals("debugoverlay") || flag.equals("overlay")) {
            PerfOverlayStats.resetGpuQuery();
        }
        return "DX11 perf " + flag + "=" + value + " (saved)";
    }

    private static String regionSize(String rawSize) {
        if (!RegionSize.isKnown(rawSize)) {
            return "Unknown region size: " + rawSize + " (expected REGION_4 or REGION_8)";
        }
        PerfConfig config = PerfState.config();
        config.regionSize = RegionSize.fromName(rawSize).name();
        PerfState.save();
        return "DX11 perf regionSize=" + config.regionSize
            + " (requires a world reload/full chunk rebuild)";
    }

    private static String reload() {
        PerfState.reload();
        return "DX11 perf config reloaded from " + PerfState.configPath();
    }

    private static String save() {
        PerfState.save();
        return "DX11 perf config saved to " + PerfState.configPath();
    }

    private static String help() {
        return """
            DX11 perf commands:
              /dx11perf stats | reset | leaks | reload | save
              /dx11perf debugOverlay [on|off] - toggle the FPS/CPU/GPU overlay
              /dx11perf performancemode [on|off|toggle] - all region, immediate and micro features
                                            (off keeps the overlay and counters live)
              /dx11perf toggle            - enable/disable the perf layer
              /dx11perf toggle <flag>     - immediateEnabled (or immediate/if),
                                            regionEnabled (or sodium/renderer),
                                            debugHud, debugOverlay, debugLog, regionUpload, regionDraws,
                                            uploadBudget, compactVertices,
                                            compactVerticesForce, translucentRegion,
                                            fogOcclusion, deferredOffscreenUpdates,
                                            entityCulling, fastEntityLookup,
                                            skipClientAmbientSounds, fastAmbientParticles,
                                            fastEntityQueries,
                                            fastStitcher,
                                            visibleTextureAnimation,
                                            immediateBatching, fastTextLookup,
                                            skipTextSorting, fontAtlasResizing,
                                            fastGuiLayerLookup, mapAtlasGeneration,
                                            signTextBuffering
              /dx11perf regionSize <REGION_4|REGION_8>
            The perf layer is experimental and requires the D3D11 backend.""";
    }
}
