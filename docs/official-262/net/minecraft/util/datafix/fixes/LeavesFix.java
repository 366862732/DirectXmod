/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.types.templates.List$ListType
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.ints.Int2IntMap
 *  it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntIterator
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.util.datafix.PackedBitStorage;
import net.minecraft.util.datafix.fixes.References;
import org.jspecify.annotations.Nullable;

public class LeavesFix
extends DataFix {
    private static final int NORTH_WEST_MASK = 128;
    private static final int WEST_MASK = 64;
    private static final int SOUTH_WEST_MASK = 32;
    private static final int SOUTH_MASK = 16;
    private static final int SOUTH_EAST_MASK = 8;
    private static final int EAST_MASK = 4;
    private static final int NORTH_EAST_MASK = 2;
    private static final int NORTH_MASK = 1;
    private static final int[][] DIRECTIONS = new int[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
    private static final int DECAY_DISTANCE = 7;
    private static final int SIZE_BITS = 12;
    private static final int SIZE = 4096;
    private static final Object2IntMap<String> LEAVES = (Object2IntMap)DataFixUtils.make((Object)new Object2IntOpenHashMap(), map -> {
        map.put((Object)"minecraft:acacia_leaves", 0);
        map.put((Object)"minecraft:birch_leaves", 1);
        map.put((Object)"minecraft:dark_oak_leaves", 2);
        map.put((Object)"minecraft:jungle_leaves", 3);
        map.put((Object)"minecraft:oak_leaves", 4);
        map.put((Object)"minecraft:spruce_leaves", 5);
    });
    private static final Set<String> LOGS = ImmutableSet.of((Object)"minecraft:acacia_bark", (Object)"minecraft:birch_bark", (Object)"minecraft:dark_oak_bark", (Object)"minecraft:jungle_bark", (Object)"minecraft:oak_bark", (Object)"minecraft:spruce_bark", (Object[])new String[]{"minecraft:acacia_log", "minecraft:birch_log", "minecraft:dark_oak_log", "minecraft:jungle_log", "minecraft:oak_log", "minecraft:spruce_log", "minecraft:stripped_acacia_log", "minecraft:stripped_birch_log", "minecraft:stripped_dark_oak_log", "minecraft:stripped_jungle_log", "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log"});

    public LeavesFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder levelFinder = chunkType.findField("Level");
        OpticFinder sectionsFinder = levelFinder.type().findField("Sections");
        Type sectionsType = sectionsFinder.type();
        if (!(sectionsType instanceof List.ListType)) {
            throw new IllegalStateException("Expecting sections to be a list.");
        }
        Type sectionType = ((List.ListType)sectionsType).getElement();
        OpticFinder sectionFinder = DSL.typeFinder((Type)sectionType);
        return this.fixTypeEverywhereTyped("Leaves fix", chunkType, chunk -> chunk.updateTyped(levelFinder, level -> {
            int[] sides = new int[]{0};
            Typed newLevel = level.updateTyped(sectionsFinder, sections -> {
                Int2ObjectOpenHashMap sectionMap = new Int2ObjectOpenHashMap(sections.getAllTyped(sectionFinder).stream().map(section -> new LeavesSection((Typed<?>)section, this.getInputSchema())).collect(Collectors.toMap(Section::getIndex, s -> s)));
                if (sectionMap.values().stream().allMatch(Section::isSkippable)) {
                    return sections;
                }
                ArrayList queue = Lists.newArrayList();
                for (int i = 0; i < 7; ++i) {
                    queue.add(new IntOpenHashSet());
                }
                for (LeavesSection section2 : sectionMap.values()) {
                    if (section2.isSkippable()) continue;
                    for (int i = 0; i < 4096; ++i) {
                        int block = section2.getBlock(i);
                        if (section2.isLog(block)) {
                            ((IntSet)queue.get(0)).add(section2.getIndex() << 12 | i);
                            continue;
                        }
                        if (!section2.isLeaf(block)) continue;
                        int x = this.getX(i);
                        int z = this.getZ(i);
                        sides[0] = sides[0] | LeavesFix.getSideMask(x == 0, x == 15, z == 0, z == 15);
                    }
                }
                for (int i = 1; i < 7; ++i) {
                    IntSet set = (IntSet)queue.get(i - 1);
                    IntSet newSet = (IntSet)queue.get(i);
                    IntIterator iterator = set.iterator();
                    while (iterator.hasNext()) {
                        int posChunk = iterator.nextInt();
                        int x = this.getX(posChunk);
                        int y = this.getY(posChunk);
                        int z = this.getZ(posChunk);
                        for (int[] direction : DIRECTIONS) {
                            int oldDistance;
                            int posSection;
                            int block;
                            LeavesSection section3;
                            int nx = x + direction[0];
                            int nyChunk = y + direction[1];
                            int nz = z + direction[2];
                            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || nyChunk < 0 || nyChunk > 255 || (section3 = (LeavesSection)sectionMap.get(nyChunk >> 4)) == null || section3.isSkippable() || !section3.isLeaf(block = section3.getBlock(posSection = LeavesFix.getIndex(nx, nyChunk & 0xF, nz))) || (oldDistance = section3.getDistance(block)) <= i) continue;
                            section3.setDistance(posSection, block, i);
                            newSet.add(LeavesFix.getIndex(nx, nyChunk, nz));
                        }
                    }
                }
                return sections.updateTyped(sectionFinder, arg_0 -> LeavesFix.lambda$makeRule$5((Int2ObjectMap)sectionMap, arg_0));
            });
            if (sides[0] != 0) {
                newLevel = newLevel.update(DSL.remainderFinder(), tag -> {
                    Dynamic upgradeData = (Dynamic)DataFixUtils.orElse((Optional)tag.get("UpgradeData").result(), (Object)tag.emptyMap());
                    return tag.set("UpgradeData", upgradeData.set("Sides", tag.createByte((byte)(upgradeData.get("Sides").asByte((byte)0) | sides[0]))));
                });
            }
            return newLevel;
        }));
    }

    public static int getIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private int getX(int index) {
        return index & 0xF;
    }

    private int getY(int index) {
        return index >> 8 & 0xFF;
    }

    private int getZ(int index) {
        return index >> 4 & 0xF;
    }

    public static int getSideMask(boolean west, boolean east, boolean north, boolean south) {
        int s = 0;
        if (north) {
            s = east ? (s |= 2) : (west ? (s |= 0x80) : (s |= 1));
        } else if (south) {
            s = west ? (s |= 0x20) : (east ? (s |= 8) : (s |= 0x10));
        } else if (east) {
            s |= 4;
        } else if (west) {
            s |= 0x40;
        }
        return s;
    }

    private static /* synthetic */ Typed lambda$makeRule$5(Int2ObjectMap sectionMap, Typed section) {
        return ((LeavesSection)sectionMap.get(((Dynamic)section.get(DSL.remainderFinder())).get("Y").asInt(0))).write(section);
    }

    public static final class LeavesSection
    extends Section {
        private static final String PERSISTENT = "persistent";
        private static final String DECAYABLE = "decayable";
        private static final String DISTANCE = "distance";
        private @Nullable IntSet leaveIds;
        private @Nullable IntSet logIds;
        private @Nullable Int2IntMap stateToIdMap;

        public LeavesSection(Typed<?> section, Schema inputSchema) {
            super(section, inputSchema);
        }

        @Override
        protected boolean skippable() {
            this.leaveIds = new IntOpenHashSet();
            this.logIds = new IntOpenHashSet();
            this.stateToIdMap = new Int2IntOpenHashMap();
            for (int i = 0; i < this.palette.size(); ++i) {
                Dynamic paletteTag = (Dynamic)this.palette.get(i);
                String blockName = paletteTag.get("Name").asString("");
                if (LEAVES.containsKey((Object)blockName)) {
                    boolean persistent = Objects.equals(paletteTag.get("Properties").get(DECAYABLE).asString(""), "false");
                    this.leaveIds.add(i);
                    this.stateToIdMap.put(LeavesSection.getStateId(blockName, persistent, 7), i);
                    this.palette.set(i, this.makeLeafTag(paletteTag, blockName, persistent, 7));
                }
                if (!LOGS.contains(blockName)) continue;
                this.logIds.add(i);
            }
            return this.leaveIds.isEmpty() && this.logIds.isEmpty();
        }

        private Dynamic<?> makeLeafTag(Dynamic<?> input, String blockName, boolean persistent, int distance) {
            Dynamic properties = input.emptyMap();
            properties = properties.set(PERSISTENT, properties.createString(persistent ? "true" : "false"));
            properties = properties.set(DISTANCE, properties.createString(Integer.toString(distance)));
            Dynamic tag = input.emptyMap();
            tag = tag.set("Properties", properties);
            tag = tag.set("Name", tag.createString(blockName));
            return tag;
        }

        public boolean isLog(int block) {
            return this.logIds.contains(block);
        }

        public boolean isLeaf(int block) {
            return this.leaveIds.contains(block);
        }

        private int getDistance(int block) {
            if (this.isLog(block)) {
                return 0;
            }
            return Integer.parseInt(((Dynamic)this.palette.get(block)).get("Properties").get(DISTANCE).asString(""));
        }

        private void setDistance(int pos, int block, int distance) {
            int id;
            boolean persistent;
            Dynamic baseTag = (Dynamic)this.palette.get(block);
            String blockName = baseTag.get("Name").asString("");
            int stateId = LeavesSection.getStateId(blockName, persistent = Objects.equals(baseTag.get("Properties").get(PERSISTENT).asString(""), "true"), distance);
            if (!this.stateToIdMap.containsKey(stateId)) {
                id = this.palette.size();
                this.leaveIds.add(id);
                this.stateToIdMap.put(stateId, id);
                this.palette.add(this.makeLeafTag(baseTag, blockName, persistent, distance));
            }
            id = this.stateToIdMap.get(stateId);
            if (1 << this.storage.getBits() <= id) {
                PackedBitStorage newStorage = new PackedBitStorage(this.storage.getBits() + 1, 4096);
                for (int i = 0; i < 4096; ++i) {
                    newStorage.set(i, this.storage.get(i));
                }
                this.storage = newStorage;
            }
            this.storage.set(pos, id);
        }
    }

    public static abstract class Section {
        protected static final String BLOCK_STATES_TAG = "BlockStates";
        protected static final String NAME_TAG = "Name";
        protected static final String PROPERTIES_TAG = "Properties";
        private final Type<Pair<String, Dynamic<?>>> blockStateType = DSL.named((String)References.BLOCK_STATE.typeName(), (Type)DSL.remainderType());
        protected final OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder = DSL.fieldFinder((String)"Palette", (Type)DSL.list(this.blockStateType));
        protected final List<Dynamic<?>> palette;
        protected final int index;
        protected @Nullable PackedBitStorage storage;

        public Section(Typed<?> section, Schema inputSchema) {
            if (!Objects.equals(inputSchema.getType(References.BLOCK_STATE), this.blockStateType)) {
                throw new IllegalStateException("Block state type is not what was expected.");
            }
            Optional typedPalette = section.getOptional(this.paletteFinder);
            this.palette = typedPalette.map(p -> p.stream().map(Pair::getSecond).collect(Collectors.toList())).orElse((List)ImmutableList.of());
            Dynamic tag = (Dynamic)section.get(DSL.remainderFinder());
            this.index = tag.get("Y").asInt(0);
            this.readStorage(tag);
        }

        protected void readStorage(Dynamic<?> tag) {
            if (this.skippable()) {
                this.storage = null;
            } else {
                long[] states = tag.get(BLOCK_STATES_TAG).asLongStream().toArray();
                int size = Math.max(4, DataFixUtils.ceillog2((int)this.palette.size()));
                this.storage = new PackedBitStorage(size, 4096, states);
            }
        }

        public Typed<?> write(Typed<?> section) {
            if (this.isSkippable()) {
                return section;
            }
            return section.update(DSL.remainderFinder(), tag -> tag.set(BLOCK_STATES_TAG, tag.createLongList(Arrays.stream(this.storage.getRaw())))).set(this.paletteFinder, this.palette.stream().map(b -> Pair.of((Object)References.BLOCK_STATE.typeName(), (Object)b)).collect(Collectors.toList()));
        }

        public boolean isSkippable() {
            return this.storage == null;
        }

        public int getBlock(int pos) {
            return this.storage.get(pos);
        }

        protected static int getStateId(String blockName, boolean persistent, int distance) {
            return LEAVES.get((Object)blockName) << 5 | (persistent ? 16 : 0) | distance;
        }

        public int getIndex() {
            return this.index;
        }

        protected abstract boolean skippable();
    }
}

