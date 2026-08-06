/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.NonNull
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface DefaultedRegistry<T>
extends Registry<T> {
    @Override
    public @NonNull Identifier getKey(T var1);

    @Override
    public @NonNull T getValue(@Nullable Identifier var1);

    @Override
    public @NonNull T byId(int var1);

    public Identifier getDefaultKey();
}

