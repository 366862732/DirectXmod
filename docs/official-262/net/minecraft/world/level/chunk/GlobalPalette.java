/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PaletteResize;

public class GlobalPalette<T>
implements Palette<T> {
    private final IdMap<T> registry;

    public GlobalPalette(IdMap<T> registry) {
        this.registry = registry;
    }

    @Override
    public int idFor(T value, PaletteResize<T> resizeHandler) {
        int id = this.registry.getId(value);
        return id == -1 ? 0 : id;
    }

    @Override
    public boolean maybeHas(Predicate<T> predicate) {
        return true;
    }

    @Override
    public T valueFor(int index) {
        T value = this.registry.byId(index);
        if (value == null) {
            throw new MissingPaletteEntryException(index);
        }
        return value;
    }

    @Override
    public void read(FriendlyByteBuf buffer, IdMap<T> globalMap) {
    }

    @Override
    public void write(FriendlyByteBuf buffer, IdMap<T> globalMap) {
    }

    @Override
    public int getSerializedSize(IdMap<T> globalMap) {
        return 0;
    }

    @Override
    public int getSize() {
        return this.registry.size();
    }

    @Override
    public Palette<T> copy() {
        return this;
    }
}

