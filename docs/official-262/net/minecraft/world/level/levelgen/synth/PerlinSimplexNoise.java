/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntRBTreeSet
 *  it.unimi.dsi.fastutil.ints.IntSortedSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jspecify.annotations.Nullable;

public class PerlinSimplexNoise {
    private final @Nullable SimplexNoise[] noiseLevels;
    private final double highestFreqValueFactor;
    private final double highestFreqInputFactor;

    public PerlinSimplexNoise(RandomSource random, List<Integer> octaveSet) {
        this(random, (IntSortedSet)new IntRBTreeSet(octaveSet));
    }

    private PerlinSimplexNoise(RandomSource random, IntSortedSet octaveSet) {
        int highFreqOctaves;
        if (octaveSet.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }
        int lowFreqOctaves = -octaveSet.firstInt();
        int octaves = lowFreqOctaves + (highFreqOctaves = octaveSet.lastInt()) + 1;
        if (octaves < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }
        SimplexNoise zeroOctave = new SimplexNoise(random);
        int zeroOctaveIndex = highFreqOctaves;
        this.noiseLevels = new SimplexNoise[octaves];
        if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaves && octaveSet.contains(0)) {
            this.noiseLevels[zeroOctaveIndex] = zeroOctave;
        }
        for (int i = zeroOctaveIndex + 1; i < octaves; ++i) {
            if (i >= 0 && octaveSet.contains(zeroOctaveIndex - i)) {
                this.noiseLevels[i] = new SimplexNoise(random);
                continue;
            }
            random.consumeCount(262);
        }
        if (highFreqOctaves > 0) {
            long positiveOctaveSeed = (long)(zeroOctave.getValue(zeroOctave.xo, zeroOctave.yo, zeroOctave.zo) * 9.223372036854776E18);
            WorldgenRandom highFreqRandom = new WorldgenRandom(new LegacyRandomSource(positiveOctaveSeed));
            for (int i = zeroOctaveIndex - 1; i >= 0; --i) {
                if (i < octaves && octaveSet.contains(zeroOctaveIndex - i)) {
                    this.noiseLevels[i] = new SimplexNoise(highFreqRandom);
                    continue;
                }
                highFreqRandom.consumeCount(262);
            }
        }
        this.highestFreqInputFactor = Math.pow(2.0, highFreqOctaves);
        this.highestFreqValueFactor = 1.0 / (Math.pow(2.0, octaves) - 1.0);
    }

    public double getValue(double x, double y, boolean useNoiseStart) {
        double value = 0.0;
        double factor = this.highestFreqInputFactor;
        double valueFactor = this.highestFreqValueFactor;
        for (SimplexNoise noiseLevel : this.noiseLevels) {
            if (noiseLevel != null) {
                value += noiseLevel.getValue(x * factor + (useNoiseStart ? noiseLevel.xo : 0.0), y * factor + (useNoiseStart ? noiseLevel.yo : 0.0)) * valueFactor;
            }
            factor /= 2.0;
            valueFactor *= 2.0;
        }
        return value;
    }
}

