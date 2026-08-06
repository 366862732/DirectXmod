/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import org.jspecify.annotations.Nullable;

public interface IdMap<T>
extends Iterable<T> {
    public static final int DEFAULT = -1;

    public int getId(T var1);

    public @Nullable T byId(int var1);

    default public T byIdOrThrow(int id) {
        T result = this.byId(id);
        if (result == null) {
            throw new IllegalArgumentException("No value with id " + id);
        }
        return result;
    }

    default public int getIdOrThrow(T value) {
        int id = this.getId(value);
        if (id == -1) {
            throw new IllegalArgumentException("Can't find id for '" + String.valueOf(value) + "' in map " + String.valueOf(this));
        }
        return id;
    }

    public int size();
}

