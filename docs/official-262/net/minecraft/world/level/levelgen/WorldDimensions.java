/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;

public record WorldDimensions(Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
    public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.unboundedMap(ResourceKey.codec(Registries.LEVEL_STEM), LevelStem.CODEC).fieldOf("dimensions").forGetter(WorldDimensions::dimensions)).apply((Applicative)i, i.stable(WorldDimensions::new)));
    private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);

    public WorldDimensions {
        LevelStem overworld = dimensions.get(LevelStem.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld settings missing");
        }
    }

    public WorldDimensions(Registry<LevelStem> registry) {
        this(registry.listElements().collect(Collectors.toMap(Holder.Reference::key, Holder.Reference::value)));
    }

    public static Stream<ResourceKey<LevelStem>> keysInOrder(Set<ResourceKey<LevelStem>> knownKeys) {
        return Stream.concat(BUILTIN_ORDER.stream().filter(knownKeys::contains), knownKeys.stream().filter(k -> !BUILTIN_ORDER.contains(k)));
    }

    public WorldDimensions replaceOverworldGenerator(HolderLookup.Provider registries, ChunkGenerator generator) {
        HolderGetter dimensionTypes = registries.lookupOrThrow(Registries.DIMENSION_TYPE);
        Map<ResourceKey<LevelStem>, LevelStem> newDimensions = WorldDimensions.withOverworld((HolderLookup<DimensionType>)dimensionTypes, this.dimensions, generator);
        return new WorldDimensions(newDimensions);
    }

    public static Map<ResourceKey<LevelStem>, LevelStem> withOverworld(HolderLookup<DimensionType> dimensionTypes, Map<ResourceKey<LevelStem>, LevelStem> dimensions, ChunkGenerator generator) {
        LevelStem stem = dimensions.get(LevelStem.OVERWORLD);
        Holder<DimensionType> type = stem == null ? dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD) : stem.type();
        return WorldDimensions.withOverworld(dimensions, type, generator);
    }

    public static Map<ResourceKey<LevelStem>, LevelStem> withOverworld(Map<ResourceKey<LevelStem>, LevelStem> dimensions, Holder<DimensionType> type, ChunkGenerator generator) {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        builder.putAll(dimensions);
        builder.put(LevelStem.OVERWORLD, (Object)new LevelStem(type, generator));
        return builder.buildKeepingLast();
    }

    public ChunkGenerator overworld() {
        LevelStem stem = this.dimensions.get(LevelStem.OVERWORLD);
        if (stem == null) {
            throw new IllegalStateException("Overworld settings missing");
        }
        return stem.generator();
    }

    public Optional<LevelStem> get(ResourceKey<LevelStem> key) {
        return Optional.ofNullable(this.dimensions.get(key));
    }

    public ImmutableSet<ResourceKey<Level>> levels() {
        return (ImmutableSet)this.dimensions().keySet().stream().map(Registries::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
    }

    public boolean isDebug() {
        return this.overworld() instanceof DebugLevelSource;
    }

    private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(Registry<LevelStem> registry) {
        return registry.getOptional(LevelStem.OVERWORLD).map(overworld -> {
            ChunkGenerator generator = overworld.generator();
            if (generator instanceof DebugLevelSource) {
                return PrimaryLevelData.SpecialWorldProperty.DEBUG;
            }
            if (generator instanceof FlatLevelSource) {
                return PrimaryLevelData.SpecialWorldProperty.FLAT;
            }
            return PrimaryLevelData.SpecialWorldProperty.NONE;
        }).orElse(PrimaryLevelData.SpecialWorldProperty.NONE);
    }

    private static Lifecycle checkStability(ResourceKey<LevelStem> key, LevelStem dimension) {
        return WorldDimensions.isVanillaLike(key, dimension) ? Lifecycle.stable() : Lifecycle.experimental();
    }

    private static boolean isVanillaLike(ResourceKey<LevelStem> key, LevelStem dimension) {
        if (key == LevelStem.OVERWORLD) {
            return WorldDimensions.isStableOverworld(dimension);
        }
        if (key == LevelStem.NETHER) {
            return WorldDimensions.isStableNether(dimension);
        }
        if (key == LevelStem.END) {
            return WorldDimensions.isStableEnd(dimension);
        }
        return false;
    }

    private static boolean isStableOverworld(LevelStem dimension) {
        MultiNoiseBiomeSource biomeSource;
        Holder<DimensionType> dimensionType = dimension.type();
        if (!dimensionType.is(BuiltinDimensionTypes.OVERWORLD) && !dimensionType.is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
            return false;
        }
        BiomeSource biomeSource2 = dimension.generator().getBiomeSource();
        return !(biomeSource2 instanceof MultiNoiseBiomeSource) || (biomeSource = (MultiNoiseBiomeSource)biomeSource2).stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
    }

    private static boolean isStableNether(LevelStem dimension) {
        MultiNoiseBiomeSource biomeSource;
        NoiseBasedChunkGenerator generator;
        Object object;
        return dimension.type().is(BuiltinDimensionTypes.NETHER) && (object = dimension.generator()) instanceof NoiseBasedChunkGenerator && (generator = (NoiseBasedChunkGenerator)object).stable(NoiseGeneratorSettings.NETHER) && (object = generator.getBiomeSource()) instanceof MultiNoiseBiomeSource && (biomeSource = (MultiNoiseBiomeSource)object).stable(MultiNoiseBiomeSourceParameterLists.NETHER);
    }

    private static boolean isStableEnd(LevelStem dimension) {
        NoiseBasedChunkGenerator generator;
        ChunkGenerator chunkGenerator;
        return dimension.type().is(BuiltinDimensionTypes.END) && (chunkGenerator = dimension.generator()) instanceof NoiseBasedChunkGenerator && (generator = (NoiseBasedChunkGenerator)chunkGenerator).stable(NoiseGeneratorSettings.END) && generator.getBiomeSource() instanceof TheEndBiomeSource;
    }

    public Complete bake(Registry<LevelStem> baseDimensions) {
        record Entry(ResourceKey<LevelStem> key, LevelStem value) {
            private RegistrationInfo registrationInfo() {
                return new RegistrationInfo(Optional.empty(), WorldDimensions.checkStability(this.key, this.value));
            }
        }
        Sets.SetView knownDimensions = Sets.union(baseDimensions.registryKeySet(), this.dimensions.keySet());
        ArrayList results = new ArrayList();
        WorldDimensions.keysInOrder((Set<ResourceKey<LevelStem>>)knownDimensions).forEach(key -> baseDimensions.getOptional((ResourceKey<LevelStem>)key).or(() -> Optional.ofNullable(this.dimensions.get(key))).ifPresent(levelStem -> results.add(new Entry((ResourceKey<LevelStem>)key, (LevelStem)levelStem))));
        Lifecycle initialStability = knownDimensions.containsAll(BUILTIN_ORDER) ? Lifecycle.stable() : Lifecycle.experimental();
        MappedRegistry<LevelStem> writableDimensions = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, initialStability);
        results.forEach(entry -> writableDimensions.register(entry.key, entry.value, entry.registrationInfo()));
        Registry<LevelStem> newDimensions = writableDimensions.freeze();
        PrimaryLevelData.SpecialWorldProperty specialWorldProperty = WorldDimensions.specialWorldProperty(newDimensions);
        return new Complete(newDimensions.freeze(), specialWorldProperty);
    }

    public record Complete(Registry<LevelStem> dimensions, PrimaryLevelData.SpecialWorldProperty specialWorldProperty) {
        public Lifecycle lifecycle() {
            return this.dimensions.registryLifecycle();
        }

        public RegistryAccess.Frozen dimensionsRegistryAccess() {
            return new RegistryAccess.ImmutableRegistryAccess(List.of(this.dimensions)).freeze();
        }
    }
}

