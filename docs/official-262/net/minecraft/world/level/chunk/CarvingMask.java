/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.BitSet;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public class CarvingMask {
    private final int minY;
    private final BitSet mask;
    private Mask additionalMask = (x, y, z) -> false;

    public CarvingMask(int height, int minY) {
        this.minY = minY;
        this.mask = new BitSet(256 * height);
    }

    public void setAdditionalMask(Mask additionalMask) {
        this.additionalMask = additionalMask;
    }

    public CarvingMask(long[] array, int minY) {
        this.minY = minY;
        this.mask = BitSet.valueOf(array);
    }

    private int getIndex(int x, int y, int z) {
        return x & 0xF | (z & 0xF) << 4 | y - this.minY << 8;
    }

    public void set(int x, int y, int z) {
        this.mask.set(this.getIndex(x, y, z));
    }

    public boolean get(int x, int y, int z) {
        return this.additionalMask.test(x, y, z) || this.mask.get(this.getIndex(x, y, z));
    }

    public Stream<BlockPos> stream(ChunkPos pos) {
        return this.mask.stream().mapToObj(i -> {
            int x = i & 0xF;
            int z = i >> 4 & 0xF;
            int y = i >> 8;
            return pos.getBlockAt(x, y + this.minY, z);
        });
    }

    public long[] toArray() {
        return this.mask.toLongArray();
    }

    public static interface Mask {
        public boolean test(int var1, int var2, int var3);
    }
}

