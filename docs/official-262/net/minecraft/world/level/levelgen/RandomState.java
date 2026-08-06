/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
    private final PositionalRandomFactory random;
    private final HolderGetter<NormalNoise.NoiseParameters> noises;
    private final NoiseRouter router;
    private final Climate.Sampler sampler;
    private final SurfaceSystem surfaceSystem;
    private final PositionalRandomFactory aquiferRandom;
    private final PositionalRandomFactory oreRandom;
    private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
    private final Map<Identifier, PositionalRandomFactory> positionalRandoms;

    public static RandomState create(HolderGetter.Provider holders, ResourceKey<NoiseGeneratorSettings> noiseSettings, long seed) {
        return RandomState.create(holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(noiseSettings).value(), holders.lookupOrThrow(Registries.NOISE), seed);
    }

    public static RandomState create(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises, long seed) {
        return new RandomState(settings, noises, seed);
    }

    private RandomState(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises, final long seed) {
        this.random = settings.getRandomSource().newInstance(seed).forkPositional();
        this.noises = noises;
        this.aquiferRandom = this.random.fromHashOf(Identifier.withDefaultNamespace("aquifer")).forkPositional();
        this.oreRandom = this.random.fromHashOf(Identifier.withDefaultNamespace("ore")).forkPositional();
        this.noiseIntances = new ConcurrentHashMap<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise>();
        this.positionalRandoms = new ConcurrentHashMap<Identifier, PositionalRandomFactory>();
        this.surfaceSystem = new SurfaceSystem(this, settings.defaultBlock(), settings.seaLevel(), this.random);
        final boolean useLegacyInit = settings.useLegacyRandomSource();
        class NoiseWiringHelper
        implements DensityFunction.Visitor {
            private final Map<DensityFunction, DensityFunction> wrapped;
            final /* synthetic */ RandomState this$0;

            NoiseWiringHelper() {
                RandomState randomState = this$0;
                Objects.requireNonNull(randomState);
                this.this$0 = randomState;
                this.wrapped = new HashMap<DensityFunction, DensityFunction>();
            }

            private RandomSource newLegacyInstance(long seedOffset) {
                return new LegacyRandomSource(seed + seedOffset);
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                Holder<NormalNoise.NoiseParameters> noiseData = noise.noiseData();
                if (noiseData.is(Noises.TEMPERATURE_NETHER)) {
                    NormalNoise newNoise = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), noiseData.value());
                    return new DensityFunction.NoiseHolder(noiseData, newNoise);
                }
                if (noiseData.is(Noises.VEGETATION_NETHER)) {
                    NormalNoise newNoise = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), noiseData.value());
                    return new DensityFunction.NoiseHolder(noiseData, newNoise);
                }
                NormalNoise instantiate = this.this$0.getOrCreateNoise(noiseData.unwrapKey().orElseThrow());
                return new DensityFunction.NoiseHolder(noiseData, instantiate);
            }

            private DensityFunction wrapNew(DensityFunction function) {
                if (function instanceof BlendedNoise) {
                    BlendedNoise noise = (BlendedNoise)function;
                    RandomSource terrainRandom = useLegacyInit ? this.newLegacyInstance(0L) : this.this$0.random.fromHashOf(Identifier.withDefaultNamespace("terrain"));
                    return noise.withNewRandom(terrainRandom);
                }
                if (function instanceof DensityFunctions.EndIslandDensityFunction) {
                    return new DensityFunctions.EndIslandDensityFunction(seed);
                }
                return function;
            }

            @Override
            public DensityFunction apply(DensityFunction function) {
                return this.wrapped.computeIfAbsent(function, this::wrapNew);
            }
        }
        this.router = settings.noiseRouter().mapAll(new NoiseWiringHelper());
        DensityFunction.Visitor noiseFlattener = new DensityFunction.Visitor(this){
            private final Map<DensityFunction, DensityFunction> wrapped;
            {
                Objects.requireNonNull(this$0);
                this.wrapped = new HashMap<DensityFunction, DensityFunction>();
            }

            private DensityFunction wrapNew(DensityFunction function) {
                if (function instanceof DensityFunctions.HolderHolder) {
                    DensityFunctions.HolderHolder holder = (DensityFunctions.HolderHolder)function;
                    return holder.function().value();
                }
                if (function instanceof DensityFunctions.Marker) {
                    DensityFunctions.Marker marker = (DensityFunctions.Marker)function;
                    return marker.wrapped();
                }
                return function;
            }

            @Override
            public DensityFunction apply(DensityFunction input) {
                return this.wrapped.computeIfAbsent(input, this::wrapNew);
            }
        };
        this.sampler = new Climate.Sampler(this.router.temperature().mapAll(noiseFlattener), this.router.vegetation().mapAll(noiseFlattener), this.router.continents().mapAll(noiseFlattener), this.router.erosion().mapAll(noiseFlattener), this.router.depth().mapAll(noiseFlattener), this.router.ridges().mapAll(noiseFlattener), settings.spawnTarget());
    }

    public NormalNoise getOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> noise) {
        return this.noiseIntances.computeIfAbsent(noise, key -> Noises.instantiate(this.noises, this.random, noise));
    }

    public PositionalRandomFactory getOrCreateRandomFactory(Identifier name) {
        return this.positionalRandoms.computeIfAbsent(name, key -> this.random.fromHashOf(name).forkPositional());
    }

    public NoiseRouter router() {
        return this.router;
    }

    public Climate.Sampler sampler() {
        return this.sampler;
    }

    public SurfaceSystem surfaceSystem() {
        return this.surfaceSystem;
    }

    public PositionalRandomFactory aquiferRandom() {
        return this.aquiferRandom;
    }

    public PositionalRandomFactory oreRandom() {
        return this.oreRandom;
    }
}

