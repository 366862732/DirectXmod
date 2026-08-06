/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.OptionalDynamic
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongList
 */
package net.minecraft.util.filefix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.filefix.CanceledFileFixException;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.access.ChunkNbt;
import net.minecraft.util.filefix.access.CompressedNbt;
import net.minecraft.util.filefix.access.FileAccess;
import net.minecraft.util.filefix.access.FileAccessProvider;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.access.FileResourceTypes;
import net.minecraft.util.filefix.access.LevelDat;
import net.minecraft.util.filefix.access.SavedDataNbt;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

public class LegacyStructureFileFix
extends FileFix {
    public static final int STRUCTURE_RANGE = 8;
    public static final List<String> OVERWORLD_LEGACY_STRUCTURES = List.of("Monument", "Stronghold", "Mineshaft", "Temple", "Mansion");
    public static final Map<String, String> LEGACY_TO_CURRENT_MAP = Util.make(Maps.newHashMap(), map -> {
        map.put("Iglu", "Igloo");
        map.put("TeDP", "Desert_Pyramid");
        map.put("TeJP", "Jungle_Pyramid");
        map.put("TeSH", "Swamp_Hut");
    });
    public static final List<String> NETHER_LEGACY_STRUCTURES = List.of("Fortress");
    public static final List<String> END_LEGACY_STRUCTURES = List.of("EndCity");
    private static final ResourceKey<Level> OVERWORLD_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));
    private static final ResourceKey<Level> END_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_end"));

    public LegacyStructureFileFix(Schema schema) {
        super(schema);
    }

    @Override
    public void makeFixer() {
        this.addFileContentFix(files -> {
            List<FileAccess> overworldStructureData = OVERWORLD_LEGACY_STRUCTURES.stream().map(structureId -> LegacyStructureFileFix.getLegacyStructureData(files, structureId)).toList();
            RegionStorageInfo overworldInfo = new RegionStorageInfo("overworld", OVERWORLD_KEY, "chunk");
            List<FileAccess> netherStructureData = NETHER_LEGACY_STRUCTURES.stream().map(structureId -> LegacyStructureFileFix.getLegacyStructureData(files, structureId)).toList();
            RegionStorageInfo netherInfo = new RegionStorageInfo("the_nether", NETHER_KEY, "chunk");
            List<FileAccess> endStructureData = END_LEGACY_STRUCTURES.stream().map(structureId -> LegacyStructureFileFix.getLegacyStructureData(files, structureId)).toList();
            RegionStorageInfo endInfo = new RegionStorageInfo("the_end", END_KEY, "chunk");
            FileAccess<LevelDat> levelDat = files.getFileAccess(FileResourceTypes.LEVEL_DAT, FileRelation.ORIGIN.forFile("level.dat"));
            FileAccess<ChunkNbt> overworldChunks = files.getFileAccess(FileResourceTypes.chunk(DataFixTypes.CHUNK, overworldInfo), FileRelation.OLD_OVERWORLD.resolve(FileRelation.REGION));
            FileAccess<ChunkNbt> netherChunks = files.getFileAccess(FileResourceTypes.chunk(DataFixTypes.CHUNK, netherInfo), FileRelation.OLD_NETHER.resolve(FileRelation.REGION));
            FileAccess<ChunkNbt> endChunks = files.getFileAccess(FileResourceTypes.chunk(DataFixTypes.CHUNK, endInfo), FileRelation.OLD_END.resolve(FileRelation.REGION));
            return upgradeProgress -> {
                Optional<Dynamic<Tag>> levelData = ((LevelDat)levelDat.getOnlyFile()).read();
                if (levelData.isEmpty()) {
                    return;
                }
                upgradeProgress.setType(UpgradeProgress.Type.LEGACY_STRUCTURES);
                LegacyStructureFileFix.extractAndStoreLegacyStructureData(levelData.get(), List.of(new DimensionFixEntry(OVERWORLD_KEY, overworldStructureData, overworldChunks, (Long2ObjectOpenHashMap<LegacyStructureData>)new Long2ObjectOpenHashMap()), new DimensionFixEntry(NETHER_KEY, netherStructureData, netherChunks, (Long2ObjectOpenHashMap<LegacyStructureData>)new Long2ObjectOpenHashMap()), new DimensionFixEntry(END_KEY, endStructureData, endChunks, (Long2ObjectOpenHashMap<LegacyStructureData>)new Long2ObjectOpenHashMap())), upgradeProgress);
            };
        });
    }

    private static void extractAndStoreLegacyStructureData(Dynamic<Tag> levelData, List<DimensionFixEntry> dimensionFixEntries, UpgradeProgress upgradeProgress) throws IOException {
        upgradeProgress.setStatus(UpgradeProgress.Status.COUNTING);
        for (DimensionFixEntry dimensionFixEntry : dimensionFixEntries) {
            Long2ObjectOpenHashMap<LegacyStructureData> structures = dimensionFixEntry.structures;
            for (FileAccess<SavedDataNbt> structureDataFileAccess : dimensionFixEntry.structureFileAccess) {
                SavedDataNbt targetFile = structureDataFileAccess.getOnlyFile();
                Optional<Dynamic<Tag>> structureData = targetFile.read();
                if (structureData.isEmpty()) continue;
                LegacyStructureFileFix.extractLegacyStructureData(structureData.get(), structures);
            }
            upgradeProgress.addTotalFileFixOperations(structures.size());
        }
        upgradeProgress.setStatus(UpgradeProgress.Status.UPGRADING);
        for (DimensionFixEntry dimensionFixEntry : dimensionFixEntries) {
            String chunkGeneratorType;
            ResourceKey<Level> dimensionKey = dimensionFixEntry.dimensionKey;
            ChunkNbt chunkNbt = dimensionFixEntry.chunkFileAccess.getOnlyFile();
            if (dimensionKey == OVERWORLD_KEY) {
                String generatorName = levelData.get("generatorName").asString("buffet");
                chunkGeneratorType = switch (generatorName) {
                    case "flat" -> "minecraft:flat";
                    case "debug_all_block_states" -> "minecraft:debug";
                    default -> "minecraft:noise";
                };
            } else {
                chunkGeneratorType = "minecraft:noise";
            }
            Optional<Identifier> generatorIdentifier = Optional.ofNullable(Identifier.tryParse(chunkGeneratorType));
            CompoundTag dataFixContext = ChunkMap.getChunkDataFixContextTag(dimensionKey, generatorIdentifier);
            LegacyStructureFileFix.storeLegacyStructureDataToChunks(dimensionFixEntry.structures, chunkNbt, dataFixContext, upgradeProgress);
        }
    }

    private static FileAccess<SavedDataNbt> getLegacyStructureData(FileAccessProvider files, String structureId) {
        return files.getFileAccess(FileResourceTypes.savedData(References.SAVED_DATA_STRUCTURE_FEATURE_INDICES, CompressedNbt.MissingSeverity.MINOR), FileRelation.DATA.forFile(structureId + ".dat"));
    }

    private static void extractLegacyStructureData(Dynamic<Tag> structureData, Long2ObjectMap<LegacyStructureData> extractedDataContainer) {
        OptionalDynamic features = structureData.get("Features");
        Map map = features.asMap(Function.identity(), Function.identity());
        for (Dynamic value : map.values()) {
            long pos = ChunkPos.pack(value.get("ChunkX").asInt(0), value.get("ChunkZ").asInt(0));
            List childList = value.get("Children").asList(Function.identity());
            if (!childList.isEmpty()) {
                Optional<String> id2 = ((Dynamic)childList.getFirst()).get("id").asString().result().map(LEGACY_TO_CURRENT_MAP::get);
                if (id2.isPresent()) {
                    value = value.set("id", value.createString(id2.get()));
                }
            }
            Dynamic finalValue = value;
            value.get("id").asString().ifSuccess(id -> {
                ((LegacyStructureData)extractedDataContainer.computeIfAbsent(pos, l -> new LegacyStructureData())).addStart((String)id, (Dynamic<Tag>)finalValue);
                for (int neighborX = ChunkPos.getX(pos) - 8; neighborX <= ChunkPos.getX(pos) + 8; ++neighborX) {
                    for (int neighborZ = ChunkPos.getZ(pos) - 8; neighborZ <= ChunkPos.getZ(pos) + 8; ++neighborZ) {
                        ((LegacyStructureData)extractedDataContainer.computeIfAbsent(ChunkPos.pack(neighborX, neighborZ), l -> new LegacyStructureData())).addIndex((String)id, pos);
                    }
                }
            });
        }
    }

    private static void storeLegacyStructureDataToChunks(Long2ObjectMap<LegacyStructureData> structures, ChunkNbt chunksAccess, CompoundTag dataFixContext, UpgradeProgress upgradeProgress) {
        List<Long2ObjectMap.Entry> entries = structures.long2ObjectEntrySet().stream().sorted(Comparator.comparingLong(entry -> ChunkPos.pack(ChunkPos.getRegionX(entry.getLongKey()), ChunkPos.getRegionZ(entry.getLongKey())))).toList();
        IncrementalFutureSequence futures = new IncrementalFutureSequence(8);
        for (Long2ObjectMap.Entry entry2 : entries) {
            if (upgradeProgress.isCanceled()) {
                throw new CanceledFileFixException();
            }
            long pos = entry2.getLongKey();
            LegacyStructureData legacyData = (LegacyStructureData)entry2.getValue();
            int finished = futures.push(chunksAccess.updateChunk(ChunkPos.unpack(pos), dataFixContext, tag -> {
                CompoundTag levelTag = tag.getCompoundOrEmpty("Level");
                CompoundTag structureTag = levelTag.getCompoundOrEmpty("Structures");
                CompoundTag startTag = structureTag.getCompoundOrEmpty("Starts");
                CompoundTag referencesTag = structureTag.getCompoundOrEmpty("References");
                legacyData.starts().forEach((id, value) -> startTag.put((String)id, (Tag)value.convert((DynamicOps)NbtOps.INSTANCE).getValue()));
                legacyData.indexes().forEach((id, indexes) -> referencesTag.putLongArray((String)id, indexes.toLongArray()));
                structureTag.put("Starts", startTag);
                structureTag.put("References", referencesTag);
                levelTag.put("Structures", structureTag);
                tag.put("Level", levelTag);
                return tag;
            }));
            upgradeProgress.incrementFinishedOperationsBy(finished);
        }
        upgradeProgress.incrementFinishedOperationsBy(futures.waitForAll());
    }

    private record DimensionFixEntry(ResourceKey<Level> dimensionKey, List<FileAccess<SavedDataNbt>> structureFileAccess, FileAccess<ChunkNbt> chunkFileAccess, Long2ObjectOpenHashMap<LegacyStructureData> structures) {
    }

    private static class IncrementalFutureSequence {
        private final int maxConcurrency;
        private final List<CompletableFuture<?>> futures;

        public IncrementalFutureSequence(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            this.futures = new ArrayList(maxConcurrency);
        }

        public int push(CompletableFuture<?> future) {
            int finished = 0;
            if (this.futures.size() >= this.maxConcurrency) {
                finished += this.waitOnAny();
            }
            this.futures.add(future);
            return finished;
        }

        private int waitOnAny() {
            int oldSize = this.futures.size();
            CompletableFuture.anyOf((CompletableFuture[])this.futures.toArray(CompletableFuture[]::new)).join();
            this.futures.removeIf(CompletableFuture::isDone);
            return oldSize - this.futures.size();
        }

        public int waitForAll() {
            int oldSize = this.futures.size();
            CompletableFuture.allOf((CompletableFuture[])this.futures.toArray(CompletableFuture[]::new)).join();
            this.futures.clear();
            return oldSize;
        }
    }

    public record LegacyStructureData(Map<String, Dynamic<?>> starts, Map<String, LongList> indexes) {
        public LegacyStructureData() {
            this(new HashMap(), new HashMap<String, LongList>());
        }

        public void addStart(String id, Dynamic<Tag> data) {
            this.starts.put(id, data);
        }

        public void addIndex(String id, long sourcePos) {
            this.indexes.computeIfAbsent(id, l -> new LongArrayList()).add(sourcePos);
        }
    }
}

