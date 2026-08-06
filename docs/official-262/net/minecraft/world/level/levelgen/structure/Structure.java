/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.Keyable
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  com.mojang.serialization.codecs.RecordCodecBuilder$Instance
 */
package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public abstract class Structure {
    public static final Codec<Structure> DIRECT_CODEC = BuiltInRegistries.STRUCTURE_TYPE.byNameCodec().dispatch(Structure::type, StructureType::codec);
    public static final Codec<Holder<Structure>> CODEC = RegistryFileCodec.create(Registries.STRUCTURE, DIRECT_CODEC);
    protected final StructureSettings settings;

    public static <S extends Structure> RecordCodecBuilder<S, StructureSettings> settingsCodec(RecordCodecBuilder.Instance<S> i) {
        return StructureSettings.CODEC.forGetter(e -> e.settings);
    }

    public static <S extends Structure> MapCodec<S> simpleCodec(Function<StructureSettings, S> constructor) {
        return RecordCodecBuilder.mapCodec(i -> i.group(Structure.settingsCodec(i)).apply((Applicative)i, constructor));
    }

    protected Structure(StructureSettings settings) {
        this.settings = settings;
    }

    public HolderSet<Biome> biomes() {
        return this.settings.biomes;
    }

    public Map<MobCategory, StructureSpawnOverride> spawnOverrides() {
        return this.settings.spawnOverrides;
    }

    public GenerationStep.Decoration step() {
        return this.settings.step;
    }

    public TerrainAdjustment terrainAdaptation() {
        return this.settings.terrainAdaptation;
    }

    public BoundingBox adjustBoundingBox(BoundingBox boundingBox) {
        if (this.terrainAdaptation() != TerrainAdjustment.NONE) {
            return boundingBox.inflatedBy(12);
        }
        return boundingBox;
    }

    public StructureStart generate(Holder<Structure> selected, ResourceKey<Level> dimension, RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkPos sourceChunkPos, int references, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome) {
        StructurePiecesBuilder builder;
        StructureStart testStart;
        ProfiledDuration profiled = JvmProfiler.INSTANCE.onStructureGenerate(sourceChunkPos, dimension, selected);
        GenerationContext context = new GenerationContext(registryAccess, chunkGenerator, biomeSource, randomState, structureTemplateManager, seed, sourceChunkPos, heightAccessor, validBiome);
        Optional<GenerationStub> generation = this.findValidGenerationPoint(context);
        if (generation.isPresent() && (testStart = new StructureStart(this, sourceChunkPos, references, (builder = generation.get().getPiecesBuilder()).build())).isValid()) {
            if (profiled != null) {
                profiled.finish(true);
            }
            return testStart;
        }
        if (profiled != null) {
            profiled.finish(false);
        }
        return StructureStart.INVALID_START;
    }

    protected static Optional<GenerationStub> onTopOfChunkCenter(GenerationContext context, Heightmap.Types heightmap, Consumer<StructurePiecesBuilder> generator) {
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMiddleBlockX();
        int blockZ = chunkPos.getMiddleBlockZ();
        int blockY = context.chunkGenerator().getFirstOccupiedHeight(blockX, blockZ, heightmap, context.heightAccessor(), context.randomState());
        return Optional.of(new GenerationStub(new BlockPos(blockX, blockY, blockZ), generator));
    }

    private static boolean isValidBiome(GenerationStub stub, GenerationContext context) {
        BlockPos startPos = stub.position();
        return context.validBiome.test(context.chunkGenerator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(startPos.getX()), QuartPos.fromBlock(startPos.getY()), QuartPos.fromBlock(startPos.getZ()), context.randomState.sampler()));
    }

    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces) {
    }

    private static int[] getCornerHeights(GenerationContext context, int minX, int sizeX, int minZ, int sizeZ) {
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        return new int[]{chunkGenerator.getFirstOccupiedHeight(minX, minZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState), chunkGenerator.getFirstOccupiedHeight(minX, minZ + sizeZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState), chunkGenerator.getFirstOccupiedHeight(minX + sizeX, minZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState), chunkGenerator.getFirstOccupiedHeight(minX + sizeX, minZ + sizeZ, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState)};
    }

    public static int getMeanFirstOccupiedHeight(GenerationContext context, int minX, int sizeX, int minZ, int sizeZ) {
        int[] cornerHeights = Structure.getCornerHeights(context, minX, sizeX, minZ, sizeZ);
        return (cornerHeights[0] + cornerHeights[1] + cornerHeights[2] + cornerHeights[3]) / 4;
    }

    protected static int getLowestY(GenerationContext context, int sizeX, int sizeZ) {
        ChunkPos chunkPos = context.chunkPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        return Structure.getLowestY(context, minX, minZ, sizeX, sizeZ);
    }

    protected static int getLowestY(GenerationContext context, int minX, int minZ, int sizeX, int sizeZ) {
        int[] cornerHeights = Structure.getCornerHeights(context, minX, sizeX, minZ, sizeZ);
        return Math.min(Math.min(cornerHeights[0], cornerHeights[1]), Math.min(cornerHeights[2], cornerHeights[3]));
    }

    @Deprecated
    protected BlockPos getLowestYIn5by5BoxOffset7Blocks(GenerationContext context, Rotation rotation) {
        int offsetX = 5;
        int offsetZ = 5;
        if (rotation == Rotation.CLOCKWISE_90) {
            offsetX = -5;
        } else if (rotation == Rotation.CLOCKWISE_180) {
            offsetX = -5;
            offsetZ = -5;
        } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
            offsetZ = -5;
        }
        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getBlockX(7);
        int blockZ = chunkPos.getBlockZ(7);
        return new BlockPos(blockX, Structure.getLowestY(context, blockX, blockZ, offsetX, offsetZ), blockZ);
    }

    protected abstract Optional<GenerationStub> findGenerationPoint(GenerationContext var1);

    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        return this.findGenerationPoint(context).filter(generation -> Structure.isValidBiome(generation, context));
    }

    public abstract StructureType<?> type();

    public record StructureSettings(HolderSet<Biome> biomes, Map<MobCategory, StructureSpawnOverride> spawnOverrides, GenerationStep.Decoration step, TerrainAdjustment terrainAdaptation) {
        private static final StructureSettings DEFAULT = new StructureSettings(HolderSet.empty(), Map.of(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
        public static final MapCodec<StructureSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(StructureSettings::biomes), (App)Codec.simpleMap(MobCategory.CODEC, StructureSpawnOverride.CODEC, (Keyable)StringRepresentable.keys(MobCategory.values())).fieldOf("spawn_overrides").forGetter(StructureSettings::spawnOverrides), (App)GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(StructureSettings::step), (App)TerrainAdjustment.CODEC.optionalFieldOf("terrain_adaptation", (Object)StructureSettings.DEFAULT.terrainAdaptation).forGetter(StructureSettings::terrainAdaptation)).apply((Applicative)i, StructureSettings::new));

        public StructureSettings(HolderSet<Biome> biomes) {
            this(biomes, StructureSettings.DEFAULT.spawnOverrides, StructureSettings.DEFAULT.step, StructureSettings.DEFAULT.terrainAdaptation);
        }

        public static class Builder {
            private final HolderSet<Biome> biomes;
            private Map<MobCategory, StructureSpawnOverride> spawnOverrides;
            private GenerationStep.Decoration step;
            private TerrainAdjustment terrainAdaption;

            public Builder(HolderSet<Biome> biomes) {
                this.spawnOverrides = StructureSettings.DEFAULT.spawnOverrides;
                this.step = StructureSettings.DEFAULT.step;
                this.terrainAdaption = StructureSettings.DEFAULT.terrainAdaptation;
                this.biomes = biomes;
            }

            public Builder spawnOverrides(Map<MobCategory, StructureSpawnOverride> spawnOverrides) {
                this.spawnOverrides = spawnOverrides;
                return this;
            }

            public Builder generationStep(GenerationStep.Decoration step) {
                this.step = step;
                return this;
            }

            public Builder terrainAdapation(TerrainAdjustment terrainAdaption) {
                this.terrainAdaption = terrainAdaption;
                return this;
            }

            public StructureSettings build() {
                return new StructureSettings(this.biomes, this.spawnOverrides, this.step, this.terrainAdaption);
            }
        }
    }

    public record GenerationContext(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, WorldgenRandom random, long seed, ChunkPos chunkPos, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome) {
        public GenerationContext(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, BiomeSource biomeSource, RandomState randomState, StructureTemplateManager structureTemplateManager, long seed, ChunkPos chunkPos, LevelHeightAccessor heightAccessor, Predicate<Holder<Biome>> validBiome) {
            this(registryAccess, chunkGenerator, biomeSource, randomState, structureTemplateManager, GenerationContext.makeRandom(seed, chunkPos), seed, chunkPos, heightAccessor, validBiome);
        }

        private static WorldgenRandom makeRandom(long seed, ChunkPos chunkPos) {
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(seed, chunkPos.x(), chunkPos.z());
            return random;
        }
    }

    public record GenerationStub(BlockPos position, Either<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder> generator) {
        public GenerationStub(BlockPos position, Consumer<StructurePiecesBuilder> generator) {
            this(position, (Either<Consumer<StructurePiecesBuilder>, StructurePiecesBuilder>)Either.left(generator));
        }

        public StructurePiecesBuilder getPiecesBuilder() {
            return (StructurePiecesBuilder)this.generator.map(pieceGenerator -> {
                StructurePiecesBuilder newBuilder = new StructurePiecesBuilder();
                pieceGenerator.accept(newBuilder);
                return newBuilder;
            }, previousBuilder -> previousBuilder);
        }
    }
}

