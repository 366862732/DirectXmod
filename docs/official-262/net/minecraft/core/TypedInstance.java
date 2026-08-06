/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface TypedInstance<T> {
    public Holder<T> typeHolder();

    default public Stream<TagKey<T>> tags() {
        return this.typeHolder().tags();
    }

    default public boolean is(TagKey<T> tag) {
        return this.typeHolder().is(tag);
    }

    default public boolean is(HolderSet<T> set) {
        return set.contains(this.typeHolder());
    }

    default public boolean is(T rawType) {
        return this.typeHolder().value() == rawType;
    }

    default public boolean is(Holder<T> type) {
        return this.typeHolder() == type;
    }

    default public boolean is(ResourceKey<T> type) {
        return this.typeHolder().is(type);
    }
}

