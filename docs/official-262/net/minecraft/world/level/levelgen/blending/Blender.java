/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableDouble
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class Blender {
    private static final Blender EMPTY = new Blender(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()){

        @Override
        public BlendingOutput blendOffsetAndFactor(int blockX, int blockZ) {
            return new BlendingOutput(1.0, 0.0);
        }

        @Override
        public double blendDensity(DensityFunction.FunctionContext context, double noiseValue) {
            return noiseValue;
        }

        @Override
        public BiomeResolver getBiomeResolver(BiomeResolver biomeResolver) {
            return biomeResolver;
        }
    };
    private static final NormalNoise SHIFT_NOISE = NormalNoise.create(new XoroshiroRandomSource(42L), NoiseData.DEFAULT_SHIFT);
    private static final int HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
    private static final int HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
    private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
    private static final int DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
    private static final double OLD_CHUNK_XZ_RADIUS = 8.0;
    private final Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData;
    private final Long2ObjectOpenHashMap<BlendingData> densityBlendingData;

    public static Blender empty() {
        return EMPTY;
    }

    public static Blender of(@Nullable WorldGenRegion region) {
        if (SharedConstants.DEBUG_DISABLE_BLENDING || region == null) {
            return EMPTY;
        }
        ChunkPos centerPos = region.getCenter();
        if (!region.isOldChunkAround(centerPos, HEIGHT_BLENDING_RANGE_CHUNKS)) {
            return EMPTY;
        }
        Long2ObjectOpenHashMap heightAndBiomeData = new Long2ObjectOpenHashMap();
        Long2ObjectOpenHashMap densityData = new Long2ObjectOpenHashMap();
        int maxDistSq = Mth.square(HEIGHT_BLENDING_RANGE_CHUNKS + 1);
        for (int dx = -HEIGHT_BLENDING_RANGE_CHUNKS; dx <= HEIGHT_BLENDING_RANGE_CHUNKS; ++dx) {
            for (int dz = -HEIGHT_BLENDING_RANGE_CHUNKS; dz <= HEIGHT_BLENDING_RANGE_CHUNKS; ++dz) {
                int chunkZ;
                int chunkX;
                BlendingData blendingData;
                if (dx * dx + dz * dz > maxDistSq || (blendingData = BlendingData.getOrUpdateBlendingData(region, chunkX = centerPos.x() + dx, chunkZ = centerPos.z() + dz)) == null) continue;
                heightAndBiomeData.put(ChunkPos.pack(chunkX, chunkZ), (Object)blendingData);
                if (dx < -DENSITY_BLENDING_RANGE_CHUNKS || dx > DENSITY_BLENDING_RANGE_CHUNKS || dz < -DENSITY_BLENDING_RANGE_CHUNKS || dz > DENSITY_BLENDING_RANGE_CHUNKS) continue;
                densityData.put(ChunkPos.pack(chunkX, chunkZ), (Object)blendingData);
            }
        }
        if (heightAndBiomeData.isEmpty() && densityData.isEmpty()) {
            return EMPTY;
        }
        return new Blender((Long2ObjectOpenHashMap<BlendingData>)heightAndBiomeData, (Long2ObjectOpenHashMap<BlendingData>)densityData);
    }

    private Blender(Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData, Long2ObjectOpenHashMap<BlendingData> densityBlendingData) {
        this.heightAndBiomeBlendingData = heightAndBiomeBlendingData;
        this.densityBlendingData = densityBlendingData;
    }

    public boolean isEmpty() {
        return this.heightAndBiomeBlendingData.isEmpty() && this.densityBlendingData.isEmpty();
    }

    public BlendingOutput blendOffsetAndFactor(int blockX, int blockZ) {
        int cellZ;
        int cellX = QuartPos.fromBlock(blockX);
        double fixedHeight = this.getBlendingDataValue(cellX, 0, cellZ = QuartPos.fromBlock(blockZ), BlendingData::getHeight);
        if (fixedHeight != Double.MAX_VALUE) {
            return new BlendingOutput(0.0, Blender.heightToOffset(fixedHeight));
        }
        MutableDouble totalWeight = new MutableDouble(0.0);
        MutableDouble weightedHeights = new MutableDouble(0.0);
        MutableDouble closestDistance = new MutableDouble(Double.POSITIVE_INFINITY);
        this.heightAndBiomeBlendingData.forEach((chunkPos, blendingData) -> blendingData.iterateHeights(QuartPos.fromSection(ChunkPos.getX(chunkPos)), QuartPos.fromSection(ChunkPos.getZ(chunkPos)), (testCellX, testCellZ, height) -> {
            double distance = Mth.length(cellX - testCellX, cellZ - testCellZ);
            if (distance > (double)HEIGHT_BLENDING_RANGE_CELLS) {
                return;
            }
            if (distance < closestDistance.doubleValue()) {
                closestDistance.setValue(distance);
            }
            double weight = 1.0 / (distance * distance * distance * distance);
            weightedHeights.add(height * weight);
            totalWeight.add(weight);
        }));
        if (closestDistance.doubleValue() == Double.POSITIVE_INFINITY) {
            return new BlendingOutput(1.0, 0.0);
        }
        double averageHeight = weightedHeights.doubleValue() / totalWeight.doubleValue();
        double alpha = Mth.clamp(closestDistance.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
        alpha = 3.0 * alpha * alpha - 2.0 * alpha * alpha * alpha;
        return new BlendingOutput(alpha, Blender.heightToOffset(averageHeight));
    }

    private static double heightToOffset(double height) {
        double dimensionFactor = 1.0;
        double targetY = height + 0.5;
        double targetYMod = Mth.positiveModulo(targetY, 8.0);
        return 1.0 * (32.0 * (targetY - 128.0) - 3.0 * (targetY - 120.0) * targetYMod + 3.0 * targetYMod * targetYMod) / (128.0 * (32.0 - 3.0 * targetYMod));
    }

    public double blendDensity(DensityFunction.FunctionContext context, double noiseValue) {
        int cellZ;
        int cellY;
        int cellX = QuartPos.fromBlock(context.blockX());
        double fixedDensity = this.getBlendingDataValue(cellX, cellY = context.blockY() / 8, cellZ = QuartPos.fromBlock(context.blockZ()), BlendingData::getDensity);
        if (fixedDensity != Double.MAX_VALUE) {
            return fixedDensity;
        }
        MutableDouble totalWeight = new MutableDouble(0.0);
        MutableDouble weightedHeights = new MutableDouble(0.0);
        MutableDouble closestDistance = new MutableDouble(Double.POSITIVE_INFINITY);
        this.densityBlendingData.forEach((chunkPos, blendingData) -> blendingData.iterateDensities(QuartPos.fromSection(ChunkPos.getX(chunkPos)), QuartPos.fromSection(ChunkPos.getZ(chunkPos)), cellY - 1, cellY + 1, (testCellX, testCellY, testCellZ, density) -> {
            double distance = Mth.length(cellX - testCellX, (cellY - testCellY) * 2, cellZ - testCellZ);
            if (distance > 2.0) {
                return;
            }
            if (distance < closestDistance.doubleValue()) {
                closestDistance.setValue(distance);
            }
            double weight = 1.0 / (distance * distance * distance * distance);
            weightedHeights.add(density * weight);
            totalWeight.add(weight);
        }));
        if (closestDistance.doubleValue() == Double.POSITIVE_INFINITY) {
            return noiseValue;
        }
        double averageDensity = weightedHeights.doubleValue() / totalWeight.doubleValue();
        double alpha = Mth.clamp(closestDistance.doubleValue() / 3.0, 0.0, 1.0);
        return Mth.lerp(alpha, averageDensity, noiseValue);
    }

    private double getBlendingDataValue(int cellX, int cellY, int cellZ, CellValueGetter cellValueGetter) {
        int chunkX = QuartPos.toSection(cellX);
        int chunkZ = QuartPos.toSection(cellZ);
        boolean minX = (cellX & 3) == 0;
        boolean minZ = (cellZ & 3) == 0;
        double value = this.getBlendingDataValue(cellValueGetter, chunkX, chunkZ, cellX, cellY, cellZ);
        if (value == Double.MAX_VALUE) {
            if (minX && minZ) {
                value = this.getBlendingDataValue(cellValueGetter, chunkX - 1, chunkZ - 1, cellX, cellY, cellZ);
            }
            if (value == Double.MAX_VALUE) {
                if (minX) {
                    value = this.getBlendingDataValue(cellValueGetter, chunkX - 1, chunkZ, cellX, cellY, cellZ);
                }
                if (value == Double.MAX_VALUE && minZ) {
                    value = this.getBlendingDataValue(cellValueGetter, chunkX, chunkZ - 1, cellX, cellY, cellZ);
                }
            }
        }
        return value;
    }

    private double getBlendingDataValue(CellValueGetter cellValueGetter, int chunkX, int chunkZ, int cellX, int cellY, int cellZ) {
        BlendingData blendingData = (BlendingData)this.heightAndBiomeBlendingData.get(ChunkPos.pack(chunkX, chunkZ));
        if (blendingData != null) {
            return cellValueGetter.get(blendingData, cellX - QuartPos.fromSection(chunkX), cellY, cellZ - QuartPos.fromSection(chunkZ));
        }
        return Double.MAX_VALUE;
    }

    public BiomeResolver getBiomeResolver(BiomeResolver biomeResolver) {
        return (quartX, quartY, quartZ, sampler) -> {
            Holder<Biome> biome = this.blendBiome(quartX, quartY, quartZ);
            if (biome == null) {
                return biomeResolver.getNoiseBiome(quartX, quartY, quartZ, sampler);
            }
            return biome;
        };
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    private @Nullable Holder<Biome> blendBiome(int quartX, int quartY, int quartZ) {
        MutableDouble closestDistance = new MutableDouble(Double.POSITIVE_INFINITY);
        @Nullable MutableObject closestBiome = new MutableObject();
        this.heightAndBiomeBlendingData.forEach((chunkPos, blendingData) -> blendingData.iterateBiomes(QuartPos.fromSection(ChunkPos.getX(chunkPos)), quartY, QuartPos.fromSection(ChunkPos.getZ(chunkPos)), (testCellX, testCellZ, biome) -> {
            double distance = Mth.length(quartX - testCellX, quartZ - testCellZ);
            if (distance > (double)HEIGHT_BLENDING_RANGE_CELLS) {
                return;
            }
            if (distance < closestDistance.doubleValue()) {
                closestBiome.setValue((Object)biome);
                closestDistance.setValue(distance);
            }
        }));
        if (closestDistance.doubleValue() == Double.POSITIVE_INFINITY) {
            return null;
        }
        double shiftNoise = SHIFT_NOISE.getValue(quartX, 0.0, quartZ) * 12.0;
        double alpha = Mth.clamp((closestDistance.doubleValue() + shiftNoise) / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0, 1.0);
        if (alpha > 0.5) {
            return null;
        }
        return (Holder)closestBiome.get();
    }

    public static void generateBorderTicks(WorldGenRegion region, ChunkAccess chunk) {
        if (SharedConstants.DEBUG_DISABLE_BLENDING) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        boolean oldNoiseGeneration = chunk.isOldNoiseGeneration();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos chunkOrigin = new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ());
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData == null) {
            return;
        }
        int oldMinY = blendingData.getAreaWithOldGeneration().getMinY();
        int oldMaxY = blendingData.getAreaWithOldGeneration().getMaxY();
        if (oldNoiseGeneration) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    Blender.generateBorderTick(chunk, pos.setWithOffset(chunkOrigin, x, oldMinY - 1, z));
                    Blender.generateBorderTick(chunk, pos.setWithOffset(chunkOrigin, x, oldMinY, z));
                    Blender.generateBorderTick(chunk, pos.setWithOffset(chunkOrigin, x, oldMaxY, z));
                    Blender.generateBorderTick(chunk, pos.setWithOffset(chunkOrigin, x, oldMaxY + 1, z));
                }
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (region.getChunk(chunkPos.x() + direction.getStepX(), chunkPos.z() + direction.getStepZ()).isOldNoiseGeneration() == oldNoiseGeneration) continue;
            int minX = direction == Direction.EAST ? 15 : 0;
            int maxX = direction == Direction.WEST ? 0 : 15;
            int minZ = direction == Direction.SOUTH ? 15 : 0;
            int maxZ = direction == Direction.NORTH ? 0 : 15;
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    int maxY = Math.min(oldMaxY, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)) + 1;
                    for (int y = oldMinY; y < maxY; ++y) {
                        Blender.generateBorderTick(chunk, pos.setWithOffset(chunkOrigin, x, y, z));
                    }
                }
            }
        }
    }

    private static void generateBorderTick(ChunkAccess chunk, BlockPos pos) {
        FluidState fluidState;
        BlockState blockState = chunk.getBlockState(pos);
        if (blockState.is(BlockTags.LEAVES)) {
            chunk.markPosForPostProcessing(pos);
        }
        if (!(fluidState = chunk.getFluidState(pos)).isEmpty()) {
            chunk.markPosForPostProcessing(pos);
        }
    }

    public static void addAroundOldChunksCarvingMaskFilter(WorldGenLevel region, ProtoChunk chunk) {
        if (SharedConstants.DEBUG_DISABLE_BLENDING) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (Direction8 direction8 : Direction8.values()) {
            int testChunkZ;
            int testChunkX = chunkPos.x() + direction8.getStepX();
            BlendingData blendingData = region.getChunk(testChunkX, testChunkZ = chunkPos.z() + direction8.getStepZ()).getBlendingData();
            if (blendingData == null) continue;
            builder.put((Object)direction8, (Object)blendingData);
        }
        ImmutableMap oldSidesBlendingData = builder.build();
        if (!chunk.isOldNoiseGeneration() && oldSidesBlendingData.isEmpty()) {
            return;
        }
        DistanceGetter distanceGetter = Blender.makeOldChunkDistanceGetter(chunk.getBlendingData(), (Map<Direction8, BlendingData>)oldSidesBlendingData);
        CarvingMask.Mask filter = (x, y, z) -> {
            double shiftedZ;
            double shiftedY;
            double shiftedX = (double)x + 0.5 + SHIFT_NOISE.getValue(x, y, z) * 4.0;
            return distanceGetter.getDistance(shiftedX, shiftedY = (double)y + 0.5 + SHIFT_NOISE.getValue(y, z, x) * 4.0, shiftedZ = (double)z + 0.5 + SHIFT_NOISE.getValue(z, x, y) * 4.0) < 4.0;
        };
        chunk.getOrCreateCarvingMask().setAdditionalMask(filter);
    }

    public static DistanceGetter makeOldChunkDistanceGetter(@Nullable BlendingData centerBlendingData, Map<Direction8, BlendingData> oldSidesBlendingData) {
        ArrayList distanceGetters = Lists.newArrayList();
        if (centerBlendingData != null) {
            distanceGetters.add(Blender.makeOffsetOldChunkDistanceGetter(null, centerBlendingData));
        }
        oldSidesBlendingData.forEach((side, blendingData) -> distanceGetters.add(Blender.makeOffsetOldChunkDistanceGetter(side, blendingData)));
        return (x, y, z) -> {
            double closest = Double.POSITIVE_INFINITY;
            for (DistanceGetter getter : distanceGetters) {
                double distance = getter.getDistance(x, y, z);
                if (!(distance < closest)) continue;
                closest = distance;
            }
            return closest;
        };
    }

    private static DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable Direction8 offset, BlendingData blendingData) {
        double offsetX = 0.0;
        double offsetZ = 0.0;
        if (offset != null) {
            for (Direction direction : offset.getDirections()) {
                offsetX += (double)(direction.getStepX() * 16);
                offsetZ += (double)(direction.getStepZ() * 16);
            }
        }
        double finalOffsetX = offsetX;
        double finalOffsetZ = offsetZ;
        double oldChunkYRadius = (double)blendingData.getAreaWithOldGeneration().getHeight() / 2.0;
        double oldChunkCenterY = (double)blendingData.getAreaWithOldGeneration().getMinY() + oldChunkYRadius;
        return (x, y, z) -> Blender.distanceToCube(x - 8.0 - finalOffsetX, y - oldChunkCenterY, z - 8.0 - finalOffsetZ, 8.0, oldChunkYRadius, 8.0);
    }

    private static double distanceToCube(double x, double y, double z, double radiusX, double radiusY, double radiusZ) {
        double deltaX = Math.abs(x) - radiusX;
        double deltaY = Math.abs(y) - radiusY;
        double deltaZ = Math.abs(z) - radiusZ;
        return Mth.length(Math.max(0.0, deltaX), Math.max(0.0, deltaY), Math.max(0.0, deltaZ));
    }

    private static interface CellValueGetter {
        public double get(BlendingData var1, int var2, int var3, int var4);
    }

    public record BlendingOutput(double alpha, double blendingOffset) {
    }

    public static interface DistanceGetter {
        public double getDistance(double var1, double var3, double var5);
    }
}

