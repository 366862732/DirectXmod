/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

public class LayeredRegistryAccess<T> {
    private final List<T> keys;
    private final List<RegistryAccess.Frozen> values;
    private final RegistryAccess.Frozen composite;

    public LayeredRegistryAccess(List<T> keys) {
        this(keys, Util.make(() -> {
            Object[] layers = new RegistryAccess.Frozen[keys.size()];
            Arrays.fill(layers, RegistryAccess.EMPTY);
            return Arrays.asList(layers);
        }));
    }

    private LayeredRegistryAccess(List<T> keys, List<RegistryAccess.Frozen> values) {
        this.keys = List.copyOf(keys);
        this.values = List.copyOf(values);
        this.composite = new RegistryAccess.ImmutableRegistryAccess(LayeredRegistryAccess.collectRegistries(values.stream())).freeze();
    }

    private int getLayerIndexOrThrow(T layer) {
        int index = this.keys.indexOf(layer);
        if (index == -1) {
            throw new IllegalStateException("Can't find " + String.valueOf(layer) + " inside " + String.valueOf(this.keys));
        }
        return index;
    }

    public RegistryAccess.Frozen getLayer(T layer) {
        int index = this.getLayerIndexOrThrow(layer);
        return this.values.get(index);
    }

    public RegistryAccess.Frozen getAccessForLoading(T forLayer) {
        int index = this.getLayerIndexOrThrow(forLayer);
        return this.getCompositeAccessForLayers(0, index);
    }

    public RegistryAccess.Frozen getAccessFrom(T forLayer) {
        int index = this.getLayerIndexOrThrow(forLayer);
        return this.getCompositeAccessForLayers(index, this.values.size());
    }

    private RegistryAccess.Frozen getCompositeAccessForLayers(int from, int to) {
        return new RegistryAccess.ImmutableRegistryAccess(LayeredRegistryAccess.collectRegistries(this.values.subList(from, to).stream())).freeze();
    }

    public LayeredRegistryAccess<T> replaceFrom(T fromLayer, RegistryAccess.Frozen ... layers) {
        return this.replaceFrom(fromLayer, Arrays.asList(layers));
    }

    public LayeredRegistryAccess<T> replaceFrom(T fromLayer, List<RegistryAccess.Frozen> layers) {
        int index = this.getLayerIndexOrThrow(fromLayer);
        if (layers.size() > this.values.size() - index) {
            throw new IllegalStateException("Too many values to replace");
        }
        ArrayList<RegistryAccess.Frozen> newValues = new ArrayList<RegistryAccess.Frozen>();
        for (int i = 0; i < index; ++i) {
            newValues.add(this.values.get(i));
        }
        newValues.addAll(layers);
        while (newValues.size() < this.values.size()) {
            newValues.add(RegistryAccess.EMPTY);
        }
        return new LayeredRegistryAccess<T>(this.keys, newValues);
    }

    public RegistryAccess.Frozen compositeAccess() {
        return this.composite;
    }

    private static Map<ResourceKey<? extends Registry<?>>, Registry<?>> collectRegistries(Stream<? extends RegistryAccess> registries) {
        HashMap result = new HashMap();
        registries.forEach(access -> access.registries().forEach(e -> {
            if (result.put(e.key(), e.value()) != null) {
                throw new IllegalStateException("Duplicated registry " + String.valueOf(e.key()));
            }
        }));
        return result;
    }
}

