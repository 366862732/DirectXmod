/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.common.collect.ImmutableSortedMap
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class StateDefinition<O, S extends StateHolder<O, S>> {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    private static final Comparable<?>[] EMPTY_VALUES = new Comparable[0];
    private static final Property<?>[] EMPTY_KEYS = new Property[0];
    private static final StateHolder<?, ?>[][] EMPTY_NEIGHBORS = new StateHolder[0][];
    private final O owner;
    private final ImmutableSortedMap<String, Property<?>> propertiesByName;
    private final ImmutableList<S> states;
    private final MapCodec<S> propertiesCodec;

    protected StateDefinition(Function<O, S> defaultState, O owner, Factory<O, S> factory, Map<String, Property<?>> properties) {
        this.owner = owner;
        int propertyCount = properties.size();
        if (propertyCount == 0) {
            this.propertiesByName = ImmutableSortedMap.of();
            this.propertiesCodec = StateDefinition.createCodec(owner, defaultState, this.propertiesByName);
            this.states = StateDefinition.createSingletonState(owner, factory);
        } else {
            this.propertiesByName = ImmutableSortedMap.copyOf(properties);
            this.propertiesCodec = StateDefinition.createCodec(owner, defaultState, this.propertiesByName);
            this.states = propertyCount == 1 ? StateDefinition.createSinglePropertyStates(owner, factory, this.propertiesByName) : StateDefinition.createMultiPropertyStates(owner, factory, this.propertiesByName);
        }
    }

    private static <O, S extends StateHolder<O, S>> MapCodec<S> createCodec(O owner, Function<O, S> defaultState, Map<String, Property<?>> propertiesByName) {
        Supplier<StateHolder> defaultSupplier = () -> (StateHolder)defaultState.apply(owner);
        MapCodec<StateHolder> codec = MapCodec.unit(defaultSupplier);
        for (Map.Entry<String, Property<?>> entry : propertiesByName.entrySet()) {
            codec = StateDefinition.appendPropertyCodec(codec, defaultSupplier, entry.getKey(), entry.getValue());
        }
        return codec;
    }

    private static <O, S extends StateHolder<O, S>> ImmutableList<S> createSingletonState(O owner, Factory<O, S> factory) {
        StateHolder singletonState = (StateHolder)factory.create(owner, EMPTY_KEYS, EMPTY_VALUES);
        singletonState.initializeNeighbors(StateDefinition.emptyNeighbors());
        return ImmutableList.of((Object)singletonState);
    }

    private static <O, S extends StateHolder<O, S>> ImmutableList<S> createSinglePropertyStates(O owner, Factory<O, S> factory, Map<String, Property<?>> propertiesByName) {
        return StateDefinition.createSinglePropertyStates(owner, factory, (Property)Iterables.getOnlyElement(propertiesByName.values()));
    }

    private static <O, S extends StateHolder<O, S>, T extends Comparable<T>> ImmutableList<S> createSinglePropertyStates(O owner, Factory<O, S> factory, Property<T> property) {
        Property[] propertyKeys = new Property[]{property};
        List<T> propertyValues = property.getPossibleValues();
        int valueCount = propertyValues.size();
        ImmutableList.Builder states = ImmutableList.builderWithExpectedSize((int)valueCount);
        StateHolder[] propertyNeighbours = new StateHolder[valueCount];
        StateHolder[][] neighbours = new StateHolder[][]{propertyNeighbours};
        for (int i = 0; i < valueCount; ++i) {
            Comparable propertyValue = (Comparable)propertyValues.get(i);
            assert (property.getInternalIndex(propertyValue) == i);
            StateHolder blockState = (StateHolder)factory.create(owner, propertyKeys, new Comparable[]{propertyValue});
            states.add((Object)blockState);
            propertyNeighbours[i] = blockState;
            blockState.initializeNeighbors(neighbours);
        }
        return states.build();
    }

    private static <O, S extends StateHolder<O, S>> ImmutableList<S> createMultiPropertyStates(O owner, Factory<O, S> factory, Map<String, Property<?>> propertiesByName) {
        Property[] propertyKeys = propertiesByName.values().toArray(EMPTY_KEYS);
        ArrayList allPropertyValues = new ArrayList(propertyKeys.length);
        for (Property property : propertyKeys) {
            allPropertyValues.add(property.getPossibleValues());
        }
        List stateValues = Lists.cartesianProduct(allPropertyValues);
        HashMap<List, StateHolder> statesByValues = new HashMap<List, StateHolder>();
        ImmutableList.Builder states = ImmutableList.builderWithExpectedSize((int)stateValues.size());
        for (List values2 : stateValues) {
            List valuesCopy = List.copyOf(values2);
            StateHolder blockState = (StateHolder)factory.create(owner, propertyKeys, valuesCopy.toArray(EMPTY_VALUES));
            statesByValues.put(valuesCopy, blockState);
            states.add((Object)blockState);
        }
        StateCollection stateCollection = new StateCollection(statesByValues, new HashMap());
        statesByValues.forEach((values, state) -> state.initializeNeighbors(stateCollection.fillNeighborsForState(propertyKeys, (List)values)));
        return states.build();
    }

    private static <S extends StateHolder<?, ?>> S[][] emptyNeighbors() {
        return EMPTY_NEIGHBORS;
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(MapCodec<S> codec, Supplier<S> defaultSupplier, String name, Property<T> property) {
        return Codec.mapPair(codec, (MapCodec)property.valueCodec().fieldOf(name).orElseGet(string -> {}, () -> property.value((StateHolder)defaultSupplier.get()))).xmap(pair -> (StateHolder)((StateHolder)pair.getFirst()).setValue(property, ((Property.Value)pair.getSecond()).value()), state -> Pair.of((Object)state, property.value((StateHolder<?, ?>)state)));
    }

    public ImmutableList<S> getPossibleStates() {
        return this.states;
    }

    public S any() {
        return (S)((StateHolder)this.states.getFirst());
    }

    public MapCodec<S> propertiesCodec() {
        return this.propertiesCodec;
    }

    public O getOwner() {
        return this.owner;
    }

    public Collection<Property<?>> getProperties() {
        return this.propertiesByName.values();
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("block", this.owner).add("properties", this.propertiesByName.values().stream().map(Property::getName).collect(Collectors.toList())).toString();
    }

    public @Nullable Property<?> getProperty(String name) {
        return (Property)this.propertiesByName.get((Object)name);
    }

    public boolean isSingletonState() {
        return this.propertiesByName.isEmpty();
    }

    public static interface Factory<O, S> {
        public S create(O var1, Property<?>[] var2, Comparable<?>[] var3);
    }

    record StateCollection<S extends StateHolder<?, ?>>(Map<List<Comparable<?>>, S> statesByValues, Map<List<Comparable<?>>, S[]> statesByPivotCache) {
        public S[][] fillNeighborsForState(Property<?>[] propertyKeys, List<Comparable<?>> propertyValues) {
            StateHolder[][] neighbors = new StateHolder[propertyKeys.length][];
            ArrayList valuesKey = new ArrayList(propertyValues);
            for (int i = 0; i < propertyKeys.length; ++i) {
                neighbors[i] = this.fillStatesForPivot(valuesKey, propertyKeys[i], i);
            }
            return neighbors;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private <T extends Comparable<T>> S[] fillStatesForPivot(List<Comparable<?>> valuesKey, Property<T> pivot, int pivotIndex) {
            Comparable ownPivotValue = (Comparable)((Object)valuesKey.set(pivotIndex, (Comparable<?>)((Object)Wildcard.INSTANCE)));
            try {
                StateHolder[] cachedResult = (StateHolder[])this.statesByPivotCache.get(valuesKey);
                if (cachedResult != null) {
                    StateHolder[] stateHolderArray = cachedResult;
                    return stateHolderArray;
                }
                StateHolder[] neighbourStatesForPivot = this.computeStatesForPivot(valuesKey, pivot, pivotIndex);
                valuesKey.set(pivotIndex, (Comparable<?>)((Object)Wildcard.INSTANCE));
                this.statesByPivotCache.put(List.copyOf(valuesKey), neighbourStatesForPivot);
                StateHolder[] stateHolderArray = neighbourStatesForPivot;
                return stateHolderArray;
            }
            finally {
                valuesKey.set(pivotIndex, ownPivotValue);
            }
        }

        private <T extends Comparable<T>> S[] computeStatesForPivot(List<Comparable<?>> valuesKey, Property<T> pivot, int pivotIndex) {
            List<T> possiblePivotValues = pivot.getPossibleValues();
            int pivotValuesCount = possiblePivotValues.size();
            StateHolder[] result = new StateHolder[pivotValuesCount];
            for (int pivotValueIndex = 0; pivotValueIndex < pivotValuesCount; ++pivotValueIndex) {
                StateHolder neighbourState;
                Comparable possiblePivotValue = (Comparable)possiblePivotValues.get(pivotValueIndex);
                assert (pivot.getInternalIndex(possiblePivotValue) == pivotValueIndex);
                valuesKey.set(pivotIndex, possiblePivotValue);
                result[pivotValueIndex] = neighbourState = Objects.requireNonNull((StateHolder)this.statesByValues.get(valuesKey));
            }
            return result;
        }

        private static enum Wildcard {
            INSTANCE;

        }
    }

    public static class Builder<O, S extends StateHolder<O, S>> {
        private final O owner;
        private final Map<String, Property<?>> properties = Maps.newHashMap();

        public Builder(O owner) {
            this.owner = owner;
        }

        public Builder<O, S> add(Property<?> ... properties) {
            for (Property<?> property : properties) {
                this.validateProperty(property);
                this.properties.put(property.getName(), property);
            }
            return this;
        }

        private <T extends Comparable<T>> void validateProperty(Property<T> property) {
            String name = property.getName();
            if (!NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException(String.valueOf(this.owner) + " has invalidly named property: " + name);
            }
            List<T> values = property.getPossibleValues();
            if (values.size() <= 1) {
                throw new IllegalArgumentException(String.valueOf(this.owner) + " attempted use property " + name + " with <= 1 possible values");
            }
            for (Comparable comparable : values) {
                String valueName = property.getName(comparable);
                if (NAME_PATTERN.matcher(valueName).matches()) continue;
                throw new IllegalArgumentException(String.valueOf(this.owner) + " has property: " + name + " with invalidly named value: " + valueName);
            }
            if (this.properties.containsKey(name)) {
                throw new IllegalArgumentException(String.valueOf(this.owner) + " has duplicate property: " + name);
            }
        }

        public StateDefinition<O, S> create(Function<O, S> defaultState, Factory<O, S> factory) {
            return new StateDefinition<O, S>(defaultState, this.owner, factory, this.properties);
        }
    }
}

