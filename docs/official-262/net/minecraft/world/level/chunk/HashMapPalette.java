/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

public class HashMapPalette<T>
implements Palette<T> {
    private final CrudeIncrementalIntIdentityHashBiMap<T> values;
    private final int bits;

    public HashMapPalette(int bits, List<T> values) {
        this(bits);
        values.forEach(this.values::add);
    }

    public HashMapPalette(int bits) {
        this(bits, CrudeIncrementalIntIdentityHashBiMap.create(1 << bits));
    }

    private HashMapPalette(int bits, CrudeIncrementalIntIdentityHashBiMap<T> values) {
        this.bits = bits;
        this.values = values;
    }

    public static <A> Palette<A> create(int bits, List<A> paletteEntries) {
        return new HashMapPalette<A>(bits, paletteEntries);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resizeHandler) {
        int id = this.values.getId(value);
        if (id == -1 && (id = this.values.add(value)) >= 1 << this.bits) {
            id = resizeHandler.onResize(this.bits + 1, value);
        }
        return id;
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        for (int i = 0; i < this.getSize(); ++i) {
            if (!predicate.test(this.values.byId(i))) continue;
            return true;
        }
        return false;
    }

    @Override
    public T valueFor(int index) {
        T value = this.values.byId(index);
        if (value == null) {
            throw new MissingPaletteEntryException(index);
        }
        return value;
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        this.values.clear();
        int size = buffer.readVarInt();
        for (int i = 0; i < size; ++i) {
            this.values.add(globalMap.byIdOrThrow(buffer.readVarInt()));
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        int size = this.getSize();
        buffer.writeVarInt(size);
        for (int i = 0; i < size; ++i) {
            buffer.writeVarInt(globalMap.getId(this.values.byId(i)));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> globalMap) {
        int size = VarInt.getByteSize(this.getSize());
        for (int i = 0; i < this.getSize(); ++i) {
            size += VarInt.getByteSize(globalMap.getId(this.values.byId(i)));
        }
        return size;
    }

    public List<T> getEntries() {
        ArrayList list = new ArrayList();
        this.values.iterator().forEachRemaining(list::add);
        return list;
    }

    @Override
    public int getSize() {
        return this.values.size();
    }

    @Override
    public Palette<T> copy() {
        return new HashMapPalette<T>(this.bits, this.values.copy());
    }
}

