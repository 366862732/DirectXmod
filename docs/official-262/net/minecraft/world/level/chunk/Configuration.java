/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.List;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.Strategy;

public interface Configuration {
    public boolean alwaysRepack();

    public int bitsInMemory();

    public int bitsInStorage();

    public <T> Palette<T> createPalette(Strategy<T> var1, List<T> var2);

    public record Global(int bitsInMemory, int bitsInStorage) implements Configuration
    {
        @Override
        public boolean alwaysRepack() {
            return true;
        }

        @Override
        public <T> Palette<T> createPalette(Strategy<T> strategy, List<T> paletteEntries) {
            return strategy.globalPalette();
        }
    }

    public record Simple(Palette.Factory factory, int bits) implements Configuration
    {
        @Override
        public boolean alwaysRepack() {
            return false;
        }

        @Override
        public <T> Palette<T> createPalette(Strategy<T> strategy, List<T> paletteEntries) {
            return this.factory.create(this.bits, paletteEntries);
        }

        @Override
        public int bitsInMemory() {
            return this.bits;
        }

        @Override
        public int bitsInStorage() {
            return this.bits;
        }
    }
}

