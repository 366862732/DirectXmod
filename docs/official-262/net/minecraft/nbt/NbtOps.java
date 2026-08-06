/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 *  com.mojang.serialization.RecordBuilder$AbstractStringBuilder
 *  it.unimi.dsi.fastutil.bytes.ByteArrayList
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.lang.runtime.SwitchBootstraps;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtOps
implements DynamicOps<Tag> {
    public static final NbtOps INSTANCE = new NbtOps();

    private NbtOps() {
    }

    public Tag empty() {
        return EndTag.INSTANCE;
    }

    public Tag emptyList() {
        return new ListTag();
    }

    public Tag emptyMap() {
        return new CompoundTag();
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public <U> U convertTo(DynamicOps<U> outOps, Tag input) {
        byte by;
        Object object;
        Tag tag = input;
        Objects.requireNonNull(tag);
        Tag tag2 = tag;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{EndTag.class, ByteTag.class, ShortTag.class, IntTag.class, LongTag.class, FloatTag.class, DoubleTag.class, ByteArrayTag.class, StringTag.class, ListTag.class, CompoundTag.class, IntArrayTag.class, LongArrayTag.class}, (Tag)tag2, n)) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                EndTag ignored = (EndTag)tag2;
                object = outOps.empty();
                return (U)object;
            }
            case 1: {
                ByteTag byteTag = (ByteTag)tag2;
                try {
                    byte by2 = by = byteTag.value();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            byte value = by;
            object = outOps.createByte(value);
            return (U)object;
            case 2: {
                short s;
                ShortTag shortTag = (ShortTag)tag2;
                {
                    short s2 = s = shortTag.value();
                }
                short value2 = s;
                object = outOps.createShort(value2);
                return (U)object;
            }
            case 3: {
                int n2;
                IntTag intTag = (IntTag)tag2;
                {
                    int n3 = n2 = intTag.value();
                }
                int value3 = n2;
                object = outOps.createInt(value3);
                return (U)object;
            }
            case 4: {
                long l;
                LongTag longTag = (LongTag)tag2;
                {
                    long l2 = l = longTag.value();
                }
                long value4 = l;
                object = outOps.createLong(value4);
                return (U)object;
            }
            case 5: {
                float f;
                FloatTag floatTag = (FloatTag)tag2;
                {
                    float f2 = f = floatTag.value();
                }
                float value5 = f;
                object = outOps.createFloat(value5);
                return (U)object;
            }
            case 6: {
                double d;
                DoubleTag doubleTag = (DoubleTag)tag2;
                {
                    double d2 = d = doubleTag.value();
                }
                double value6 = d;
                object = outOps.createDouble(value6);
                return (U)object;
            }
            case 7: {
                ByteArrayTag byteArrayTag = (ByteArrayTag)tag2;
                object = outOps.createByteList(ByteBuffer.wrap(byteArrayTag.getAsByteArray()));
                return (U)object;
            }
            case 8: {
                String value7;
                StringTag stringTag = (StringTag)tag2;
                {
                    String string;
                    value7 = string = stringTag.value();
                }
                object = outOps.createString(value7);
                return (U)object;
            }
            case 9: {
                ListTag listTag = (ListTag)tag2;
                object = this.convertList(outOps, listTag);
                return (U)object;
            }
            case 10: {
                CompoundTag compoundTag = (CompoundTag)tag2;
                object = this.convertMap(outOps, compoundTag);
                return (U)object;
            }
            case 11: {
                IntArrayTag intArrayTag = (IntArrayTag)tag2;
                object = outOps.createIntList(Arrays.stream(intArrayTag.getAsIntArray()));
                return (U)object;
            }
            case 12: 
        }
        LongArrayTag longArrayTag = (LongArrayTag)tag2;
        object = outOps.createLongList(Arrays.stream(longArrayTag.getAsLongArray()));
        return (U)object;
    }

    public DataResult<Number> getNumberValue(Tag input) {
        return input.asNumber().map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
    }

    public Tag createNumeric(Number i) {
        return DoubleTag.valueOf(i.doubleValue());
    }

    public Tag createByte(byte value) {
        return ByteTag.valueOf(value);
    }

    public Tag createShort(short value) {
        return ShortTag.valueOf(value);
    }

    public Tag createInt(int value) {
        return IntTag.valueOf(value);
    }

    public Tag createLong(long value) {
        return LongTag.valueOf(value);
    }

    public Tag createFloat(float value) {
        return FloatTag.valueOf(value);
    }

    public Tag createDouble(double value) {
        return DoubleTag.valueOf(value);
    }

    public DataResult<Boolean> getBooleanValue(Tag input) {
        return this.getNumberValue(input).map(value -> value.doubleValue() != 0.0);
    }

    public Tag createBoolean(boolean value) {
        return ByteTag.valueOf(value);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public DataResult<String> getStringValue(Tag input) {
        String value;
        if (!(input instanceof StringTag)) return DataResult.error(() -> "Not a string");
        StringTag stringTag = (StringTag)input;
        try {
            String string;
            value = string = stringTag.value();
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
        return DataResult.success((Object)value);
    }

    public Tag createString(String value) {
        return StringTag.valueOf(value);
    }

    public DataResult<Tag> mergeToList(Tag list, Tag value) {
        return NbtOps.createCollector(list).map(collector -> DataResult.success((Object)collector.accept(value).result())).orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + String.valueOf(list), (Object)list));
    }

    public DataResult<Tag> mergeToList(Tag list, List<Tag> values) {
        return NbtOps.createCollector(list).map(collector -> DataResult.success((Object)collector.acceptAll(values).result())).orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + String.valueOf(list), (Object)list));
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public DataResult<Tag> mergeToMap(Tag map, Tag key, Tag value) {
        CompoundTag compoundTag;
        String stringKey;
        if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + String.valueOf(map), (Object)map);
        }
        if (!(key instanceof StringTag)) return DataResult.error(() -> "key is not a string: " + String.valueOf(key), (Object)map);
        StringTag stringTag = (StringTag)key;
        try {
            String string;
            stringKey = string = stringTag.value();
        }
        catch (Throwable throwable) {
            throw new MatchException(throwable.toString(), throwable);
        }
        if (map instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)map;
            compoundTag = tag.shallowCopy();
        } else {
            compoundTag = new CompoundTag();
        }
        CompoundTag output = compoundTag;
        output.put(stringKey, value);
        return DataResult.success((Object)output);
    }

    public DataResult<Tag> mergeToMap(Tag map, MapLike<Tag> values) {
        CompoundTag compoundTag;
        if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + String.valueOf(map), (Object)map);
        }
        Iterator valuesIterator = values.entries().iterator();
        if (!valuesIterator.hasNext()) {
            if (map == this.empty()) {
                return DataResult.success((Object)this.emptyMap());
            }
            return DataResult.success((Object)map);
        }
        if (map instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)map;
            compoundTag = tag.shallowCopy();
        } else {
            compoundTag = new CompoundTag();
        }
        CompoundTag output = compoundTag;
        ArrayList missed = new ArrayList();
        valuesIterator.forEachRemaining(entry -> {
            String stringKey;
            Tag key = (Tag)entry.getFirst();
            if (!(key instanceof StringTag)) {
                missed.add(key);
                return;
            }
            StringTag $b$0 = (StringTag)key;
            try {
                String patt1$temp;
                stringKey = patt1$temp = $b$0.value();
            }
            catch (Throwable throwable) {
                throw new MatchException(throwable.toString(), throwable);
            }
            output.put(stringKey, (Tag)entry.getSecond());
        });
        if (!missed.isEmpty()) {
            return DataResult.error(() -> "some keys are not strings: " + String.valueOf(missed), (Object)output);
        }
        return DataResult.success((Object)output);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public DataResult<Tag> mergeToMap(Tag map, Map<Tag, Tag> values) {
        CompoundTag compoundTag;
        if (!(map instanceof CompoundTag) && !(map instanceof EndTag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + String.valueOf(map), (Object)map);
        }
        if (values.isEmpty()) {
            if (map == this.empty()) {
                return DataResult.success((Object)this.emptyMap());
            }
            return DataResult.success((Object)map);
        }
        if (map instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)map;
            compoundTag = tag.shallowCopy();
        } else {
            compoundTag = new CompoundTag();
        }
        CompoundTag output = compoundTag;
        ArrayList<Tag> missed = new ArrayList<Tag>();
        for (Map.Entry<Tag, Tag> entry : values.entrySet()) {
            Tag key = entry.getKey();
            if (key instanceof StringTag) {
                StringTag stringTag = (StringTag)key;
                try {
                    String string;
                    String stringKey = string = stringTag.value();
                    output.put(stringKey, entry.getValue());
                    continue;
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            missed.add(key);
        }
        if (!missed.isEmpty()) {
            return DataResult.error(() -> "some keys are not strings: " + String.valueOf(missed), (Object)output);
        }
        return DataResult.success((Object)output);
    }

    public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag input) {
        if (input instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)input;
            return DataResult.success(tag.entrySet().stream().map(entry -> Pair.of((Object)this.createString((String)entry.getKey()), (Object)((Tag)entry.getValue()))));
        }
        return DataResult.error(() -> "Not a map: " + String.valueOf(input));
    }

    public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag input) {
        if (input instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)input;
            return DataResult.success(c -> {
                for (Map.Entry<String, Tag> entry : tag.entrySet()) {
                    c.accept(this.createString(entry.getKey()), entry.getValue());
                }
            });
        }
        return DataResult.error(() -> "Not a map: " + String.valueOf(input));
    }

    public DataResult<MapLike<Tag>> getMap(Tag input) {
        if (input instanceof CompoundTag) {
            final CompoundTag tag = (CompoundTag)input;
            return DataResult.success((Object)new MapLike<Tag>(this){
                final /* synthetic */ NbtOps this$0;
                {
                    NbtOps nbtOps = this$0;
                    Objects.requireNonNull(nbtOps);
                    this.this$0 = nbtOps;
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public @Nullable Tag get(Tag key) {
                    if (!(key instanceof StringTag)) throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + String.valueOf(key));
                    StringTag stringTag = (StringTag)key;
                    try {
                        String string;
                        String stringKey = string = stringTag.value();
                        return tag.get(stringKey);
                    }
                    catch (Throwable throwable) {
                        throw new MatchException(throwable.toString(), throwable);
                    }
                }

                public @Nullable Tag get(String key) {
                    return tag.get(key);
                }

                public Stream<Pair<Tag, Tag>> entries() {
                    return tag.entrySet().stream().map(entry -> Pair.of((Object)this.this$0.createString((String)entry.getKey()), (Object)((Tag)entry.getValue())));
                }

                public String toString() {
                    return "MapLike[" + String.valueOf(tag) + "]";
                }
            });
        }
        return DataResult.error(() -> "Not a map: " + String.valueOf(input));
    }

    public Tag createMap(Stream<Pair<Tag, Tag>> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach(entry -> {
            Tag key = (Tag)entry.getFirst();
            Tag value = (Tag)entry.getSecond();
            if (!(key instanceof StringTag)) throw new UnsupportedOperationException("Cannot create map with non-string key: " + String.valueOf(key));
            StringTag $b$0 = (StringTag)key;
            try {
                String patt1$temp;
                String stringKey = patt1$temp = $b$0.value();
                tag.put(stringKey, value);
            }
            catch (Throwable throwable) {
                throw new MatchException(throwable.toString(), throwable);
            }
        });
        return tag;
    }

    public DataResult<Stream<Tag>> getStream(Tag input) {
        if (input instanceof CollectionTag) {
            CollectionTag collection = (CollectionTag)input;
            return DataResult.success(collection.stream());
        }
        return DataResult.error(() -> "Not a list");
    }

    public DataResult<Consumer<Consumer<Tag>>> getList(Tag input) {
        if (input instanceof CollectionTag) {
            CollectionTag collection = (CollectionTag)input;
            return DataResult.success(collection::forEach);
        }
        return DataResult.error(() -> "Not a list: " + String.valueOf(input));
    }

    public DataResult<ByteBuffer> getByteBuffer(Tag input) {
        if (input instanceof ByteArrayTag) {
            ByteArrayTag array = (ByteArrayTag)input;
            return DataResult.success((Object)ByteBuffer.wrap(array.getAsByteArray()));
        }
        return super.getByteBuffer((Object)input);
    }

    public Tag createByteList(ByteBuffer input) {
        ByteBuffer wholeBuffer = input.duplicate().clear();
        byte[] bytes = new byte[input.capacity()];
        wholeBuffer.get(0, bytes, 0, bytes.length);
        return new ByteArrayTag(bytes);
    }

    public DataResult<IntStream> getIntStream(Tag input) {
        if (input instanceof IntArrayTag) {
            IntArrayTag array = (IntArrayTag)input;
            return DataResult.success((Object)Arrays.stream(array.getAsIntArray()));
        }
        return super.getIntStream((Object)input);
    }

    public Tag createIntList(IntStream input) {
        return new IntArrayTag(input.toArray());
    }

    public DataResult<LongStream> getLongStream(Tag input) {
        if (input instanceof LongArrayTag) {
            LongArrayTag array = (LongArrayTag)input;
            return DataResult.success((Object)Arrays.stream(array.getAsLongArray()));
        }
        return super.getLongStream((Object)input);
    }

    public Tag createLongList(LongStream input) {
        return new LongArrayTag(input.toArray());
    }

    public Tag createList(Stream<Tag> input) {
        return new ListTag(input.collect(Util.toMutableList()));
    }

    public Tag remove(Tag input, String key) {
        if (input instanceof CompoundTag) {
            CompoundTag tag = (CompoundTag)input;
            CompoundTag result = tag.shallowCopy();
            result.remove(key);
            return result;
        }
        return input;
    }

    public String toString() {
        return "NBT";
    }

    public RecordBuilder<Tag> mapBuilder() {
        return new NbtRecordBuilder(this);
    }

    private static Optional<ListCollector> createCollector(Tag tag) {
        if (tag instanceof EndTag) {
            return Optional.of(new GenericListCollector());
        }
        if (tag instanceof CollectionTag) {
            CollectionTag collection = (CollectionTag)tag;
            if (collection.isEmpty()) {
                return Optional.of(new GenericListCollector());
            }
            CollectionTag collectionTag = collection;
            Objects.requireNonNull(collectionTag);
            CollectionTag collectionTag2 = collectionTag;
            int n = 0;
            return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ListTag.class, ByteArrayTag.class, IntArrayTag.class, LongArrayTag.class}, (CollectionTag)collectionTag2, n)) {
                default -> throw new MatchException(null, null);
                case 0 -> {
                    ListTag list = (ListTag)collectionTag2;
                    yield Optional.of(new GenericListCollector(list));
                }
                case 1 -> {
                    ByteArrayTag array = (ByteArrayTag)collectionTag2;
                    yield Optional.of(new ByteListCollector(array.getAsByteArray()));
                }
                case 2 -> {
                    IntArrayTag array = (IntArrayTag)collectionTag2;
                    yield Optional.of(new IntListCollector(array.getAsIntArray()));
                }
                case 3 -> {
                    LongArrayTag array = (LongArrayTag)collectionTag2;
                    yield Optional.of(new LongListCollector(array.getAsLongArray()));
                }
            };
        }
        return Optional.empty();
    }

    private class NbtRecordBuilder
    extends RecordBuilder.AbstractStringBuilder<Tag, CompoundTag> {
        protected NbtRecordBuilder(NbtOps nbtOps) {
            Objects.requireNonNull(nbtOps);
            super((DynamicOps)nbtOps);
        }

        protected CompoundTag initBuilder() {
            return new CompoundTag();
        }

        protected CompoundTag append(String key, Tag value, CompoundTag builder) {
            builder.put(key, value);
            return builder;
        }

        protected DataResult<Tag> build(CompoundTag builder, Tag prefix) {
            if (prefix == null || prefix == EndTag.INSTANCE) {
                return DataResult.success((Object)builder);
            }
            if (prefix instanceof CompoundTag) {
                CompoundTag compound = (CompoundTag)prefix;
                CompoundTag result = compound.shallowCopy();
                for (Map.Entry<String, Tag> entry : builder.entrySet()) {
                    result.put(entry.getKey(), entry.getValue());
                }
                return DataResult.success((Object)result);
            }
            return DataResult.error(() -> "mergeToMap called with not a map: " + String.valueOf(prefix), (Object)prefix);
        }
    }

    private static class GenericListCollector
    implements ListCollector {
        private final ListTag result = new ListTag();

        private GenericListCollector() {
        }

        private GenericListCollector(ListTag initial) {
            this.result.addAll(initial);
        }

        public GenericListCollector(IntArrayList initials) {
            initials.forEach(v -> this.result.add(IntTag.valueOf(v)));
        }

        public GenericListCollector(ByteArrayList initials) {
            initials.forEach(v -> this.result.add(ByteTag.valueOf(v)));
        }

        public GenericListCollector(LongArrayList initials) {
            initials.forEach(v -> this.result.add(LongTag.valueOf(v)));
        }

        @Override
        public ListCollector accept(Tag tag) {
            this.result.add(tag);
            return this;
        }

        @Override
        public Tag result() {
            return this.result;
        }
    }

    private static class ByteListCollector
    implements ListCollector {
        private final ByteArrayList values = new ByteArrayList();

        public ByteListCollector(byte[] initialValues) {
            this.values.addElements(0, initialValues);
        }

        @Override
        public ListCollector accept(Tag tag) {
            if (tag instanceof ByteTag) {
                ByteTag byteTag = (ByteTag)tag;
                this.values.add(byteTag.byteValue());
                return this;
            }
            return new GenericListCollector(this.values).accept(tag);
        }

        @Override
        public Tag result() {
            return new ByteArrayTag(this.values.toByteArray());
        }
    }

    private static class IntListCollector
    implements ListCollector {
        private final IntArrayList values = new IntArrayList();

        public IntListCollector(int[] initialValues) {
            this.values.addElements(0, initialValues);
        }

        @Override
        public ListCollector accept(Tag tag) {
            if (tag instanceof IntTag) {
                IntTag intTag = (IntTag)tag;
                this.values.add(intTag.intValue());
                return this;
            }
            return new GenericListCollector(this.values).accept(tag);
        }

        @Override
        public Tag result() {
            return new IntArrayTag(this.values.toIntArray());
        }
    }

    private static class LongListCollector
    implements ListCollector {
        private final LongArrayList values = new LongArrayList();

        public LongListCollector(long[] initialValues) {
            this.values.addElements(0, initialValues);
        }

        @Override
        public ListCollector accept(Tag tag) {
            if (tag instanceof LongTag) {
                LongTag longTag = (LongTag)tag;
                this.values.add(longTag.longValue());
                return this;
            }
            return new GenericListCollector(this.values).accept(tag);
        }

        @Override
        public Tag result() {
            return new LongArrayTag(this.values.toLongArray());
        }
    }

    private static interface ListCollector {
        public ListCollector accept(Tag var1);

        default public ListCollector acceptAll(Iterable<Tag> tags) {
            ListCollector collector = this;
            for (Tag tag : tags) {
                collector = collector.accept(tag);
            }
            return collector;
        }

        public Tag result();
    }
}

