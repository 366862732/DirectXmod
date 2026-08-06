/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data.worldgen;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseData {
    @Deprecated
    public static final NormalNoise.NoiseParameters DEFAULT_SHIFT = new NormalNoise.NoiseParameters(-3, 1.0, 1.0, 1.0, 0.0);

    public static void bootstrap(BootstrapContext<NormalNoise.NoiseParameters> context) {
        NoiseData.registerBiomeNoises(context, 0, Noises.TEMPERATURE, Noises.VEGETATION, Noises.CONTINENTALNESS, Noises.EROSION);
        NoiseData.registerBiomeNoises(context, -2, Noises.TEMPERATURE_LARGE, Noises.VEGETATION_LARGE, Noises.CONTINENTALNESS_LARGE, Noises.EROSION_LARGE);
        NoiseData.register(context, Noises.TEMPERATURE_NETHER, -7, 1.0, 1.0);
        NoiseData.register(context, Noises.VEGETATION_NETHER, -7, 1.0, 1.0);
        NoiseData.register(context, Noises.RIDGE, -7, 1.0, 2.0, 1.0, 0.0, 0.0, 0.0);
        context.register(Noises.SHIFT, DEFAULT_SHIFT);
        NoiseData.register(context, Noises.AQUIFER_BARRIER, -3, 1.0, new double[0]);
        NoiseData.register(context, Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.AQUIFER_LAVA, -1, 1.0, new double[0]);
        NoiseData.register(context, Noises.AQUIFER_FLUID_LEVEL_SPREAD, -5, 1.0, new double[0]);
        NoiseData.register(context, Noises.PILLAR, -7, 1.0, 1.0);
        NoiseData.register(context, Noises.PILLAR_RARENESS, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.PILLAR_THICKNESS, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_2D, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_2D_ELEVATION, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_2D_MODULATOR, -11, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_2D_THICKNESS, -11, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_3D_1, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_3D_2, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_3D_RARITY, -11, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_3D_THICKNESS, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_ROUGHNESS, -5, 1.0, new double[0]);
        NoiseData.register(context, Noises.SPAGHETTI_ROUGHNESS_MODULATOR, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.CAVE_ENTRANCE, -7, 0.4, 0.5, 1.0);
        NoiseData.register(context, Noises.CAVE_LAYER, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.CAVE_CHEESE, -8, 0.5, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0);
        NoiseData.register(context, Noises.ORE_VEININESS, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.ORE_VEIN_A, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.ORE_VEIN_B, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.ORE_GAP, -5, 1.0, new double[0]);
        NoiseData.register(context, Noises.NOODLE, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.NOODLE_THICKNESS, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.NOODLE_RIDGE_A, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.NOODLE_RIDGE_B, -7, 1.0, new double[0]);
        NoiseData.register(context, Noises.JAGGED, -16, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.SURFACE, -6, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.SURFACE_SECONDARY, -6, 1.0, 1.0, 0.0, 1.0);
        NoiseData.register(context, Noises.CLAY_BANDS_OFFSET, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.BADLANDS_PILLAR, -2, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.BADLANDS_PILLAR_ROOF, -8, 1.0, new double[0]);
        NoiseData.register(context, Noises.BADLANDS_SURFACE, -6, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.ICEBERG_PILLAR, -6, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.ICEBERG_PILLAR_ROOF, -3, 1.0, new double[0]);
        NoiseData.register(context, Noises.ICEBERG_SURFACE, -6, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.SULFUR_CAVE_GRADIENT, -5, 1.0, 0.0, 1.0);
        NoiseData.register(context, Noises.SWAMP, -2, 1.0, new double[0]);
        NoiseData.register(context, Noises.CALCITE, -9, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.GRAVEL, -8, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.POWDER_SNOW, -6, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.PACKED_ICE, -7, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.ICE, -4, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, Noises.SOUL_SAND_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        NoiseData.register(context, Noises.GRAVEL_LAYER, -8, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        NoiseData.register(context, Noises.PATCH, -5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.013333333333333334);
        NoiseData.register(context, Noises.NETHERRACK, -3, 1.0, 0.0, 0.0, 0.35);
        NoiseData.register(context, Noises.NETHER_WART, -3, 1.0, 0.0, 0.0, 0.9);
        NoiseData.register(context, Noises.NETHER_STATE_SELECTOR, -4, 1.0, new double[0]);
    }

    private static void registerBiomeNoises(BootstrapContext<NormalNoise.NoiseParameters> context, int octaveOffset, ResourceKey<NormalNoise.NoiseParameters> temperature, ResourceKey<NormalNoise.NoiseParameters> vegetation, ResourceKey<NormalNoise.NoiseParameters> continentalness, ResourceKey<NormalNoise.NoiseParameters> erosion) {
        NoiseData.register(context, temperature, -10 + octaveOffset, 1.5, 0.0, 1.0, 0.0, 0.0, 0.0);
        NoiseData.register(context, vegetation, -8 + octaveOffset, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0);
        NoiseData.register(context, continentalness, -9 + octaveOffset, 1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);
        NoiseData.register(context, erosion, -9 + octaveOffset, 1.0, 1.0, 0.0, 1.0, 1.0);
    }

    private static void register(BootstrapContext<NormalNoise.NoiseParameters> context, ResourceKey<NormalNoise.NoiseParameters> key, int firstOctave, double firstAmplitude, double ... amplitudes) {
        context.register(key, new NormalNoise.NoiseParameters(firstOctave, firstAmplitude, amplitudes));
    }
}

