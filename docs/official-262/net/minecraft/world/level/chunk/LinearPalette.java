/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.Validate
 */
package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T>
implements Palette<T> {
    private final T[] values;
    private final int bits;
    private int size;

    private LinearPalette(int bits, List<T> paletteEntries) {
        this.values = new Object[1 << bits];
        this.bits = bits;
        Validate.isTrue((paletteEntries.size() <= this.values.length ? 1 : 0) != 0, (String)"Can't initialize LinearPalette of size %d with %d entries", (Object[])new Object[]{this.values.length, paletteEntries.size()});
        for (int i = 0; i < paletteEntries.size(); ++i) {
            this.values[i] = paletteEntries.get(i);
        }
        this.size = paletteEntries.size();
    }

    private LinearPalette(T[] values, int bits, int size) {
        this.values = values;
        this.bits = bits;
        this.size = size;
    }

    public static <A> Palette<A> create(int bits, List<A> paletteEntries) {
        return new LinearPalette<A>(bits, paletteEntries);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resizeHandler) {
        int index;
        for (int i = 0; i < this.size; ++i) {
            if (this.values[i] != value) continue;
            return i;
        }
        if ((index = this.size++) < this.values.length) {
            this.values[index] = value;
            return index;
        }
        return resizeHandler.onResize(this.bits + 1, value);
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        for (int i = 0; i < this.size; ++i) {
            if (!predicate.test(this.values[i])) continue;
            return true;
        }
        return false;
    }

    @Override
    public T valueFor(int index) {
        if (index >= 0 && index < this.size) {
            return this.values[index];
        }
        throw new MissingPaletteEntryException(index);
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        this.size = buffer.readVarInt();
        for (int i = 0; i < this.size; ++i) {
            this.values[i] = globalMap.byIdOrThrow(buffer.readVarInt());
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        buffer.writeVarInt(this.size);
        for (int i = 0; i < this.size; ++i) {
            buffer.writeVarInt(globalMap.getId(this.values[i]));
        }
    }

    @Override
    public int getSerializedSize(IdMap<T> globalMap) {
        int result = VarInt.getByteSize(this.getSize());
        for (int i = 0; i < this.getSize(); ++i) {
            result += VarInt.getByteSize(globalMap.getId(this.values[i]));
        }
        return result;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Palette<T> copy() {
        return new LinearPalette<Object>((Object[])this.values.clone(), this.bits, this.size);
    }
}

