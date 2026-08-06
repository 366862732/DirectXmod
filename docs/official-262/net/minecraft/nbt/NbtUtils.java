/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.Splitter
 *  com.google.common.base.Strings
 *  com.google.common.collect.Comparators
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.PrimitiveTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class NbtUtils {
    private static final Comparator<ListTag> YXZ_LISTTAG_INT_COMPARATOR = Comparator.comparingInt(list -> list.getIntOr(1, 0)).thenComparingInt(list -> list.getIntOr(0, 0)).thenComparingInt(list -> list.getIntOr(2, 0));
    private static final Comparator<ListTag> YXZ_LISTTAG_DOUBLE_COMPARATOR = Comparator.comparingDouble(list -> list.getDoubleOr(1, 0.0)).thenComparingDouble(list -> list.getDoubleOr(0, 0.0)).thenComparingDouble(list -> list.getDoubleOr(2, 0.0));
    private static final Codec<ResourceKey<Block>> BLOCK_NAME_CODEC = ResourceKey.codec(Registries.BLOCK);
    public static final String SNBT_DATA_TAG = "data";
    private static final char PROPERTIES_START = '{';
    private static final char PROPERTIES_END = '}';
    private static final String ELEMENT_SEPARATOR = ",";
    private static final char KEY_VALUE_SEPARATOR = ':';
    private static final Splitter COMMA_SPLITTER = Splitter.on((String)",");
    private static final Splitter COLON_SPLITTER = Splitter.on((char)':').limit(2);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INDENT = 2;
    private static final int NOT_FOUND = -1;

    private NbtUtils() {
    }

    @VisibleForTesting
    public static boolean compareNbt(@Nullable Tag expected, @Nullable Tag actual, boolean partialListMatches) {
        if (expected == actual) {
            return true;
        }
        if (expected == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        if (!expected.getClass().equals(actual.getClass())) {
            return false;
        }
        if (expected instanceof CompoundTag) {
            CompoundTag expectedCompound = (CompoundTag)expected;
            CompoundTag actualCompound = (CompoundTag)actual;
            if (actualCompound.size() < expectedCompound.size()) {
                return false;
            }
            for (Map.Entry<String, Tag> entry : expectedCompound.entrySet()) {
                Tag tag = entry.getValue();
                if (NbtUtils.compareNbt(tag, actualCompound.get(entry.getKey()), partialListMatches)) continue;
                return false;
            }
            return true;
        }
        if (expected instanceof ListTag) {
            ListTag expectedList = (ListTag)expected;
            if (partialListMatches) {
                ListTag actualList = (ListTag)actual;
                if (expectedList.isEmpty()) {
                    return actualList.isEmpty();
                }
                if (actualList.size() < expectedList.size()) {
                    return false;
                }
                for (Tag tag : expectedList) {
                    boolean found = false;
                    for (Tag value : actualList) {
                        if (!NbtUtils.compareNbt(tag, value, partialListMatches)) continue;
                        found = true;
                        break;
                    }
                    if (found) continue;
                    return false;
                }
                return true;
            }
        }
        return expected.equals(actual);
    }

    public static BlockState readBlockState(HolderGetter<Block> blocks, CompoundTag tag) {
        Optional blockHolder = tag.read("Name", BLOCK_NAME_CODEC).flatMap(blocks::get);
        if (blockHolder.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        Block block = (Block)((Holder)blockHolder.get()).value();
        BlockState result = block.defaultBlockState();
        Optional<CompoundTag> properties = tag.getCompound("Properties");
        if (properties.isPresent()) {
            StateDefinition<Block, BlockState> definition = block.getStateDefinition();
            for (String key : properties.get().keySet()) {
                Property<?> property = definition.getProperty(key);
                if (property == null) continue;
                result = NbtUtils.setValueHelper(result, property, key, properties.get(), tag);
            }
        }
        return result;
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S result, Property<T> property, String key, CompoundTag properties, CompoundTag tag) {
        Optional value = properties.getString(key).flatMap(property::getValue);
        if (value.isPresent()) {
            return (S)((StateHolder)result.setValue(property, (Comparable)((Comparable)value.get())));
        }
        LOGGER.warn("Unable to read property: {} with value: {} for blockstate: {}", new Object[]{key, properties.get(key), tag});
        return result;
    }

    private static void writeStateProperties(StateHolder<?, ?> state, CompoundTag tag) {
        if (!state.isSingletonState()) {
            CompoundTag properties = new CompoundTag();
            state.getValues().forEach(value -> properties.putString(value.property().getName(), value.valueName()));
            tag.put("Properties", properties);
        }
    }

    public static CompoundTag writeBlockState(BlockState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        NbtUtils.writeStateProperties(state, tag);
        return tag;
    }

    public static CompoundTag writeFluidState(FluidState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", BuiltInRegistries.FLUID.getKey(state.getType()).toString());
        NbtUtils.writeStateProperties(state, tag);
        return tag;
    }

    public static String prettyPrint(Tag tag, boolean withBinaryBlobs) {
        return NbtUtils.prettyPrint(new StringBuilder(), tag, 0, withBinaryBlobs).toString();
    }

    public static StringBuilder prettyPrint(StringBuilder builder, Tag input, int indent, boolean withBinaryBlobs) {
        Tag tag = input;
        Objects.requireNonNull(tag);
        Tag tag2 = tag;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{PrimitiveTag.class, EndTag.class, ByteArrayTag.class, ListTag.class, IntArrayTag.class, CompoundTag.class, LongArrayTag.class}, (Tag)tag2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                PrimitiveTag primitive = (PrimitiveTag)tag2;
                yield builder.append(primitive);
            }
            case 1 -> {
                EndTag ignored = (EndTag)tag2;
                yield builder;
            }
            case 2 -> {
                ByteArrayTag tag = (ByteArrayTag)tag2;
                byte[] array = tag.getAsByteArray();
                int length = array.length;
                NbtUtils.indent(indent, builder).append("byte[").append(length).append("] {\n");
                if (withBinaryBlobs) {
                    NbtUtils.indent(indent + 1, builder);
                    for (int i = 0; i < array.length; ++i) {
                        if (i != 0) {
                            builder.append(',');
                        }
                        if (i % 16 == 0 && i / 16 > 0) {
                            builder.append('\n');
                            if (i < array.length) {
                                NbtUtils.indent(indent + 1, builder);
                            }
                        } else if (i != 0) {
                            builder.append(' ');
                        }
                        builder.append(String.format(Locale.ROOT, "0x%02X", array[i] & 0xFF));
                    }
                } else {
                    NbtUtils.indent(indent + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
                }
                builder.append('\n');
                NbtUtils.indent(indent, builder).append('}');
                yield builder;
            }
            case 3 -> {
                ListTag tag = (ListTag)tag2;
                int size = tag.size();
                NbtUtils.indent(indent, builder).append("list").append("[").append(size).append("] [");
                if (size != 0) {
                    builder.append('\n');
                }
                for (int i = 0; i < size; ++i) {
                    if (i != 0) {
                        builder.append(",\n");
                    }
                    NbtUtils.indent(indent + 1, builder);
                    NbtUtils.prettyPrint(builder, tag.get(i), indent + 1, withBinaryBlobs);
                }
                if (size != 0) {
                    builder.append('\n');
                }
                NbtUtils.indent(indent, builder).append(']');
                yield builder;
            }
            case 4 -> {
                IntArrayTag tag = (IntArrayTag)tag2;
                int[] array = tag.getAsIntArray();
                int size = 0;
                for (int i : array) {
                    size = Math.max(size, String.format(Locale.ROOT, "%X", i).length());
                }
                int length = array.length;
                NbtUtils.indent(indent, builder).append("int[").append(length).append("] {\n");
                if (withBinaryBlobs) {
                    NbtUtils.indent(indent + 1, builder);
                    for (int i = 0; i < array.length; ++i) {
                        if (i != 0) {
                            builder.append(',');
                        }
                        if (i % 16 == 0 && i / 16 > 0) {
                            builder.append('\n');
                            if (i < array.length) {
                                NbtUtils.indent(indent + 1, builder);
                            }
                        } else if (i != 0) {
                            builder.append(' ');
                        }
                        builder.append(String.format(Locale.ROOT, "0x%0" + size + "X", array[i]));
                    }
                } else {
                    NbtUtils.indent(indent + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
                }
                builder.append('\n');
                NbtUtils.indent(indent, builder).append('}');
                yield builder;
            }
            case 5 -> {
                CompoundTag tag = (CompoundTag)tag2;
                ArrayList keys = Lists.newArrayList(tag.keySet());
                Collections.sort(keys);
                NbtUtils.indent(indent, builder).append('{');
                if (builder.length() - builder.lastIndexOf("\n") > 2 * (indent + 1)) {
                    builder.append('\n');
                    NbtUtils.indent(indent + 1, builder);
                }
                int paddingLength = keys.stream().mapToInt(String::length).max().orElse(0);
                String padding = Strings.repeat((String)" ", (int)paddingLength);
                for (int i = 0; i < keys.size(); ++i) {
                    if (i != 0) {
                        builder.append(",\n");
                    }
                    String key = (String)keys.get(i);
                    NbtUtils.indent(indent + 1, builder).append('\"').append(key).append('\"').append(padding, 0, padding.length() - key.length()).append(": ");
                    NbtUtils.prettyPrint(builder, tag.get(key), indent + 1, withBinaryBlobs);
                }
                if (!keys.isEmpty()) {
                    builder.append('\n');
                }
                NbtUtils.indent(indent, builder).append('}');
                yield builder;
            }
            case 6 -> {
                LongArrayTag tag = (LongArrayTag)tag2;
                long[] array = tag.getAsLongArray();
                long size = 0L;
                for (long i : array) {
                    size = Math.max(size, (long)String.format(Locale.ROOT, "%X", i).length());
                }
                long length = array.length;
                NbtUtils.indent(indent, builder).append("long[").append(length).append("] {\n");
                if (withBinaryBlobs) {
                    NbtUtils.indent(indent + 1, builder);
                    for (int i = 0; i < array.length; ++i) {
                        if (i != 0) {
                            builder.append(',');
                        }
                        if (i % 16 == 0 && i / 16 > 0) {
                            builder.append('\n');
                            if (i < array.length) {
                                NbtUtils.indent(indent + 1, builder);
                            }
                        } else if (i != 0) {
                            builder.append(' ');
                        }
                        builder.append(String.format(Locale.ROOT, "0x%0" + size + "X", array[i]));
                    }
                } else {
                    NbtUtils.indent(indent + 1, builder).append(" // Skipped, supply withBinaryBlobs true");
                }
                builder.append('\n');
                NbtUtils.indent(indent, builder).append('}');
                yield builder;
            }
        };
    }

    private static StringBuilder indent(int indent, StringBuilder builder) {
        int index = builder.lastIndexOf("\n") + 1;
        int len = builder.length() - index;
        for (int i = 0; i < 2 * indent - len; ++i) {
            builder.append(' ');
        }
        return builder;
    }

    public static Component toPrettyComponent(Tag tag) {
        return new TextComponentTagVisitor("").visit(tag);
    }

    public static String structureToSnbt(CompoundTag structure) {
        return new SnbtPrinterTagVisitor().visit(NbtUtils.packStructureTemplate(structure));
    }

    public static CompoundTag snbtToStructure(String snbt) throws CommandSyntaxException {
        return NbtUtils.unpackStructureTemplate(TagParser.parseCompoundFully(snbt));
    }

    @VisibleForTesting
    static CompoundTag packStructureTemplate(CompoundTag snbt) {
        Optional<ListTag> oldEntities;
        Optional<ListTag> palettes = snbt.getList("palettes");
        ListTag palette = palettes.isPresent() ? palettes.get().getListOrEmpty(0) : snbt.getListOrEmpty("palette");
        ListTag deflatedPalette = palette.compoundStream().map(NbtUtils::packBlockState).map(StringTag::valueOf).collect(Collectors.toCollection(ListTag::new));
        snbt.put("palette", deflatedPalette);
        if (palettes.isPresent()) {
            ListTag newPalettes = new ListTag();
            palettes.get().stream().flatMap(tag -> tag.asList().stream()).forEach(oldPalette -> {
                CompoundTag newPalette = new CompoundTag();
                for (int i = 0; i < oldPalette.size(); ++i) {
                    newPalette.putString(deflatedPalette.getString(i).orElseThrow(), NbtUtils.packBlockState(oldPalette.getCompound(i).orElseThrow()));
                }
                newPalettes.add(newPalette);
            });
            snbt.put("palettes", newPalettes);
        }
        if ((oldEntities = snbt.getList("entities")).isPresent()) {
            ListTag newEntities = oldEntities.get().compoundStream().sorted(Comparator.comparing(tag -> tag.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_DOUBLE_COMPARATOR))).collect(Collectors.toCollection(ListTag::new));
            snbt.put("entities", newEntities);
        }
        ListTag blockData = snbt.getList("blocks").stream().flatMap(ListTag::compoundStream).sorted(Comparator.comparing(tag -> tag.getList("pos"), Comparators.emptiesLast(YXZ_LISTTAG_INT_COMPARATOR))).peek(block -> block.putString("state", deflatedPalette.getString(block.getIntOr("state", 0)).orElseThrow())).collect(Collectors.toCollection(ListTag::new));
        snbt.put(SNBT_DATA_TAG, blockData);
        snbt.remove("blocks");
        return snbt;
    }

    @VisibleForTesting
    static CompoundTag unpackStructureTemplate(CompoundTag template) {
        ListTag packedPalette = template.getListOrEmpty("palette");
        Map palette = (Map)packedPalette.stream().flatMap(tag -> tag.asString().stream()).collect(ImmutableMap.toImmutableMap(Function.identity(), NbtUtils::unpackBlockState));
        Optional<ListTag> oldPalettes = template.getList("palettes");
        if (oldPalettes.isPresent()) {
            template.put("palettes", oldPalettes.get().compoundStream().map(oldPalette -> palette.keySet().stream().map(key -> oldPalette.getString((String)key).orElseThrow()).map(NbtUtils::unpackBlockState).collect(Collectors.toCollection(ListTag::new))).collect(Collectors.toCollection(ListTag::new)));
            template.remove("palette");
        } else {
            template.put("palette", palette.values().stream().collect(Collectors.toCollection(ListTag::new)));
        }
        Optional<ListTag> maybeBlocks = template.getList(SNBT_DATA_TAG);
        if (maybeBlocks.isPresent()) {
            Object2IntOpenHashMap paletteToId = new Object2IntOpenHashMap();
            paletteToId.defaultReturnValue(-1);
            for (int i = 0; i < packedPalette.size(); ++i) {
                paletteToId.put((Object)packedPalette.getString(i).orElseThrow(), i);
            }
            ListTag blocks = maybeBlocks.get();
            for (int i = 0; i < blocks.size(); ++i) {
                CompoundTag block = blocks.getCompound(i).orElseThrow();
                String stateName = block.getString("state").orElseThrow();
                int stateId = paletteToId.getInt((Object)stateName);
                if (stateId == -1) {
                    throw new IllegalStateException("Entry " + stateName + " missing from palette");
                }
                block.putInt("state", stateId);
            }
            template.put("blocks", blocks);
            template.remove(SNBT_DATA_TAG);
        }
        return template;
    }

    @VisibleForTesting
    static String packBlockState(CompoundTag compound) {
        StringBuilder builder = new StringBuilder(compound.getString("Name").orElseThrow());
        compound.getCompound("Properties").ifPresent(properties -> {
            String keyValues = properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> (String)entry.getKey() + ":" + ((Tag)entry.getValue()).asString().orElseThrow()).collect(Collectors.joining(ELEMENT_SEPARATOR));
            builder.append('{').append(keyValues).append('}');
        });
        return builder.toString();
    }

    @VisibleForTesting
    static CompoundTag unpackBlockState(String compound) {
        String name;
        CompoundTag tag = new CompoundTag();
        int openIndex = compound.indexOf(123);
        if (openIndex >= 0) {
            name = compound.substring(0, openIndex);
            CompoundTag properties = new CompoundTag();
            if (openIndex + 2 <= compound.length()) {
                String values = compound.substring(openIndex + 1, compound.indexOf(125, openIndex));
                COMMA_SPLITTER.split((CharSequence)values).forEach(keyValue -> {
                    List parts = COLON_SPLITTER.splitToList((CharSequence)keyValue);
                    if (parts.size() == 2) {
                        properties.putString((String)parts.get(0), (String)parts.get(1));
                    } else {
                        LOGGER.error("Something went wrong parsing: '{}' -- incorrect gamedata!", (Object)compound);
                    }
                });
                tag.put("Properties", properties);
            }
        } else {
            name = compound;
        }
        tag.putString("Name", name);
        return tag;
    }

    public static CompoundTag addCurrentDataVersion(CompoundTag tag) {
        int version = SharedConstants.getCurrentVersion().dataVersion().version();
        return NbtUtils.addDataVersion(tag, version);
    }

    public static CompoundTag addDataVersion(CompoundTag tag, int version) {
        tag.putInt("DataVersion", version);
        return tag;
    }

    public static <T> Dynamic<T> addDataVersion(Dynamic<T> tag, int version) {
        return tag.set("DataVersion", tag.createInt(version));
    }

    public static void addCurrentDataVersion(ValueOutput output) {
        int version = SharedConstants.getCurrentVersion().dataVersion().version();
        NbtUtils.addDataVersion(output, version);
    }

    public static void addDataVersion(ValueOutput output, int version) {
        output.putInt("DataVersion", version);
    }

    public static int getDataVersion(CompoundTag tag) {
        return NbtUtils.getDataVersion(tag, -1);
    }

    public static int getDataVersion(CompoundTag tag, int _default) {
        return tag.getIntOr("DataVersion", _default);
    }

    public static int getDataVersion(Dynamic<?> dynamic) {
        return NbtUtils.getDataVersion(dynamic, -1);
    }

    public static int getDataVersion(Dynamic<?> dynamic, int _default) {
        return dynamic.get("DataVersion").asInt(_default);
    }
}

