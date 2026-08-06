/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  io.netty.util.ResourceLeakDetector
 *  io.netty.util.ResourceLeakDetector$Level
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.ResourceLeakDetector;
import java.time.Duration;
import net.minecraft.DetectedVersion;
import net.minecraft.SuppressForbidden;
import net.minecraft.WorldVersion;
import net.minecraft.commands.BrigadierExceptions;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

@SuppressForbidden(reason="System.out needed before bootstrap")
public class SharedConstants {
    @Deprecated
    public static final boolean SNAPSHOT = false;
    @Deprecated
    public static final int WORLD_VERSION = 4903;
    @Deprecated
    public static final String SERIES = "main";
    @Deprecated
    public static final int RELEASE_NETWORK_PROTOCOL_VERSION = 776;
    @Deprecated
    public static final int SNAPSHOT_NETWORK_PROTOCOL_VERSION = 322;
    public static final int SNBT_NAG_VERSION = 4882;
    private static final int SNAPSHOT_PROTOCOL_BIT = 30;
    public static final boolean CRASH_EAGERLY = false;
    @Deprecated
    public static final int RESOURCE_PACK_FORMAT_MAJOR = 88;
    @Deprecated
    public static final int RESOURCE_PACK_FORMAT_MINOR = 0;
    @Deprecated
    public static final int DATA_PACK_FORMAT_MAJOR = 107;
    @Deprecated
    public static final int DATA_PACK_FORMAT_MINOR = 1;
    public static final String RPC_MANAGEMENT_SERVER_API_VERSION = "3.0.0";
    @Deprecated
    public static final int LANGUAGE_FORMAT = 1;
    public static final int REPORT_FORMAT_VERSION = 1;
    public static final String DATA_VERSION_TAG = "DataVersion";
    public static final String DEBUG_FLAG_PREFIX = "MC_DEBUG_";
    public static final boolean DEBUG_ENABLED = SharedConstants.booleanProperty(SharedConstants.prefixDebugFlagName("ENABLED"));
    private static final boolean DEBUG_PRINT_PROPERTIES = SharedConstants.booleanProperty(SharedConstants.prefixDebugFlagName("PRINT_PROPERTIES"));
    public static final boolean FIX_TNT_DUPE = false;
    public static final boolean FIX_SAND_DUPE = false;
    public static final boolean DEBUG_OPEN_INCOMPATIBLE_WORLDS = SharedConstants.debugFlag("OPEN_INCOMPATIBLE_WORLDS");
    public static final boolean DEBUG_ALLOW_LOW_SIM_DISTANCE = SharedConstants.debugFlag("ALLOW_LOW_SIM_DISTANCE");
    public static final boolean DEBUG_HOTKEYS = SharedConstants.debugFlag("HOTKEYS");
    public static final boolean DEBUG_UI_NARRATION = SharedConstants.debugFlag("UI_NARRATION");
    public static final boolean DEBUG_SHUFFLE_UI_RENDERING_ORDER = SharedConstants.debugFlag("SHUFFLE_UI_RENDERING_ORDER");
    public static final boolean DEBUG_SHUFFLE_MODELS = SharedConstants.debugFlag("SHUFFLE_MODELS");
    public static final boolean DEBUG_RENDER_UI_LAYERING_RECTANGLES = SharedConstants.debugFlag("RENDER_UI_LAYERING_RECTANGLES");
    public static final boolean DEBUG_PATHFINDING = SharedConstants.debugFlag("PATHFINDING");
    public static final boolean DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES = SharedConstants.debugFlag("SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES");
    public static final boolean DEBUG_SHAPES = SharedConstants.debugFlag("SHAPES");
    public static final boolean DEBUG_NEIGHBORSUPDATE = SharedConstants.debugFlag("NEIGHBORSUPDATE");
    public static final boolean DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = SharedConstants.debugFlag("EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER");
    public static final boolean DEBUG_STRUCTURES = SharedConstants.debugFlag("STRUCTURES");
    public static final boolean DEBUG_GAME_EVENT_LISTENERS = SharedConstants.debugFlag("GAME_EVENT_LISTENERS");
    public static final boolean DEBUG_DUMP_TEXTURE_ATLAS = SharedConstants.debugFlag("DUMP_TEXTURE_ATLAS");
    public static final boolean DEBUG_STRUCTURE_EDIT_MODE = SharedConstants.debugFlag("STRUCTURE_EDIT_MODE");
    public static final boolean DEBUG_SAVE_STRUCTURES_AS_SNBT = SharedConstants.debugFlag("SAVE_STRUCTURES_AS_SNBT");
    public static final boolean DEBUG_SYNCHRONOUS_GL_LOGS = SharedConstants.debugFlag("SYNCHRONOUS_GL_LOGS");
    public static final boolean DEBUG_VERBOSE_SERVER_EVENTS = SharedConstants.debugFlag("VERBOSE_SERVER_EVENTS");
    public static final boolean DEBUG_NAMED_RUNNABLES = SharedConstants.debugFlag("NAMED_RUNNABLES");
    public static final boolean DEBUG_GOAL_SELECTOR = SharedConstants.debugFlag("GOAL_SELECTOR");
    public static final boolean DEBUG_VILLAGE_SECTIONS = SharedConstants.debugFlag("VILLAGE_SECTIONS");
    public static final boolean DEBUG_BRAIN = SharedConstants.debugFlag("BRAIN");
    public static final boolean DEBUG_POI = SharedConstants.debugFlag("POI");
    public static final boolean DEBUG_BEES = SharedConstants.debugFlag("BEES");
    public static final boolean DEBUG_RAIDS = SharedConstants.debugFlag("RAIDS");
    public static final boolean DEBUG_BLOCK_BREAK = SharedConstants.debugFlag("BLOCK_BREAK");
    public static final boolean DEBUG_MONITOR_TICK_TIMES = SharedConstants.debugFlag("MONITOR_TICK_TIMES");
    public static final boolean DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN = SharedConstants.debugFlag("KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN");
    public static final boolean DEBUG_DONT_SAVE_WORLD = SharedConstants.debugFlag("DONT_SAVE_WORLD");
    public static final boolean DEBUG_LARGE_DRIPSTONE = SharedConstants.debugFlag("LARGE_DRIPSTONE");
    public static final boolean DEBUG_CARVERS = SharedConstants.debugFlag("CARVERS");
    public static final boolean DEBUG_ORE_VEINS = SharedConstants.debugFlag("ORE_VEINS");
    public static final boolean DEBUG_SCULK_CATALYST = SharedConstants.debugFlag("SCULK_CATALYST");
    public static final boolean DEBUG_BYPASS_REALMS_VERSION_CHECK = SharedConstants.debugFlag("BYPASS_REALMS_VERSION_CHECK");
    public static final boolean DEBUG_SOCIAL_INTERACTIONS = SharedConstants.debugFlag("SOCIAL_INTERACTIONS");
    public static final boolean DEBUG_CHAT_DISABLED = SharedConstants.debugFlag("CHAT_DISABLED");
    public static final boolean DEBUG_CHAT_FRIENDS_ONLY = SharedConstants.debugFlag("CHAT_FRIENDS_ONLY");
    public static final boolean DEBUG_VALIDATE_RESOURCE_PATH_CASE = SharedConstants.debugFlag("VALIDATE_RESOURCE_PATH_CASE");
    public static final boolean DEBUG_UNLOCK_ALL_TRADES = SharedConstants.debugFlag("UNLOCK_ALL_TRADES");
    public static final boolean DEBUG_BREEZE_MOB = SharedConstants.debugFlag("BREEZE_MOB");
    public static final boolean DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS = SharedConstants.debugFlag("TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS = SharedConstants.debugFlag("VAULT_DETECTS_SHEEP_AS_PLAYERS");
    public static final boolean DEBUG_FORCE_ONBOARDING_SCREEN = SharedConstants.debugFlag("FORCE_ONBOARDING_SCREEN");
    public static final boolean DEBUG_CURSOR_POS = SharedConstants.debugFlag("CURSOR_POS");
    public static final boolean DEBUG_DEFAULT_SKIN_OVERRIDE = SharedConstants.debugFlag("DEFAULT_SKIN_OVERRIDE");
    public static final boolean DEBUG_PANORAMA_SCREENSHOT = SharedConstants.debugFlag("PANORAMA_SCREENSHOT");
    public static final boolean DEBUG_CHASE_COMMAND = SharedConstants.debugFlag("CHASE_COMMAND");
    public static final boolean DEBUG_VERBOSE_COMMAND_ERRORS = SharedConstants.debugFlag("VERBOSE_COMMAND_ERRORS");
    public static final boolean DEBUG_DEV_COMMANDS = SharedConstants.debugFlag("DEV_COMMANDS");
    public static final boolean DEBUG_ACTIVE_TEXT_AREAS = SharedConstants.debugFlag("ACTIVE_TEXT_AREAS");
    public static final boolean DEBUG_PREFER_WAYLAND = SharedConstants.debugFlag("PREFER_WAYLAND");
    public static final boolean DEBUG_SIMULATE_LIBRARY_LOAD_FAILURE = SharedConstants.debugFlag("SIMULATE_LIBRARY_LOAD_FAILURE");
    public static final boolean DEBUG_IGNORE_LOCAL_MOB_CAP = SharedConstants.debugFlag("IGNORE_LOCAL_MOB_CAP");
    public static final boolean DEBUG_DISABLE_LIQUID_SPREADING = SharedConstants.debugFlag("DISABLE_LIQUID_SPREADING");
    public static final boolean DEBUG_AQUIFERS = SharedConstants.debugFlag("AQUIFERS");
    public static final boolean DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING = SharedConstants.debugFlag("JFR_PROFILING_ENABLE_LEVEL_LOADING");
    public static final boolean DEBUG_ENTITY_BLOCK_INTERSECTION = SharedConstants.debugFlag("ENTITY_BLOCK_INTERSECTION");
    public static boolean debugGenerateSquareTerrainWithoutNoise = SharedConstants.debugFlag("GENERATE_SQUARE_TERRAIN_WITHOUT_NOISE");
    public static final boolean DEBUG_ONLY_GENERATE_HALF_THE_WORLD = SharedConstants.debugFlag("ONLY_GENERATE_HALF_THE_WORLD");
    public static final boolean DEBUG_DISABLE_FLUID_GENERATION = SharedConstants.debugFlag("DISABLE_FLUID_GENERATION");
    public static final boolean DEBUG_DISABLE_AQUIFERS = SharedConstants.debugFlag("DISABLE_AQUIFERS");
    public static final boolean DEBUG_DISABLE_SURFACE = SharedConstants.debugFlag("DISABLE_SURFACE");
    public static final boolean DEBUG_DISABLE_CARVERS = SharedConstants.debugFlag("DISABLE_CARVERS");
    public static final boolean DEBUG_DISABLE_STRUCTURES = SharedConstants.debugFlag("DISABLE_STRUCTURES");
    public static final boolean DEBUG_DISABLE_FEATURES = SharedConstants.debugFlag("DISABLE_FEATURES");
    public static final boolean DEBUG_DISABLE_ORE_VEINS = SharedConstants.debugFlag("DISABLE_ORE_VEINS");
    public static final boolean DEBUG_DISABLE_BLENDING = SharedConstants.debugFlag("DISABLE_BLENDING");
    public static final boolean DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION = SharedConstants.debugFlag("DISABLE_BELOW_ZERO_RETROGENERATION");
    public static final int DEFAULT_MINECRAFT_PORT = 25565;
    public static final boolean DEBUG_SUBTITLES = SharedConstants.debugFlag("SUBTITLES");
    public static final int DEBUG_FAKE_LATENCY_MS = SharedConstants.debugIntValue("FAKE_LATENCY_MS");
    public static final int DEBUG_FAKE_JITTER_MS = SharedConstants.debugIntValue("FAKE_JITTER_MS");
    public static final ResourceLeakDetector.Level NETTY_LEAK_DETECTION = ResourceLeakDetector.Level.DISABLED;
    public static final boolean COMMAND_STACK_TRACES = SharedConstants.debugFlag("COMMAND_STACK_TRACES");
    public static final boolean DEBUG_WORLD_RECREATE = SharedConstants.debugFlag("WORLD_RECREATE");
    public static final boolean DEBUG_SHOW_SERVER_DEBUG_VALUES = SharedConstants.debugFlag("SHOW_SERVER_DEBUG_VALUES");
    public static final boolean DEBUG_FEATURE_COUNT = SharedConstants.debugFlag("FEATURE_COUNT");
    public static final boolean DEBUG_FORCE_TELEMETRY = SharedConstants.debugFlag("FORCE_TELEMETRY");
    public static final boolean DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND = SharedConstants.debugFlag("DONT_SEND_TELEMETRY_TO_BACKEND");
    public static final long MAXIMUM_TICK_TIME_NANOS = Duration.ofMillis(300L).toNanos();
    public static final float MAXIMUM_BLOCK_EXPLOSION_RESISTANCE = 3600000.0f;
    public static final boolean USE_DEVONLY = false;
    public static boolean CHECK_DATA_FIXER_SCHEMA = true;
    public static boolean IS_RUNNING_IN_IDE;
    public static final int WORLD_RESOLUTION = 16;
    public static final int MAX_CHAT_LENGTH = 256;
    public static final int MAX_USER_INPUT_COMMAND_LENGTH = 32500;
    public static final int MAX_FUNCTION_COMMAND_LENGTH = 2000000;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final int MAX_CHAINED_NEIGHBOR_UPDATES = 1000000;
    public static final int MAX_RENDER_DISTANCE = 32;
    public static final int MAX_CLOUD_DISTANCE = 128;
    public static final char[] ILLEGAL_FILE_CHARACTERS;
    public static final int TICKS_PER_SECOND = 20;
    public static final int MILLIS_PER_TICK = 50;
    public static final int TICKS_PER_MINUTE = 1200;
    public static final int TICKS_PER_GAME_DAY = 24000;
    public static final int DEFAULT_RANDOM_TICK_SPEED = 3;
    public static final float AVERAGE_GAME_TICKS_PER_RANDOM_TICK_PER_BLOCK = 1365.3334f;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_MINUTE = 0.87890625f;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_GAME_DAY = 17.578125f;
    public static final int WORLD_ICON_SIZE = 64;
    private static @Nullable WorldVersion CURRENT_VERSION;

    private static String prefixDebugFlagName(String name) {
        return DEBUG_FLAG_PREFIX + name;
    }

    private static boolean booleanProperty(String name) {
        String value = System.getProperty(name);
        return value != null && (value.isEmpty() || Boolean.parseBoolean(value));
    }

    private static boolean debugFlag(String name) {
        if (!DEBUG_ENABLED) {
            return false;
        }
        String prefixedName = SharedConstants.prefixDebugFlagName(name);
        if (DEBUG_PRINT_PROPERTIES) {
            System.out.println("Debug property available: " + prefixedName + ": bool");
        }
        return SharedConstants.booleanProperty(prefixedName);
    }

    private static int debugIntValue(String name) {
        if (!DEBUG_ENABLED) {
            return 0;
        }
        String prefixedName = SharedConstants.prefixDebugFlagName(name);
        if (DEBUG_PRINT_PROPERTIES) {
            System.out.println("Debug property available: " + prefixedName + ": int");
        }
        return Integer.parseInt(System.getProperty(prefixedName, "0"));
    }

    public static void setVersion(WorldVersion version) {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = version;
        } else if (version != CURRENT_VERSION) {
            throw new IllegalStateException("Cannot override the current game version!");
        }
    }

    public static void tryDetectVersion() {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = DetectedVersion.tryDetectVersion();
        }
    }

    public static WorldVersion getCurrentVersion() {
        if (CURRENT_VERSION == null) {
            throw new IllegalStateException("Game version not set");
        }
        return CURRENT_VERSION;
    }

    public static int getProtocolVersion() {
        return 776;
    }

    public static boolean debugVoidTerrain(ChunkPos pos) {
        int posX = pos.getMinBlockX();
        int posZ = pos.getMinBlockZ();
        if (DEBUG_ONLY_GENERATE_HALF_THE_WORLD) {
            return posZ < 0;
        }
        if (debugGenerateSquareTerrainWithoutNoise) {
            return posX > 8192 || posX < 0 || posZ > 1024 || posZ < 0;
        }
        return false;
    }

    static {
        ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};
        ResourceLeakDetector.setLevel((ResourceLeakDetector.Level)NETTY_LEAK_DETECTION);
        CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = COMMAND_STACK_TRACES;
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BrigadierExceptions();
    }
}

