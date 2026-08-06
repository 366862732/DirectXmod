/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.Validate
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

public class SingleValuePalette<T>
implements Palette<T> {
    private @Nullable T value;

    public SingleValuePalette(List<T> paletteEntries) {
        if (!paletteEntries.isEmpty()) {
            Validate.isTrue((paletteEntries.size() <= 1 ? 1 : 0) != 0, (String)"Can't initialize SingleValuePalette with %d values.", (long)paletteEntries.size());
            this.value = paletteEntries.getFirst();
        }
    }

    public static <A> Palette<A> create(int bits, List<A> paletteEntries) {
        return new SingleValuePalette<A>(paletteEntries);
    }

    @Override
    public int idFor(T value, PaletteResize<T> resizeHandler) {
        if (this.value == null || this.value == value) {
            this.value = value;
            return 0;
        }
        return resizeHandler.onResize(1, value);
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        }
        return predicate.test(this.value);
    }

    @Override
    public T valueFor(int index) {
        if (this.value == null || index != 0) {
            throw new IllegalStateException("Missing Palette entry for id " + index + ".");
        }
        return this.value;
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        this.value = globalMap.byIdOrThrow(buffer.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> globalMap) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        }
        buffer.writeVarInt(globalMap.getId(this.value));
    }

    @Override
    public int getSerializedSize(IdMap<T> globalMap) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        }
        return VarInt.getByteSize(globalMap.getId(this.value));
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        }
        return this;
    }
}

