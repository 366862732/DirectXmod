/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.Encoder
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.ListBuilder
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 */
package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DelegatingOps<T>
implements DynamicOps<T> {
    protected final DynamicOps<T> delegate;

    protected DelegatingOps(DynamicOps<T> delegate) {
        this.delegate = delegate;
    }

    public T empty() {
        return (T)this.delegate.empty();
    }

    public T emptyMap() {
        return (T)this.delegate.emptyMap();
    }

    public T emptyList() {
        return (T)this.delegate.emptyList();
    }

    public <U> U convertTo(DynamicOps<U> outOps, T input) {
        if (Objects.equals(outOps, this.delegate)) {
            return (U)input;
        }
        return (U)this.delegate.convertTo(outOps, input);
    }

    public DataResult<Number> getNumberValue(T input) {
        return this.delegate.getNumberValue(input);
    }

    public T createNumeric(Number i) {
        return (T)this.delegate.createNumeric(i);
    }

    public T createByte(byte value) {
        return (T)this.delegate.createByte(value);
    }

    public T createShort(short value) {
        return (T)this.delegate.createShort(value);
    }

    public T createInt(int value) {
        return (T)this.delegate.createInt(value);
    }

    public T createLong(long value) {
        return (T)this.delegate.createLong(value);
    }

    public T createFloat(float value) {
        return (T)this.delegate.createFloat(value);
    }

    public T createDouble(double value) {
        return (T)this.delegate.createDouble(value);
    }

    public DataResult<Boolean> getBooleanValue(T input) {
        return this.delegate.getBooleanValue(input);
    }

    public T createBoolean(boolean value) {
        return (T)this.delegate.createBoolean(value);
    }

    public DataResult<String> getStringValue(T input) {
        return this.delegate.getStringValue(input);
    }

    public T createString(String value) {
        return (T)this.delegate.createString(value);
    }

    public DataResult<T> mergeToList(T list, T value) {
        return this.delegate.mergeToList(list, value);
    }

    public DataResult<T> mergeToList(T list, List<T> values) {
        return this.delegate.mergeToList(list, values);
    }

    public DataResult<T> mergeToMap(T map, T key, T value) {
        return this.delegate.mergeToMap(map, key, value);
    }

    public DataResult<T> mergeToMap(T map, MapLike<T> values) {
        return this.delegate.mergeToMap(map, values);
    }

    public DataResult<T> mergeToMap(T map, Map<T, T> values) {
        return this.delegate.mergeToMap(map, values);
    }

    public DataResult<T> mergeToPrimitive(T prefix, T value) {
        return this.delegate.mergeToPrimitive(prefix, value);
    }

    public DataResult<Stream<Pair<T, T>>> getMapValues(T input) {
        return this.delegate.getMapValues(input);
    }

    public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T input) {
        return this.delegate.getMapEntries(input);
    }

    public T createMap(Map<T, T> map) {
        return (T)this.delegate.createMap(map);
    }

    public T createMap(Stream<Pair<T, T>> map) {
        return (T)this.delegate.createMap(map);
    }

    public DataResult<MapLike<T>> getMap(T input) {
        return this.delegate.getMap(input);
    }

    public DataResult<Stream<T>> getStream(T input) {
        return this.delegate.getStream(input);
    }

    public DataResult<Consumer<Consumer<T>>> getList(T input) {
        return this.delegate.getList(input);
    }

    public T createList(Stream<T> input) {
        return (T)this.delegate.createList(input);
    }

    public DataResult<ByteBuffer> getByteBuffer(T input) {
        return this.delegate.getByteBuffer(input);
    }

    public T createByteList(ByteBuffer input) {
        return (T)this.delegate.createByteList(input);
    }

    public DataResult<IntStream> getIntStream(T input) {
        return this.delegate.getIntStream(input);
    }

    public T createIntList(IntStream input) {
        return (T)this.delegate.createIntList(input);
    }

    public DataResult<LongStream> getLongStream(T input) {
        return this.delegate.getLongStream(input);
    }

    public T createLongList(LongStream input) {
        return (T)this.delegate.createLongList(input);
    }

    public T remove(T input, String key) {
        return (T)this.delegate.remove(input, key);
    }

    public boolean compressMaps() {
        return this.delegate.compressMaps();
    }

    public ListBuilder<T> listBuilder() {
        return new DelegateListBuilder(this, this.delegate.listBuilder());
    }

    public RecordBuilder<T> mapBuilder() {
        return new DelegateRecordBuilder(this, this.delegate.mapBuilder());
    }

    protected class DelegateListBuilder
    implements ListBuilder<T> {
        private final ListBuilder<T> original;
        final /* synthetic */ DelegatingOps this$0;

        protected DelegateListBuilder(DelegatingOps this$0, ListBuilder<T> original) {
            DelegatingOps delegatingOps = this$0;
            Objects.requireNonNull(delegatingOps);
            this.this$0 = delegatingOps;
            this.original = original;
        }

        public DynamicOps<T> ops() {
            return this.this$0;
        }

        public DataResult<T> build(T prefix) {
            return this.original.build(prefix);
        }

        public ListBuilder<T> add(T value) {
            this.original.add(value);
            return this;
        }

        public ListBuilder<T> add(DataResult<T> value) {
            this.original.add(value);
            return this;
        }

        public <E> ListBuilder<T> add(E value, Encoder<E> encoder) {
            this.original.add(encoder.encodeStart(this.ops(), value));
            return this;
        }

        public <E> ListBuilder<T> addAll(Iterable<E> values, Encoder<E> encoder) {
            values.forEach(v -> this.original.add(encoder.encode(v, this.ops(), this.ops().empty())));
            return this;
        }

        public ListBuilder<T> withErrorsFrom(DataResult<?> result) {
            this.original.withErrorsFrom(result);
            return this;
        }

        public ListBuilder<T> mapError(UnaryOperator<String> onError) {
            this.original.mapError(onError);
            return this;
        }

        public DataResult<T> build(DataResult<T> prefix) {
            return this.original.build(prefix);
        }
    }

    protected class DelegateRecordBuilder
    implements RecordBuilder<T> {
        private final RecordBuilder<T> original;
        final /* synthetic */ DelegatingOps this$0;

        protected DelegateRecordBuilder(DelegatingOps this$0, RecordBuilder<T> original) {
            DelegatingOps delegatingOps = this$0;
            Objects.requireNonNull(delegatingOps);
            this.this$0 = delegatingOps;
            this.original = original;
        }

        public DynamicOps<T> ops() {
            return this.this$0;
        }

        public RecordBuilder<T> add(T key, T value) {
            this.original.add(key, value);
            return this;
        }

        public RecordBuilder<T> add(T key, DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        public RecordBuilder<T> add(String key, T value) {
            this.original.add(key, value);
            return this;
        }

        public RecordBuilder<T> add(String key, DataResult<T> value) {
            this.original.add(key, value);
            return this;
        }

        public <E> RecordBuilder<T> add(String key, E value, Encoder<E> encoder) {
            return this.original.add(key, encoder.encodeStart(this.ops(), value));
        }

        public RecordBuilder<T> withErrorsFrom(DataResult<?> result) {
            this.original.withErrorsFrom(result);
            return this;
        }

        public RecordBuilder<T> setLifecycle(Lifecycle lifecycle) {
            this.original.setLifecycle(lifecycle);
            return this;
        }

        public RecordBuilder<T> mapError(UnaryOperator<String> onError) {
            this.original.mapError(onError);
            return this;
        }

        public DataResult<T> build(T prefix) {
            return this.original.build(prefix);
        }

        public DataResult<T> build(DataResult<T> prefix) {
            return this.original.build(prefix);
        }
    }
}

