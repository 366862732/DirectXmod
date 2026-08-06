/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.base.Suppliers
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public final class NoiseBasedChunkGenerator
extends ChunkGenerator {
    public static final MapCodec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource), (App)NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings)).apply((Applicative)i, i.stable(NoiseBasedChunkGenerator::new)));
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final Holder<NoiseGeneratorSettings> settings;
    private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

    public NoiseBasedChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.globalFluidPicker = Suppliers.memoize(() -> NoiseBasedChunkGenerator.createFluidPicker((NoiseGeneratorSettings)settings.value()));
    }

    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings settings) {
        Aquifer.FluidStatus lavaStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int seaLevel = settings.seaLevel();
        Aquifer.FluidStatus seaStatus = new Aquifer.FluidStatus(seaLevel, settings.defaultFluid());
        Aquifer.FluidStatus emptyStatus = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
        return (x, y, z) -> {
            if (SharedConstants.DEBUG_DISABLE_FLUID_GENERATION) {
                return emptyStatus;
            }
            if (y < Math.min(-54, seaLevel)) {
                return lavaStatus;
            }
            return seaStatus;
        };
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess protoChunk) {
        return CompletableFuture.supplyAsync(() -> {
            this.doCreateBiomes(blender, randomState, structureManager, protoChunk);
            return protoChunk;
        }, Util.backgroundExecutor().forName("init_biomes"));
    }

    private void doCreateBiomes(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess protoChunk) {
        NoiseChunk noiseChunk = protoChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk((ChunkAccess)chunk, structureManager, blender, randomState));
        BiomeResolver biomeResolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.biomeSource), protoChunk);
        protoChunk.fillBiomesFromNoise(biomeResolver, noiseChunk.cachedClimateSampler(randomState.router(), this.settings.value().spawnTarget()));
    }

    private NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager structureManager, Blender blender, RandomState randomState) {
        return NoiseChunk.forChunk(chunk, randomState, Beardifier.forStructuresInChunk(structureManager, chunk.getPos()), this.settings.value(), this.globalFluidPicker.get(), blender);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public Holder<NoiseGeneratorSettings> generatorSettings() {
        return this.settings;
    }

    public boolean stable(ResourceKey<NoiseGeneratorSettings> expectedPreset) {
        return this.settings.is(expectedPreset);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return this.iterateNoiseColumn(heightAccessor, randomState, x, z, null, type.isOpaque()).orElse(heightAccessor.getMinY());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        MutableObject result = new MutableObject();
        this.iterateNoiseColumn(heightAccessor, randomState, x, z, (MutableObject<NoiseColumn>)result, null);
        return (NoiseColumn)result.get();
    }

    @VisibleForTesting
    public double getInterpolatedNoiseValue(RandomState randomState, DensityFunction.FunctionContext context) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings();
        int cellWidth = noiseSettings.getCellWidth();
        int cellHeight = noiseSettings.getCellHeight();
        int minY = noiseSettings.minY();
        int blockX = context.blockX();
        int blockY = context.blockY();
        int blockZ = context.blockZ();
        if (blockY < minY || blockY >= minY + noiseSettings.height()) {
            return Double.NaN;
        }
        NoiseChunk noiseChunk = new NoiseChunk(1, randomState, blockX - Math.floorMod(blockX, cellWidth), blockZ - Math.floorMod(blockZ, cellWidth), noiseSettings, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty());
        noiseChunk.initializeForFirstCellX();
        noiseChunk.advanceCellX(0);
        noiseChunk.selectCellYZ(Math.floorDiv(blockY - minY, cellHeight), 0);
        noiseChunk.updateForY(blockY, (double)Math.floorMod(blockY - minY, cellHeight) / (double)cellHeight);
        noiseChunk.updateForX(blockX, (double)Math.floorMod(blockX, cellWidth) / (double)cellWidth);
        noiseChunk.updateForZ(blockZ, (double)Math.floorMod(blockZ, cellWidth) / (double)cellWidth);
        return noiseChunk.getInterpolatedDensity();
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        DecimalFormat format = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.ROOT));
        NoiseRouter router = randomState.router();
        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(feetPos.getX(), feetPos.getY(), feetPos.getZ());
        double weirdness = router.ridges().compute(context);
        result.add("NoiseRouter N: " + format.format(this.getInterpolatedNoiseValue(randomState, context)) + " T: " + format.format(router.temperature().compute(context)) + " V: " + format.format(router.vegetation().compute(context)) + " C: " + format.format(router.continents().compute(context)) + " E: " + format.format(router.erosion().compute(context)) + " D: " + format.format(router.depth().compute(context)) + " W: " + format.format(weirdness) + " PV: " + format.format(NoiseRouterData.peaksAndValleys((float)weirdness)) + " PS: " + format.format(router.preliminarySurfaceLevel().compute(context)));
    }

    private OptionalInt iterateNoiseColumn(LevelHeightAccessor heightAccessor, RandomState randomState, int blockX, int blockZ, @Nullable MutableObject<NoiseColumn> columnReference, @Nullable Predicate<BlockState> tester) {
        BlockState[] writeTo;
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(heightAccessor);
        int cellHeight = noiseSettings.getCellHeight();
        int minY = noiseSettings.minY();
        int cellMinY = Mth.floorDiv(minY, cellHeight);
        int cellCountY = Mth.floorDiv(noiseSettings.height(), cellHeight);
        if (cellCountY <= 0) {
            return OptionalInt.empty();
        }
        if (columnReference == null) {
            writeTo = null;
        } else {
            writeTo = new BlockState[noiseSettings.height()];
            columnReference.setValue((Object)new NoiseColumn(minY, writeTo));
        }
        int cellWidth = noiseSettings.getCellWidth();
        int noiseChunkX = Math.floorDiv(blockX, cellWidth);
        int noiseChunkZ = Math.floorDiv(blockZ, cellWidth);
        int xInCell = Math.floorMod(blockX, cellWidth);
        int zInCell = Math.floorMod(blockZ, cellWidth);
        int firstBlockX = noiseChunkX * cellWidth;
        int firstBlockZ = noiseChunkZ * cellWidth;
        double factorX = (double)xInCell / (double)cellWidth;
        double factorZ = (double)zInCell / (double)cellWidth;
        NoiseChunk noiseChunk = new NoiseChunk(1, randomState, firstBlockX, firstBlockZ, noiseSettings, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty());
        noiseChunk.initializeForFirstCellX();
        noiseChunk.advanceCellX(0);
        for (int cellYIndex = cellCountY - 1; cellYIndex >= 0; --cellYIndex) {
            noiseChunk.selectCellYZ(cellYIndex, 0);
            for (int yInCell = cellHeight - 1; yInCell >= 0; --yInCell) {
                BlockState state;
                int posY = (cellMinY + cellYIndex) * cellHeight + yInCell;
                double factorY = (double)yInCell / (double)cellHeight;
                noiseChunk.updateForY(posY, factorY);
                noiseChunk.updateForX(blockX, factorX);
                noiseChunk.updateForZ(blockZ, factorZ);
                BlockState baseState = noiseChunk.getInterpolatedState();
                BlockState blockState = state = baseState == null ? this.settings.value().defaultBlock() : baseState;
                if (writeTo != null) {
                    int yIndex = cellYIndex * cellHeight + yInCell;
                    writeTo[yIndex] = state;
                }
                if (tester == null || !tester.test(state)) continue;
                noiseChunk.stopInterpolation();
                return OptionalInt.of(posY + 1);
            }
        }
        noiseChunk.stopInterpolation();
        return OptionalInt.empty();
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess protoChunk) {
        if (SharedConstants.debugVoidTerrain(protoChunk.getPos()) || SharedConstants.DEBUG_DISABLE_SURFACE) {
            return;
        }
        WorldGenerationContext context = new WorldGenerationContext(this, region);
        Set<Holder<Biome>> possibleBiomes = NoiseBasedChunkGenerator.collectPossibleBiomes(region, 1);
        this.buildSurface(protoChunk, context, randomState, structureManager, region.getBiomeManager(), Blender.of(region), possibleBiomes);
    }

    private static Set<Holder<Biome>> collectPossibleBiomes(WorldGenRegion region, int chunkRadius) {
        ReferenceOpenHashSet chunkBiomes = new ReferenceOpenHashSet();
        ChunkPos center = region.getCenter();
        for (int z = center.z() - chunkRadius; z <= center.z() + chunkRadius; ++z) {
            for (int x = center.x() - chunkRadius; x <= center.x() + chunkRadius; ++x) {
                region.getChunk(x, z).collectBiomesInPalette((Set<Holder<Biome>>)chunkBiomes);
            }
        }
        return chunkBiomes;
    }

    @VisibleForTesting
    public void buildSurface(ChunkAccess protoChunk, WorldGenerationContext context, RandomState randomState, StructureManager structureManager, BiomeManager biomeManager, Blender blender, @Nullable Set<Holder<Biome>> possibleBiomes) {
        NoiseChunk noiseChunk = protoChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk((ChunkAccess)chunk, structureManager, blender, randomState));
        NoiseGeneratorSettings settings = this.settings.value();
        randomState.surfaceSystem().buildSurface(randomState, biomeManager, settings.useLegacyRandomSource(), context, protoChunk, noiseChunk, settings.surfaceRule(), possibleBiomes);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        if (SharedConstants.DEBUG_DISABLE_CARVERS) {
            return;
        }
        BiomeManager correctBiomeManager = biomeManager.withDifferentSource((quartX, quartY, quartZ) -> this.biomeSource.getNoiseBiome(quartX, quartY, quartZ, randomState.sampler()));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        int range = 8;
        ChunkPos pos = chunk.getPos();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(c -> this.createNoiseChunk((ChunkAccess)c, structureManager, Blender.of(region), randomState));
        Aquifer aquifer = noiseChunk.aquifer();
        CarvingContext context = new CarvingContext(this, region.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk, randomState, this.settings.value().surfaceRule());
        CarvingMask mask = ((ProtoChunk)chunk).getOrCreateCarvingMask();
        for (int dx = -8; dx <= 8; ++dx) {
            for (int dz = -8; dz <= 8; ++dz) {
                ChunkPos sourcePos = new ChunkPos(pos.x() + dx, pos.z() + dz);
                ChunkAccess carverCenterChunk = region.getChunk(sourcePos.x(), sourcePos.z());
                BiomeGenerationSettings sourceBiomeGenerationSettings = carverCenterChunk.carverBiome(() -> this.getBiomeGenerationSettings(this.biomeSource.getNoiseBiome(QuartPos.fromBlock(sourcePos.getMinBlockX()), 0, QuartPos.fromBlock(sourcePos.getMinBlockZ()), randomState.sampler())));
                Iterable<Holder<ConfiguredWorldCarver<?>>> carvers = sourceBiomeGenerationSettings.getCarvers();
                int index = 0;
                for (Holder<ConfiguredWorldCarver<?>> carverHolder : carvers) {
                    ConfiguredWorldCarver<?> carver = carverHolder.value();
                    random.setLargeFeatureSeed(seed + (long)index, sourcePos.x(), sourcePos.z());
                    if (carver.isStartChunk(random)) {
                        carver.carve(context, chunk, correctBiomeManager::getBiome, random, aquifer, sourcePos, mask);
                    }
                    ++index;
                }
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess centerChunk) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(centerChunk.getHeightAccessorForGeneration());
        int minY = noiseSettings.minY();
        int cellYMin = Mth.floorDiv(minY, noiseSettings.getCellHeight());
        int cellCountY = Mth.floorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        if (cellCountY <= 0) {
            return CompletableFuture.completedFuture(centerChunk);
        }
        return CompletableFuture.supplyAsync(() -> {
            int topSectionIndex = centerChunk.getSectionIndex(cellCountY * noiseSettings.getCellHeight() - 1 + minY);
            int bottomSectionIndex = centerChunk.getSectionIndex(minY);
            HashSet sections = Sets.newHashSet();
            for (int sectionIndex = topSectionIndex; sectionIndex >= bottomSectionIndex; --sectionIndex) {
                LevelChunkSection section = centerChunk.getSection(sectionIndex);
                section.acquire();
                sections.add(section);
            }
            try {
                ChunkAccess chunkAccess = this.doFill(blender, structureManager, randomState, centerChunk, cellYMin, cellCountY);
                return chunkAccess;
            }
            finally {
                for (LevelChunkSection section : sections) {
                    section.release();
                }
            }
        }, Util.backgroundExecutor().forName("wgen_fill_noise"));
    }

    private ChunkAccess doFill(Blender blender, StructureManager structureManager, RandomState randomState, ChunkAccess centerChunk, int cellMinY, int cellCountY) {
        NoiseChunk noiseChunk = centerChunk.getOrCreateNoiseChunk(chunk -> this.createNoiseChunk((ChunkAccess)chunk, structureManager, blender, randomState));
        Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = centerChunk.getPos();
        int chunkStartBlockX = chunkPos.getMinBlockX();
        int chunkStartBlockZ = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int cellWidth = noiseChunk.cellWidth();
        int cellHeight = noiseChunk.cellHeight();
        int cellCountX = 16 / cellWidth;
        int cellCountZ = 16 / cellWidth;
        for (int cellXIndex = 0; cellXIndex < cellCountX; ++cellXIndex) {
            noiseChunk.advanceCellX(cellXIndex);
            for (int cellZIndex = 0; cellZIndex < cellCountZ; ++cellZIndex) {
                int lastSectionIndex = centerChunk.getSectionsCount() - 1;
                LevelChunkSection section = centerChunk.getSection(lastSectionIndex);
                for (int cellYIndex = cellCountY - 1; cellYIndex >= 0; --cellYIndex) {
                    noiseChunk.selectCellYZ(cellYIndex, cellZIndex);
                    for (int yInCell = cellHeight - 1; yInCell >= 0; --yInCell) {
                        int posY = (cellMinY + cellYIndex) * cellHeight + yInCell;
                        int yInSection = posY & 0xF;
                        int sectionIndex = centerChunk.getSectionIndex(posY);
                        if (lastSectionIndex != sectionIndex) {
                            lastSectionIndex = sectionIndex;
                            section = centerChunk.getSection(sectionIndex);
                        }
                        double factorY = (double)yInCell / (double)cellHeight;
                        noiseChunk.updateForY(posY, factorY);
                        for (int xInCell = 0; xInCell < cellWidth; ++xInCell) {
                            int posX = chunkStartBlockX + cellXIndex * cellWidth + xInCell;
                            int xInSection = posX & 0xF;
                            double factorX = (double)xInCell / (double)cellWidth;
                            noiseChunk.updateForX(posX, factorX);
                            for (int zInCell = 0; zInCell < cellWidth; ++zInCell) {
                                int posZ = chunkStartBlockZ + cellZIndex * cellWidth + zInCell;
                                int zInSection = posZ & 0xF;
                                double factorZ = (double)zInCell / (double)cellWidth;
                                noiseChunk.updateForZ(posZ, factorZ);
                                BlockState state = noiseChunk.getInterpolatedState();
                                if (state == null) {
                                    state = this.settings.value().defaultBlock();
                                }
                                if ((state = this.debugPreliminarySurfaceLevel(noiseChunk, posX, posY, posZ, state)) == AIR || SharedConstants.debugVoidTerrain(centerChunk.getPos())) continue;
                                section.setBlockState(xInSection, yInSection, zInSection, state, false);
                                oceanFloor.update(xInSection, posY, zInSection, state);
                                worldSurface.update(xInSection, posY, zInSection, state);
                                if (!aquifer.shouldScheduleFluidUpdate() || state.getFluidState().isEmpty()) continue;
                                blockPos.set(posX, posY, posZ);
                                centerChunk.markPosForPostProcessing(blockPos);
                            }
                        }
                    }
                }
            }
            noiseChunk.swapSlices();
        }
        noiseChunk.stopInterpolation();
        return centerChunk;
    }

    private BlockState debugPreliminarySurfaceLevel(NoiseChunk noiseChunk, int posX, int posY, int posZ, BlockState state) {
        int preliminarySurfaceLevel;
        int adjustedSurfaceLevel;
        if (SharedConstants.DEBUG_AQUIFERS && posZ >= 0 && posZ % 4 == 0 && posY == (adjustedSurfaceLevel = (preliminarySurfaceLevel = noiseChunk.preliminarySurfaceLevel(posX, posZ)) + 8)) {
            state = adjustedSurfaceLevel < this.getSeaLevel() ? Blocks.SLIME_BLOCK.defaultBlockState() : Blocks.HONEY_BLOCK.defaultBlockState();
        }
        return state;
    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.value().noiseSettings().minY();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        if (this.settings.value().disableMobGeneration()) {
            return;
        }
        ChunkPos center = worldGenRegion.getCenter();
        Holder<Biome> biome = worldGenRegion.getBiome(center.getWorldPosition().atY(worldGenRegion.getMaxY()));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(worldGenRegion.getSeed(), center.getMinBlockX(), center.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(worldGenRegion, biome, center, random);
    }
}

