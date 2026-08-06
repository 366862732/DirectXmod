/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.BitRandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.jspecify.annotations.Nullable;

public class SingleThreadedRandomSource
implements BitRandomSource {
    private static final int MODULUS_BITS = 48;
    private static final long MODULUS_MASK = 0xFFFFFFFFFFFFL;
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private long seed;
    private @Nullable MarsagliaPolarGaussian gaussianSource;

    public SingleThreadedRandomSource(long seed) {
        this.setSeed(seed);
    }

    @Override
    public RandomSource fork() {
        return new SingleThreadedRandomSource(this.nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new LegacyRandomSource.LegacyPositionalRandomFactory(this.nextLong());
    }

    @Override
    public void setSeed(long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & 0xFFFFFFFFFFFFL;
        if (this.gaussianSource != null) {
            this.gaussianSource.reset();
        }
    }

    @Override
    public int next(int bits) {
        long newSeed;
        this.seed = newSeed = this.seed * 25214903917L + 11L & 0xFFFFFFFFFFFFL;
        return (int)(newSeed >> 48 - bits);
    }

    @Override
    public double nextGaussian() {
        if (this.gaussianSource == null) {
            this.gaussianSource = new MarsagliaPolarGaussian(this);
        }
        return this.gaussianSource.nextGaussian();
    }
}

