/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jspecify.annotations.Nullable;

public interface ValueOutput {
    public <T> void store(String var1, Codec<T> var2, T var3);

    public <T> void storeNullable(String var1, Codec<T> var2, @Nullable T var3);

    @Deprecated
    public <T> void store(MapCodec<T> var1, T var2);

    public void putBoolean(String var1, boolean var2);

    public void putByte(String var1, byte var2);

    public void putShort(String var1, short var2);

    public void putInt(String var1, int var2);

    public void putLong(String var1, long var2);

    public void putFloat(String var1, float var2);

    public void putDouble(String var1, double var2);

    public void putString(String var1, String var2);

    public void putIntArray(String var1, int[] var2);

    public ValueOutput child(String var1);

    public ValueOutputList childrenList(String var1);

    public <T> TypedOutputList<T> list(String var1, Codec<T> var2);

    public void discard(String var1);

    public boolean isEmpty();

    public static interface TypedOutputList<T> {
        public void add(T var1);

        public boolean isEmpty();
    }

    public static interface ValueOutputList {
        public ValueOutput addChild();

        public void discardLast();

        public boolean isEmpty();
    }
}

