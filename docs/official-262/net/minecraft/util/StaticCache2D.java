/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import java.util.Locale;
import java.util.function.Consumer;

public class StaticCache2D<T> {
    private final int minX;
    private final int minZ;
    private final int sizeX;
    private final int sizeZ;
    private final Object[] cache;

    public static <T> StaticCache2D<T> create(int centerX, int centerZ, int range, Initializer<T> initializer) {
        int minX = centerX - range;
        int minZ = centerZ - range;
        int size = 2 * range + 1;
        return new StaticCache2D<T>(minX, minZ, size, size, initializer);
    }

    private StaticCache2D(int minX, int minZ, int sizeX, int sizeZ, Initializer<T> initializer) {
        this.minX = minX;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeZ = sizeZ;
        this.cache = new Object[this.sizeX * this.sizeZ];
        for (int x = minX; x < minX + sizeX; ++x) {
            for (int z = minZ; z < minZ + sizeZ; ++z) {
                this.cache[this.getIndex((int)x, (int)z)] = initializer.get(x, z);
            }
        }
    }

    public void forEach(Consumer<T> consumer) {
        for (Object o : this.cache) {
            consumer.accept(o);
        }
    }

    public T get(int x, int z) {
        if (!this.contains(x, z)) {
            throw new IllegalArgumentException("Requested out of range value (" + x + "," + z + ") from " + String.valueOf(this));
        }
        return (T)this.cache[this.getIndex(x, z)];
    }

    public boolean contains(int x, int z) {
        int deltaX = x - this.minX;
        int deltaZ = z - this.minZ;
        return deltaX >= 0 && deltaX < this.sizeX && deltaZ >= 0 && deltaZ < this.sizeZ;
    }

    public String toString() {
        return String.format(Locale.ROOT, "StaticCache2D[%d, %d, %d, %d]", this.minX, this.minZ, this.minX + this.sizeX, this.minZ + this.sizeZ);
    }

    private int getIndex(int x, int z) {
        int deltaX = x - this.minX;
        int deltaZ = z - this.minZ;
        return deltaX * this.sizeZ + deltaZ;
    }

    @FunctionalInterface
    public static interface Initializer<T> {
        public T get(int var1, int var2);
    }
}

