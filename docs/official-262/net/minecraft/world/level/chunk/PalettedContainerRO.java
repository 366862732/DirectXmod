/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.serialization.DataResult
 */
package net.minecraft.world.level.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;

public interface PalettedContainerRO<T> {
    public T get(int var1, int var2, int var3);

    public void getAll(Consumer<T> var1);

    public void write(FriendlyByteBuf var1);

    public int getSerializedSize();

    @VisibleForTesting
    public int bitsPerEntry();

    public boolean maybeHas(Predicate<T> var1);

    public void forEachInPalette(Consumer<T> var1);

    public void count(PalettedContainer.CountConsumer<T> var1);

    public PalettedContainer<T> copy();

    public PalettedContainer<T> recreate();

    public PackedData<T> pack(Strategy<T> var1);

    public static interface Unpacker<T, C extends PalettedContainerRO<T>> {
        public DataResult<C> read(Strategy<T> var1, PackedData<T> var2);
    }

    public record PackedData<T>(List<T> paletteEntries, Optional<LongStream> storage, int bitsPerEntry) {
        public static final int UNKNOWN_BITS_PER_ENTRY = -1;

        public PackedData(List<T> paletteEntries, Optional<LongStream> storage) {
            this(paletteEntries, storage, -1);
        }
    }
}

