/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.HashCode
 *  com.google.common.hash.HashFunction
 *  com.google.common.hash.Hasher
 *  com.google.common.hash.Hashing
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.ListBuilder
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 *  com.mojang.serialization.RecordBuilder$AbstractUniversalBuilder
 */
package net.minecraft.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.lang.runtime.SwitchBootstraps;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.util.AbstractListBuilder;

public class HashOps
implements DynamicOps<HashCode> {
    private static final byte TAG_EMPTY = 1;
    private static final byte TAG_MAP_START = 2;
    private static final byte TAG_MAP_END = 3;
    private static final byte TAG_LIST_START = 4;
    private static final byte TAG_LIST_END = 5;
    private static final byte TAG_BYTE = 6;
    private static final byte TAG_SHORT = 7;
    private static final byte TAG_INT = 8;
    private static final byte TAG_LONG = 9;
    private static final byte TAG_FLOAT = 10;
    private static final byte TAG_DOUBLE = 11;
    private static final byte TAG_STRING = 12;
    private static final byte TAG_BOOLEAN = 13;
    private static final byte TAG_BYTE_ARRAY_START = 14;
    private static final byte TAG_BYTE_ARRAY_END = 15;
    private static final byte TAG_INT_ARRAY_START = 16;
    private static final byte TAG_INT_ARRAY_END = 17;
    private static final byte TAG_LONG_ARRAY_START = 18;
    private static final byte TAG_LONG_ARRAY_END = 19;
    private static final byte[] EMPTY_PAYLOAD = new byte[]{1};
    private static final byte[] FALSE_PAYLOAD = new byte[]{13, 0};
    private static final byte[] TRUE_PAYLOAD = new byte[]{13, 1};
    public static final byte[] EMPTY_MAP_PAYLOAD = new byte[]{2, 3};
    public static final byte[] EMPTY_LIST_PAYLOAD = new byte[]{4, 5};
    private static final DataResult<Object> UNSUPPORTED_OPERATION_ERROR = DataResult.error(() -> "Unsupported operation");
    private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
    private static final Comparator<Map.Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Map.Entry.comparingByKey(HASH_COMPARATOR).thenComparing(Map.Entry.comparingByValue(HASH_COMPARATOR));
    private static final Comparator<Pair<HashCode, HashCode>> MAPLIKE_ENTRY_ORDER = Comparator.comparing(Pair::getFirst, HASH_COMPARATOR).thenComparing(Pair::getSecond, HASH_COMPARATOR);
    public static final HashOps CRC32C_INSTANCE = new HashOps(Hashing.crc32c());
    private final HashFunction hashFunction;
    private final HashCode empty;
    private final HashCode emptyMap;
    private final HashCode emptyList;
    private final HashCode trueHash;
    private final HashCode falseHash;

    public HashOps(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
        this.empty = hashFunction.hashBytes(EMPTY_PAYLOAD);
        this.emptyMap = hashFunction.hashBytes(EMPTY_MAP_PAYLOAD);
        this.emptyList = hashFunction.hashBytes(EMPTY_LIST_PAYLOAD);
        this.falseHash = hashFunction.hashBytes(FALSE_PAYLOAD);
        this.trueHash = hashFunction.hashBytes(TRUE_PAYLOAD);
    }

    public HashCode empty() {
        return this.empty;
    }

    public HashCode emptyMap() {
        return this.emptyMap;
    }

    public HashCode emptyList() {
        return this.emptyList;
    }

    public HashCode createNumeric(Number value) {
        Number number = value;
        Objects.requireNonNull(number);
        Number number2 = number;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Byte.class, Short.class, Integer.class, Long.class, Double.class, Float.class}, (Number)number2, n)) {
            case 0 -> {
                Byte v = (Byte)number2;
                yield this.createByte(v);
            }
            case 1 -> {
                Short v = (Short)number2;
                yield this.createShort(v);
            }
            case 2 -> {
                Integer v = (Integer)number2;
                yield this.createInt(v);
            }
            case 3 -> {
                Long v = (Long)number2;
                yield this.createLong(v);
            }
            case 4 -> {
                Double v = (Double)number2;
                yield this.createDouble(v);
            }
            case 5 -> {
                Float v = (Float)number2;
                yield this.createFloat(v.floatValue());
            }
            default -> this.createDouble(value.doubleValue());
        };
    }

    public HashCode createByte(byte value) {
        return this.hashFunction.newHasher(2).putByte((byte)6).putByte(value).hash();
    }

    public HashCode createShort(short value) {
        return this.hashFunction.newHasher(3).putByte((byte)7).putShort(value).hash();
    }

    public HashCode createInt(int value) {
        return this.hashFunction.newHasher(5).putByte((byte)8).putInt(value).hash();
    }

    public HashCode createLong(long value) {
        return this.hashFunction.newHasher(9).putByte((byte)9).putLong(value).hash();
    }

    public HashCode createFloat(float value) {
        return this.hashFunction.newHasher(5).putByte((byte)10).putFloat(value).hash();
    }

    public HashCode createDouble(double value) {
        return this.hashFunction.newHasher(9).putByte((byte)11).putDouble(value).hash();
    }

    public HashCode createString(String value) {
        return this.hashFunction.newHasher().putByte((byte)12).putInt(value.length()).putUnencodedChars((CharSequence)value).hash();
    }

    public HashCode createBoolean(boolean value) {
        return value ? this.trueHash : this.falseHash;
    }

    private static Hasher hashMap(Hasher hasher, Map<HashCode, HashCode> map) {
        hasher.putByte((byte)2);
        map.entrySet().stream().sorted(MAP_ENTRY_ORDER).forEach(e -> hasher.putBytes(((HashCode)e.getKey()).asBytes()).putBytes(((HashCode)e.getValue()).asBytes()));
        hasher.putByte((byte)3);
        return hasher;
    }

    private static Hasher hashMap(Hasher hasher, Stream<Pair<HashCode, HashCode>> map) {
        hasher.putByte((byte)2);
        map.sorted(MAPLIKE_ENTRY_ORDER).forEach(e -> hasher.putBytes(((HashCode)e.getFirst()).asBytes()).putBytes(((HashCode)e.getSecond()).asBytes()));
        hasher.putByte((byte)3);
        return hasher;
    }

    public HashCode createMap(Stream<Pair<HashCode, HashCode>> map) {
        return HashOps.hashMap(this.hashFunction.newHasher(), map).hash();
    }

    public HashCode createMap(Map<HashCode, HashCode> map) {
        return HashOps.hashMap(this.hashFunction.newHasher(), map).hash();
    }

    public HashCode createList(Stream<HashCode> input) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)4);
        input.forEach(value -> hasher.putBytes(value.asBytes()));
        hasher.putByte((byte)5);
        return hasher.hash();
    }

    public HashCode createByteList(ByteBuffer input) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)14);
        hasher.putBytes(input);
        hasher.putByte((byte)15);
        return hasher.hash();
    }

    public HashCode createIntList(IntStream input) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)16);
        input.forEach(arg_0 -> ((Hasher)hasher).putInt(arg_0));
        hasher.putByte((byte)17);
        return hasher.hash();
    }

    public HashCode createLongList(LongStream input) {
        Hasher hasher = this.hashFunction.newHasher();
        hasher.putByte((byte)18);
        input.forEach(arg_0 -> ((Hasher)hasher).putLong(arg_0));
        hasher.putByte((byte)19);
        return hasher.hash();
    }

    public HashCode remove(HashCode input, String key) {
        return input;
    }

    public RecordBuilder<HashCode> mapBuilder() {
        return new MapHashBuilder(this);
    }

    public ListBuilder<HashCode> listBuilder() {
        return new ListHashBuilder(this);
    }

    public String toString() {
        return "Hash " + String.valueOf(this.hashFunction);
    }

    public <U> U convertTo(DynamicOps<U> outOps, HashCode input) {
        throw new UnsupportedOperationException("Can't convert from this type");
    }

    public Number getNumberValue(HashCode input, Number defaultValue) {
        return defaultValue;
    }

    public HashCode set(HashCode input, String key, HashCode value) {
        return input;
    }

    public HashCode update(HashCode input, String key, Function<HashCode, HashCode> function) {
        return input;
    }

    public HashCode updateGeneric(HashCode input, HashCode key, Function<HashCode, HashCode> function) {
        return input;
    }

    private static <T> DataResult<T> unsupported() {
        return UNSUPPORTED_OPERATION_ERROR;
    }

    public DataResult<HashCode> get(HashCode input, String key) {
        return HashOps.unsupported();
    }

    public DataResult<HashCode> getGeneric(HashCode input, HashCode key) {
        return HashOps.unsupported();
    }

    public DataResult<Number> getNumberValue(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<Boolean> getBooleanValue(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<String> getStringValue(HashCode input) {
        return HashOps.unsupported();
    }

    private boolean isEmpty(HashCode value) {
        return value.equals((Object)this.empty);
    }

    public DataResult<HashCode> mergeToList(HashCode prefix, HashCode value) {
        if (this.isEmpty(prefix)) {
            return DataResult.success((Object)this.createList(Stream.of(value)));
        }
        return HashOps.unsupported();
    }

    public DataResult<HashCode> mergeToList(HashCode prefix, List<HashCode> values) {
        if (this.isEmpty(prefix)) {
            return DataResult.success((Object)this.createList(values.stream()));
        }
        return HashOps.unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode prefix, HashCode key, HashCode value) {
        if (this.isEmpty(prefix)) {
            return DataResult.success((Object)this.createMap(Map.of(key, value)));
        }
        return HashOps.unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode prefix, Map<HashCode, HashCode> values) {
        if (this.isEmpty(prefix)) {
            return DataResult.success((Object)this.createMap(values));
        }
        return HashOps.unsupported();
    }

    public DataResult<HashCode> mergeToMap(HashCode prefix, MapLike<HashCode> values) {
        if (this.isEmpty(prefix)) {
            return DataResult.success((Object)this.createMap((Stream<Pair<HashCode, HashCode>>)values.entries()));
        }
        return HashOps.unsupported();
    }

    public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<Stream<HashCode>> getStream(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<MapLike<HashCode>> getMap(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<ByteBuffer> getByteBuffer(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<IntStream> getIntStream(HashCode input) {
        return HashOps.unsupported();
    }

    public DataResult<LongStream> getLongStream(HashCode input) {
        return HashOps.unsupported();
    }

    private final class MapHashBuilder
    extends RecordBuilder.AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {
        final /* synthetic */ HashOps this$0;

        public MapHashBuilder(HashOps hashOps) {
            HashOps hashOps2 = hashOps;
            Objects.requireNonNull(hashOps2);
            this.this$0 = hashOps2;
            super((DynamicOps)hashOps);
        }

        protected List<Pair<HashCode, HashCode>> initBuilder() {
            return new ArrayList<Pair<HashCode, HashCode>>();
        }

        protected List<Pair<HashCode, HashCode>> append(HashCode key, HashCode value, List<Pair<HashCode, HashCode>> builder) {
            builder.add((Pair<HashCode, HashCode>)Pair.of((Object)key, (Object)value));
            return builder;
        }

        protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> builder, HashCode prefix) {
            assert (this.this$0.isEmpty(prefix));
            return DataResult.success((Object)HashOps.hashMap(this.this$0.hashFunction.newHasher(), builder.stream()).hash());
        }
    }

    private class ListHashBuilder
    extends AbstractListBuilder<HashCode, Hasher> {
        final /* synthetic */ HashOps this$0;

        public ListHashBuilder(HashOps hashOps) {
            HashOps hashOps2 = hashOps;
            Objects.requireNonNull(hashOps2);
            this.this$0 = hashOps2;
            super(hashOps);
        }

        @Override
        protected Hasher initBuilder() {
            return this.this$0.hashFunction.newHasher().putByte((byte)4);
        }

        @Override
        protected Hasher append(Hasher hasher, HashCode value) {
            return hasher.putBytes(value.asBytes());
        }

        @Override
        protected DataResult<HashCode> build(Hasher hasher, HashCode prefix) {
            assert (prefix.equals((Object)this.this$0.empty));
            hasher.putByte((byte)5);
            return DataResult.success((Object)hasher.hash());
        }
    }
}

