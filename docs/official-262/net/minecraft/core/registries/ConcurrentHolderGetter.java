/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core.registries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public class ConcurrentHolderGetter<T>
implements HolderGetter<T> {
    private final Object lock;
    private final HolderGetter<T> original;
    private final Map<ResourceKey<T>, Optional<Holder.Reference<T>>> elementCache = new ConcurrentHashMap<ResourceKey<T>, Optional<Holder.Reference<T>>>();
    private final Map<TagKey<T>, Optional<HolderSet.Named<T>>> tagCache = new ConcurrentHashMap<TagKey<T>, Optional<HolderSet.Named<T>>>();

    public ConcurrentHolderGetter(Object lock, HolderGetter<T> original) {
        this.lock = lock;
        this.original = original;
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> elementId) {
        return this.elementCache.computeIfAbsent(elementId, id -> {
            Object object = this.lock;
            synchronized (object) {
                return this.original.get((ResourceKey<T>)id);
            }
        });
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> tagId) {
        return this.tagCache.computeIfAbsent(tagId, id -> {
            Object object = this.lock;
            synchronized (object) {
                return this.original.get((TagKey<T>)id);
            }
        });
    }
}

