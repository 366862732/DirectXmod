/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.components.debug;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.debug.DebugEntryBiome;
import net.minecraft.client.gui.components.debug.DebugEntryChunkGeneration;
import net.minecraft.client.gui.components.debug.DebugEntryChunkRenderStats;
import net.minecraft.client.gui.components.debug.DebugEntryChunkSourceStats;
import net.minecraft.client.gui.components.debug.DebugEntryDayCount;
import net.minecraft.client.gui.components.debug.DebugEntryDetailedMemory;
import net.minecraft.client.gui.components.debug.DebugEntryEntityRenderStats;
import net.minecraft.client.gui.components.debug.DebugEntryFps;
import net.minecraft.client.gui.components.debug.DebugEntryGpuUtilization;
import net.minecraft.client.gui.components.debug.DebugEntryHeightmap;
import net.minecraft.client.gui.components.debug.DebugEntryLight;
import net.minecraft.client.gui.components.debug.DebugEntryLocalDifficulty;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAt;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAtEntity;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAtEntityTags;
import net.minecraft.client.gui.components.debug.DebugEntryMemory;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugEntryParticleRenderStats;
import net.minecraft.client.gui.components.debug.DebugEntryPosition;
import net.minecraft.client.gui.components.debug.DebugEntryPostEffect;
import net.minecraft.client.gui.components.debug.DebugEntrySectionPosition;
import net.minecraft.client.gui.components.debug.DebugEntrySimplePerformanceImpactors;
import net.minecraft.client.gui.components.debug.DebugEntrySoundCache;
import net.minecraft.client.gui.components.debug.DebugEntrySoundMood;
import net.minecraft.client.gui.components.debug.DebugEntrySpawnCounts;
import net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs;
import net.minecraft.client.gui.components.debug.DebugEntryTps;
import net.minecraft.client.gui.components.debug.DebugEntryVersion;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class DebugScreenEntries {
    private static final Map<Identifier, DebugScreenEntry> ENTRIES_BY_ID = new HashMap<Identifier, DebugScreenEntry>();
    public static final Identifier GAME_VERSION = DebugScreenEntries.register("game_version", (DebugScreenEntry)new DebugEntryVersion());
    public static final Identifier FPS = DebugScreenEntries.register("fps", (DebugScreenEntry)new DebugEntryFps());
    public static final Identifier TPS = DebugScreenEntries.register("tps", (DebugScreenEntry)new DebugEntryTps());
    public static final Identifier MEMORY = DebugScreenEntries.register("memory", (DebugScreenEntry)new DebugEntryMemory());
    public static final Identifier DETAILED_MEMORY = DebugScreenEntries.register("detailed_memory", (DebugScreenEntry)new DebugEntryDetailedMemory());
    public static final Identifier SYSTEM_SPECS = DebugScreenEntries.register("system_specs", (DebugScreenEntry)new DebugEntrySystemSpecs());
    public static final Identifier LOOKING_AT_BLOCK_STATE = DebugScreenEntries.register("looking_at_block_state", (DebugScreenEntry)new DebugEntryLookingAt.BlockStateInfo());
    public static final Identifier LOOKING_AT_BLOCK_TAGS = DebugScreenEntries.register("looking_at_block_tags", (DebugScreenEntry)new DebugEntryLookingAt.BlockTagInfo());
    public static final Identifier LOOKING_AT_FLUID_STATE = DebugScreenEntries.register("looking_at_fluid_state", (DebugScreenEntry)new DebugEntryLookingAt.FluidStateInfo());
    public static final Identifier LOOKING_AT_FLUID_TAGS = DebugScreenEntries.register("looking_at_fluid_tags", (DebugScreenEntry)new DebugEntryLookingAt.FluidTagInfo());
    public static final Identifier LOOKING_AT_ENTITY = DebugScreenEntries.register("looking_at_entity", (DebugScreenEntry)new DebugEntryLookingAtEntity());
    public static final Identifier LOOKING_AT_ENTITY_TAGS = DebugScreenEntries.register("looking_at_entity_tags", (DebugScreenEntry)new DebugEntryLookingAtEntityTags());
    public static final Identifier CHUNK_RENDER_STATS = DebugScreenEntries.register("chunk_render_stats", (DebugScreenEntry)new DebugEntryChunkRenderStats());
    public static final Identifier CHUNK_GENERATION_STATS = DebugScreenEntries.register("chunk_generation_stats", (DebugScreenEntry)new DebugEntryChunkGeneration());
    public static final Identifier ENTITY_RENDER_STATS = DebugScreenEntries.register("entity_render_stats", (DebugScreenEntry)new DebugEntryEntityRenderStats());
    public static final Identifier PARTICLE_RENDER_STATS = DebugScreenEntries.register("particle_render_stats", (DebugScreenEntry)new DebugEntryParticleRenderStats());
    public static final Identifier CHUNK_SOURCE_STATS = DebugScreenEntries.register("chunk_source_stats", (DebugScreenEntry)new DebugEntryChunkSourceStats());
    public static final Identifier PLAYER_POSITION = DebugScreenEntries.register("player_position", (DebugScreenEntry)new DebugEntryPosition());
    public static final Identifier PLAYER_SECTION_POSITION = DebugScreenEntries.register("player_section_position", (DebugScreenEntry)new DebugEntrySectionPosition());
    public static final Identifier LIGHT_LEVELS = DebugScreenEntries.register("light_levels", (DebugScreenEntry)new DebugEntryLight());
    public static final Identifier HEIGHTMAP = DebugScreenEntries.register("heightmap", (DebugScreenEntry)new DebugEntryHeightmap());
    public static final Identifier BIOME = DebugScreenEntries.register("biome", (DebugScreenEntry)new DebugEntryBiome());
    public static final Identifier LOCAL_DIFFICULTY = DebugScreenEntries.register("local_difficulty", (DebugScreenEntry)new DebugEntryLocalDifficulty());
    public static final Identifier DAY_COUNT = DebugScreenEntries.register("day_count", (DebugScreenEntry)new DebugEntryDayCount());
    public static final Identifier ENTITY_SPAWN_COUNTS = DebugScreenEntries.register("entity_spawn_counts", (DebugScreenEntry)new DebugEntrySpawnCounts());
    public static final Identifier SOUND_MOOD = DebugScreenEntries.register("sound_mood", (DebugScreenEntry)new DebugEntrySoundMood());
    public static final Identifier SOUND_CACHE = DebugScreenEntries.register("sound_cache", (DebugScreenEntry)new DebugEntrySoundCache());
    public static final Identifier POST_EFFECT = DebugScreenEntries.register("post_effect", (DebugScreenEntry)new DebugEntryPostEffect());
    public static final Identifier ENTITY_HITBOXES = DebugScreenEntries.register("entity_hitboxes", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier CHUNK_BORDERS = DebugScreenEntries.register("chunk_borders", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier THREE_DIMENSIONAL_CROSSHAIR = DebugScreenEntries.register("3d_crosshair", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_PATHS = DebugScreenEntries.register("chunk_section_paths", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier GPU_UTILIZATION = DebugScreenEntries.register("gpu_utilization", (DebugScreenEntry)new DebugEntryGpuUtilization());
    public static final Identifier SIMPLE_PERFORMANCE_IMPACTORS = DebugScreenEntries.register("simple_performance_impactors", (DebugScreenEntry)new DebugEntrySimplePerformanceImpactors());
    public static final Identifier CHUNK_SECTION_OCTREE = DebugScreenEntries.register("chunk_section_octree", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_WATER_LEVELS = DebugScreenEntries.register("visualize_water_levels", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_HEIGHTMAP = DebugScreenEntries.register("visualize_heightmap", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_COLLISION_BOXES = DebugScreenEntries.register("visualize_collision_boxes", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_ENTITY_SUPPORTING_BLOCKS = DebugScreenEntries.register("visualize_entity_supporting_blocks", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_BLOCK_LIGHT_LEVELS = DebugScreenEntries.register("visualize_block_light_levels", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_LEVELS = DebugScreenEntries.register("visualize_sky_light_levels", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_SOLID_FACES = DebugScreenEntries.register("visualize_solid_faces", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_CHUNKS_ON_SERVER = DebugScreenEntries.register("visualize_chunks_on_server", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier VISUALIZE_SKY_LIGHT_SECTIONS = DebugScreenEntries.register("visualize_sky_light_sections", (DebugScreenEntry)new DebugEntryNoop());
    public static final Identifier CHUNK_SECTION_VISIBILITY = DebugScreenEntries.register("chunk_section_visibility", (DebugScreenEntry)new DebugEntryNoop());
    public static final Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> PROFILES;

    private static Identifier register(String id, DebugScreenEntry entry) {
        return DebugScreenEntries.register(Identifier.withDefaultNamespace(id), entry);
    }

    private static Identifier register(Identifier identifier, DebugScreenEntry entry) {
        ENTRIES_BY_ID.put(identifier, entry);
        return identifier;
    }

    public static Map<Identifier, DebugScreenEntry> allEntries() {
        return Map.copyOf(ENTRIES_BY_ID);
    }

    public static @Nullable DebugScreenEntry getEntry(Identifier id) {
        return ENTRIES_BY_ID.get(id);
    }

    static {
        Map<Identifier, DebugScreenEntryStatus> defaultProfile = Map.of(THREE_DIMENSIONAL_CROSSHAIR, DebugScreenEntryStatus.IN_OVERLAY, GAME_VERSION, DebugScreenEntryStatus.IN_OVERLAY, TPS, DebugScreenEntryStatus.IN_OVERLAY, FPS, DebugScreenEntryStatus.IN_OVERLAY, MEMORY, DebugScreenEntryStatus.IN_OVERLAY, SYSTEM_SPECS, DebugScreenEntryStatus.IN_OVERLAY, PLAYER_POSITION, DebugScreenEntryStatus.IN_OVERLAY, PLAYER_SECTION_POSITION, DebugScreenEntryStatus.IN_OVERLAY, SIMPLE_PERFORMANCE_IMPACTORS, DebugScreenEntryStatus.IN_OVERLAY);
        Map<Identifier, DebugScreenEntryStatus> performance = Map.of(TPS, DebugScreenEntryStatus.IN_OVERLAY, FPS, DebugScreenEntryStatus.ALWAYS_ON, GPU_UTILIZATION, DebugScreenEntryStatus.IN_OVERLAY, MEMORY, DebugScreenEntryStatus.IN_OVERLAY, SIMPLE_PERFORMANCE_IMPACTORS, DebugScreenEntryStatus.IN_OVERLAY);
        PROFILES = Map.of(DebugScreenProfile.DEFAULT, defaultProfile, DebugScreenProfile.PERFORMANCE, performance);
    }
}

