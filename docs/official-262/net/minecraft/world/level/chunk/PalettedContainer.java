/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
 *  it.unimi.dsi.fastutil.ints.IntArraySet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.chunk.Configuration;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import org.jspecify.annotations.Nullable;

public class PalettedContainer<T>
implements PaletteResize<T>,
PalettedContainerRO<T> {
    private static final int MIN_PALETTE_BITS = 0;
    private volatile Data<T> data;
    private final Strategy<T> strategy;
    private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

    public void acquire() {
        this.threadingDetector.checkAndLock();
    }

    public void release() {
        this.threadingDetector.checkAndUnlock();
    }

    public static <T> Codec<PalettedContainer<T>> codecRW(Codec<T> elementCodec, Strategy<T> strategy, T defaultValue) {
        PalettedContainerRO.Unpacker unpacker = PalettedContainer::unpack;
        return PalettedContainer.codec(elementCodec, strategy, defaultValue, unpacker);
    }

    public static <T> Codec<PalettedContainerRO<T>> codecRO(Codec<T> elementCodec, Strategy<T> strategy, T defaultValue) {
        PalettedContainerRO.Unpacker unpacker = (s, data) -> PalettedContainer.unpack(s, data).map(e -> e);
        return PalettedContainer.codec(elementCodec, strategy, defaultValue, unpacker);
    }

    private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(Codec<T> elementCodec, Strategy<T> strategy, T defaultValue, PalettedContainerRO.Unpacker<T, C> unpacker) {
        return RecordCodecBuilder.create(i -> i.group((App)elementCodec.mapResult(ExtraCodecs.orElsePartial(defaultValue)).listOf().fieldOf("palette").forGetter(PalettedContainerRO.PackedData::paletteEntries), (App)Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)).apply((Applicative)i, PalettedContainerRO.PackedData::new)).comapFlatMap(discData -> unpacker.read(strategy, (PalettedContainerRO.PackedData)discData), palettedContainer -> palettedContainer.pack(strategy));
    }

    private PalettedContainer(Strategy<T> strategy, Configuration dataConfiguration, BitStorage storage, Palette<T> palette) {
        this.strategy = strategy;
        this.data = new Data<T>(dataConfiguration, storage, palette);
    }

    private PalettedContainer(PalettedContainer<T> source) {
        this.strategy = source.strategy;
        this.data = source.data.copy();
    }

    public PalettedContainer(T initialValue, Strategy<T> strategy) {
        this.strategy = strategy;
        this.data = this.createOrReuseData(null, 0);
        this.data.palette.idFor(initialValue, this);
    }

    private Data<T> createOrReuseData(@Nullable Data<T> oldData, int targetBits) {
        Configuration dataConfiguration = this.strategy.getConfigurationForBitCount(targetBits);
        if (oldData != null && dataConfiguration.equals(oldData.configuration())) {
            return oldData;
        }
        BitStorage storage = dataConfiguration.bitsInMemory() == 0 ? new ZeroBitStorage(this.strategy.entryCount()) : new SimpleBitStorage(dataConfiguration.bitsInMemory(), this.strategy.entryCount());
        Palette<T> palette = dataConfiguration.createPalette(this.strategy, List.of());
        return new Data<T>(dataConfiguration, storage, palette);
    }

    @Override
    public int onResize(int bits, T lastAddedValue) {
        Data<T> oldData = this.data;
        Data newData = this.createOrReuseData(oldData, bits);
        newData.copyFrom(oldData.palette, oldData.storage);
        this.data = newData;
        return newData.palette.idFor(lastAddedValue, PaletteResize.noResizeExpected());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public T getAndSet(int x, int y, int z, T value) {
        this.acquire();
        try {
            T t = this.getAndSet(this.strategy.getIndex(x, y, z), value);
            return t;
        }
        finally {
            this.release();
        }
    }

    public T getAndSetUnchecked(int x, int y, int z, T value) {
        return this.getAndSet(this.strategy.getIndex(x, y, z), value);
    }

    private T getAndSet(int index, T value) {
        int id = this.data.palette.idFor(value, this);
        int oldId = this.data.storage.getAndSet(index, id);
        return this.data.palette.valueFor(oldId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void set(int x, int y, int z, T value) {
        this.acquire();
        try {
            this.set(this.strategy.getIndex(x, y, z), value);
        }
        finally {
            this.release();
        }
    }

    private void set(int index, T value) {
        int id = this.data.palette.idFor(value, this);
        this.data.storage.set(index, id);
    }

    @Override
    public T get(int x, int y, int z) {
        return this.get(this.strategy.getIndex(x, y, z));
    }

    protected T get(int index) {
        Data<T> data = this.data;
        return data.palette.valueFor(data.storage.get(index));
    }

    @Override
    public void getAll(Consumer<T> consumer) {
        Palette palette = this.data.palette();
        IntArraySet allExistingEntries = new IntArraySet();
        this.data.storage.getAll(arg_0 -> ((IntSet)allExistingEntries).add(arg_0));
        allExistingEntries.forEach(state -> consumer.accept(palette.valueFor(state)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void read(FriendlyByteBuf buffer) {
        this.acquire();
        try {
            byte newBits = buffer.readByte();
            Data<T> newData = this.createOrReuseData(this.data, newBits);
            newData.palette.read(buffer, this.strategy.globalMap());
            buffer.readFixedSizeLongArray(newData.storage.getRaw());
            this.data = newData;
        }
        finally {
            this.release();
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        this.acquire();
        try {
            this.data.write(buffer, this.strategy.globalMap());
        }
        finally {
            this.release();
        }
    }

    @VisibleForTesting
    public static <T> DataResult<PalettedContainer<T>> unpack(Strategy<T> strategy, PalettedContainerRO.PackedData<T> discData) {
        BitStorage storage;
        Palette<T> palette;
        List<T> paletteEntries = discData.paletteEntries();
        int entryCount = strategy.entryCount();
        Configuration storedConfiguration = strategy.getConfigurationForPaletteSize(paletteEntries.size());
        int bitsOnDisc = storedConfiguration.bitsInStorage();
        if (discData.bitsPerEntry() != -1 && bitsOnDisc != discData.bitsPerEntry()) {
            return DataResult.error(() -> "Invalid bit count, calculated " + bitsOnDisc + ", but container declared " + discData.bitsPerEntry());
        }
        if (storedConfiguration.bitsInMemory() == 0) {
            palette = storedConfiguration.createPalette(strategy, paletteEntries);
            storage = new ZeroBitStorage(entryCount);
        } else {
            Optional<LongStream> dataOpt = discData.storage();
            if (dataOpt.isEmpty()) {
                return DataResult.error(() -> "Missing values for non-zero storage");
            }
            long[] data = dataOpt.get().toArray();
            try {
                if (storedConfiguration.alwaysRepack() || storedConfiguration.bitsInMemory() != bitsOnDisc) {
                    HashMapPalette<T> oldPalette = new HashMapPalette<T>(bitsOnDisc, paletteEntries);
                    SimpleBitStorage oldStorage = new SimpleBitStorage(bitsOnDisc, entryCount, data);
                    Palette<T> newPalette = storedConfiguration.createPalette(strategy, paletteEntries);
                    int[] newContents = PalettedContainer.reencodeContents(oldStorage, oldPalette, newPalette);
                    palette = newPalette;
                    storage = new SimpleBitStorage(storedConfiguration.bitsInMemory(), entryCount, newContents);
                } else {
                    palette = storedConfiguration.createPalette(strategy, paletteEntries);
                    storage = new SimpleBitStorage(storedConfiguration.bitsInMemory(), entryCount, data);
                }
            }
            catch (SimpleBitStorage.InitializationException exception) {
                return DataResult.error(() -> "Failed to read PalettedContainer: " + exception.getMessage());
            }
        }
        return DataResult.success(new PalettedContainer<T>(strategy, storedConfiguration, storage, palette));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PalettedContainerRO.PackedData<T> pack(Strategy<T> strategy) {
        this.acquire();
        try {
            Optional<LongStream> values;
            BitStorage currentStorage = this.data.storage;
            Palette currentPalette = this.data.palette;
            HashMapPalette newPalette = new HashMapPalette(currentStorage.getBits());
            int entryCount = strategy.entryCount();
            int[] newContents = PalettedContainer.reencodeContents(currentStorage, currentPalette, newPalette);
            Configuration storedConfiguration = strategy.getConfigurationForPaletteSize(newPalette.getSize());
            int bitsOnDisc = storedConfiguration.bitsInStorage();
            if (bitsOnDisc != 0) {
                SimpleBitStorage storage = new SimpleBitStorage(bitsOnDisc, entryCount, newContents);
                values = Optional.of(Arrays.stream(storage.getRaw()));
            } else {
                values = Optional.empty();
            }
            PalettedContainerRO.PackedData packedData = new PalettedContainerRO.PackedData(newPalette.getEntries(), values, bitsOnDisc);
            return packedData;
        }
        finally {
            this.release();
        }
    }

    private static <T> int[] reencodeContents(BitStorage storage, Palette<T> oldPalette, Palette<T> newPalette) {
        int[] buffer = new int[storage.getSize()];
        storage.unpack(buffer);
        PaletteResize dummyResizer = PaletteResize.noResizeExpected();
        int lastReadId = -1;
        int lastWrittenId = -1;
        for (int index = 0; index < buffer.length; ++index) {
            int id = buffer[index];
            if (id != lastReadId) {
                lastReadId = id;
                lastWrittenId = newPalette.idFor(oldPalette.valueFor(id), dummyResizer);
            }
            buffer[index] = lastWrittenId;
        }
        return buffer;
    }

    @Override
    public int getSerializedSize() {
        return this.data.getSerializedSize(this.strategy.globalMap());
    }

    @Override
    public int bitsPerEntry() {
        return this.data.storage().getBits();
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        return this.data.palette.maybeHas(predicate);
    }

    @Override
    public void forEachInPalette(Consumer<T> consumer) {
        for (int i = 0; i < this.data.palette.getSize(); ++i) {
            consumer.accept(this.data.palette.valueFor(i));
        }
    }

    @Override
    public PalettedContainer<T> copy() {
        return new PalettedContainer<T>(this);
    }

    @Override
    public PalettedContainer<T> recreate() {
        return new PalettedContainer(this.data.palette.valueFor(0), this.strategy);
    }

    @Override
    public void count(CountConsumer<T> output) {
        if (this.data.palette.getSize() == 1) {
            output.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
            return;
        }
        Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        this.data.storage.getAll((int state) -> counts.addTo(state, 1));
        counts.int2IntEntrySet().forEach(entry -> output.accept(this.data.palette.valueFor(entry.getIntKey()), entry.getIntValue()));
    }

    private record Data<T>(Configuration configuration, BitStorage storage, Palette<T> palette) {
        public void copyFrom(Palette<T> oldPalette, BitStorage oldStorage) {
            PaletteResize dummyResizer = PaletteResize.noResizeExpected();
            for (int i = 0; i < oldStorage.getSize(); ++i) {
                T value = oldPalette.valueFor(oldStorage.get(i));
                this.storage.set(i, this.palette.idFor(value, dummyResizer));
            }
        }

        public int getSerializedSize(IdMap<T> globalMap) {
            return 1 + this.palette.getSerializedSize(globalMap) + this.storage.getRaw().length * 8;
        }

        public void write(FriendlyByteBuf buffer, IdMap<T> globalMap) {
            buffer.writeByte(this.storage.getBits());
            this.palette.write(buffer, globalMap);
            buffer.writeFixedSizeLongArray(this.storage.getRaw());
        }

        public Data<T> copy() {
            return new Data<T>(this.configuration, this.storage.copy(), this.palette.copy());
        }
    }

    @FunctionalInterface
    public static interface CountConsumer<T> {
        public void accept(T var1, int var2);
    }
}

