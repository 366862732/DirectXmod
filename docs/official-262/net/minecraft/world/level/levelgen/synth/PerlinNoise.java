/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.doubles.DoubleArrayList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 *  it.unimi.dsi.fastutil.ints.IntBidirectionalIterator
 *  it.unimi.dsi.fastutil.ints.IntRBTreeSet
 *  it.unimi.dsi.fastutil.ints.IntSortedSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.jspecify.annotations.Nullable;

public class PerlinNoise {
    private static final int ROUND_OFF = 0x2000000;
    private final @Nullable ImprovedNoise[] noiseLevels;
    private final int firstOctave;
    private final DoubleList amplitudes;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;
    private final double maxValue;

    @Deprecated
    public static PerlinNoise createLegacyForBlendedNoise(RandomSource random, IntStream octaves) {
        return new PerlinNoise(random, PerlinNoise.makeAmplitudes((IntSortedSet)new IntRBTreeSet((Collection)octaves.boxed().collect(ImmutableList.toImmutableList()))), false);
    }

    @Deprecated
    public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource random, int firstOctave, DoubleList amplitudes) {
        return new PerlinNoise(random, (Pair<Integer, DoubleList>)Pair.of((Object)firstOctave, (Object)amplitudes), false);
    }

    public static PerlinNoise create(RandomSource random, IntStream octaves) {
        return PerlinNoise.create(random, (List)octaves.boxed().collect(ImmutableList.toImmutableList()));
    }

    public static PerlinNoise create(RandomSource random, List<Integer> octaveSet) {
        return new PerlinNoise(random, PerlinNoise.makeAmplitudes((IntSortedSet)new IntRBTreeSet(octaveSet)), true);
    }

    public static PerlinNoise create(RandomSource random, int firstOctave, double firstAmplitude, double ... amplitudes) {
        DoubleArrayList amplitudeList = new DoubleArrayList(amplitudes);
        amplitudeList.add(0, firstAmplitude);
        return new PerlinNoise(random, (Pair<Integer, DoubleList>)Pair.of((Object)firstOctave, (Object)amplitudeList), true);
    }

    public static PerlinNoise create(RandomSource random, int firstOctave, DoubleList amplitudes) {
        return new PerlinNoise(random, (Pair<Integer, DoubleList>)Pair.of((Object)firstOctave, (Object)amplitudes), true);
    }

    private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet octaveSet) {
        int highFreqOctaves;
        if (octaveSet.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }
        int lowFreqOctaves = -octaveSet.firstInt();
        int octaves = lowFreqOctaves + (highFreqOctaves = octaveSet.lastInt()) + 1;
        if (octaves < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }
        DoubleArrayList amplitudes = new DoubleArrayList(new double[octaves]);
        IntBidirectionalIterator iterator = octaveSet.iterator();
        while (iterator.hasNext()) {
            int octave = iterator.nextInt();
            amplitudes.set(octave + lowFreqOctaves, 1.0);
        }
        return Pair.of((Object)(-lowFreqOctaves), (Object)amplitudes);
    }

    protected PerlinNoise(RandomSource random, Pair<Integer, DoubleList> pair, boolean useNewInitialization) {
        this.firstOctave = (Integer)pair.getFirst();
        this.amplitudes = (DoubleList)pair.getSecond();
        int octaves = this.amplitudes.size();
        int zeroOctaveIndex = -this.firstOctave;
        this.noiseLevels = new ImprovedNoise[octaves];
        if (useNewInitialization) {
            PositionalRandomFactory positional = random.forkPositional();
            for (int i = 0; i < octaves; ++i) {
                if (this.amplitudes.getDouble(i) == 0.0) continue;
                int octave = this.firstOctave + i;
                this.noiseLevels[i] = new ImprovedNoise(positional.fromHashOf("octave_" + octave));
            }
        } else {
            double zeroOctaveAmplitude;
            ImprovedNoise zeroOctave = new ImprovedNoise(random);
            if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaves && (zeroOctaveAmplitude = this.amplitudes.getDouble(zeroOctaveIndex)) != 0.0) {
                this.noiseLevels[zeroOctaveIndex] = zeroOctave;
            }
            for (int i = zeroOctaveIndex - 1; i >= 0; --i) {
                if (i < octaves) {
                    double amplitude = this.amplitudes.getDouble(i);
                    if (amplitude != 0.0) {
                        this.noiseLevels[i] = new ImprovedNoise(random);
                        continue;
                    }
                    PerlinNoise.skipOctave(random);
                    continue;
                }
                PerlinNoise.skipOctave(random);
            }
            if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(a -> a != 0.0).count()) {
                throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
            }
            if (zeroOctaveIndex < octaves - 1) {
                throw new IllegalArgumentException("Positive octaves are temporarily disabled");
            }
        }
        this.lowestFreqInputFactor = Math.pow(2.0, -zeroOctaveIndex);
        this.lowestFreqValueFactor = Math.pow(2.0, octaves - 1) / (Math.pow(2.0, octaves) - 1.0);
        this.maxValue = this.edgeValue(2.0);
    }

    protected double maxValue() {
        return this.maxValue;
    }

    private static void skipOctave(RandomSource random) {
        random.consumeCount(262);
    }

    public double getValue(double x, double y, double z) {
        return this.getValue(x, y, z, 0.0, 0.0);
    }

    @Deprecated
    public double getValue(double x, double y, double z, double yScale, double yFudge) {
        double value = 0.0;
        double factor = this.lowestFreqInputFactor;
        double valueFactor = this.lowestFreqValueFactor;
        for (int i = 0; i < this.noiseLevels.length; ++i) {
            ImprovedNoise noise = this.noiseLevels[i];
            if (noise != null) {
                double noiseVal = noise.noise(PerlinNoise.wrap(x * factor), PerlinNoise.wrap(y * factor), PerlinNoise.wrap(z * factor), yScale * factor, yFudge * factor);
                value += this.amplitudes.getDouble(i) * noiseVal * valueFactor;
            }
            factor *= 2.0;
            valueFactor /= 2.0;
        }
        return value;
    }

    public double maxBrokenValue(double yScale) {
        return this.edgeValue(yScale + 2.0);
    }

    private double edgeValue(double noiseValue) {
        double value = 0.0;
        double valueFactor = this.lowestFreqValueFactor;
        for (int i = 0; i < this.noiseLevels.length; ++i) {
            ImprovedNoise noise = this.noiseLevels[i];
            if (noise != null) {
                value += this.amplitudes.getDouble(i) * noiseValue * valueFactor;
            }
            valueFactor /= 2.0;
        }
        return value;
    }

    public @Nullable ImprovedNoise getOctaveNoise(int i) {
        return this.noiseLevels[this.noiseLevels.length - 1 - i];
    }

    public static double wrap(double x) {
        return x - (double)Mth.lfloor(x / 3.3554432E7 + 0.5) * 3.3554432E7;
    }

    protected int firstOctave() {
        return this.firstOctave;
    }

    protected DoubleList amplitudes() {
        return this.amplitudes;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder sb) {
        sb.append("PerlinNoise{");
        List<String> amplitudeStrings = this.amplitudes.stream().map(d -> String.format(Locale.ROOT, "%.2f", d)).toList();
        sb.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(amplitudeStrings).append(", noise levels: [");
        for (int i = 0; i < this.noiseLevels.length; ++i) {
            sb.append(i).append(": ");
            ImprovedNoise noiseLevel = this.noiseLevels[i];
            if (noiseLevel == null) {
                sb.append("null");
            } else {
                noiseLevel.parityConfigString(sb);
            }
            sb.append(", ");
        }
        sb.append("]");
        sb.append("}");
    }
}

