/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMaps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

public final class DataComponentPatch {
    public static final DataComponentPatch EMPTY = new DataComponentPatch(Reference2ObjectMaps.emptyMap());
    public static final Codec<DataComponentPatch> CODEC = Codec.dispatchedMap(PatchKey.CODEC, PatchKey::valueCodec).xmap(data -> {
        if (data.isEmpty()) {
            return EMPTY;
        }
        Reference2ObjectArrayMap map = new Reference2ObjectArrayMap(data.size());
        for (Map.Entry entry : data.entrySet()) {
            PatchKey key = (PatchKey)entry.getKey();
            if (key.removed()) {
                map.put(key.type(), Optional.empty());
                continue;
            }
            map.put(key.type(), Optional.of(entry.getValue()));
        }
        return new DataComponentPatch((Reference2ObjectMap<DataComponentType<?>, Optional<?>>)map);
    }, patch -> {
        Reference2ObjectArrayMap map = new Reference2ObjectArrayMap(patch.map.size());
        for (Map.Entry entry : Reference2ObjectMaps.fastIterable(patch.map)) {
            DataComponentType type = (DataComponentType)entry.getKey();
            if (type.isTransient()) continue;
            Optional value = (Optional)entry.getValue();
            if (value.isPresent()) {
                map.put((Object)new PatchKey(type, false), value.get());
                continue;
            }
            map.put((Object)new PatchKey(type, true), (Object)Unit.INSTANCE);
        }
        return map;
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC = DataComponentPatch.createStreamCodec(new CodecGetter(){

        public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> type) {
            return type.streamCodec().cast();
        }
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> DELIMITED_STREAM_CODEC = DataComponentPatch.createStreamCodec(new CodecGetter(){

        public <T> StreamCodec<RegistryFriendlyByteBuf, T> apply(DataComponentType<T> type) {
            StreamCodec original = type.streamCodec().cast();
            return original.apply(ByteBufCodecs.registryFriendlyLengthPrefixed(Integer.MAX_VALUE));
        }
    });
    private static final String REMOVED_PREFIX = "!";
    final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map;

    private static StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> createStreamCodec(final CodecGetter codecGetter) {
        return new StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch>(){

            @Override
            public DataComponentPatch decode(RegistryFriendlyByteBuf input) {
                DataComponentType type;
                int i;
                int positiveCount = input.readVarInt();
                int negativeCount = input.readVarInt();
                if (positiveCount == 0 && negativeCount == 0) {
                    return EMPTY;
                }
                int expectedSize = positiveCount + negativeCount;
                Reference2ObjectArrayMap map = new Reference2ObjectArrayMap(Math.min(expectedSize, 65536));
                for (i = 0; i < positiveCount; ++i) {
                    type = (DataComponentType)DataComponentType.STREAM_CODEC.decode(input);
                    Object value = codecGetter.apply(type).decode(input);
                    map.put((Object)type, Optional.of(value));
                }
                for (i = 0; i < negativeCount; ++i) {
                    type = (DataComponentType)DataComponentType.STREAM_CODEC.decode(input);
                    map.put((Object)type, Optional.empty());
                }
                return new DataComponentPatch((Reference2ObjectMap<DataComponentType<?>, Optional<?>>)map);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, DataComponentPatch patch) {
                if (patch.isEmpty()) {
                    output.writeVarInt(0);
                    output.writeVarInt(0);
                    return;
                }
                int positiveCount = 0;
                int negativeCount = 0;
                for (Reference2ObjectMap.Entry entry : Reference2ObjectMaps.fastIterable(patch.map)) {
                    if (((Optional)entry.getValue()).isPresent()) {
                        ++positiveCount;
                        continue;
                    }
                    ++negativeCount;
                }
                output.writeVarInt(positiveCount);
                output.writeVarInt(negativeCount);
                for (Reference2ObjectMap.Entry entry : Reference2ObjectMaps.fastIterable(patch.map)) {
                    Optional value = (Optional)entry.getValue();
                    if (!value.isPresent()) continue;
                    DataComponentType type = (DataComponentType)entry.getKey();
                    DataComponentType.STREAM_CODEC.encode(output, type);
                    this.encodeComponent(output, type, value.get());
                }
                for (Reference2ObjectMap.Entry entry : Reference2ObjectMaps.fastIterable(patch.map)) {
                    if (!((Optional)entry.getValue()).isEmpty()) continue;
                    DataComponentType type = (DataComponentType)entry.getKey();
                    DataComponentType.STREAM_CODEC.encode(output, type);
                }
            }

            private <T> void encodeComponent(RegistryFriendlyByteBuf output, DataComponentType<T> type, Object value) {
                codecGetter.apply(type).encode(output, value);
            }
        };
    }

    DataComponentPatch(Reference2ObjectMap<DataComponentType<?>, Optional<?>> map) {
        this.map = map;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> @Nullable T get(DataComponentGetter prototype, DataComponentType<? extends T> type) {
        return DataComponentPatch.getFromPatchAndPrototype(this.map, prototype, type);
    }

    static <T> @Nullable T getFromPatchAndPrototype(Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch, DataComponentGetter prototype, DataComponentType<? extends T> type) {
        Optional value = (Optional)patch.get(type);
        if (value != null) {
            return value.orElse(null);
        }
        return prototype.get(type);
    }

    public Set<Map.Entry<DataComponentType<?>, Optional<?>>> entrySet() {
        return this.map.entrySet();
    }

    public int size() {
        return this.map.size();
    }

    public DataComponentPatch forget(Predicate<DataComponentType<?>> test) {
        if (this.isEmpty()) {
            return EMPTY;
        }
        Reference2ObjectArrayMap newMap = new Reference2ObjectArrayMap(this.map);
        newMap.keySet().removeIf(test);
        if (newMap.isEmpty()) {
            return EMPTY;
        }
        return new DataComponentPatch((Reference2ObjectMap<DataComponentType<?>, Optional<?>>)newMap);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public SplitResult split() {
        if (this.isEmpty()) {
            return SplitResult.EMPTY;
        }
        DataComponentMap.Builder added = DataComponentMap.builder();
        Set removed = Sets.newIdentityHashSet();
        this.map.forEach((type, optionalValue) -> {
            if (optionalValue.isPresent()) {
                added.setUnchecked(type, optionalValue.get());
            } else {
                removed.add(type);
            }
        });
        return new SplitResult(added.build(), removed);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataComponentPatch)) return false;
        DataComponentPatch patch = (DataComponentPatch)obj;
        if (!this.map.equals(patch.map)) return false;
        return true;
    }

    public int hashCode() {
        return this.map.hashCode();
    }

    public String toString() {
        return DataComponentPatch.toString(this.map);
    }

    static String toString(Reference2ObjectMap<DataComponentType<?>, Optional<?>> map) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean first = true;
        for (Map.Entry entry : Reference2ObjectMaps.fastIterable(map)) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            Optional value = (Optional)entry.getValue();
            if (value.isPresent()) {
                builder.append(entry.getKey());
                builder.append("=>");
                builder.append(value.get());
                continue;
            }
            builder.append(REMOVED_PREFIX);
            builder.append(entry.getKey());
        }
        builder.append('}');
        return builder.toString();
    }

    @FunctionalInterface
    private static interface CodecGetter {
        public <T> StreamCodec<? super RegistryFriendlyByteBuf, T> apply(DataComponentType<T> var1);
    }

    public static class Builder {
        private final Reference2ObjectMap<DataComponentType<?>, Optional<?>> map = new Reference2ObjectArrayMap();

        private Builder() {
        }

        public <T> Builder set(DataComponentType<T> type, T value) {
            this.map.put(type, Optional.of(value));
            return this;
        }

        public <T> Builder remove(DataComponentType<T> type) {
            this.map.put(type, Optional.empty());
            return this;
        }

        public <T> Builder set(TypedDataComponent<T> component) {
            return this.set(component.type(), component.value());
        }

        public <T> Builder set(Iterable<TypedDataComponent<?>> components) {
            for (TypedDataComponent<?> component : components) {
                this.set(component);
            }
            return this;
        }

        public DataComponentPatch build() {
            if (this.map.isEmpty()) {
                return EMPTY;
            }
            return new DataComponentPatch(this.map);
        }
    }

    public record SplitResult(DataComponentMap added, Set<DataComponentType<?>> removed) {
        public static final SplitResult EMPTY = new SplitResult(DataComponentMap.EMPTY, Set.of());
    }

    private record PatchKey(DataComponentType<?> type, boolean removed) {
        public static final Codec<PatchKey> CODEC = Codec.STRING.flatXmap(string -> {
            Identifier id;
            DataComponentType<?> type;
            boolean removed = string.startsWith(DataComponentPatch.REMOVED_PREFIX);
            if (removed) {
                string = string.substring(DataComponentPatch.REMOVED_PREFIX.length());
            }
            if ((type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id = Identifier.tryParse(string))) == null) {
                return DataResult.error(() -> "No component with type: '" + String.valueOf(id) + "'");
            }
            if (type.isTransient()) {
                return DataResult.error(() -> "'" + String.valueOf(id) + "' is not a persistent component");
            }
            return DataResult.success((Object)new PatchKey(type, removed));
        }, key -> {
            DataComponentType<?> type = key.type();
            Identifier id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id == null) {
                return DataResult.error(() -> "Unregistered component: " + String.valueOf(type));
            }
            return DataResult.success((Object)(key.removed() ? DataComponentPatch.REMOVED_PREFIX + String.valueOf(id) : id.toString()));
        });

        public Codec<?> valueCodec() {
            return this.removed ? Codec.EMPTY.codec() : this.type.codecOrThrow();
        }
    }
}

