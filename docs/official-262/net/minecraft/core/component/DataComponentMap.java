/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterators
 *  com.google.common.collect.Sets
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMaps
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core.component;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import org.jspecify.annotations.Nullable;

public interface DataComponentMap
extends Iterable<TypedDataComponent<?>>,
DataComponentGetter {
    public static final DataComponentMap EMPTY = new DataComponentMap(){

        @Override
        public <T> @Nullable T get(DataComponentType<? extends T> type) {
            return null;
        }

        @Override
        public Set<DataComponentType<?>> keySet() {
            return Set.of();
        }

        @Override
        public Iterator<TypedDataComponent<?>> iterator() {
            return Collections.emptyIterator();
        }
    };
    public static final Codec<DataComponentMap> CODEC = DataComponentMap.makeCodecFromMap(DataComponentType.VALUE_MAP_CODEC);

    public static Codec<DataComponentMap> makeCodec(Codec<DataComponentType<?>> componentTypeCodec) {
        return DataComponentMap.makeCodecFromMap(Codec.dispatchedMap(componentTypeCodec, DataComponentType::codecOrThrow));
    }

    public static Codec<DataComponentMap> makeCodecFromMap(Codec<Map<DataComponentType<?>, Object>> mapCodec) {
        return mapCodec.flatComapMap(Builder::buildFromMapTrusted, components -> {
            int size = components.size();
            if (size == 0) {
                return DataResult.success((Object)Reference2ObjectMaps.emptyMap());
            }
            Reference2ObjectArrayMap map = new Reference2ObjectArrayMap(size);
            for (TypedDataComponent<?> entry : components) {
                if (entry.type().isTransient()) continue;
                map.put(entry.type(), entry.value());
            }
            return DataResult.success((Object)map);
        });
    }

    public static DataComponentMap composite(final DataComponentMap prototype, final DataComponentMap overrides) {
        return new DataComponentMap(){

            @Override
            public <T> @Nullable T get(DataComponentType<? extends T> type) {
                T value = overrides.get(type);
                if (value != null) {
                    return value;
                }
                return prototype.get(type);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return Sets.union(prototype.keySet(), overrides.keySet());
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<DataComponentType<?>> keySet();

    default public boolean has(DataComponentType<?> type) {
        return this.get(type) != null;
    }

    @Override
    default public Iterator<TypedDataComponent<?>> iterator() {
        return Iterators.transform(this.keySet().iterator(), type -> Objects.requireNonNull(this.getTyped(type)));
    }

    default public Stream<TypedDataComponent<?>> stream() {
        return StreamSupport.stream(Spliterators.spliterator(this.iterator(), (long)this.size(), 1345), false);
    }

    default public int size() {
        return this.keySet().size();
    }

    default public boolean isEmpty() {
        return this.size() == 0;
    }

    default public DataComponentMap filter(final Predicate<DataComponentType<?>> predicate) {
        return new DataComponentMap(){
            final /* synthetic */ DataComponentMap this$0;
            {
                DataComponentMap dataComponentMap = this$0;
                Objects.requireNonNull(dataComponentMap);
                this.this$0 = dataComponentMap;
            }

            @Override
            public <T> @Nullable T get(DataComponentType<? extends T> type) {
                return predicate.test(type) ? (T)this.this$0.get(type) : null;
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return Sets.filter(this.this$0.keySet(), predicate::test);
            }
        };
    }

    public static class Builder {
        private final Reference2ObjectMap<DataComponentType<?>, Object> map = new Reference2ObjectArrayMap();
        private Consumer<DataComponentMap> validator = components -> {};

        private Builder() {
        }

        public <T> Builder set(DataComponentType<T> type, @Nullable T value) {
            this.setUnchecked(type, value);
            return this;
        }

        <T> void setUnchecked(DataComponentType<T> type, @Nullable Object value) {
            if (value != null) {
                this.map.put(type, value);
            } else {
                this.map.remove(type);
            }
        }

        public Builder addAll(DataComponentMap map) {
            for (TypedDataComponent<?> entry : map) {
                this.map.put(entry.type(), entry.value());
            }
            return this;
        }

        public Builder addValidator(Consumer<DataComponentMap> newValidator) {
            this.validator = this.validator.andThen(newValidator);
            return this;
        }

        public DataComponentMap build() {
            DataComponentMap result = Builder.buildFromMapTrusted(this.map);
            this.validator.accept(result);
            return result;
        }

        private static DataComponentMap buildFromMapTrusted(Map<DataComponentType<?>, Object> map) {
            if (map.isEmpty()) {
                return EMPTY;
            }
            if (map.size() < 8) {
                return new SimpleMap((Reference2ObjectMap<DataComponentType<?>, Object>)new Reference2ObjectArrayMap(map));
            }
            return new SimpleMap((Reference2ObjectMap<DataComponentType<?>, Object>)new Reference2ObjectOpenHashMap(map));
        }

        private record SimpleMap(Reference2ObjectMap<DataComponentType<?>, Object> map) implements DataComponentMap
        {
            @Override
            public <T> @Nullable T get(DataComponentType<? extends T> type) {
                return (T)this.map.get(type);
            }

            @Override
            public boolean has(DataComponentType<?> type) {
                return this.map.containsKey(type);
            }

            @Override
            public Set<DataComponentType<?>> keySet() {
                return this.map.keySet();
            }

            @Override
            public Iterator<TypedDataComponent<?>> iterator() {
                return Iterators.transform((Iterator)Reference2ObjectMaps.fastIterator(this.map), TypedDataComponent::fromEntryUnchecked);
            }

            @Override
            public int size() {
                return this.map.size();
            }

            @Override
            public String toString() {
                return this.map.toString();
            }
        }
    }
}

