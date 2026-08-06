/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.util.internal.ThreadLocalRandom
 */
package net.minecraft.util;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.ThreadSafeLegacyRandomSource;

public interface RandomSource {
    @Deprecated
    public static final double GAUSSIAN_SPREAD_FACTOR = 2.297;

    public static RandomSource create() {
        return RandomSource.create(RandomSupport.generateUniqueSeed());
    }

    @Deprecated
    public static RandomSource createThreadSafe() {
        return new ThreadSafeLegacyRandomSource(RandomSupport.generateUniqueSeed());
    }

    public static RandomSource create(long seed) {
        return new LegacyRandomSource(seed);
    }

    public static RandomSource createThreadLocalInstance() {
        return new SingleThreadedRandomSource(ThreadLocalRandom.current().nextLong());
    }

    public static RandomSource createThreadLocalInstance(long seed) {
        return new SingleThreadedRandomSource(seed);
    }

    public RandomSource fork();

    public PositionalRandomFactory forkPositional();

    public void setSeed(long var1);

    public int nextInt();

    public int nextInt(int var1);

    default public int nextIntBetweenInclusive(int min, int maxInclusive) {
        return this.nextInt(maxInclusive - min + 1) + min;
    }

    public long nextLong();

    public boolean nextBoolean();

    public float nextFloat();

    public double nextDouble();

    public double nextGaussian();

    default public double triangle(double mean, double spread) {
        return mean + spread * (this.nextDouble() - this.nextDouble());
    }

    default public float triangle(float mean, float spread) {
        return mean + spread * (this.nextFloat() - this.nextFloat());
    }

    default public void consumeCount(int rounds) {
        for (int i = 0; i < rounds; ++i) {
            this.nextInt();
        }
    }

    default public int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound - origin is non positive");
        }
        return origin + this.nextInt(bound - origin);
    }
}

