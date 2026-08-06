/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;

public interface BitRandomSource
extends RandomSource {
    public static final float FLOAT_MULTIPLIER = 5.9604645E-8f;
    public static final double DOUBLE_MULTIPLIER = (double)1.110223E-16f;

    public int next(int var1);

    @Override
    default public int nextInt() {
        return this.next(32);
    }

    @Override
    default public int nextInt(int bound) {
        int modulo;
        int sample;
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        if ((bound & bound - 1) == 0) {
            return (int)((long)bound * (long)this.next(31) >> 31);
        }
        while ((sample = this.next(31)) - (modulo = sample % bound) + (bound - 1) < 0) {
        }
        return modulo;
    }

    @Override
    default public long nextLong() {
        int upper = this.next(32);
        int lower = this.next(32);
        long shifted = (long)upper << 32;
        return shifted + (long)lower;
    }

    @Override
    default public boolean nextBoolean() {
        return this.next(1) != 0;
    }

    @Override
    default public float nextFloat() {
        return (float)this.next(24) * 5.9604645E-8f;
    }

    @Override
    default public double nextDouble() {
        int upper = this.next(26);
        int lower = this.next(27);
        long combined = ((long)upper << 27) + (long)lower;
        return (double)combined * (double)1.110223E-16f;
    }
}

