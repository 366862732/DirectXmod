/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  it.unimi.dsi.fastutil.floats.Float2FloatFunction
 */
package net.minecraft.world.level.biome;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public final class OverworldBiomeBuilder {
    private static final float VALLEY_SIZE = 0.05f;
    private static final float LOW_START = 0.26666668f;
    public static final float HIGH_START = 0.4f;
    private static final float HIGH_END = 0.93333334f;
    private static final float PEAK_SIZE = 0.1f;
    public static final float PEAK_START = 0.56666666f;
    private static final float PEAK_END = 0.7666667f;
    public static final float NEAR_INLAND_START = -0.11f;
    public static final float MID_INLAND_START = 0.03f;
    public static final float FAR_INLAND_START = 0.3f;
    public static final float EROSION_INDEX_1_START = -0.78f;
    public static final float EROSION_INDEX_2_START = -0.375f;
    private static final float EROSION_DEEP_DARK_DRYNESS_THRESHOLD = -0.225f;
    private static final float DEPTH_DEEP_DARK_DRYNESS_THRESHOLD = 0.9f;
    private final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0f, 1.0f);
    private final Climate.Parameter[] temperatures = new Climate.Parameter[]{Climate.Parameter.span(-1.0f, -0.45f), Climate.Parameter.span(-0.45f, -0.15f), Climate.Parameter.span(-0.15f, 0.2f), Climate.Parameter.span(0.2f, 0.55f), Climate.Parameter.span(0.55f, 1.0f)};
    private final Climate.Parameter[] humidities = new Climate.Parameter[]{Climate.Parameter.span(-1.0f, -0.35f), Climate.Parameter.span(-0.35f, -0.1f), Climate.Parameter.span(-0.1f, 0.1f), Climate.Parameter.span(0.1f, 0.3f), Climate.Parameter.span(0.3f, 1.0f)};
    private final Climate.Parameter[] erosions = new Climate.Parameter[]{Climate.Parameter.span(-1.0f, -0.78f), Climate.Parameter.span(-0.78f, -0.375f), Climate.Parameter.span(-0.375f, -0.2225f), Climate.Parameter.span(-0.2225f, 0.05f), Climate.Parameter.span(0.05f, 0.45f), Climate.Parameter.span(0.45f, 0.55f), Climate.Parameter.span(0.55f, 1.0f)};
    private final Climate.Parameter FROZEN_RANGE = this.temperatures[0];
    private final Climate.Parameter UNFROZEN_RANGE = Climate.Parameter.span(this.temperatures[1], this.temperatures[4]);
    private final Climate.Parameter mushroomFieldsContinentalness = Climate.Parameter.span(-1.2f, -1.05f);
    private final Climate.Parameter deepOceanContinentalness = Climate.Parameter.span(-1.05f, -0.455f);
    private final Climate.Parameter oceanContinentalness = Climate.Parameter.span(-0.455f, -0.19f);
    private final Climate.Parameter coastContinentalness = Climate.Parameter.span(-0.19f, -0.11f);
    private final Climate.Parameter inlandContinentalness = Climate.Parameter.span(-0.11f, 0.55f);
    private final Climate.Parameter nearInlandContinentalness = Climate.Parameter.span(-0.11f, 0.03f);
    private final Climate.Parameter midInlandContinentalness = Climate.Parameter.span(0.03f, 0.3f);
    private final Climate.Parameter farInlandContinentalness = Climate.Parameter.span(0.3f, 1.0f);
    private final ResourceKey<Biome>[][] OCEANS = new ResourceKey[][]{{Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN}, {Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN, Biomes.OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.WARM_OCEAN}};
    private final ResourceKey<Biome>[][] MIDDLE_BIOMES = new ResourceKey[][]{{Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.TAIGA}, {Biomes.PLAINS, Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA}, {Biomes.FLOWER_FOREST, Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST}, {Biomes.SAVANNA, Biomes.SAVANNA, Biomes.FOREST, Biomes.JUNGLE, Biomes.JUNGLE}, {Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.DESERT}};
    private final ResourceKey<Biome>[][] MIDDLE_BIOMES_VARIANT = new ResourceKey[][]{{Biomes.ICE_SPIKES, null, Biomes.SNOWY_TAIGA, null, null}, {null, null, null, null, Biomes.OLD_GROWTH_PINE_TAIGA}, {Biomes.SUNFLOWER_PLAINS, null, null, Biomes.OLD_GROWTH_BIRCH_FOREST, null}, {null, null, Biomes.PLAINS, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE}, {null, null, null, null, null}};
    private final ResourceKey<Biome>[][] PLATEAU_BIOMES = new ResourceKey[][]{{Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_TAIGA}, {Biomes.MEADOW, Biomes.MEADOW, Biomes.FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA}, {Biomes.MEADOW, Biomes.MEADOW, Biomes.MEADOW, Biomes.MEADOW, Biomes.PALE_GARDEN}, {Biomes.SAVANNA_PLATEAU, Biomes.SAVANNA_PLATEAU, Biomes.FOREST, Biomes.FOREST, Biomes.JUNGLE}, {Biomes.BADLANDS, Biomes.BADLANDS, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.WOODED_BADLANDS}};
    private final ResourceKey<Biome>[][] PLATEAU_BIOMES_VARIANT = new ResourceKey[][]{{Biomes.ICE_SPIKES, null, null, null, null}, {Biomes.CHERRY_GROVE, null, Biomes.MEADOW, Biomes.MEADOW, Biomes.OLD_GROWTH_PINE_TAIGA}, {Biomes.CHERRY_GROVE, Biomes.CHERRY_GROVE, Biomes.FOREST, Biomes.BIRCH_FOREST, null}, {null, null, null, null, null}, {Biomes.ERODED_BADLANDS, Biomes.ERODED_BADLANDS, null, null, null}};
    private final ResourceKey<Biome>[][] SHATTERED_BIOMES = new ResourceKey[][]{{Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST}, {Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST}, {Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_FOREST}, {null, null, null, null, null}, {null, null, null, null, null}};

    public List<Climate.ParameterPoint> spawnTarget() {
        Climate.Parameter surfaceDepth = Climate.Parameter.point(0.0f);
        float riverClearance = 0.16f;
        return List.of(new Climate.ParameterPoint(this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.inlandContinentalness, this.FULL_RANGE), this.FULL_RANGE, surfaceDepth, Climate.Parameter.span(-1.0f, -0.16f), 0L), new Climate.ParameterPoint(this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.inlandContinentalness, this.FULL_RANGE), this.FULL_RANGE, surfaceDepth, Climate.Parameter.span(0.16f, 1.0f), 0L));
    }

    void addBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        if (SharedConstants.debugGenerateSquareTerrainWithoutNoise) {
            this.addDebugBiomes(biomes);
            return;
        }
        this.addOffCoastBiomes(biomes);
        this.addInlandBiomes(biomes);
        this.addUndergroundBiomes(biomes);
    }

    private void addDebugBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        CubicSpline<DensityFunctions.Spline.Coordinate> overworldOffset;
        HolderLookup.Provider builtIns = new RegistrySetBuilder().add(Registries.DENSITY_FUNCTION, NoiseRouterData::bootstrap).add(Registries.NOISE, NoiseData::bootstrap).build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        HolderGetter densityFunctions = builtIns.lookupOrThrow(Registries.DENSITY_FUNCTION);
        DensityFunctions.Spline.Coordinate continents = new DensityFunctions.Spline.Coordinate(new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.CONTINENTS)));
        DensityFunctions.Spline.Coordinate erosion = new DensityFunctions.Spline.Coordinate(new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.EROSION)));
        DensityFunctions.Spline.Coordinate ridges = new DensityFunctions.Spline.Coordinate(new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(NoiseRouterData.RIDGES_FOLDED)));
        biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.point(0.0f), this.FULL_RANGE, 0.01f), Biomes.PLAINS));
        CubicSpline<DensityFunctions.Spline.Coordinate> erosionOffsetSpline = TerrainProvider.buildErosionOffsetSpline(erosion, ridges, -0.15f, 0.0f, 0.0f, 0.1f, 0.0f, -0.03f, false, false, Float2FloatFunction.identity());
        if (erosionOffsetSpline instanceof CubicSpline.Multipoint) {
            CubicSpline.Multipoint multipoint = (CubicSpline.Multipoint)erosionOffsetSpline;
            ResourceKey<Biome> biome = Biomes.DESERT;
            for (float location : multipoint.locations()) {
                biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.point(location), Climate.Parameter.point(0.0f), this.FULL_RANGE, 0.0f), biome));
                biome = biome == Biomes.DESERT ? Biomes.BADLANDS : Biomes.DESERT;
            }
        }
        if ((overworldOffset = TerrainProvider.overworldOffset(continents, erosion, ridges, false)) instanceof CubicSpline.Multipoint) {
            CubicSpline.Multipoint multipoint = (CubicSpline.Multipoint)overworldOffset;
            for (float location : multipoint.locations()) {
                biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.point(location), this.FULL_RANGE, Climate.Parameter.point(0.0f), this.FULL_RANGE, 0.0f), Biomes.SNOWY_TAIGA));
            }
        }
    }

    private void addOffCoastBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addSurfaceBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, this.mushroomFieldsContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0f, Biomes.MUSHROOM_FIELDS);
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            this.addSurfaceBiome(biomes, temperature, this.FULL_RANGE, this.deepOceanContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0f, this.OCEANS[0][temperatureIndex]);
            this.addSurfaceBiome(biomes, temperature, this.FULL_RANGE, this.oceanContinentalness, this.FULL_RANGE, this.FULL_RANGE, 0.0f, this.OCEANS[1][temperatureIndex]);
        }
    }

    private void addInlandBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addMidSlice(biomes, Climate.Parameter.span(-1.0f, -0.93333334f));
        this.addHighSlice(biomes, Climate.Parameter.span(-0.93333334f, -0.7666667f));
        this.addPeaks(biomes, Climate.Parameter.span(-0.7666667f, -0.56666666f));
        this.addHighSlice(biomes, Climate.Parameter.span(-0.56666666f, -0.4f));
        this.addMidSlice(biomes, Climate.Parameter.span(-0.4f, -0.26666668f));
        this.addLowSlice(biomes, Climate.Parameter.span(-0.26666668f, -0.05f));
        this.addValleys(biomes, Climate.Parameter.span(-0.05f, 0.05f));
        this.addLowSlice(biomes, Climate.Parameter.span(0.05f, 0.26666668f));
        this.addMidSlice(biomes, Climate.Parameter.span(0.26666668f, 0.4f));
        this.addHighSlice(biomes, Climate.Parameter.span(0.4f, 0.56666666f));
        this.addPeaks(biomes, Climate.Parameter.span(0.56666666f, 0.7666667f));
        this.addHighSlice(biomes, Climate.Parameter.span(0.7666667f, 0.93333334f));
        this.addMidSlice(biomes, Climate.Parameter.span(0.93333334f, 1.0f));
    }

    private void addPeaks(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < this.humidities.length; ++humidityIndex) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, shatteredBiome);
                ResourceKey<Biome> peakBiome = this.pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[0], weirdness, 0.0f, peakBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), this.erosions[1], weirdness, 0.0f, middleBiomeOrBadlandsIfHotOrSlopeIfCold);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[1], weirdness, 0.0f, peakBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), Climate.Parameter.span(this.erosions[2], this.erosions[3]), weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[2], weirdness, 0.0f, plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[3], weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[3], weirdness, 0.0f, plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[4], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), this.erosions[5], weirdness, 0.0f, shatteredBiomeOrWindsweptSavanna);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[5], weirdness, 0.0f, shatteredBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, middleBiome);
            }
        }
    }

    private void addHighSlice(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < this.humidities.length; ++humidityIndex) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> slopeBiome = this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> peakBiome = this.pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[0], weirdness, 0.0f, slopeBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[0], weirdness, 0.0f, peakBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[1], weirdness, 0.0f, middleBiomeOrBadlandsIfHotOrSlopeIfCold);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[1], weirdness, 0.0f, slopeBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), Climate.Parameter.span(this.erosions[2], this.erosions[3]), weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[2], weirdness, 0.0f, plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[3], weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[3], weirdness, 0.0f, plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[4], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), this.erosions[5], weirdness, 0.0f, middleBiomeOrWindsweptSavanna);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[5], weirdness, 0.0f, shatteredBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, middleBiome);
            }
        }
    }

    private void addMidSlice(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter weirdness) {
        this.addSurfaceBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, this.coastContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[2]), weirdness, 0.0f, Biomes.STONY_SHORE);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[1], this.temperatures[2]), this.FULL_RANGE, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.SWAMP);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[3], this.temperatures[4]), this.FULL_RANGE, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.MANGROVE_SWAMP);
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < this.humidities.length; ++humidityIndex) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> shatteredBiome = this.pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> plateauBiome = this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> beachBiome = this.pickBeachBiome(temperatureIndex, humidityIndex);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> shatteredCoastBiome = this.pickShatteredCoastBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> slopeBiome = this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[0], weirdness, 0.0f, slopeBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.midInlandContinentalness), this.erosions[1], weirdness, 0.0f, middleBiomeOrBadlandsIfHotOrSlopeIfCold);
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[1], weirdness, 0.0f, temperatureIndex == 0 ? slopeBiome : plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[2], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.midInlandContinentalness, this.erosions[2], weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                this.addSurfaceBiome(biomes, temperature, humidity, this.farInlandContinentalness, this.erosions[2], weirdness, 0.0f, plateauBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.nearInlandContinentalness), this.erosions[3], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[3], weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                if (weirdness.max() < 0L) {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[4], weirdness, 0.0f, beachBiome);
                    this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[4], weirdness, 0.0f, middleBiome);
                } else {
                    this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), this.erosions[4], weirdness, 0.0f, middleBiome);
                }
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[5], weirdness, 0.0f, shatteredCoastBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[5], weirdness, 0.0f, middleBiomeOrWindsweptSavanna);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[5], weirdness, 0.0f, shatteredBiome);
                if (weirdness.max() < 0L) {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0f, beachBiome);
                } else {
                    this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0f, middleBiome);
                }
                if (temperatureIndex != 0) continue;
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, middleBiome);
            }
        }
    }

    private void addLowSlice(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter weirdness) {
        this.addSurfaceBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, this.coastContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[2]), weirdness, 0.0f, Biomes.STONY_SHORE);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[1], this.temperatures[2]), this.FULL_RANGE, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.SWAMP);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[3], this.temperatures[4]), this.FULL_RANGE, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.MANGROVE_SWAMP);
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < this.humidities.length; ++humidityIndex) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiome = this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> middleBiomeOrBadlandsIfHotOrSlopeIfCold = this.pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(temperatureIndex, humidityIndex, weirdness);
                ResourceKey<Biome> beachBiome = this.pickBeachBiome(temperatureIndex, humidityIndex);
                ResourceKey<Biome> middleBiomeOrWindsweptSavanna = this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, middleBiome);
                ResourceKey<Biome> shatteredCoastBiome = this.pickShatteredCoastBiome(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, middleBiomeOrBadlandsIfHotOrSlopeIfCold);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, Climate.Parameter.span(this.erosions[2], this.erosions[3]), weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), Climate.Parameter.span(this.erosions[2], this.erosions[3]), weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, Climate.Parameter.span(this.erosions[3], this.erosions[4]), weirdness, 0.0f, beachBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[4], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[5], weirdness, 0.0f, shatteredCoastBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.nearInlandContinentalness, this.erosions[5], weirdness, 0.0f, middleBiomeOrWindsweptSavanna);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), this.erosions[5], weirdness, 0.0f, middleBiome);
                this.addSurfaceBiome(biomes, temperature, humidity, this.coastContinentalness, this.erosions[6], weirdness, 0.0f, beachBiome);
                if (temperatureIndex != 0) continue;
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.nearInlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, middleBiome);
            }
        }
    }

    private void addValleys(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter weirdness) {
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, weirdness.max() < 0L ? Biomes.STONY_SHORE : Biomes.FROZEN_RIVER);
        this.addSurfaceBiome(biomes, this.UNFROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, weirdness.max() < 0L ? Biomes.STONY_SHORE : Biomes.RIVER);
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, this.nearInlandContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, Biomes.FROZEN_RIVER);
        this.addSurfaceBiome(biomes, this.UNFROZEN_RANGE, this.FULL_RANGE, this.nearInlandContinentalness, Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, Biomes.RIVER);
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), Climate.Parameter.span(this.erosions[2], this.erosions[5]), weirdness, 0.0f, Biomes.FROZEN_RIVER);
        this.addSurfaceBiome(biomes, this.UNFROZEN_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.coastContinentalness, this.farInlandContinentalness), Climate.Parameter.span(this.erosions[2], this.erosions[5]), weirdness, 0.0f, Biomes.RIVER);
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, this.erosions[6], weirdness, 0.0f, Biomes.FROZEN_RIVER);
        this.addSurfaceBiome(biomes, this.UNFROZEN_RANGE, this.FULL_RANGE, this.coastContinentalness, this.erosions[6], weirdness, 0.0f, Biomes.RIVER);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[1], this.temperatures[2]), this.FULL_RANGE, Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.SWAMP);
        this.addSurfaceBiome(biomes, Climate.Parameter.span(this.temperatures[3], this.temperatures[4]), this.FULL_RANGE, Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.MANGROVE_SWAMP);
        this.addSurfaceBiome(biomes, this.FROZEN_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.inlandContinentalness, this.farInlandContinentalness), this.erosions[6], weirdness, 0.0f, Biomes.FROZEN_RIVER);
        for (int temperatureIndex = 0; temperatureIndex < this.temperatures.length; ++temperatureIndex) {
            Climate.Parameter temperature = this.temperatures[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < this.humidities.length; ++humidityIndex) {
                Climate.Parameter humidity = this.humidities[humidityIndex];
                ResourceKey<Biome> middleBiomeOrBadlandsIfHot = this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
                this.addSurfaceBiome(biomes, temperature, humidity, Climate.Parameter.span(this.midInlandContinentalness, this.farInlandContinentalness), Climate.Parameter.span(this.erosions[0], this.erosions[1]), weirdness, 0.0f, middleBiomeOrBadlandsIfHot);
            }
        }
    }

    private void addUndergroundBiomes(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
        this.addUndergroundBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(0.8f, 1.0f), this.FULL_RANGE, this.FULL_RANGE, 0.0f, Biomes.DRIPSTONE_CAVES);
        this.addUndergroundBiome(biomes, this.FULL_RANGE, Climate.Parameter.span(0.7f, 1.0f), this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, 0.0f, Biomes.LUSH_CAVES);
        this.addUndergroundBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.coastContinentalness, this.inlandContinentalness), Climate.Parameter.span(this.erosions[5], this.erosions[6]), Climate.Parameter.span(-1.1f, -0.85f), 0.0f, Biomes.SULFUR_CAVES);
        this.addBottomBiome(biomes, this.FULL_RANGE, this.FULL_RANGE, this.FULL_RANGE, Climate.Parameter.span(this.erosions[0], this.erosions[1]), this.FULL_RANGE, 0.0f, Biomes.DEEP_DARK);
    }

    private ResourceKey<Biome> pickMiddleBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        if (weirdness.max() < 0L) {
            return this.MIDDLE_BIOMES[temperatureIndex][humidityIndex];
        }
        ResourceKey<Biome> variant = this.MIDDLE_BIOMES_VARIANT[temperatureIndex][humidityIndex];
        return variant == null ? this.MIDDLE_BIOMES[temperatureIndex][humidityIndex] : variant;
    }

    private ResourceKey<Biome> pickMiddleBiomeOrBadlandsIfHot(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        return temperatureIndex == 4 ? this.pickBadlandsBiome(humidityIndex, weirdness) : this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
    }

    private ResourceKey<Biome> pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        return temperatureIndex == 0 ? this.pickSlopeBiome(temperatureIndex, humidityIndex, weirdness) : this.pickMiddleBiomeOrBadlandsIfHot(temperatureIndex, humidityIndex, weirdness);
    }

    private ResourceKey<Biome> maybePickWindsweptSavannaBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness, ResourceKey<Biome> underlyingBiome) {
        if (temperatureIndex > 1 && humidityIndex < 4 && weirdness.max() >= 0L) {
            return Biomes.WINDSWEPT_SAVANNA;
        }
        return underlyingBiome;
    }

    private ResourceKey<Biome> pickShatteredCoastBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        ResourceKey<Biome> beachOrMiddleBiome = weirdness.max() >= 0L ? this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness) : this.pickBeachBiome(temperatureIndex, humidityIndex);
        return this.maybePickWindsweptSavannaBiome(temperatureIndex, humidityIndex, weirdness, beachOrMiddleBiome);
    }

    private ResourceKey<Biome> pickBeachBiome(int temperatureIndex, int humidityIndex) {
        if (temperatureIndex == 0) {
            return Biomes.SNOWY_BEACH;
        }
        if (temperatureIndex == 4) {
            return Biomes.DESERT;
        }
        return Biomes.BEACH;
    }

    private ResourceKey<Biome> pickBadlandsBiome(int humidityIndex, Climate.Parameter weirdness) {
        if (humidityIndex < 2) {
            return weirdness.max() < 0L ? Biomes.BADLANDS : Biomes.ERODED_BADLANDS;
        }
        if (humidityIndex < 3) {
            return Biomes.BADLANDS;
        }
        return Biomes.WOODED_BADLANDS;
    }

    private ResourceKey<Biome> pickPlateauBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        ResourceKey<Biome> variant;
        if (weirdness.max() >= 0L && (variant = this.PLATEAU_BIOMES_VARIANT[temperatureIndex][humidityIndex]) != null) {
            return variant;
        }
        return this.PLATEAU_BIOMES[temperatureIndex][humidityIndex];
    }

    private ResourceKey<Biome> pickPeakBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        if (temperatureIndex <= 2) {
            return weirdness.max() < 0L ? Biomes.JAGGED_PEAKS : Biomes.FROZEN_PEAKS;
        }
        if (temperatureIndex == 3) {
            return Biomes.STONY_PEAKS;
        }
        return this.pickBadlandsBiome(humidityIndex, weirdness);
    }

    private ResourceKey<Biome> pickSlopeBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        if (temperatureIndex >= 3) {
            return this.pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
        }
        if (humidityIndex <= 1) {
            return Biomes.SNOWY_SLOPES;
        }
        return Biomes.GROVE;
    }

    private ResourceKey<Biome> pickShatteredBiome(int temperatureIndex, int humidityIndex, Climate.Parameter weirdness) {
        ResourceKey<Biome> biome = this.SHATTERED_BIOMES[temperatureIndex][humidityIndex];
        return biome == null ? this.pickMiddleBiome(temperatureIndex, humidityIndex, weirdness) : biome;
    }

    private void addSurfaceBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, float offset, ResourceKey<Biome> second) {
        biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(0.0f), weirdness, offset), second));
        biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(1.0f), weirdness, offset), second));
    }

    private void addUndergroundBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, float offset, ResourceKey<Biome> biome) {
        biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.span(0.2f, 0.9f), weirdness, offset), biome));
    }

    private void addBottomBiome(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, float offset, ResourceKey<Biome> biome) {
        biomes.accept((Pair<Climate.ParameterPoint, ResourceKey<Biome>>)Pair.of((Object)Climate.parameters(temperature, humidity, continentalness, erosion, Climate.Parameter.point(1.1f), weirdness, offset), biome));
    }

    public static boolean isDeepDarkRegion(DensityFunction erosion, DensityFunction depth, DensityFunction.FunctionContext context) {
        return erosion.compute(context) < (double)-0.225f && depth.compute(context) > (double)0.9f;
    }

    public static String getDebugStringForPeaksAndValleys(double peaksAndValleys) {
        if (peaksAndValleys < (double)NoiseRouterData.peaksAndValleys(0.05f)) {
            return "Valley";
        }
        if (peaksAndValleys < (double)NoiseRouterData.peaksAndValleys(0.26666668f)) {
            return "Low";
        }
        if (peaksAndValleys < (double)NoiseRouterData.peaksAndValleys(0.4f)) {
            return "Mid";
        }
        if (peaksAndValleys < (double)NoiseRouterData.peaksAndValleys(0.56666666f)) {
            return "High";
        }
        return "Peak";
    }

    public String getDebugStringForContinentalness(double continentalness) {
        double continentalnessQuantized = Climate.quantizeCoord((float)continentalness);
        if (continentalnessQuantized < (double)this.mushroomFieldsContinentalness.max()) {
            return "Mushroom fields";
        }
        if (continentalnessQuantized < (double)this.deepOceanContinentalness.max()) {
            return "Deep ocean";
        }
        if (continentalnessQuantized < (double)this.oceanContinentalness.max()) {
            return "Ocean";
        }
        if (continentalnessQuantized < (double)this.coastContinentalness.max()) {
            return "Coast";
        }
        if (continentalnessQuantized < (double)this.nearInlandContinentalness.max()) {
            return "Near inland";
        }
        if (continentalnessQuantized < (double)this.midInlandContinentalness.max()) {
            return "Mid inland";
        }
        return "Far inland";
    }

    public String getDebugStringForErosion(double erosion) {
        return OverworldBiomeBuilder.getDebugStringForNoiseValue(erosion, this.erosions);
    }

    public String getDebugStringForTemperature(double temperature) {
        return OverworldBiomeBuilder.getDebugStringForNoiseValue(temperature, this.temperatures);
    }

    public String getDebugStringForHumidity(double humidity) {
        return OverworldBiomeBuilder.getDebugStringForNoiseValue(humidity, this.humidities);
    }

    private static String getDebugStringForNoiseValue(double noiseValue, Climate.Parameter[] array) {
        double noiseValueQuantized = Climate.quantizeCoord((float)noiseValue);
        for (int i = 0; i < array.length; ++i) {
            if (!(noiseValueQuantized < (double)array[i].max())) continue;
            return "" + i;
        }
        return "?";
    }

    @VisibleForDebug
    public Climate.Parameter[] getTemperatureThresholds() {
        return this.temperatures;
    }

    @VisibleForDebug
    public Climate.Parameter[] getHumidityThresholds() {
        return this.humidities;
    }

    @VisibleForDebug
    public Climate.Parameter[] getErosionThresholds() {
        return this.erosions;
    }

    @VisibleForDebug
    public Climate.Parameter[] getContinentalnessThresholds() {
        return new Climate.Parameter[]{this.mushroomFieldsContinentalness, this.deepOceanContinentalness, this.oceanContinentalness, this.coastContinentalness, this.nearInlandContinentalness, this.midInlandContinentalness, this.farInlandContinentalness};
    }

    @VisibleForDebug
    public Climate.Parameter[] getPeaksAndValleysThresholds() {
        return new Climate.Parameter[]{Climate.Parameter.span(-2.0f, NoiseRouterData.peaksAndValleys(0.05f)), Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.05f), NoiseRouterData.peaksAndValleys(0.26666668f)), Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.26666668f), NoiseRouterData.peaksAndValleys(0.4f)), Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.4f), NoiseRouterData.peaksAndValleys(0.56666666f)), Climate.Parameter.span(NoiseRouterData.peaksAndValleys(0.56666666f), 2.0f)};
    }

    @VisibleForDebug
    public Climate.Parameter[] getWeirdnessThresholds() {
        return new Climate.Parameter[]{Climate.Parameter.span(-2.0f, 0.0f), Climate.Parameter.span(0.0f, 2.0f)};
    }
}

