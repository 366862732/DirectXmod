/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface WritableRegistry<T>
extends Registry<T> {
    public Holder.Reference<T> register(ResourceKey<T> var1, T var2, RegistrationInfo var3);

    public void bindTags(Map<TagKey<T>, List<Holder<T>>> var1);

    public boolean isEmpty();

    public HolderGetter<T> createRegistrationLookup();
}

