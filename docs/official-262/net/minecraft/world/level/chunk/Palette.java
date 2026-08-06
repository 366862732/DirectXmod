/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.PaletteResize;

public interface Palette<T> {
    public int idFor(T var1, PaletteResize<T> var2);

    public boolean maybeHas(Predicate<T> var1);

    public T valueFor(int var1);

    public void read(FriendlyByteBuf var1, IdMap<T> var2);

    public void write(FriendlyByteBuf var1, IdMap<T> var2);

    public int getSerializedSize(IdMap<T> var1);

    public int getSize();

    public Palette<T> copy();

    public static interface Factory {
        public <A> Palette<A> create(int var1, List<A> var2);
    }
}

