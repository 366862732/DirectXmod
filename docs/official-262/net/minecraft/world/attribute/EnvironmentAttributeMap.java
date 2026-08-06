/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public final class EnvironmentAttributeMap {
    public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
    public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(() -> Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(Entry::createCodec)).xmap(EnvironmentAttributeMap::new, v -> v.entries));
    public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(EnvironmentAttributeMap::filterSyncable, EnvironmentAttributeMap::filterSyncable);
    public static final Codec<EnvironmentAttributeMap> CODEC_ONLY_POSITIONAL = CODEC.validate(map -> {
        List<EnvironmentAttribute> illegalAttributes = map.keySet().stream().filter(attribute -> !attribute.isPositional()).toList();
        if (!illegalAttributes.isEmpty()) {
            return DataResult.error(() -> "The following attributes cannot be positional: " + String.valueOf(illegalAttributes));
        }
        return DataResult.success((Object)map);
    });
    private final Map<EnvironmentAttribute<?>, Entry<?, ?>> entries;

    private static EnvironmentAttributeMap filterSyncable(EnvironmentAttributeMap attributes) {
        return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(attributes.entries, EnvironmentAttribute::isSyncable)));
    }

    private EnvironmentAttributeMap(Map<EnvironmentAttribute<?>, Entry<?, ?>> entries) {
        this.entries = entries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <Value> @Nullable Entry<Value, ?> get(EnvironmentAttribute<Value> attribute) {
        return this.entries.get(attribute);
    }

    public <Value> Value applyModifier(EnvironmentAttribute<Value> attribute, Value baseValue) {
        Entry<Value, ?> entry = this.get(attribute);
        return entry != null ? entry.applyModifier(baseValue) : baseValue;
    }

    public boolean contains(EnvironmentAttribute<?> attribute) {
        return this.entries.containsKey(attribute);
    }

    public Set<EnvironmentAttribute<?>> keySet() {
        return this.entries.keySet();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EnvironmentAttributeMap)) return false;
        EnvironmentAttributeMap attributes = (EnvironmentAttributeMap)obj;
        if (!this.entries.equals(attributes.entries)) return false;
        return true;
    }

    public int hashCode() {
        return this.entries.hashCode();
    }

    public String toString() {
        return this.entries.toString();
    }

    public static class Builder {
        private final Map<EnvironmentAttribute<?>, Entry<?, ?>> entries = new HashMap();

        private Builder() {
        }

        public Builder putAll(EnvironmentAttributeMap map) {
            this.entries.putAll(map.entries);
            return this;
        }

        public <Value, Parameter> Builder modify(EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Parameter> modifier, Parameter value) {
            attribute.type().checkAllowedModifier(modifier);
            this.entries.put(attribute, new Entry<Value, Parameter>(value, modifier));
            return this;
        }

        public <Value> Builder set(EnvironmentAttribute<Value> attribute, Value value) {
            return this.modify(attribute, AttributeModifier.override(), value);
        }

        public EnvironmentAttributeMap build() {
            if (this.entries.isEmpty()) {
                return EMPTY;
            }
            return new EnvironmentAttributeMap(Map.copyOf(this.entries));
        }
    }

    public record Entry<Value, Argument>(Argument argument, AttributeModifier<Value, Argument> modifier) {
        private static <Value> Codec<Entry<Value, ?>> createCodec(EnvironmentAttribute<Value> attribute) {
            Codec fullCodec = attribute.type().modifierCodec().dispatch("modifier", Entry::modifier, Util.memoize(modifier -> Entry.createFullCodec(attribute, modifier)));
            return Codec.either(attribute.valueCodec(), (Codec)fullCodec).xmap(either -> (Entry)either.map(value -> new Entry(value, AttributeModifier.override()), e -> e), entry -> {
                if (entry.modifier == AttributeModifier.override()) {
                    return Either.left(entry.argument());
                }
                return Either.right((Object)entry);
            });
        }

        private static <Value, Argument> MapCodec<Entry<Value, Argument>> createFullCodec(EnvironmentAttribute<Value> attribute, AttributeModifier<Value, Argument> modifier) {
            return RecordCodecBuilder.mapCodec(i -> i.group((App)modifier.argumentCodec(attribute).fieldOf("argument").forGetter(Entry::argument)).apply((Applicative)i, value -> new Entry(value, modifier)));
        }

        public Value applyModifier(Value subject) {
            return this.modifier.apply(subject, this.argument);
        }
    }
}

