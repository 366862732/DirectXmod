/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.data.worldgen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface BootstrapContext<T> {
    public Holder.Reference<T> register(ResourceKey<T> var1, T var2, Lifecycle var3);

    default public Holder.Reference<T> register(ResourceKey<T> key, T value) {
        return this.register(key, value, Lifecycle.stable());
    }

    public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> var1);
}

