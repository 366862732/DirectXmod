/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMultimap
 *  com.google.common.collect.ImmutableMultimap$Builder
 *  com.google.common.collect.Multimap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core.component;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class DataComponentLookup<T> {
    private final Iterable<? extends Holder<T>> elements;
    private volatile Map<DataComponentType<?>, ComponentStorage<?, T>> cache = Map.of();

    public DataComponentLookup(Iterable<? extends Holder<T>> elements) {
        this.elements = elements;
    }

    private <C> @Nullable ComponentStorage<C, T> getFromCache(DataComponentType<C> type) {
        return this.cache.get(type);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private <C> ComponentStorage<C, T> getOrCreateStorage(DataComponentType<C> type) {
        ComponentStorage<C, T> existingStorage = this.getFromCache(type);
        if (existingStorage != null) {
            return existingStorage;
        }
        ComponentStorage<C, T> newStorage = this.scanForComponents(type);
        DataComponentLookup dataComponentLookup = this;
        synchronized (dataComponentLookup) {
            ComponentStorage<C, T> foreignStorage = this.getFromCache(type);
            if (foreignStorage != null) {
                return foreignStorage;
            }
            this.cache = Util.copyAndPut(this.cache, type, newStorage);
            return newStorage;
        }
    }

    private <C> ComponentStorage<C, T> scanForComponents(DataComponentType<C> type) {
        ImmutableMultimap.Builder results = ImmutableMultimap.builder();
        for (Holder<T> element : this.elements) {
            C componentValue = element.components().get(type);
            if (componentValue == null) continue;
            results.put(componentValue, element);
        }
        return new ComponentStorage(results.build());
    }

    public <C> Stream<Holder<T>> findMatching(DataComponentType<C> type, Predicate<C> predicate) {
        return this.getOrCreateStorage(type).findMatching(predicate);
    }

    public <C> Collection<Holder<T>> findAll(DataComponentType<C> type, C value) {
        return this.getOrCreateStorage(type).findAll(value);
    }

    public <C> Collection<Holder<T>> findAll(DataComponentType<C> type) {
        return this.getOrCreateStorage(type).valueToComponent.values();
    }

    private record ComponentStorage<C, T>(Multimap<C, Holder<T>> valueToComponent) {
        public Collection<Holder<T>> findAll(C value) {
            return this.valueToComponent.get(value);
        }

        public Stream<Holder<T>> findMatching(Predicate<C> predicate) {
            if (this.valueToComponent.isEmpty()) {
                return Stream.empty();
            }
            return this.valueToComponent.entries().stream().filter(e -> predicate.test(e.getKey())).map(Map.Entry::getValue);
        }
    }
}

