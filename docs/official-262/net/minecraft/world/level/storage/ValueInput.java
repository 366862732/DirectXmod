/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;

public interface ValueInput {
    public <T> Optional<T> read(String var1, Codec<T> var2);

    @Deprecated
    public <T> Optional<T> read(MapCodec<T> var1);

    public Optional<ValueInput> child(String var1);

    public ValueInput childOrEmpty(String var1);

    public Optional<ValueInputList> childrenList(String var1);

    public ValueInputList childrenListOrEmpty(String var1);

    public <T> Optional<TypedInputList<T>> list(String var1, Codec<T> var2);

    public <T> TypedInputList<T> listOrEmpty(String var1, Codec<T> var2);

    public boolean getBooleanOr(String var1, boolean var2);

    public byte getByteOr(String var1, byte var2);

    public int getShortOr(String var1, short var2);

    public Optional<Integer> getInt(String var1);

    public int getIntOr(String var1, int var2);

    public long getLongOr(String var1, long var2);

    public Optional<Long> getLong(String var1);

    public float getFloatOr(String var1, float var2);

    public double getDoubleOr(String var1, double var2);

    public Optional<String> getString(String var1);

    public String getStringOr(String var1, String var2);

    public Optional<int[]> getIntArray(String var1);

    @Deprecated
    public HolderLookup.Provider lookup();

    public static interface TypedInputList<T>
    extends Iterable<T> {
        public boolean isEmpty();

        public Stream<T> stream();
    }

    public static interface ValueInputList
    extends Iterable<ValueInput> {
        public boolean isEmpty();

        public Stream<ValueInput> stream();
    }
}

