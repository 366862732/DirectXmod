/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public class BlendedNoise
implements DensityFunction.SimpleFunction {
    private static final Codec<Double> SCALE_RANGE = Codec.doubleRange((double)0.001, (double)1000.0);
    private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)SCALE_RANGE.fieldOf("xz_scale").forGetter(n -> n.xzScale), (App)SCALE_RANGE.fieldOf("y_scale").forGetter(n -> n.yScale), (App)SCALE_RANGE.fieldOf("xz_factor").forGetter(n -> n.xzFactor), (App)SCALE_RANGE.fieldOf("y_factor").forGetter(n -> n.yFactor), (App)Codec.doubleRange((double)1.0, (double)8.0).fieldOf("smear_scale_multiplier").forGetter(n -> n.smearScaleMultiplier)).apply((Applicative)i, BlendedNoise::createUnseeded));
    public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final double xzMultiplier;
    private final double yMultiplier;
    private final double xzFactor;
    private final double yFactor;
    private final double smearScaleMultiplier;
    private final double maxValue;
    private final double xzScale;
    private final double yScale;

    public static BlendedNoise createUnseeded(double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        return new BlendedNoise(new XoroshiroRandomSource(0L), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
    }

    private BlendedNoise(PerlinNoise minLimitNoise, PerlinNoise maxLimitNoise, PerlinNoise mainNoise, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        this.minLimitNoise = minLimitNoise;
        this.maxLimitNoise = maxLimitNoise;
        this.mainNoise = mainNoise;
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.xzFactor = xzFactor;
        this.yFactor = yFactor;
        this.smearScaleMultiplier = smearScaleMultiplier;
        this.xzMultiplier = 684.412 * this.xzScale;
        this.yMultiplier = 684.412 * this.yScale;
        this.maxValue = minLimitNoise.maxBrokenValue(this.yMultiplier);
    }

    @VisibleForTesting
    public BlendedNoise(RandomSource random, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        this(PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
    }

    public BlendedNoise withNewRandom(RandomSource terrainRandom) {
        return new BlendedNoise(terrainRandom, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        double limitX = (double)context.blockX() * this.xzMultiplier;
        double limitY = (double)context.blockY() * this.yMultiplier;
        double limitZ = (double)context.blockZ() * this.xzMultiplier;
        double mainX = limitX / this.xzFactor;
        double mainY = limitY / this.yFactor;
        double mainZ = limitZ / this.xzFactor;
        double limitSmear = this.yMultiplier * this.smearScaleMultiplier;
        double mainSmear = limitSmear / this.yFactor;
        double blendMin = 0.0;
        double blendMax = 0.0;
        double mainNoiseValue = 0.0;
        boolean optimizeLoop = true;
        double pow = 1.0;
        for (int i = 0; i < 8; ++i) {
            ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
            if (noise != null) {
                mainNoiseValue += noise.noise(PerlinNoise.wrap(mainX * pow), PerlinNoise.wrap(mainY * pow), PerlinNoise.wrap(mainZ * pow), mainSmear * pow, mainY * pow) / pow;
            }
            pow /= 2.0;
        }
        double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
        boolean isMax = factor >= 1.0;
        boolean isMin = factor <= 0.0;
        pow = 1.0;
        for (int i = 0; i < 16; ++i) {
            ImprovedNoise maxNoise;
            ImprovedNoise minNoise;
            double wx = PerlinNoise.wrap(limitX * pow);
            double wy = PerlinNoise.wrap(limitY * pow);
            double wz = PerlinNoise.wrap(limitZ * pow);
            double yScalePow = limitSmear * pow;
            if (!isMax && (minNoise = this.minLimitNoise.getOctaveNoise(i)) != null) {
                blendMin += minNoise.noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }
            if (!isMin && (maxNoise = this.maxLimitNoise.getOctaveNoise(i)) != null) {
                blendMax += maxNoise.noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }
            pow /= 2.0;
        }
        return Mth.clampedLerp(factor, blendMin / 512.0, blendMax / 512.0) / 128.0;
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.maxValue;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder sb) {
        sb.append("BlendedNoise{minLimitNoise=");
        this.minLimitNoise.parityConfigString(sb);
        sb.append(", maxLimitNoise=");
        this.maxLimitNoise.parityConfigString(sb);
        sb.append(", mainNoise=");
        this.mainNoise.parityConfigString(sb);
        sb.append(String.format(Locale.ROOT, ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8", 684.412, 684.412, 8.555150000000001, 4.277575000000001)).append('}');
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}

