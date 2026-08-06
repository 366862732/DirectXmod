/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.floats.Float2FloatFunction
 */
package net.minecraft.data.worldgen;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;

public class TerrainProvider {
    private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51f;
    private static final float OCEAN_CONTINENTALNESS = -0.4f;
    private static final float PLAINS_CONTINENTALNESS = 0.1f;
    private static final float BEACH_CONTINENTALNESS = -0.15f;
    private static final Float2FloatFunction NO_TRANSFORM = Float2FloatFunction.identity();
    private static final Float2FloatFunction AMPLIFIED_OFFSET = offset -> offset < 0.0f ? offset : offset * 2.0f;
    private static final Float2FloatFunction AMPLIFIED_FACTOR = factor -> 1.25f - 6.25f / (factor + 5.0f);
    private static final Float2FloatFunction AMPLIFIED_JAGGEDNESS = jaggedness -> jaggedness * 2.0f;

    public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldOffset(I continents, I erosion, I ridges, boolean amplified) {
        Float2FloatFunction offsetTransformer = amplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
        CubicSpline<I> beachSpline = TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.15f, 0.0f, 0.0f, 0.1f, 0.0f, -0.03f, false, false, offsetTransformer);
        CubicSpline<I> lowSpline = TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.1f, 0.03f, 0.1f, 0.1f, 0.01f, -0.03f, false, false, offsetTransformer);
        CubicSpline<I> midSpline = TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.1f, 0.03f, 0.1f, 0.7f, 0.01f, -0.03f, true, true, offsetTransformer);
        CubicSpline<I> highSpline = TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.05f, 0.03f, 0.1f, 1.0f, 0.01f, 0.01f, true, true, offsetTransformer);
        return CubicSpline.builder(continents, offsetTransformer).addPoint(-1.1f, 0.044f).addPoint(-1.02f, -0.2222f).addPoint(-0.51f, -0.2222f).addPoint(-0.44f, -0.12f).addPoint(-0.18f, -0.12f).addPoint(-0.16f, beachSpline).addPoint(-0.15f, beachSpline).addPoint(-0.1f, lowSpline).addPoint(0.25f, midSpline).addPoint(1.0f, highSpline).build();
    }

    public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldFactor(I continents, I erosion, I weirdness, I ridges, boolean amplified) {
        Float2FloatFunction factorTransformer = amplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
        return CubicSpline.builder(continents, NO_TRANSFORM).addPoint(-0.19f, 3.95f).addPoint(-0.15f, TerrainProvider.getErosionFactor(erosion, weirdness, ridges, 6.25f, true, NO_TRANSFORM)).addPoint(-0.1f, TerrainProvider.getErosionFactor(erosion, weirdness, ridges, 5.47f, true, factorTransformer)).addPoint(0.03f, TerrainProvider.getErosionFactor(erosion, weirdness, ridges, 5.08f, true, factorTransformer)).addPoint(0.06f, TerrainProvider.getErosionFactor(erosion, weirdness, ridges, 4.69f, false, factorTransformer)).build();
    }

    public static <I extends BoundedFloatFunction<?>> CubicSpline<I> overworldJaggedness(I continents, I erosion, I weirdness, I ridges, boolean amplified) {
        Float2FloatFunction jaggednessTransformer = amplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
        float farInlandMiddle = 0.65f;
        return CubicSpline.builder(continents, jaggednessTransformer).addPoint(-0.11f, 0.0f).addPoint(0.03f, TerrainProvider.buildErosionJaggednessSpline(erosion, weirdness, ridges, 1.0f, 0.5f, 0.0f, 0.0f, jaggednessTransformer)).addPoint(0.65f, TerrainProvider.buildErosionJaggednessSpline(erosion, weirdness, ridges, 1.0f, 1.0f, 1.0f, 0.0f, jaggednessTransformer)).build();
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildErosionJaggednessSpline(I erosion, I weirdness, I ridges, float jaggednessFactorAtPeakRidgeAndErosionIndex0, float jaggednessFactorAtPeakRidgeAndErosionIndex1, float jaggednessFactorAtHighRidgeAndErosionIndex0, float jaggednessFactorAtHighRidgeAndErosionIndex1, Float2FloatFunction jaggednessTransformer) {
        float erosionIndex1Middle = -0.5775f;
        CubicSpline<I> ridgeJaggednessSplineAtErosion0 = TerrainProvider.buildRidgeJaggednessSpline(weirdness, ridges, jaggednessFactorAtPeakRidgeAndErosionIndex0, jaggednessFactorAtHighRidgeAndErosionIndex0, jaggednessTransformer);
        CubicSpline<I> ridgeJaggednessSplineAtErosion1 = TerrainProvider.buildRidgeJaggednessSpline(weirdness, ridges, jaggednessFactorAtPeakRidgeAndErosionIndex1, jaggednessFactorAtHighRidgeAndErosionIndex1, jaggednessTransformer);
        return CubicSpline.builder(erosion, jaggednessTransformer).addPoint(-1.0f, ridgeJaggednessSplineAtErosion0).addPoint(-0.78f, ridgeJaggednessSplineAtErosion1).addPoint(-0.5775f, ridgeJaggednessSplineAtErosion1).addPoint(-0.375f, 0.0f).build();
    }

    public static float peaksAndValleys(float weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 0.6666667f) - 0.33333334f) * 3.0f;
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildRidgeJaggednessSpline(I weirdness, I ridges, float jaggednessFactorAtPeakRidge, float jaggednessFactorAtHighRidge, Float2FloatFunction jaggednessTransformer) {
        float highSliceStart = TerrainProvider.peaksAndValleys(0.4f);
        float highSliceEnd = TerrainProvider.peaksAndValleys(0.56666666f);
        float highSliceMiddle = (highSliceStart + highSliceEnd) / 2.0f;
        CubicSpline.Builder<I> ridgeSpline = CubicSpline.builder(ridges, jaggednessTransformer);
        ridgeSpline.addPoint(highSliceStart, 0.0f);
        if (jaggednessFactorAtHighRidge > 0.0f) {
            ridgeSpline.addPoint(highSliceMiddle, TerrainProvider.buildWeirdnessJaggednessSpline(weirdness, jaggednessFactorAtHighRidge, jaggednessTransformer));
        } else {
            ridgeSpline.addPoint(highSliceMiddle, 0.0f);
        }
        if (jaggednessFactorAtPeakRidge > 0.0f) {
            ridgeSpline.addPoint(1.0f, TerrainProvider.buildWeirdnessJaggednessSpline(weirdness, jaggednessFactorAtPeakRidge, jaggednessTransformer));
        } else {
            ridgeSpline.addPoint(1.0f, 0.0f);
        }
        return ridgeSpline.build();
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildWeirdnessJaggednessSpline(I weirdness, float jaggednessFactor, Float2FloatFunction jaggednessTransformer) {
        float maxJaggednessAtNegativeWeirdness = 0.63f * jaggednessFactor;
        float maxJaggednessAtPositiveWeirdness = 0.3f * jaggednessFactor;
        return CubicSpline.builder(weirdness, jaggednessTransformer).addPoint(-0.01f, maxJaggednessAtNegativeWeirdness).addPoint(0.01f, maxJaggednessAtPositiveWeirdness).build();
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> getErosionFactor(I erosion, I weirdness, I ridges, float baseValue, boolean shatteredTerrain, Float2FloatFunction factorTransformer) {
        CubicSpline<I> baseSpline = CubicSpline.builder(weirdness, factorTransformer).addPoint(-0.2f, 6.3f).addPoint(0.2f, baseValue).build();
        CubicSpline.Builder<I> erosionPoints = CubicSpline.builder(erosion, factorTransformer).addPoint(-0.6f, baseSpline).addPoint(-0.5f, CubicSpline.builder(weirdness, factorTransformer).addPoint(-0.05f, 6.3f).addPoint(0.05f, 2.67f).build()).addPoint(-0.35f, baseSpline).addPoint(-0.25f, baseSpline).addPoint(-0.1f, CubicSpline.builder(weirdness, factorTransformer).addPoint(-0.05f, 2.67f).addPoint(0.05f, 6.3f).build()).addPoint(0.03f, baseSpline);
        if (shatteredTerrain) {
            CubicSpline<I> weirdnessShattered = CubicSpline.builder(weirdness, factorTransformer).addPoint(0.0f, baseValue).addPoint(0.1f, 0.625f).build();
            CubicSpline<I> ridgesShattered = CubicSpline.builder(ridges, factorTransformer).addPoint(-0.9f, baseValue).addPoint(-0.69f, weirdnessShattered).build();
            erosionPoints.addPoint(0.35f, baseValue).addPoint(0.45f, ridgesShattered).addPoint(0.55f, ridgesShattered).addPoint(0.62f, baseValue);
        } else {
            CubicSpline<I> extremeHillsTerrainFromMidSliceAndUp = CubicSpline.builder(ridges, factorTransformer).addPoint(-0.7f, baseSpline).addPoint(-0.15f, 1.37f).build();
            CubicSpline<I> extra3dNoiseOnPeaksOnly = CubicSpline.builder(ridges, factorTransformer).addPoint(0.45f, baseSpline).addPoint(0.7f, 1.56f).build();
            erosionPoints.addPoint(0.05f, extra3dNoiseOnPeaksOnly).addPoint(0.4f, extra3dNoiseOnPeaksOnly).addPoint(0.45f, extremeHillsTerrainFromMidSliceAndUp).addPoint(0.55f, extremeHillsTerrainFromMidSliceAndUp).addPoint(0.58f, baseValue);
        }
        return erosionPoints.build();
    }

    private static float calculateSlope(float y1, float y2, float x1, float x2) {
        return (y2 - y1) / (x2 - x1);
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildMountainRidgeSplineWithPoints(I ridges, float modulation, boolean saddle, Float2FloatFunction offsetTransformer) {
        CubicSpline.Builder<I> build = CubicSpline.builder(ridges, offsetTransformer);
        float allowRiversBelow = -0.7f;
        float minPoint = -1.0f;
        float minPointContinentalness = TerrainProvider.mountainContinentalness(-1.0f, modulation, -0.7f);
        float maxPoint = 1.0f;
        float maxPointContinentalness = TerrainProvider.mountainContinentalness(1.0f, modulation, -0.7f);
        float ridgeZeroPoint = TerrainProvider.calculateMountainRidgeZeroContinentalnessPoint(modulation);
        float afterRiverPoint = -0.65f;
        if (-0.65f < ridgeZeroPoint && ridgeZeroPoint < 1.0f) {
            float afterRiverThresholdContinentalness = TerrainProvider.mountainContinentalness(-0.65f, modulation, -0.7f);
            float beforeRiverPoint = -0.75f;
            float beforeRiverThresholdContinentalness = TerrainProvider.mountainContinentalness(-0.75f, modulation, -0.7f);
            float minPointDerivative = TerrainProvider.calculateSlope(minPointContinentalness, beforeRiverThresholdContinentalness, -1.0f, -0.75f);
            build.addPoint(-1.0f, minPointContinentalness, minPointDerivative);
            build.addPoint(-0.75f, beforeRiverThresholdContinentalness);
            build.addPoint(-0.65f, afterRiverThresholdContinentalness);
            float ridgeZeroPointContinentalness = TerrainProvider.mountainContinentalness(ridgeZeroPoint, modulation, -0.7f);
            float maxPointDerivative = TerrainProvider.calculateSlope(ridgeZeroPointContinentalness, maxPointContinentalness, ridgeZeroPoint, 1.0f);
            float smallOffset = 0.01f;
            build.addPoint(ridgeZeroPoint - 0.01f, ridgeZeroPointContinentalness);
            build.addPoint(ridgeZeroPoint, ridgeZeroPointContinentalness, maxPointDerivative);
            build.addPoint(1.0f, maxPointContinentalness, maxPointDerivative);
        } else {
            float simpleDerivative = TerrainProvider.calculateSlope(minPointContinentalness, maxPointContinentalness, -1.0f, 1.0f);
            if (saddle) {
                build.addPoint(-1.0f, Math.max(0.2f, minPointContinentalness));
                build.addPoint(0.0f, Mth.lerp(0.5f, minPointContinentalness, maxPointContinentalness), simpleDerivative);
            } else {
                build.addPoint(-1.0f, minPointContinentalness, simpleDerivative);
            }
            build.addPoint(1.0f, maxPointContinentalness, simpleDerivative);
        }
        return build.build();
    }

    private static float mountainContinentalness(float ridge, float modulation, float allowRiversBelow) {
        float ridgeOffset = 1.17f;
        float ridgeAmplitude = 0.46082947f;
        float ridgeSlope = 1.0f - (1.0f - modulation) * 0.5f;
        float ridgeIntersect = 0.5f * (1.0f - modulation);
        float adjustedRidgeHeight = (ridge + 1.17f) * 0.46082947f;
        float continentalness = adjustedRidgeHeight * ridgeSlope - ridgeIntersect;
        if (ridge < allowRiversBelow) {
            return Math.max(continentalness, -0.2222f);
        }
        return Math.max(continentalness, 0.0f);
    }

    private static float calculateMountainRidgeZeroContinentalnessPoint(float modulation) {
        float ridgeOffset = 1.17f;
        float ridgeAmplitude = 0.46082947f;
        float ridgeSlope = 1.0f - (1.0f - modulation) * 0.5f;
        float ridgeIntersect = 0.5f * (1.0f - modulation);
        return ridgeIntersect / (0.46082947f * ridgeSlope) - 1.17f;
    }

    public static <I extends BoundedFloatFunction<?>> CubicSpline<I> buildErosionOffsetSpline(I erosion, I ridges, float lowValley, float hill, float tallHill, float mountainFactor, float plain, float swamp, boolean includeExtremeHills, boolean saddle, Float2FloatFunction offsetTransformer) {
        float lowPeaks = 0.6f;
        float valleyPlateau = 0.5f;
        float plateau = 0.5f;
        CubicSpline<I> veryLowErosionMountains = TerrainProvider.buildMountainRidgeSplineWithPoints(ridges, Mth.lerp(mountainFactor, 0.6f, 1.5f), saddle, offsetTransformer);
        CubicSpline<I> lowErosionMountains = TerrainProvider.buildMountainRidgeSplineWithPoints(ridges, Mth.lerp(mountainFactor, 0.6f, 1.0f), saddle, offsetTransformer);
        CubicSpline<I> mountains = TerrainProvider.buildMountainRidgeSplineWithPoints(ridges, mountainFactor, saddle, offsetTransformer);
        CubicSpline<I> widePlateau = TerrainProvider.ridgeSpline(ridges, lowValley - 0.15f, 0.5f * mountainFactor, Mth.lerp(0.5f, 0.5f, 0.5f) * mountainFactor, 0.5f * mountainFactor, 0.6f * mountainFactor, 0.5f, offsetTransformer);
        CubicSpline<I> narrowPlateau = TerrainProvider.ridgeSpline(ridges, lowValley, plain * mountainFactor, hill * mountainFactor, 0.5f * mountainFactor, 0.6f * mountainFactor, 0.5f, offsetTransformer);
        CubicSpline<I> plains = TerrainProvider.ridgeSpline(ridges, lowValley, plain, plain, hill, tallHill, 0.5f, offsetTransformer);
        CubicSpline<I> plainsFarInland = TerrainProvider.ridgeSpline(ridges, lowValley, plain, plain, hill, tallHill, 0.5f, offsetTransformer);
        CubicSpline<I> extremeHills = CubicSpline.builder(ridges, offsetTransformer).addPoint(-1.0f, lowValley).addPoint(-0.4f, plains).addPoint(0.0f, tallHill + 0.07f).build();
        CubicSpline<I> swamps = TerrainProvider.ridgeSpline(ridges, -0.02f, swamp, swamp, hill, tallHill, 0.0f, offsetTransformer);
        CubicSpline.Builder<I> builder = CubicSpline.builder(erosion, offsetTransformer).addPoint(-0.85f, veryLowErosionMountains).addPoint(-0.7f, lowErosionMountains).addPoint(-0.4f, mountains).addPoint(-0.35f, widePlateau).addPoint(-0.1f, narrowPlateau).addPoint(0.2f, plains);
        if (includeExtremeHills) {
            builder.addPoint(0.4f, plainsFarInland).addPoint(0.45f, extremeHills).addPoint(0.55f, extremeHills).addPoint(0.58f, plainsFarInland);
        }
        builder.addPoint(0.7f, swamps);
        return builder.build();
    }

    private static <I extends BoundedFloatFunction<?>> CubicSpline<I> ridgeSpline(I ridges, float valley, float low, float mid, float high, float peaks, float minValleySteepness, Float2FloatFunction offsetTransformer) {
        float d1 = Math.max(0.5f * (low - valley), minValleySteepness);
        float d2 = 5.0f * (mid - low);
        return CubicSpline.builder(ridges, offsetTransformer).addPoint(-1.0f, valley, d1).addPoint(-0.4f, low, Math.min(d1, d2)).addPoint(0.0f, mid, d2).addPoint(0.4f, high, 2.0f * (high - mid)).addPoint(1.0f, peaks, 0.7f * (peaks - high)).build();
    }
}

