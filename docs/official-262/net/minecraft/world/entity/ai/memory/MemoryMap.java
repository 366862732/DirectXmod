/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Iterators
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.memory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jspecify.annotations.Nullable;

public final class MemoryMap
implements Iterable<Value<?>> {
    private static final Codec<MemoryModuleType<?>> SERIALIZABLE_MEMORY_MODULE_CODEC = BuiltInRegistries.MEMORY_MODULE_TYPE.byNameCodec().validate(type -> type.canSerialize() ? DataResult.success((Object)type) : DataResult.error(() -> "Memory module " + String.valueOf(type) + " cannot be encoded"));
    public static final Codec<MemoryMap> CODEC = Codec.dispatchedMap(SERIALIZABLE_MEMORY_MODULE_CODEC, type -> type.getCodec().orElseThrow()).xmap(MemoryMap::new, m -> m.memories);
    public static final MemoryMap EMPTY = new MemoryMap(Map.of());
    private final Map<MemoryModuleType<?>, ExpirableValue<?>> memories;

    private MemoryMap(Map<MemoryModuleType<?>, ExpirableValue<?>> memories) {
        this.memories = Map.copyOf(memories);
    }

    public static MemoryMap of(Stream<Value<?>> memories) {
        return new MemoryMap(memories.collect(Collectors.toMap(Value::type, Value::value)));
    }

    public <U> @Nullable ExpirableValue<U> get(MemoryModuleType<U> type) {
        return this.memories.get(type);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof MemoryMap)) return false;
        MemoryMap map = (MemoryMap)obj;
        if (!this.memories.equals(map.memories)) return false;
        return true;
    }

    public int hashCode() {
        return this.memories.hashCode();
    }

    public String toString() {
        return this.memories.toString();
    }

    @Override
    public Iterator<Value<?>> iterator() {
        return Iterators.transform(this.memories.entrySet().iterator(), entry -> Value.createUnchecked((MemoryModuleType)entry.getKey(), (ExpirableValue)entry.getValue()));
    }

    public record Value<U>(MemoryModuleType<U> type, ExpirableValue<U> value) {
        public static <U> Value<U> createUnchecked(MemoryModuleType<U> type, ExpirableValue<?> value) {
            return new Value<U>(type, value);
        }
    }

    public static class Builder {
        private final ImmutableMap.Builder<MemoryModuleType<?>, ExpirableValue<?>> builder = ImmutableMap.builder();

        public <U> Builder add(MemoryModuleType<U> type, ExpirableValue<U> value) {
            this.builder.put(type, value);
            return this;
        }

        public MemoryMap build() {
            return new MemoryMap((Map<MemoryModuleType<?>, ExpirableValue<?>>)this.builder.buildOrThrow());
        }
    }
}

