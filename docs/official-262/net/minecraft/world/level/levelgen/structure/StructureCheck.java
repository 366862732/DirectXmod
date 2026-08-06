/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.Long2BooleanMap
 *  it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureCheck {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_STRUCTURE = -1;
    private final ChunkScanAccess storageAccess;
    private final RegistryAccess registryAccess;
    private final StructureTemplateManager structureTemplateManager;
    private final ResourceKey<Level> dimension;
    private final ChunkGenerator chunkGenerator;
    private final RandomState randomState;
    private final LevelHeightAccessor heightAccessor;
    private final BiomeSource biomeSource;
    private final long seed;
    private final DataFixer fixerUpper;
    private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap();
    private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<Structure, Long2BooleanMap>();

    public StructureCheck(ChunkScanAccess storageAccess, RegistryAccess registryAccess, StructureTemplateManager structureTemplateManager, ResourceKey<Level> dimension, ChunkGenerator chunkGenerator, RandomState randomState, LevelHeightAccessor heightAccessor, BiomeSource biomeSource, long seed, DataFixer fixerUpper) {
        this.storageAccess = storageAccess;
        this.registryAccess = registryAccess;
        this.structureTemplateManager = structureTemplateManager;
        this.dimension = dimension;
        this.chunkGenerator = chunkGenerator;
        this.randomState = randomState;
        this.heightAccessor = heightAccessor;
        this.biomeSource = biomeSource;
        this.seed = seed;
        this.fixerUpper = fixerUpper;
    }

    public StructureCheckResult checkStart(ChunkPos pos, Structure structure, StructurePlacement placement, boolean requireUnreferenced) {
        long posKey = pos.pack();
        Object2IntMap cachedResult = (Object2IntMap)this.loadedChunks.get(posKey);
        if (cachedResult != null) {
            return this.checkStructureInfo((Object2IntMap<Structure>)cachedResult, structure, requireUnreferenced);
        }
        StructureCheckResult storageCheckResult = this.tryLoadFromStorage(pos, structure, requireUnreferenced, posKey);
        if (storageCheckResult != null) {
            return storageCheckResult;
        }
        if (!placement.applyAdditionalChunkRestrictions(pos.x(), pos.z(), this.seed)) {
            return StructureCheckResult.START_NOT_PRESENT;
        }
        boolean isFeatureChunk = this.featureChecks.computeIfAbsent(structure, k -> new Long2BooleanOpenHashMap()).computeIfAbsent(posKey, k -> this.canCreateStructure(pos, structure));
        if (!isFeatureChunk) {
            return StructureCheckResult.START_NOT_PRESENT;
        }
        return StructureCheckResult.CHUNK_LOAD_NEEDED;
    }

    private boolean canCreateStructure(ChunkPos pos, Structure structure) {
        return structure.findValidGenerationPoint(new Structure.GenerationContext(this.registryAccess, this.chunkGenerator, this.biomeSource, this.randomState, this.structureTemplateManager, this.seed, pos, this.heightAccessor, structure.biomes()::contains)).isPresent();
    }

    private @Nullable StructureCheckResult tryLoadFromStorage(ChunkPos pos, Structure structure, boolean requireUnreferenced, long posKey) {
        CompoundTag fixedChunkTag;
        CollectFields collectFields = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"), new FieldSelector("structures", CompoundTag.TYPE, "starts"));
        try {
            this.storageAccess.scanChunk(pos, collectFields).join();
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read chunk {}", (Object)pos, (Object)e);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
        }
        Tag result = collectFields.getResult();
        if (!(result instanceof CompoundTag)) {
            return null;
        }
        CompoundTag chunkTag = (CompoundTag)result;
        int version = NbtUtils.getDataVersion(chunkTag);
        SimpleRegionStorage.injectDatafixingContext(chunkTag, ChunkMap.getChunkDataFixContextTag(this.dimension, this.chunkGenerator.getTypeNameForDataFixer()));
        try {
            fixedChunkTag = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, chunkTag, version);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to partially datafix chunk {}", (Object)pos, (Object)e);
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
        }
        Object2IntMap<Structure> knownStarts = this.loadStructures(fixedChunkTag);
        if (knownStarts == null) {
            return null;
        }
        this.storeFullResults(posKey, knownStarts);
        return this.checkStructureInfo(knownStarts, structure, requireUnreferenced);
    }

    private @Nullable Object2IntMap<Structure> loadStructures(CompoundTag chunkTag) {
        Optional maybeStartsTag = chunkTag.getCompound("structures").flatMap(tag -> tag.getCompound("starts"));
        if (maybeStartsTag.isEmpty()) {
            return null;
        }
        CompoundTag startsTag = (CompoundTag)maybeStartsTag.get();
        if (startsTag.isEmpty()) {
            return Object2IntMaps.emptyMap();
        }
        Object2IntOpenHashMap knownStarts = new Object2IntOpenHashMap();
        HolderLookup.RegistryLookup structuresRegistry = this.registryAccess.lookupOrThrow(Registries.STRUCTURE);
        startsTag.forEach((arg_0, arg_1) -> StructureCheck.lambda$loadStructures$1((Registry)structuresRegistry, (Object2IntMap)knownStarts, arg_0, arg_1));
        return knownStarts;
    }

    private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> map) {
        return map.isEmpty() ? Object2IntMaps.emptyMap() : map;
    }

    private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> cachedResult, Structure structure, boolean requireUnreferenced) {
        int referenceCount = cachedResult.getOrDefault((Object)structure, -1);
        return referenceCount != -1 && (!requireUnreferenced || referenceCount == 0) ? StructureCheckResult.START_PRESENT : StructureCheckResult.START_NOT_PRESENT;
    }

    public void onStructureLoad(ChunkPos pos, Map<Structure, StructureStart> starts) {
        long posKey = pos.pack();
        Object2IntOpenHashMap startsToReferences = new Object2IntOpenHashMap();
        starts.forEach((arg_0, arg_1) -> StructureCheck.lambda$onStructureLoad$0((Object2IntMap)startsToReferences, arg_0, arg_1));
        this.storeFullResults(posKey, (Object2IntMap<Structure>)startsToReferences);
    }

    private void storeFullResults(long posKey, Object2IntMap<Structure> starts) {
        this.loadedChunks.put(posKey, StructureCheck.deduplicateEmptyMap(starts));
        this.featureChecks.values().forEach(m -> m.remove(posKey));
    }

    public void incrementReference(ChunkPos chunkPos, Structure structure) {
        this.loadedChunks.compute(chunkPos.pack(), (key, counts) -> {
            if (counts == null || counts.isEmpty()) {
                counts = new Object2IntOpenHashMap();
            }
            counts.computeInt((Object)structure, (k, value) -> value == null ? 1 : value + 1);
            return counts;
        });
    }

    private static /* synthetic */ void lambda$onStructureLoad$0(Object2IntMap startsToReferences, Structure structure, StructureStart structureStart) {
        if (structureStart.isValid()) {
            startsToReferences.put((Object)structure, structureStart.getReferences());
        }
    }

    private static /* synthetic */ void lambda$loadStructures$1(Registry structuresRegistry, Object2IntMap knownStarts, String key, Tag tag) {
        Identifier id = Identifier.tryParse(key);
        if (id == null) {
            return;
        }
        Structure foundFeature = (Structure)structuresRegistry.getValue(id);
        if (foundFeature == null) {
            return;
        }
        tag.asCompound().ifPresent(structureData -> {
            String pieceId = structureData.getStringOr("id", "");
            if (!"INVALID".equals(pieceId)) {
                int referenceCount = structureData.getIntOr("references", 0);
                knownStarts.put((Object)foundFeature, referenceCount);
            }
        });
    }
}

