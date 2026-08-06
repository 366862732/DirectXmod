/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.level.levelgen;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
    public static final float GLOBAL_OFFSET = -0.50375f;
    private static final float ORE_THICKNESS = 0.08f;
    private static final double VEININESS_FREQUENCY = 1.5;
    private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5;
    private static final double SURFACE_DENSITY_THRESHOLD = 1.5625;
    private static final double CHEESE_NOISE_TARGET = -0.703125;
    public static final double NOISE_ZERO = 0.390625;
    public static final int ISLAND_CHUNK_DISTANCE = 64;
    public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
    private static final int DENSITY_Y_ANCHOR_BOTTOM = -64;
    private static final int DENSITY_Y_ANCHOR_TOP = 320;
    private static final double DENSITY_Y_BOTTOM = 1.5;
    private static final double DENSITY_Y_TOP = -1.5;
    private static final int OVERWORLD_BOTTOM_SLIDE_HEIGHT = 24;
    private static final double BASE_DENSITY_MULTIPLIER = 4.0;
    private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0);
    private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
    private static final ResourceKey<DensityFunction> ZERO = NoiseRouterData.createKey("zero");
    private static final ResourceKey<DensityFunction> Y = NoiseRouterData.createKey("y");
    private static final ResourceKey<DensityFunction> SHIFT_X = NoiseRouterData.createKey("shift_x");
    private static final ResourceKey<DensityFunction> SHIFT_Z = NoiseRouterData.createKey("shift_z");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = NoiseRouterData.createKey("overworld/base_3d_noise");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = NoiseRouterData.createKey("nether/base_3d_noise");
    private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = NoiseRouterData.createKey("end/base_3d_noise");
    public static final ResourceKey<DensityFunction> CONTINENTS = NoiseRouterData.createKey("overworld/continents");
    public static final ResourceKey<DensityFunction> EROSION = NoiseRouterData.createKey("overworld/erosion");
    public static final ResourceKey<DensityFunction> RIDGES = NoiseRouterData.createKey("overworld/ridges");
    public static final ResourceKey<DensityFunction> RIDGES_FOLDED = NoiseRouterData.createKey("overworld/ridges_folded");
    public static final ResourceKey<DensityFunction> OFFSET = NoiseRouterData.createKey("overworld/offset");
    public static final ResourceKey<DensityFunction> FACTOR = NoiseRouterData.createKey("overworld/factor");
    public static final ResourceKey<DensityFunction> JAGGEDNESS = NoiseRouterData.createKey("overworld/jaggedness");
    public static final ResourceKey<DensityFunction> DEPTH = NoiseRouterData.createKey("overworld/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE = NoiseRouterData.createKey("overworld/sloped_cheese");
    public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = NoiseRouterData.createKey("overworld_large_biomes/continents");
    public static final ResourceKey<DensityFunction> EROSION_LARGE = NoiseRouterData.createKey("overworld_large_biomes/erosion");
    private static final ResourceKey<DensityFunction> OFFSET_LARGE = NoiseRouterData.createKey("overworld_large_biomes/offset");
    private static final ResourceKey<DensityFunction> FACTOR_LARGE = NoiseRouterData.createKey("overworld_large_biomes/factor");
    private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = NoiseRouterData.createKey("overworld_large_biomes/jaggedness");
    private static final ResourceKey<DensityFunction> DEPTH_LARGE = NoiseRouterData.createKey("overworld_large_biomes/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = NoiseRouterData.createKey("overworld_large_biomes/sloped_cheese");
    private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = NoiseRouterData.createKey("overworld_amplified/offset");
    private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = NoiseRouterData.createKey("overworld_amplified/factor");
    private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = NoiseRouterData.createKey("overworld_amplified/jaggedness");
    private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = NoiseRouterData.createKey("overworld_amplified/depth");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = NoiseRouterData.createKey("overworld_amplified/sloped_cheese");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = NoiseRouterData.createKey("end/sloped_cheese");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = NoiseRouterData.createKey("overworld/caves/spaghetti_roughness_function");
    private static final ResourceKey<DensityFunction> ENTRANCES = NoiseRouterData.createKey("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> NOODLE = NoiseRouterData.createKey("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> PILLARS = NoiseRouterData.createKey("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = NoiseRouterData.createKey("overworld/caves/spaghetti_2d_thickness_modulator");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = NoiseRouterData.createKey("overworld/caves/spaghetti_2d");

    private static ResourceKey<DensityFunction> createKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Identifier.withDefaultNamespace(name));
    }

    public static Holder<? extends DensityFunction> bootstrap(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
        context.register(ZERO, DensityFunctions.zero());
        int belowBottom = DimensionType.MIN_Y * 2;
        int aboveTop = DimensionType.MAX_Y * 2;
        context.register(Y, DensityFunctions.yClampedGradient(belowBottom, aboveTop, belowBottom, aboveTop));
        DensityFunction shiftX = NoiseRouterData.registerAndWrap(context, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(noises.getOrThrow(Noises.SHIFT)))));
        DensityFunction shiftZ = NoiseRouterData.registerAndWrap(context, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(noises.getOrThrow(Noises.SHIFT)))));
        context.register(BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25, 0.125, 80.0, 160.0, 8.0));
        context.register(BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25, 0.375, 80.0, 60.0, 8.0));
        context.register(BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25, 0.25, 80.0, 160.0, 4.0));
        DensityFunction continents = NoiseRouterData.registerAndWrap(context, CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.CONTINENTALNESS))));
        DensityFunction erosion = NoiseRouterData.registerAndWrap(context, EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.EROSION))));
        DensityFunction ridge = NoiseRouterData.registerAndWrap(context, RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.RIDGE))));
        context.register(RIDGES_FOLDED, NoiseRouterData.peaksAndValleys(ridge));
        DensityFunction jaggedNoise = DensityFunctions.noise(noises.getOrThrow(Noises.JAGGED), 1500.0, 0.0);
        NoiseRouterData.registerTerrainNoises(context, functions, jaggedNoise, continents, erosion, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
        DensityFunction continentsLarge = NoiseRouterData.registerAndWrap(context, CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.CONTINENTALNESS_LARGE))));
        DensityFunction erosionLarge = NoiseRouterData.registerAndWrap(context, EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(Noises.EROSION_LARGE))));
        NoiseRouterData.registerTerrainNoises(context, functions, jaggedNoise, continentsLarge, erosionLarge, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false);
        NoiseRouterData.registerTerrainNoises(context, functions, jaggedNoise, continents, erosion, OFFSET_AMPLIFIED, FACTOR_AMPLIFIED, JAGGEDNESS_AMPLIFIED, DEPTH_AMPLIFIED, SLOPED_CHEESE_AMPLIFIED, true);
        context.register(SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), NoiseRouterData.getFunction(functions, BASE_3D_NOISE_END)));
        context.register(SPAGHETTI_ROUGHNESS_FUNCTION, NoiseRouterData.spaghettiRoughnessFunction(noises));
        context.register(SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(noises.getOrThrow(Noises.SPAGHETTI_2D_THICKNESS), 2.0, 1.0, -0.6, -1.3)));
        context.register(SPAGHETTI_2D, NoiseRouterData.spaghetti2D(functions, noises));
        context.register(ENTRANCES, NoiseRouterData.entrances(functions, noises));
        context.register(NOODLE, NoiseRouterData.noodle(functions, noises));
        return context.register(PILLARS, NoiseRouterData.pillars(noises));
    }

    private static void registerTerrainNoises(BootstrapContext<DensityFunction> context, HolderGetter<DensityFunction> functions, DensityFunction jaggedNoise, DensityFunction continentsFunction, DensityFunction erosionFunction, ResourceKey<DensityFunction> offsetName, ResourceKey<DensityFunction> factorName, ResourceKey<DensityFunction> jaggednessName, ResourceKey<DensityFunction> depthName, ResourceKey<DensityFunction> slopedCheeseName, boolean amplified) {
        DensityFunctions.Spline.Coordinate continents = new DensityFunctions.Spline.Coordinate(continentsFunction);
        DensityFunctions.Spline.Coordinate erosion = new DensityFunctions.Spline.Coordinate(erosionFunction);
        DensityFunctions.Spline.Coordinate weirdness = new DensityFunctions.Spline.Coordinate(NoiseRouterData.getFunction(functions, RIDGES));
        DensityFunctions.Spline.Coordinate ridges = new DensityFunctions.Spline.Coordinate(NoiseRouterData.getFunction(functions, RIDGES_FOLDED));
        DensityFunction offset = NoiseRouterData.registerAndWrap(context, offsetName, NoiseRouterData.splineWithBlending(DensityFunctions.add(DensityFunctions.constant(-0.50375f), DensityFunctions.spline(TerrainProvider.overworldOffset(continents, erosion, ridges, amplified))), DensityFunctions.blendOffset()));
        DensityFunction factor = NoiseRouterData.registerAndWrap(context, factorName, NoiseRouterData.splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(continents, erosion, weirdness, ridges, amplified)), BLENDING_FACTOR));
        DensityFunction depth = NoiseRouterData.registerAndWrap(context, depthName, NoiseRouterData.offsetToDepth(offset));
        DensityFunction unscaledJaggedness = NoiseRouterData.registerAndWrap(context, jaggednessName, NoiseRouterData.splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(continents, erosion, weirdness, ridges, amplified)), BLENDING_JAGGEDNESS));
        DensityFunction jaggedness = DensityFunctions.flatCache(DensityFunctions.mul(unscaledJaggedness, jaggedNoise.halfNegative()));
        DensityFunction initialDensity = NoiseRouterData.noiseGradientDensity(factor, DensityFunctions.add(depth, jaggedness));
        context.register(slopedCheeseName, DensityFunctions.add(initialDensity, NoiseRouterData.getFunction(functions, BASE_3D_NOISE_OVERWORLD)));
    }

    private static DensityFunction offsetToDepth(DensityFunction offset) {
        return DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5), offset);
    }

    private static DensityFunction registerAndWrap(BootstrapContext<DensityFunction> context, ResourceKey<DensityFunction> name, DensityFunction value) {
        return new DensityFunctions.HolderHolder(context.register(name, value));
    }

    private static DensityFunction getFunction(HolderGetter<DensityFunction> functions, ResourceKey<DensityFunction> name) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(name));
    }

    private static DensityFunction peaksAndValleys(DensityFunction weirdness) {
        return DensityFunctions.mul(DensityFunctions.add(DensityFunctions.add(weirdness.abs(), DensityFunctions.constant(-0.6666666666666666)).abs(), DensityFunctions.constant(-0.3333333333333333)), DensityFunctions.constant(-3.0));
    }

    public static float peaksAndValleys(float weirdness) {
        return TerrainProvider.peaksAndValleys(weirdness);
    }

    private static DensityFunction spaghettiRoughnessFunction(HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction spaghettiRoughnessNoise = DensityFunctions.noise(noises.getOrThrow(Noises.SPAGHETTI_ROUGHNESS));
        DensityFunction spaghettiRoughnessModulator = DensityFunctions.mappedNoise(noises.getOrThrow(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0, -0.1);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(spaghettiRoughnessModulator, DensityFunctions.add(spaghettiRoughnessNoise.abs(), DensityFunctions.constant(-0.4))));
    }

    private static DensityFunction entrances(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction spaghetti3DRarityModulator = DensityFunctions.cacheOnce(DensityFunctions.noise(noises.getOrThrow(Noises.SPAGHETTI_3D_RARITY), 2.0, 1.0));
        DensityFunction spaghetti3DThicknessModulator = DensityFunctions.mappedNoise(noises.getOrThrow(Noises.SPAGHETTI_3D_THICKNESS), -0.065, -0.088);
        DensityFunction spaghetti3DCave1 = QuantizedSpaghettiRarity.wrapRarity3d(spaghetti3DRarityModulator, noises.getOrThrow(Noises.SPAGHETTI_3D_1));
        DensityFunction spaghetti3DCave2 = QuantizedSpaghettiRarity.wrapRarity3d(spaghetti3DRarityModulator, noises.getOrThrow(Noises.SPAGHETTI_3D_2));
        DensityFunction spaghetti3DFunction = DensityFunctions.add(DensityFunctions.max(spaghetti3DCave1, spaghetti3DCave2), spaghetti3DThicknessModulator).clamp(-1.0, 1.0);
        DensityFunction spaghettiRoughnessFunction = NoiseRouterData.getFunction(functions, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction bigEntranceNoiseSource = DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_ENTRANCE), 0.75, 0.5);
        DensityFunction bigEntrancesFunction = DensityFunctions.add(DensityFunctions.add(bigEntranceNoiseSource, DensityFunctions.constant(0.37)), DensityFunctions.yClampedGradient(-10, 30, 0.3, 0.0));
        return DensityFunctions.cacheOnce(DensityFunctions.min(bigEntrancesFunction, DensityFunctions.add(spaghettiRoughnessFunction, spaghetti3DFunction)));
    }

    private static DensityFunction noodle(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction y = NoiseRouterData.getFunction(functions, Y);
        int minBlockY = -64;
        int noodleMinY = -60;
        int noodleMaxY = 320;
        DensityFunction noodleToggle = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.NOODLE), 1.0, 1.0), -60, 320, -1);
        DensityFunction noodleThickness = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.mappedNoise(noises.getOrThrow(Noises.NOODLE_THICKNESS), 1.0, 1.0, -0.05, -0.1), -60, 320, 0);
        double noodleRidgeFrequency = 2.6666666666666665;
        DensityFunction noodleRidgeA = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.NOODLE_RIDGE_A), 2.6666666666666665, 2.6666666666666665), -60, 320, 0);
        DensityFunction noodleRidgeB = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.NOODLE_RIDGE_B), 2.6666666666666665, 2.6666666666666665), -60, 320, 0);
        DensityFunction noodleRidged = DensityFunctions.mul(DensityFunctions.constant(1.5), DensityFunctions.max(noodleRidgeA.abs(), noodleRidgeB.abs()));
        return DensityFunctions.rangeChoice(noodleToggle, -1000000.0, 0.0, DensityFunctions.constant(64.0), DensityFunctions.add(noodleThickness, noodleRidged));
    }

    private static DensityFunction pillars(HolderGetter<NormalNoise.NoiseParameters> noises) {
        double xzFrequency = 25.0;
        double yFrequency = 0.3;
        DensityFunction pillarNoiseSource = DensityFunctions.noise(noises.getOrThrow(Noises.PILLAR), 25.0, 0.3);
        DensityFunction pillarRarenessModulator = DensityFunctions.mappedNoise(noises.getOrThrow(Noises.PILLAR_RARENESS), 0.0, -2.0);
        DensityFunction pillarThicknessModulator = DensityFunctions.mappedNoise(noises.getOrThrow(Noises.PILLAR_THICKNESS), 0.0, 1.1);
        DensityFunction pillarsWithRareness = DensityFunctions.add(DensityFunctions.mul(pillarNoiseSource, DensityFunctions.constant(2.0)), pillarRarenessModulator);
        return DensityFunctions.cacheOnce(DensityFunctions.mul(pillarsWithRareness, pillarThicknessModulator.cube()));
    }

    private static DensityFunction spaghetti2D(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction spaghetti2DRarityModulator = DensityFunctions.noise(noises.getOrThrow(Noises.SPAGHETTI_2D_MODULATOR), 2.0, 1.0);
        DensityFunction spaghetti2DCave = QuantizedSpaghettiRarity.wrapRarity2d(spaghetti2DRarityModulator, noises.getOrThrow(Noises.SPAGHETTI_2D));
        DensityFunction spaghetti2DElevationModulator = DensityFunctions.mappedNoise(noises.getOrThrow(Noises.SPAGHETTI_2D_ELEVATION), 0.0, Math.floorDiv(-64, 8), 8.0);
        DensityFunction spaghetti2DThicknessModulator = NoiseRouterData.getFunction(functions, SPAGHETTI_2D_THICKNESS_MODULATOR);
        DensityFunction slopedSpaghetti = DensityFunctions.add(DensityFunctions.flatCache(spaghetti2DElevationModulator), DensityFunctions.yClampedGradient(-64, 320, 8.0, -40.0)).abs();
        DensityFunction layerRidged = DensityFunctions.add(slopedSpaghetti, spaghetti2DThicknessModulator).cube();
        double ridgeOffset = 0.083;
        DensityFunction caveNoise = DensityFunctions.add(spaghetti2DCave, DensityFunctions.mul(DensityFunctions.constant(0.083), spaghetti2DThicknessModulator));
        return DensityFunctions.max(caveNoise, layerRidged).clamp(-1.0, 1.0);
    }

    private static DensityFunction underground(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises, DensityFunction slopedCheese) {
        DensityFunction spaghetti2DFunction = NoiseRouterData.getFunction(functions, SPAGHETTI_2D);
        DensityFunction spaghettiRoughnessFunction = NoiseRouterData.getFunction(functions, SPAGHETTI_ROUGHNESS_FUNCTION);
        DensityFunction layerNoiseSource = DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 8.0);
        DensityFunction layerizedCavernsFunction = DensityFunctions.mul(DensityFunctions.constant(4.0), layerNoiseSource.square());
        DensityFunction cheese = DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666);
        DensityFunction solidifedCheeseWithTopSlide = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27), cheese).clamp(-1.0, 1.0), DensityFunctions.add(DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), slopedCheese)).clamp(0.0, 0.5));
        DensityFunction baseCaveDensity = DensityFunctions.add(layerizedCavernsFunction, solidifedCheeseWithTopSlide);
        DensityFunction undergroundSubtractions = DensityFunctions.min(DensityFunctions.min(baseCaveDensity, NoiseRouterData.getFunction(functions, ENTRANCES)), DensityFunctions.add(spaghetti2DFunction, spaghettiRoughnessFunction));
        DensityFunction pillarsWithoutCutoff = NoiseRouterData.getFunction(functions, PILLARS);
        DensityFunction pillars = DensityFunctions.rangeChoice(pillarsWithoutCutoff, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), pillarsWithoutCutoff);
        return DensityFunctions.max(undergroundSubtractions, pillars);
    }

    private static DensityFunction postProcess(DensityFunction slide) {
        DensityFunction blended = DensityFunctions.blendDensity(slide);
        return DensityFunctions.interpolated(DensityFunctions.mul(blended, DensityFunctions.constant(0.64))).squeeze();
    }

    private static DensityFunction remap(DensityFunction input, double fromMin, double fromMax, double toMin, double toMax) {
        double factor = (toMax - toMin) / (fromMax - fromMin);
        double offset = toMin - fromMin * factor;
        return DensityFunctions.add(DensityFunctions.mul(input, DensityFunctions.constant(factor)), DensityFunctions.constant(offset));
    }

    protected static NoiseRouter overworld(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises, boolean largeBiomes, boolean amplified) {
        DensityFunction barrierNoise = DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5);
        DensityFunction fluidLevelFloodednessNoise = DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67);
        DensityFunction fluidLevelSpreadNoise = DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143);
        DensityFunction lavaNoise = DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA));
        DensityFunction shiftX = NoiseRouterData.getFunction(functions, SHIFT_X);
        DensityFunction shiftZ = NoiseRouterData.getFunction(functions, SHIFT_Z);
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(largeBiomes ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noises.getOrThrow(largeBiomes ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
        DensityFunction offset = NoiseRouterData.getFunction(functions, largeBiomes ? OFFSET_LARGE : (amplified ? OFFSET_AMPLIFIED : OFFSET));
        DensityFunction factor = NoiseRouterData.getFunction(functions, largeBiomes ? FACTOR_LARGE : (amplified ? FACTOR_AMPLIFIED : FACTOR));
        DensityFunction depth = NoiseRouterData.getFunction(functions, largeBiomes ? DEPTH_LARGE : (amplified ? DEPTH_AMPLIFIED : DEPTH));
        DensityFunction preliminarySurfaceLevel = NoiseRouterData.preliminarySurfaceLevel(offset, factor, amplified);
        DensityFunction slopedCheese = DensityFunctions.cacheOnce(NoiseRouterData.getFunction(functions, largeBiomes ? SLOPED_CHEESE_LARGE : (amplified ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE)));
        DensityFunction surfaceWithEntrances = DensityFunctions.min(slopedCheese, DensityFunctions.mul(DensityFunctions.constant(5.0), NoiseRouterData.getFunction(functions, ENTRANCES)));
        DensityFunction caves = DensityFunctions.rangeChoice(slopedCheese, -1000000.0, 1.5625, surfaceWithEntrances, NoiseRouterData.underground(functions, noises, slopedCheese));
        DensityFunction fullNoise = DensityFunctions.min(NoiseRouterData.postProcess(NoiseRouterData.slideOverworld(amplified, caves)), NoiseRouterData.getFunction(functions, NOODLE));
        DensityFunction y = NoiseRouterData.getFunction(functions, Y);
        int veinMinY = Stream.of(OreVeinifier.VeinType.values()).mapToInt(t -> t.minY).min().orElse(-DimensionType.MIN_Y * 2);
        int veinMaxY = Stream.of(OreVeinifier.VeinType.values()).mapToInt(t -> t.maxY).max().orElse(-DimensionType.MIN_Y * 2);
        DensityFunction veinToggle = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5), veinMinY, veinMaxY, 0);
        float oreRidgeFrequency = 4.0f;
        DensityFunction veinA = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0), veinMinY, veinMaxY, 0).abs();
        DensityFunction veinB = NoiseRouterData.yLimitedInterpolatable(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0), veinMinY, veinMaxY, 0).abs();
        DensityFunction veinRidged = DensityFunctions.add(DensityFunctions.constant(-0.08f), DensityFunctions.max(veinA, veinB));
        DensityFunction veinGap = DensityFunctions.noise(noises.getOrThrow(Noises.ORE_GAP));
        return new NoiseRouter(barrierNoise, fluidLevelFloodednessNoise, fluidLevelSpreadNoise, lavaNoise, temperature, vegetation, NoiseRouterData.getFunction(functions, largeBiomes ? CONTINENTS_LARGE : CONTINENTS), NoiseRouterData.getFunction(functions, largeBiomes ? EROSION_LARGE : EROSION), depth, NoiseRouterData.getFunction(functions, RIDGES), preliminarySurfaceLevel, fullNoise, veinToggle, veinRidged, veinGap);
    }

    private static DensityFunction slideOverworld(boolean isAmplified, DensityFunction caves) {
        return NoiseRouterData.slide(caves, -64, 384, isAmplified ? 16 : 80, isAmplified ? 0 : 64, -0.078125, 0, 24, isAmplified ? 0.4 : 0.1171875);
    }

    private static DensityFunction slideNetherLike(HolderGetter<DensityFunction> functions, int minY, int height) {
        return NoiseRouterData.slide(NoiseRouterData.getFunction(functions, BASE_3D_NOISE_NETHER), minY, height, 24, 0, 0.9375, -8, 24, 2.5);
    }

    private static DensityFunction slideEndLike(DensityFunction caves, int minY, int height) {
        return NoiseRouterData.slide(caves, minY, height, 72, -184, -23.4375, 4, 32, -0.234375);
    }

    protected static NoiseRouter nether(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(DensityFunctions.zero(), DensityFunctions.zero(), 0.25, noises.getOrThrow(Noises.TEMPERATURE_NETHER));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(DensityFunctions.zero(), DensityFunctions.zero(), 0.25, noises.getOrThrow(Noises.VEGETATION_NETHER));
        DensityFunction slide = NoiseRouterData.slideNetherLike(functions, 0, 128);
        DensityFunction fullNoise = NoiseRouterData.postProcess(slide);
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), temperature, vegetation, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), fullNoise, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    protected static NoiseRouter caves(HolderGetter<DensityFunction> functions) {
        DensityFunction slide = NoiseRouterData.slideNetherLike(functions, -64, 192);
        return NoiseRouterData.simpleRouter(NoiseRouterData.postProcess(slide));
    }

    protected static NoiseRouter floatingIslands(HolderGetter<DensityFunction> functions, HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction slide = NoiseRouterData.slideEndLike(NoiseRouterData.getFunction(functions, BASE_3D_NOISE_END), 0, 256);
        return NoiseRouterData.simpleRouter(NoiseRouterData.postProcess(slide));
    }

    private static DensityFunction slideEnd(DensityFunction caves) {
        return NoiseRouterData.slideEndLike(caves, 0, 128);
    }

    protected static NoiseRouter end(HolderGetter<DensityFunction> functions) {
        DensityFunction islands = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
        DensityFunction fullNoise = NoiseRouterData.postProcess(NoiseRouterData.slideEnd(NoiseRouterData.getFunction(functions, SLOPED_CHEESE_END)));
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), islands, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), fullNoise, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    private static NoiseRouter simpleRouter(DensityFunction fullNoise) {
        return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), fullNoise, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
    }

    public static NoiseRouter none() {
        return NoiseRouterData.simpleRouter(DensityFunctions.zero());
    }

    private static DensityFunction splineWithBlending(DensityFunction spline, DensityFunction blendingTarget) {
        DensityFunction blendedSpline = DensityFunctions.lerp(DensityFunctions.blendAlpha(), blendingTarget, spline);
        return DensityFunctions.flatCache(DensityFunctions.cache2d(blendedSpline));
    }

    private static DensityFunction noiseGradientDensity(DensityFunction factor, DensityFunction depthWithJaggedness) {
        DensityFunction gradientUnscaled = DensityFunctions.mul(depthWithJaggedness, factor);
        return DensityFunctions.mul(DensityFunctions.constant(4.0), gradientUnscaled.quarterNegative());
    }

    private static DensityFunction preliminarySurfaceLevel(DensityFunction offset, DensityFunction factor, boolean amplified) {
        DensityFunction cachedFactor = DensityFunctions.cache2d(factor);
        DensityFunction cachedOffset = DensityFunctions.cache2d(offset);
        DensityFunction upperBound = NoiseRouterData.remap(DensityFunctions.add(DensityFunctions.mul(DensityFunctions.constant(0.2734375), cachedFactor.invert()), DensityFunctions.mul(DensityFunctions.constant(-1.0), cachedOffset)), 1.5, -1.5, -64.0, 320.0);
        upperBound = upperBound.clamp(-40.0, 320.0);
        DensityFunction density = DensityFunctions.add(NoiseRouterData.slideOverworld(amplified, DensityFunctions.add(NoiseRouterData.noiseGradientDensity(cachedFactor, NoiseRouterData.offsetToDepth(cachedOffset)), DensityFunctions.constant(-0.703125)).clamp(-64.0, 64.0)), DensityFunctions.constant(-0.390625));
        return DensityFunctions.findTopSurface(density, upperBound, -64, NoiseSettings.OVERWORLD_NOISE_SETTINGS.getCellHeight());
    }

    private static DensityFunction yLimitedInterpolatable(DensityFunction y, DensityFunction whenInRange, int minYInclusive, int maxYInclusive, int whenOutOfRange) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, minYInclusive, maxYInclusive + 1, whenInRange, DensityFunctions.constant(whenOutOfRange)));
    }

    private static DensityFunction slide(DensityFunction caves, int minY, int height, int topStartY, int topEndY, double topTarget, int bottomStartY, int bottomEndY, double bottomTarget) {
        DensityFunction noiseValue = caves;
        DensityFunction topFactor = DensityFunctions.yClampedGradient(minY + height - topStartY, minY + height - topEndY, 1.0, 0.0);
        noiseValue = DensityFunctions.lerp(topFactor, topTarget, noiseValue);
        DensityFunction bottomFactor = DensityFunctions.yClampedGradient(minY + bottomStartY, minY + bottomEndY, 0.0, 1.0);
        noiseValue = DensityFunctions.lerp(bottomFactor, bottomTarget, noiseValue);
        return noiseValue;
    }

    protected static final class QuantizedSpaghettiRarity {
        protected QuantizedSpaghettiRarity() {
        }

        public static DensityFunction wrapRarity2d(DensityFunction input, Holder<NormalNoise.NoiseParameters> noise) {
            return DensityFunctions.intervalSelect(input, DoubleList.of((double[])new double[]{-0.75, -0.5, 0.5, 0.75}), List.of(QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 0.5), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 0.75), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 1.0), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 2.0), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 3.0))).abs();
        }

        public static DensityFunction wrapRarity3d(DensityFunction input, Holder<NormalNoise.NoiseParameters> noise) {
            return DensityFunctions.intervalSelect(input, DoubleList.of((double)-0.5, (double)0.0, (double)0.5), List.of(QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 0.75), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 1.0), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 1.5), QuantizedSpaghettiRarity.noiseFunctionForRarity(noise, 2.0))).abs();
        }

        private static DensityFunction noiseFunctionForRarity(Holder<NormalNoise.NoiseParameters> noise, double rarity) {
            return DensityFunctions.mul(DensityFunctions.constant(rarity), DensityFunctions.noise(noise, 1.0 / rarity, 1.0 / rarity));
        }
    }
}

