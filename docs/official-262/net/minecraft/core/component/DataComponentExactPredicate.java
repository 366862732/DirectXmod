/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.serialization.Codec
 */
package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentExactPredicate
implements Predicate<DataComponentGetter> {
    public static final Codec<DataComponentExactPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC.xmap(map -> new DataComponentExactPredicate(map.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())), predicate -> predicate.expectedComponents.stream().filter(e -> !e.type().isTransient()).collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value)));
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DataComponentExactPredicate::new, predicate -> predicate.expectedComponents);
    public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
    private final List<TypedDataComponent<?>> expectedComponents;

    private DataComponentExactPredicate(List<TypedDataComponent<?>> expectedComponents) {
        this.expectedComponents = expectedComponents;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static <T> DataComponentExactPredicate expect(DataComponentType<T> type, T value) {
        return new DataComponentExactPredicate(List.of(new TypedDataComponent<T>(type, value)));
    }

    public static DataComponentExactPredicate allOf(DataComponentMap components) {
        return new DataComponentExactPredicate((List<TypedDataComponent<?>>)ImmutableList.copyOf((Iterable)components));
    }

    public static DataComponentExactPredicate someOf(DataComponentMap components, DataComponentType<?> ... types) {
        Builder result = new Builder();
        for (DataComponentType<?> type : types) {
            TypedDataComponent<?> value = components.getTyped(type);
            if (value == null) continue;
            result.expect(value);
        }
        return result.build();
    }

    public boolean isEmpty() {
        return this.expectedComponents.isEmpty();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof DataComponentExactPredicate)) return false;
        DataComponentExactPredicate predicate = (DataComponentExactPredicate)obj;
        if (!this.expectedComponents.equals(predicate.expectedComponents)) return false;
        return true;
    }

    public int hashCode() {
        return this.expectedComponents.hashCode();
    }

    public String toString() {
        return this.expectedComponents.toString();
    }

    @Override
    public boolean test(DataComponentGetter actualComponents) {
        for (TypedDataComponent<?> expected : this.expectedComponents) {
            Object actual = actualComponents.get(expected.type());
            if (Objects.equals(expected.value(), actual)) continue;
            return false;
        }
        return true;
    }

    public boolean alwaysMatches() {
        return this.expectedComponents.isEmpty();
    }

    public DataComponentPatch asPatch() {
        return DataComponentPatch.builder().set(this.expectedComponents).build();
    }

    public static class Builder {
        private final List<TypedDataComponent<?>> expectedComponents = new ArrayList();

        private Builder() {
        }

        public <T> Builder expect(TypedDataComponent<T> value) {
            return this.expect(value.type(), value.value());
        }

        public <T> Builder expect(DataComponentType<? super T> type, T value) {
            for (TypedDataComponent<?> component : this.expectedComponents) {
                if (component.type() != type) continue;
                throw new IllegalArgumentException("Predicate already has component of type: '" + String.valueOf(type) + "'");
            }
            this.expectedComponents.add(new TypedDataComponent<T>(type, value));
            return this;
        }

        public DataComponentExactPredicate build() {
            return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
        }
    }
}

