/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.MapLike
 *  com.mojang.serialization.RecordBuilder
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.telemetry;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.telemetry.TelemetryProperty;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class TelemetryPropertyMap {
    private final Map<TelemetryProperty<?>, Object> entries;

    private TelemetryPropertyMap(Map<TelemetryProperty<?>, Object> entries) {
        this.entries = entries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MapCodec<TelemetryPropertyMap> createCodec(final List<TelemetryProperty<?>> properties) {
        return new MapCodec<TelemetryPropertyMap>(){

            public <T> RecordBuilder<T> encode(TelemetryPropertyMap input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                RecordBuilder<T> result = prefix;
                for (TelemetryProperty property : properties) {
                    result = this.encodeProperty(input, result, property);
                }
                return result;
            }

            private <T, V> RecordBuilder<T> encodeProperty(TelemetryPropertyMap input, RecordBuilder<T> result, TelemetryProperty<V> property) {
                V value = input.get(property);
                if (value != null) {
                    return result.add(property.id(), value, property.codec());
                }
                return result;
            }

            public <T> DataResult<TelemetryPropertyMap> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Builder> result = DataResult.success((Object)new Builder());
                for (TelemetryProperty property : properties) {
                    result = this.decodeProperty(result, ops, input, property);
                }
                return result.map(Builder::build);
            }

            private <T, V> DataResult<Builder> decodeProperty(DataResult<Builder> result, DynamicOps<T> ops, MapLike<T> input, TelemetryProperty<V> property) {
                Object value = input.get(property.id());
                if (value != null) {
                    DataResult parse = property.codec().parse(ops, value);
                    return result.apply2stable((b, v) -> b.put(property, v), parse);
                }
                return result;
            }

            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return properties.stream().map(TelemetryProperty::id).map(arg_0 -> ops.createString(arg_0));
            }
        };
    }

    public <T> @Nullable T get(TelemetryProperty<T> property) {
        return (T)this.entries.get(property);
    }

    public String toString() {
        return this.entries.toString();
    }

    public Set<TelemetryProperty<?>> propertySet() {
        return this.entries.keySet();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Map<TelemetryProperty<?>, Object> entries = new Reference2ObjectOpenHashMap();

        private Builder() {
        }

        public <T> Builder put(TelemetryProperty<T> property, T value) {
            this.entries.put(property, value);
            return this;
        }

        public <T> Builder putIfNotNull(TelemetryProperty<T> property, @Nullable T value) {
            if (value != null) {
                this.entries.put(property, value);
            }
            return this;
        }

        public Builder putAll(TelemetryPropertyMap properties) {
            this.entries.putAll(properties.entries);
            return this;
        }

        public TelemetryPropertyMap build() {
            return new TelemetryPropertyMap(this.entries);
        }
    }
}

