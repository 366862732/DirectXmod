/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 *  it.unimi.dsi.fastutil.ints.Int2IntFunction
 *  it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.ChunkProtoTickListFix;
import net.minecraft.util.datafix.fixes.References;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class ChunkHeightAndBiomeFix
extends DataFix {
    public static final String DATAFIXER_CONTEXT_TAG = "__context";
    private static final String NAME = "ChunkHeightAndBiomeFix";
    private static final int OLD_SECTION_COUNT = 16;
    private static final int NEW_SECTION_COUNT = 24;
    private static final int NEW_MIN_SECTION_Y = -4;
    public static final int BLOCKS_PER_SECTION = 4096;
    private static final int LONGS_PER_SECTION = 64;
    private static final int HEIGHTMAP_BITS = 9;
    private static final long HEIGHTMAP_MASK = 511L;
    private static final int HEIGHTMAP_OFFSET = 64;
    private static final String[] HEIGHTMAP_TYPES = new String[]{"WORLD_SURFACE_WG", "WORLD_SURFACE", "WORLD_SURFACE_IGNORE_SNOW", "OCEAN_FLOOR_WG", "OCEAN_FLOOR", "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES"};
    private static final Set<String> STATUS_IS_OR_AFTER_SURFACE = Set.of("surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full");
    private static final Set<String> STATUS_IS_OR_AFTER_NOISE = Set.of("noise", "surface", "carvers", "liquid_carvers", "features", "light", "spawn", "heightmaps", "full");
    private static final Set<String> BLOCKS_BEFORE_FEATURE_STATUS = Set.of("minecraft:air", "minecraft:basalt", "minecraft:bedrock", "minecraft:blackstone", "minecraft:calcite", "minecraft:cave_air", "minecraft:coarse_dirt", "minecraft:crimson_nylium", "minecraft:dirt", "minecraft:end_stone", "minecraft:grass_block", "minecraft:gravel", "minecraft:ice", "minecraft:lava", "minecraft:mycelium", "minecraft:nether_wart_block", "minecraft:netherrack", "minecraft:orange_terracotta", "minecraft:packed_ice", "minecraft:podzol", "minecraft:powder_snow", "minecraft:red_sand", "minecraft:red_sandstone", "minecraft:sand", "minecraft:sandstone", "minecraft:snow_block", "minecraft:soul_sand", "minecraft:soul_soil", "minecraft:stone", "minecraft:terracotta", "minecraft:warped_nylium", "minecraft:warped_wart_block", "minecraft:water", "minecraft:white_terracotta");
    private static final int BIOME_CONTAINER_LAYER_SIZE = 16;
    private static final int BIOME_CONTAINER_SIZE = 64;
    private static final int BIOME_CONTAINER_TOP_LAYER_OFFSET = 1008;
    public static final String DEFAULT_BIOME = "minecraft:plains";
    private static final Int2ObjectMap<String> BIOMES_BY_ID = new Int2ObjectOpenHashMap();

    public ChunkHeightAndBiomeFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type oldChunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder levelFinder = oldChunkType.findField("Level");
        OpticFinder sectionsFinder = levelFinder.type().findField("Sections");
        Schema outputSchema = this.getOutputSchema();
        Type chunkType = outputSchema.getType(References.CHUNK);
        Type levelType = chunkType.findField("Level").type();
        Type sectionsType = levelType.findField("Sections").type();
        return this.fixTypeEverywhereTyped(NAME, oldChunkType, chunkType, chunk -> chunk.updateTyped(levelFinder, levelType, level -> {
            Dynamic tag = (Dynamic)level.get(DSL.remainderFinder());
            OptionalDynamic contextTag = ((Dynamic)chunk.get(DSL.remainderFinder())).get(DATAFIXER_CONTEXT_TAG);
            String dimension = contextTag.get("dimension").asString().result().orElse("");
            String generator = contextTag.get("generator").asString().result().orElse("");
            boolean isOverworld = "minecraft:overworld".equals(dimension);
            MutableBoolean wasIncreasedHeight = new MutableBoolean();
            int minSection = isOverworld ? -4 : 0;
            Dynamic<?>[] biomeContainers = ChunkHeightAndBiomeFix.getBiomeContainers(tag, isOverworld, minSection, wasIncreasedHeight);
            Dynamic<?> airContainer = ChunkHeightAndBiomeFix.makePalettedContainer(tag.createList(Stream.of(tag.createMap((Map)ImmutableMap.of((Object)tag.createString("Name"), (Object)tag.createString("minecraft:air"))))));
            HashSet blocksInChunk = Sets.newHashSet();
            @Nullable MutableObject bedrockSectionBlocks = new MutableObject(() -> null);
            level = level.updateTyped(sectionsFinder, sectionsType, sections -> {
                IntOpenHashSet doneSections = new IntOpenHashSet();
                Dynamic dynamic = (Dynamic)sections.write().result().orElseThrow(() -> new IllegalStateException("Malformed Chunk.Level.Sections"));
                List sectionsList = dynamic.asStream().map(arg_0 -> ChunkHeightAndBiomeFix.lambda$makeRule$5(blocksInChunk, airContainer, minSection, biomeContainers, (IntSet)doneSections, bedrockSectionBlocks, arg_0)).collect(Collectors.toCollection(ArrayList::new));
                for (int sectionIndex = 0; sectionIndex < biomeContainers.length; ++sectionIndex) {
                    int sectionY = sectionIndex + minSection;
                    if (!doneSections.add(sectionY)) continue;
                    Dynamic section = tag.createMap(Map.of(tag.createString("Y"), tag.createInt(sectionY)));
                    section = section.set("block_states", airContainer);
                    section = section.set("biomes", biomeContainers[sectionIndex]);
                    sectionsList.add(section);
                }
                return Util.readTypedOrThrow(sectionsType, tag.createList(sectionsList.stream()));
            });
            return level.update(DSL.remainderFinder(), chunkTag -> {
                if (isOverworld) {
                    chunkTag = this.predictChunkStatusBeforeSurface((Dynamic<?>)chunkTag, blocksInChunk);
                }
                return ChunkHeightAndBiomeFix.updateChunkTag(chunkTag, isOverworld, wasIncreasedHeight.booleanValue(), "minecraft:noise".equals(generator), (Supplier)bedrockSectionBlocks.get());
            });
        }));
    }

    private Dynamic<?> predictChunkStatusBeforeSurface(Dynamic<?> chunkTag, Set<String> blocksInChunk) {
        return chunkTag.update("Status", statusDynamic -> {
            boolean hasFeatureBlocks;
            String status = statusDynamic.asString("empty");
            if (STATUS_IS_OR_AFTER_SURFACE.contains(status)) {
                return statusDynamic;
            }
            blocksInChunk.remove("minecraft:air");
            boolean hasNonAirBlocks = !blocksInChunk.isEmpty();
            blocksInChunk.removeAll(BLOCKS_BEFORE_FEATURE_STATUS);
            boolean bl = hasFeatureBlocks = !blocksInChunk.isEmpty();
            if (hasFeatureBlocks) {
                return statusDynamic.createString("liquid_carvers");
            }
            if ("noise".equals(status) || hasNonAirBlocks) {
                return statusDynamic.createString("noise");
            }
            if ("biomes".equals(status)) {
                return statusDynamic.createString("structure_references");
            }
            return statusDynamic;
        });
    }

    private static Dynamic<?>[] getBiomeContainers(Dynamic<?> tag, boolean increaseHeight, int minSection, MutableBoolean wasIncreasedHeight) {
        Object[] biomeContainers = new Dynamic[increaseHeight ? 24 : 16];
        int[] oldBiomes = tag.get("Biomes").asIntStreamOpt().result().map(IntStream::toArray).orElse(null);
        if (oldBiomes != null && oldBiomes.length == 1536) {
            wasIncreasedHeight.setValue(true);
            for (int sectionYIndex = 0; sectionYIndex < 24; ++sectionYIndex) {
                int finalSectionYIndex = sectionYIndex;
                biomeContainers[sectionYIndex] = ChunkHeightAndBiomeFix.makeBiomeContainer(tag, i -> ChunkHeightAndBiomeFix.getOldBiome(oldBiomes, finalSectionYIndex * 64 + i));
            }
        } else if (oldBiomes != null && oldBiomes.length == 1024) {
            int sectionY = 0;
            while (sectionY < 16) {
                int sectionYIndex = sectionY - minSection;
                int finalSectionY = sectionY++;
                biomeContainers[sectionYIndex] = ChunkHeightAndBiomeFix.makeBiomeContainer(tag, i -> ChunkHeightAndBiomeFix.getOldBiome(oldBiomes, finalSectionY * 64 + i));
            }
            if (increaseHeight) {
                int i2;
                Dynamic<?> belowWorldBiomes = ChunkHeightAndBiomeFix.makeBiomeContainer(tag, i -> ChunkHeightAndBiomeFix.getOldBiome(oldBiomes, i % 16));
                Dynamic<?> aboveWorldBiomes = ChunkHeightAndBiomeFix.makeBiomeContainer(tag, i -> ChunkHeightAndBiomeFix.getOldBiome(oldBiomes, i % 16 + 1008));
                for (i2 = 0; i2 < 4; ++i2) {
                    biomeContainers[i2] = belowWorldBiomes;
                }
                for (i2 = 20; i2 < 24; ++i2) {
                    biomeContainers[i2] = aboveWorldBiomes;
                }
            }
        } else {
            Arrays.fill(biomeContainers, ChunkHeightAndBiomeFix.makePalettedContainer(tag.createList(Stream.of(tag.createString(DEFAULT_BIOME)))));
        }
        return biomeContainers;
    }

    private static int getOldBiome(int[] oldBiomes, int index) {
        return oldBiomes[index] & 0xFF;
    }

    private static Dynamic<?> updateChunkTag(Dynamic<?> chunkTag, boolean isOverworld, boolean wasIncreasedHeight, boolean needsBlendingAndUpgrade, Supplier< @Nullable ChunkProtoTickListFix.PoorMansPalettedContainer> bedrockSectionBlocks) {
        Dynamic status;
        String lastStatus;
        chunkTag = chunkTag.remove("Biomes");
        if (!isOverworld) {
            return ChunkHeightAndBiomeFix.updateCarvingMasks(chunkTag, 16, 0);
        }
        if (wasIncreasedHeight) {
            return ChunkHeightAndBiomeFix.updateCarvingMasks(chunkTag, 24, 0);
        }
        chunkTag = ChunkHeightAndBiomeFix.updateHeightmaps(chunkTag);
        chunkTag = ChunkHeightAndBiomeFix.addPaddingEntries(chunkTag, "LiquidsToBeTicked");
        chunkTag = ChunkHeightAndBiomeFix.addPaddingEntries(chunkTag, "PostProcessing");
        chunkTag = ChunkHeightAndBiomeFix.addPaddingEntries(chunkTag, "ToBeTicked");
        chunkTag = ChunkHeightAndBiomeFix.updateCarvingMasks(chunkTag, 24, 4);
        chunkTag = chunkTag.update("UpgradeData", ChunkHeightAndBiomeFix::shiftUpgradeData);
        if (!needsBlendingAndUpgrade) {
            return chunkTag;
        }
        Optional statusOpt = chunkTag.get("Status").result();
        if (statusOpt.isPresent() && !"empty".equals(lastStatus = (status = (Dynamic)statusOpt.get()).asString(""))) {
            ChunkProtoTickListFix.PoorMansPalettedContainer poorMansPalettedContainer;
            chunkTag = chunkTag.set("blending_data", chunkTag.createMap((Map)ImmutableMap.of((Object)chunkTag.createString("old_noise"), (Object)chunkTag.createBoolean(STATUS_IS_OR_AFTER_NOISE.contains(lastStatus)))));
            if (!SharedConstants.DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION && (poorMansPalettedContainer = bedrockSectionBlocks.get()) != null) {
                BitSet missingBedrock = new BitSet(256);
                boolean hasAnyBedrock = lastStatus.equals("noise");
                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        boolean isAir;
                        Dynamic<?> blockState = poorMansPalettedContainer.get(x, 0, z);
                        boolean isBedrock = blockState != null && "minecraft:bedrock".equals(blockState.get("Name").asString(""));
                        boolean bl = isAir = blockState != null && "minecraft:air".equals(blockState.get("Name").asString(""));
                        if (isAir) {
                            missingBedrock.set(z * 16 + x);
                        }
                        hasAnyBedrock |= isBedrock;
                    }
                }
                if (hasAnyBedrock && missingBedrock.cardinality() != missingBedrock.size()) {
                    Dynamic targetStatus = "full".equals(lastStatus) ? chunkTag.createString("heightmaps") : status;
                    chunkTag = chunkTag.set("below_zero_retrogen", chunkTag.createMap((Map)ImmutableMap.of((Object)chunkTag.createString("target_status"), (Object)targetStatus, (Object)chunkTag.createString("missing_bedrock"), (Object)chunkTag.createLongList(LongStream.of(missingBedrock.toLongArray())))));
                    chunkTag = chunkTag.set("Status", chunkTag.createString("empty"));
                }
                chunkTag = chunkTag.set("isLightOn", chunkTag.createBoolean(false));
            }
        }
        return chunkTag;
    }

    private static <T> Dynamic<T> shiftUpgradeData(Dynamic<T> upgradeData) {
        return upgradeData.update("Indices", indices -> {
            HashMap shiftedIndices = new HashMap();
            indices.getMapValues().ifSuccess(entries -> entries.forEach((index, data) -> {
                try {
                    index.asString().result().map(Integer::parseInt).ifPresent(i -> {
                        int shiftedIndex = i - -4;
                        shiftedIndices.put(index.createString(Integer.toString(shiftedIndex)), data);
                    });
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }));
            return indices.createMap(shiftedIndices);
        });
    }

    private static Dynamic<?> updateCarvingMasks(Dynamic<?> chunkTag, int sectionCount, int addedSectionsBelow) {
        Dynamic carvingMasks = chunkTag.get("CarvingMasks").orElseEmptyMap();
        carvingMasks = carvingMasks.updateMapValues(pair -> {
            long[] oldValues = BitSet.valueOf(((Dynamic)pair.getSecond()).asByteBuffer().array()).toLongArray();
            long[] newValues = new long[64 * sectionCount];
            System.arraycopy(oldValues, 0, newValues, 64 * addedSectionsBelow, oldValues.length);
            return Pair.of((Object)((Dynamic)pair.getFirst()), (Object)chunkTag.createLongList(LongStream.of(newValues)));
        });
        return chunkTag.set("CarvingMasks", carvingMasks);
    }

    private static Dynamic<?> addPaddingEntries(Dynamic<?> chunkTag, String key) {
        List list = chunkTag.get(key).orElseEmptyList().asStream().collect(Collectors.toCollection(ArrayList::new));
        if (list.size() == 24) {
            return chunkTag;
        }
        Dynamic emptyList = chunkTag.emptyList();
        for (int i = 0; i < 4; ++i) {
            list.add(0, emptyList);
            list.add(emptyList);
        }
        return chunkTag.set(key, chunkTag.createList(list.stream()));
    }

    private static Dynamic<?> updateHeightmaps(Dynamic<?> chunkTag) {
        return chunkTag.update("Heightmaps", heightmapTag -> {
            for (String heightmapType : HEIGHTMAP_TYPES) {
                heightmapTag = heightmapTag.update(heightmapType, ChunkHeightAndBiomeFix::getFixedHeightmap);
            }
            return heightmapTag;
        });
    }

    private static Dynamic<?> getFixedHeightmap(Dynamic<?> tag) {
        return tag.createLongList(tag.asLongStream().map(value -> {
            long newValue = 0L;
            int bitIndex = 0;
            while (bitIndex + 9 <= 64) {
                long oldHeight = value >> bitIndex & 0x1FFL;
                long newHeight = oldHeight == 0L ? 0L : Math.min(oldHeight + 64L, 511L);
                newValue |= newHeight << bitIndex;
                bitIndex += 9;
            }
            return newValue;
        }));
    }

    private static Dynamic<?> makeBiomeContainer(Dynamic<?> tag, Int2IntFunction sourceStorage) {
        Int2IntLinkedOpenHashMap idMap = new Int2IntLinkedOpenHashMap();
        for (int i = 0; i < 64; ++i) {
            int biomeId = sourceStorage.applyAsInt(i);
            if (idMap.containsKey(biomeId)) continue;
            idMap.put(biomeId, idMap.size());
        }
        Dynamic palette = tag.createList(idMap.keySet().stream().map(biomeId1 -> tag.createString((String)BIOMES_BY_ID.getOrDefault(biomeId1.intValue(), (Object)DEFAULT_BIOME))));
        int bits = ChunkHeightAndBiomeFix.ceillog2(idMap.size());
        if (bits == 0) {
            return ChunkHeightAndBiomeFix.makePalettedContainer(palette);
        }
        int valuesPerLong = 64 / bits;
        int requiredLength = (64 + valuesPerLong - 1) / valuesPerLong;
        long[] bitStorage = new long[requiredLength];
        int cellIndex = 0;
        int bitIndex = 0;
        for (int i = 0; i < 64; ++i) {
            int biomeId = sourceStorage.applyAsInt(i);
            int n = cellIndex++;
            bitStorage[n] = bitStorage[n] | (long)idMap.get(biomeId) << bitIndex;
            if ((bitIndex += bits) + bits <= 64) continue;
            bitIndex = 0;
        }
        Dynamic storage = tag.createLongList(Arrays.stream(bitStorage));
        return ChunkHeightAndBiomeFix.makePalettedContainer(palette, storage);
    }

    private static Dynamic<?> makePalettedContainer(Dynamic<?> palette) {
        return palette.createMap((Map)ImmutableMap.of((Object)palette.createString("palette"), palette));
    }

    private static Dynamic<?> makePalettedContainer(Dynamic<?> palette, Dynamic<?> storage) {
        return palette.createMap((Map)ImmutableMap.of((Object)palette.createString("palette"), palette, (Object)palette.createString("data"), storage));
    }

    private static Dynamic<?> makeOptimizedPalettedContainer(Dynamic<?> palette, Dynamic<?> data) {
        List paletteList = palette.asStream().collect(Collectors.toCollection(ArrayList::new));
        if (paletteList.size() == 1) {
            return ChunkHeightAndBiomeFix.makePalettedContainer(palette);
        }
        palette = ChunkHeightAndBiomeFix.padPaletteEntries(palette, data, paletteList);
        return ChunkHeightAndBiomeFix.makePalettedContainer(palette, data);
    }

    private static Dynamic<?> padPaletteEntries(Dynamic<?> palette, Dynamic<?> data, List<Dynamic<?>> paletteList) {
        int paletteSize;
        int expectedBitsPerBlock;
        long dataSizeInBits = data.asLongStream().count() * 64L;
        long estimatedBitsPerBlock = dataSizeInBits / 4096L;
        if (estimatedBitsPerBlock > (long)(expectedBitsPerBlock = ChunkHeightAndBiomeFix.ceillog2(paletteSize = paletteList.size()))) {
            Dynamic airPalleteEntry = palette.createMap((Map)ImmutableMap.of((Object)palette.createString("Name"), (Object)palette.createString("minecraft:air")));
            int minimumPaletteSizeToMatchData = (1 << (int)(estimatedBitsPerBlock - 1L)) + 1;
            int additionalPaletteEntries = minimumPaletteSizeToMatchData - paletteSize;
            for (int i = 0; i < additionalPaletteEntries; ++i) {
                paletteList.add(airPalleteEntry);
            }
            return palette.createList(paletteList.stream());
        }
        return palette;
    }

    public static int ceillog2(int input) {
        if (input == 0) {
            return 0;
        }
        return (int)Math.ceil(Math.log(input) / Math.log(2.0));
    }

    private static /* synthetic */ Dynamic lambda$makeRule$5(Set blocksInChunk, Dynamic airContainer, int minSection, Dynamic[] biomeContainers, IntSet doneSections, MutableObject bedrockSectionBlocks, Dynamic section) {
        int sectionY = section.get("Y").asInt(0);
        Dynamic blockStatesContainer = (Dynamic)DataFixUtils.orElse(section.get("Palette").result().flatMap(palette -> {
            palette.asStream().map(blockState -> blockState.get("Name").asString("minecraft:air")).forEach(blocksInChunk::add);
            return section.get("BlockStates").result().map(blockStates -> ChunkHeightAndBiomeFix.makeOptimizedPalettedContainer(palette, blockStates));
        }), (Object)airContainer);
        Dynamic result = section;
        int sectionYIndex = sectionY - minSection;
        if (sectionYIndex >= 0 && sectionYIndex < biomeContainers.length) {
            result = result.set("biomes", biomeContainers[sectionYIndex]);
        }
        doneSections.add(sectionY);
        if (section.get("Y").asInt(Integer.MAX_VALUE) == 0) {
            bedrockSectionBlocks.setValue(() -> {
                List palette = blockStatesContainer.get("palette").asList(Function.identity());
                long[] data = blockStatesContainer.get("data").asLongStream().toArray();
                return new ChunkProtoTickListFix.PoorMansPalettedContainer(palette, data);
            });
        }
        return result.set("block_states", blockStatesContainer).remove("Palette").remove("BlockStates");
    }

    static {
        BIOMES_BY_ID.put(0, (Object)"minecraft:ocean");
        BIOMES_BY_ID.put(1, (Object)DEFAULT_BIOME);
        BIOMES_BY_ID.put(2, (Object)"minecraft:desert");
        BIOMES_BY_ID.put(3, (Object)"minecraft:mountains");
        BIOMES_BY_ID.put(4, (Object)"minecraft:forest");
        BIOMES_BY_ID.put(5, (Object)"minecraft:taiga");
        BIOMES_BY_ID.put(6, (Object)"minecraft:swamp");
        BIOMES_BY_ID.put(7, (Object)"minecraft:river");
        BIOMES_BY_ID.put(8, (Object)"minecraft:nether_wastes");
        BIOMES_BY_ID.put(9, (Object)"minecraft:the_end");
        BIOMES_BY_ID.put(10, (Object)"minecraft:frozen_ocean");
        BIOMES_BY_ID.put(11, (Object)"minecraft:frozen_river");
        BIOMES_BY_ID.put(12, (Object)"minecraft:snowy_tundra");
        BIOMES_BY_ID.put(13, (Object)"minecraft:snowy_mountains");
        BIOMES_BY_ID.put(14, (Object)"minecraft:mushroom_fields");
        BIOMES_BY_ID.put(15, (Object)"minecraft:mushroom_field_shore");
        BIOMES_BY_ID.put(16, (Object)"minecraft:beach");
        BIOMES_BY_ID.put(17, (Object)"minecraft:desert_hills");
        BIOMES_BY_ID.put(18, (Object)"minecraft:wooded_hills");
        BIOMES_BY_ID.put(19, (Object)"minecraft:taiga_hills");
        BIOMES_BY_ID.put(20, (Object)"minecraft:mountain_edge");
        BIOMES_BY_ID.put(21, (Object)"minecraft:jungle");
        BIOMES_BY_ID.put(22, (Object)"minecraft:jungle_hills");
        BIOMES_BY_ID.put(23, (Object)"minecraft:jungle_edge");
        BIOMES_BY_ID.put(24, (Object)"minecraft:deep_ocean");
        BIOMES_BY_ID.put(25, (Object)"minecraft:stone_shore");
        BIOMES_BY_ID.put(26, (Object)"minecraft:snowy_beach");
        BIOMES_BY_ID.put(27, (Object)"minecraft:birch_forest");
        BIOMES_BY_ID.put(28, (Object)"minecraft:birch_forest_hills");
        BIOMES_BY_ID.put(29, (Object)"minecraft:dark_forest");
        BIOMES_BY_ID.put(30, (Object)"minecraft:snowy_taiga");
        BIOMES_BY_ID.put(31, (Object)"minecraft:snowy_taiga_hills");
        BIOMES_BY_ID.put(32, (Object)"minecraft:giant_tree_taiga");
        BIOMES_BY_ID.put(33, (Object)"minecraft:giant_tree_taiga_hills");
        BIOMES_BY_ID.put(34, (Object)"minecraft:wooded_mountains");
        BIOMES_BY_ID.put(35, (Object)"minecraft:savanna");
        BIOMES_BY_ID.put(36, (Object)"minecraft:savanna_plateau");
        BIOMES_BY_ID.put(37, (Object)"minecraft:badlands");
        BIOMES_BY_ID.put(38, (Object)"minecraft:wooded_badlands_plateau");
        BIOMES_BY_ID.put(39, (Object)"minecraft:badlands_plateau");
        BIOMES_BY_ID.put(40, (Object)"minecraft:small_end_islands");
        BIOMES_BY_ID.put(41, (Object)"minecraft:end_midlands");
        BIOMES_BY_ID.put(42, (Object)"minecraft:end_highlands");
        BIOMES_BY_ID.put(43, (Object)"minecraft:end_barrens");
        BIOMES_BY_ID.put(44, (Object)"minecraft:warm_ocean");
        BIOMES_BY_ID.put(45, (Object)"minecraft:lukewarm_ocean");
        BIOMES_BY_ID.put(46, (Object)"minecraft:cold_ocean");
        BIOMES_BY_ID.put(47, (Object)"minecraft:deep_warm_ocean");
        BIOMES_BY_ID.put(48, (Object)"minecraft:deep_lukewarm_ocean");
        BIOMES_BY_ID.put(49, (Object)"minecraft:deep_cold_ocean");
        BIOMES_BY_ID.put(50, (Object)"minecraft:deep_frozen_ocean");
        BIOMES_BY_ID.put(127, (Object)"minecraft:the_void");
        BIOMES_BY_ID.put(129, (Object)"minecraft:sunflower_plains");
        BIOMES_BY_ID.put(130, (Object)"minecraft:desert_lakes");
        BIOMES_BY_ID.put(131, (Object)"minecraft:gravelly_mountains");
        BIOMES_BY_ID.put(132, (Object)"minecraft:flower_forest");
        BIOMES_BY_ID.put(133, (Object)"minecraft:taiga_mountains");
        BIOMES_BY_ID.put(134, (Object)"minecraft:swamp_hills");
        BIOMES_BY_ID.put(140, (Object)"minecraft:ice_spikes");
        BIOMES_BY_ID.put(149, (Object)"minecraft:modified_jungle");
        BIOMES_BY_ID.put(151, (Object)"minecraft:modified_jungle_edge");
        BIOMES_BY_ID.put(155, (Object)"minecraft:tall_birch_forest");
        BIOMES_BY_ID.put(156, (Object)"minecraft:tall_birch_hills");
        BIOMES_BY_ID.put(157, (Object)"minecraft:dark_forest_hills");
        BIOMES_BY_ID.put(158, (Object)"minecraft:snowy_taiga_mountains");
        BIOMES_BY_ID.put(160, (Object)"minecraft:giant_spruce_taiga");
        BIOMES_BY_ID.put(161, (Object)"minecraft:giant_spruce_taiga_hills");
        BIOMES_BY_ID.put(162, (Object)"minecraft:modified_gravelly_mountains");
        BIOMES_BY_ID.put(163, (Object)"minecraft:shattered_savanna");
        BIOMES_BY_ID.put(164, (Object)"minecraft:shattered_savanna_plateau");
        BIOMES_BY_ID.put(165, (Object)"minecraft:eroded_badlands");
        BIOMES_BY_ID.put(166, (Object)"minecraft:modified_wooded_badlands_plateau");
        BIOMES_BY_ID.put(167, (Object)"minecraft:modified_badlands_plateau");
        BIOMES_BY_ID.put(168, (Object)"minecraft:bamboo_jungle");
        BIOMES_BY_ID.put(169, (Object)"minecraft:bamboo_jungle_hills");
        BIOMES_BY_ID.put(170, (Object)"minecraft:soul_sand_valley");
        BIOMES_BY_ID.put(171, (Object)"minecraft:crimson_forest");
        BIOMES_BY_ID.put(172, (Object)"minecraft:warped_forest");
        BIOMES_BY_ID.put(173, (Object)"minecraft:basalt_deltas");
        BIOMES_BY_ID.put(174, (Object)"minecraft:dripstone_caves");
        BIOMES_BY_ID.put(175, (Object)"minecraft:lush_caves");
        BIOMES_BY_ID.put(177, (Object)"minecraft:meadow");
        BIOMES_BY_ID.put(178, (Object)"minecraft:grove");
        BIOMES_BY_ID.put(179, (Object)"minecraft:snowy_slopes");
        BIOMES_BY_ID.put(180, (Object)"minecraft:snowcapped_peaks");
        BIOMES_BY_ID.put(181, (Object)"minecraft:lofty_peaks");
        BIOMES_BY_ID.put(182, (Object)"minecraft:stony_peaks");
    }
}

