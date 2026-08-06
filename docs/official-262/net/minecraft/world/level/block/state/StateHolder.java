/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public abstract class StateHolder<O, S> {
    private static final int VALUE_NOT_FOUND = -1;
    public static final String NAME_TAG = "Name";
    public static final String PROPERTIES_TAG = "Properties";
    protected final O owner;
    private final Property<?>[] propertyKeys;
    private final Comparable<?>[] propertyValues;
    private S[][] neighbors;

    protected StateHolder(O owner, Property<?>[] propertyKeys, Comparable<?>[] propertyValues) {
        assert (propertyKeys.length == propertyValues.length);
        this.owner = owner;
        this.propertyKeys = propertyKeys;
        this.propertyValues = propertyValues;
    }

    public <T extends Comparable<T>> S cycle(Property<T> property) {
        return this.setValue(property, (Comparable)StateHolder.findNextInCollection(property.getPossibleValues(), this.getValue(property)));
    }

    protected static <T> T findNextInCollection(List<T> values, T current) {
        int nextIndex = values.indexOf(current) + 1;
        return nextIndex == values.size() ? values.getFirst() : values.get(nextIndex);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.owner);
        if (!this.isSingletonState()) {
            builder.append('[');
            builder.append(this.getValues().map(Property.Value::toString).collect(Collectors.joining(",")));
            builder.append(']');
        }
        return builder.toString();
    }

    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public Collection<Property<?>> getProperties() {
        return List.of(this.propertyKeys);
    }

    private int valueIndex(Property<?> property) {
        for (int i = 0; i < this.propertyKeys.length; ++i) {
            if (this.propertyKeys[i] != property) continue;
            return i;
        }
        return -1;
    }

    public boolean hasProperty(Property<?> property) {
        return this.valueIndex(property) != -1;
    }

    private <T extends Comparable<T>> @Nullable T getNullableValue(Property<T> property) {
        int index = this.valueIndex(property);
        if (index == -1) {
            return null;
        }
        return (T)((Comparable)property.getValueClass().cast(this.propertyValues[index]));
    }

    public <T extends Comparable<T>> T getValue(Property<T> property) {
        T value = this.getNullableValue(property);
        if (value == null) {
            throw new IllegalArgumentException("Cannot get property " + String.valueOf(property) + " as it does not exist in " + String.valueOf(this.owner));
        }
        return value;
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> property) {
        return Optional.ofNullable(this.getNullableValue(property));
    }

    public <T extends Comparable<T>> T getValueOrElse(Property<T> property, T defaultValue) {
        return (T)((Comparable)Objects.requireNonNullElse(this.getNullableValue(property), defaultValue));
    }

    public <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V value) {
        int index = this.valueIndex(property);
        if (index == -1) {
            throw new IllegalArgumentException("Cannot set property " + String.valueOf(property) + " as it does not exist in " + String.valueOf(this.owner));
        }
        return this.setValueInternal(property, index, value);
    }

    public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> property, V value) {
        int index = this.valueIndex(property);
        if (index == -1) {
            return (S)this;
        }
        return this.setValueInternal(property, index, value);
    }

    private <T extends Comparable<T>, V extends T> S setValueInternal(Property<T> property, int propertyIndex, V value) {
        int valueIndex = property.getInternalIndex(value);
        if (valueIndex < 0) {
            throw new IllegalArgumentException("Cannot set property " + String.valueOf(property) + " to " + String.valueOf(value) + " on " + String.valueOf(this.owner) + ", it is not an allowed value");
        }
        return this.neighbors[propertyIndex][valueIndex];
    }

    void initializeNeighbors(S[][] neighbors) {
        if (this.neighbors != null) {
            throw new IllegalStateException();
        }
        this.neighbors = neighbors;
    }

    public boolean isSingletonState() {
        return this.propertyKeys.length == 0;
    }

    public Stream<Property.Value<?>> getValues() {
        int length = this.propertyKeys.length;
        if (length == 0) {
            return Stream.empty();
        }
        return IntStream.range(0, length).mapToObj(i -> StateHolder.createValue(this.propertyKeys[i], this.propertyValues[i]));
    }

    private static <T extends Comparable<T>> Property.Value<T> createValue(Property<T> propertyKey, Comparable<?> propertyValue) {
        return new Property.Value(propertyKey, propertyValue);
    }

    protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> ownerCodec, Function<O, S> defaultState, Function<O, StateDefinition<O, S>> stateDefinition) {
        return ownerCodec.dispatch(NAME_TAG, s -> s.owner, o -> {
            StateDefinition definition = (StateDefinition)stateDefinition.apply(o);
            StateHolder defaultValue = (StateHolder)defaultState.apply(o);
            if (definition.isSingletonState()) {
                return MapCodec.unit((Object)defaultValue);
            }
            return definition.propertiesCodec().codec().lenientOptionalFieldOf(PROPERTIES_TAG).xmap(oo -> oo.orElse(defaultValue), Optional::of);
        });
    }
}

