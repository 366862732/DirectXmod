/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;

public interface HolderGetter<T> {
    public Optional<Holder.Reference<T>> get(ResourceKey<T> var1);

    default public Holder.Reference<T> getOrThrow(ResourceKey<T> id) {
        return this.get(id).orElseThrow(() -> new IllegalStateException("Missing element " + String.valueOf(id)));
    }

    public Optional<HolderSet.Named<T>> get(TagKey<T> var1);

    default public HolderSet.Named<T> getOrThrow(TagKey<T> id) {
        return this.get(id).orElseThrow(() -> new IllegalStateException("Missing tag " + String.valueOf(id)));
    }

    default public Optional<Holder<T>> getRandomElementOf(TagKey<T> tag, RandomSource random) {
        return this.get(tag).flatMap(holderSet -> holderSet.getRandomElement(random));
    }

    public static interface Provider {
        public <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

        default public <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> key) {
            return this.lookup(key).orElseThrow(() -> new IllegalStateException("Registry " + String.valueOf(key.identifier()) + " not found"));
        }

        default public <T> Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
            return this.lookup(id.registryKey()).flatMap(l -> l.get(id));
        }

        default public <T> Holder.Reference<T> getOrThrow(ResourceKey<T> id) {
            return (Holder.Reference)this.lookup(id.registryKey()).flatMap(l -> l.get(id)).orElseThrow(() -> new IllegalStateException("Missing element " + String.valueOf(id)));
        }

        default public <T> Optional<HolderSet.Named<T>> get(TagKey<T> id) {
            return this.lookup(id.registry()).flatMap(l -> l.get(id));
        }

        default public <T> HolderSet.Named<T> getOrThrow(TagKey<T> id) {
            return (HolderSet.Named)this.lookup(id.registry()).flatMap(l -> l.get(id)).orElseThrow(() -> new IllegalStateException("Missing tag " + String.valueOf(id)));
        }
    }
}

