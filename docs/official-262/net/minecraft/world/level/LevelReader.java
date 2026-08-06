/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface LevelReader
extends BlockAndLightGetter,
CollisionGetter,
SignalGetter,
BiomeManager.NoiseBiomeSource {
    public @Nullable ChunkAccess getChunk(int var1, int var2, ChunkStatus var3, boolean var4);

    @Deprecated
    public boolean hasChunk(int var1, int var2);

    public int getHeight(Heightmap.Types var1, int var2, int var3);

    default public int getHeight(Heightmap.Types type, BlockPos pos) {
        return this.getHeight(type, pos.getX(), pos.getZ());
    }

    public int getSkyDarken();

    public BiomeManager getBiomeManager();

    default public Holder<Biome> getBiome(BlockPos pos) {
        return this.getBiomeManager().getBiome(pos);
    }

    default public Stream<BlockState> getBlockStatesIfLoaded(AABB box) {
        int z1;
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.floor(box.maxX);
        int y0 = Mth.floor(box.minY);
        int y1 = Mth.floor(box.maxY);
        int z0 = Mth.floor(box.minZ);
        if (this.hasChunksAt(x0, y0, z0, x1, y1, z1 = Mth.floor(box.maxZ))) {
            return this.getBlockStates(box);
        }
        return Stream.empty();
    }

    @Override
    default public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        ChunkAccess chunk = this.getChunk(QuartPos.toSection(quartX), QuartPos.toSection(quartZ), ChunkStatus.BIOMES, false);
        if (chunk != null) {
            return chunk.getNoiseBiome(quartX, quartY, quartZ);
        }
        return this.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    public Holder<Biome> getUncachedNoiseBiome(int var1, int var2, int var3);

    public boolean isClientSide();

    public int getSeaLevel();

    public DimensionType dimensionType();

    @Override
    default public int getMinY() {
        return this.dimensionType().minY();
    }

    @Override
    default public int getHeight() {
        return this.dimensionType().height();
    }

    default public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) {
        return new BlockPos(pos.getX(), this.getHeight(type, pos.getX(), pos.getZ()), pos.getZ());
    }

    default public boolean isEmptyBlock(BlockPos pos) {
        return this.getBlockState(pos).isAir();
    }

    default public boolean canSeeSkyFromBelowWater(BlockPos pos) {
        if (pos.getY() >= this.getSeaLevel()) {
            return this.canSeeSky(pos);
        }
        BlockPos scanPoint = new BlockPos(pos.getX(), this.getSeaLevel(), pos.getZ());
        if (!this.canSeeSky(scanPoint)) {
            return false;
        }
        scanPoint = scanPoint.below();
        while (scanPoint.getY() > pos.getY()) {
            BlockState state = this.getBlockState(scanPoint);
            if (state.getLightDampening() > 0 && !state.liquid()) {
                return false;
            }
            scanPoint = scanPoint.below();
        }
        return true;
    }

    default public float getPathfindingCostFromLightLevels(BlockPos pos) {
        return this.getLightLevelDependentMagicValue(pos) - 0.5f;
    }

    @Deprecated
    default public float getLightLevelDependentMagicValue(BlockPos pos) {
        float v = (float)this.getMaxLocalRawBrightness(pos) / 15.0f;
        float curvedV = v / (4.0f - 3.0f * v);
        return Mth.lerp(this.dimensionType().ambientLight(), curvedV, 1.0f);
    }

    default public ChunkAccess getChunk(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    default public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
    }

    default public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus status) {
        return this.getChunk(chunkX, chunkZ, status, true);
    }

    @Override
    default public @Nullable BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
    }

    default public boolean isWaterAt(BlockPos pos) {
        return this.getFluidState(pos).is(FluidTags.WATER);
    }

    default public boolean containsAnyLiquid(AABB box) {
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.ceil(box.maxX);
        int y0 = Mth.floor(box.minY);
        int y1 = Mth.ceil(box.maxY);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.ceil(box.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x1; ++x) {
            for (int y = y0; y < y1; ++y) {
                for (int z = z0; z < z1; ++z) {
                    BlockState blockState = this.getBlockState(pos.set(x, y, z));
                    if (blockState.getFluidState().isEmpty()) continue;
                    return true;
                }
            }
        }
        return false;
    }

    default public int getMaxLocalRawBrightness(BlockPos pos) {
        return this.getMaxLocalRawBrightness(pos, this.getSkyDarken());
    }

    default public int getMaxLocalRawBrightness(BlockPos pos, int skyDarkening) {
        if (pos.getX() < -30000000 || pos.getZ() < -30000000 || pos.getX() >= 30000000 || pos.getZ() >= 30000000) {
            return 15;
        }
        return this.getRawBrightness(pos, skyDarkening);
    }

    default public int getEffectiveSkyBrightness(BlockPos pos) {
        return this.getBrightness(LightLayer.SKY, pos) - this.getSkyDarken();
    }

    @Deprecated
    default public boolean hasChunkAt(int blockX, int blockZ) {
        return this.hasChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
    }

    @Deprecated
    default public boolean hasChunkAt(BlockPos pos) {
        return this.hasChunkAt(pos.getX(), pos.getZ());
    }

    @Deprecated
    default public boolean hasChunksAt(BlockPos pos0, BlockPos pos1) {
        return this.hasChunksAt(pos0.getX(), pos0.getY(), pos0.getZ(), pos1.getX(), pos1.getY(), pos1.getZ());
    }

    @Deprecated
    default public boolean hasChunksAt(int x0, int y0, int z0, int x1, int y1, int z1) {
        if (y1 < this.getMinY() || y0 > this.getMaxY()) {
            return false;
        }
        return this.hasChunksAt(x0, z0, x1, z1);
    }

    @Deprecated
    default public boolean hasChunksAt(int x0, int z0, int x1, int z1) {
        int chunkX0 = SectionPos.blockToSectionCoord(x0);
        int chunkX1 = SectionPos.blockToSectionCoord(x1);
        int chunkZ0 = SectionPos.blockToSectionCoord(z0);
        int chunkZ1 = SectionPos.blockToSectionCoord(z1);
        for (int chunkX = chunkX0; chunkX <= chunkX1; ++chunkX) {
            for (int chunkZ = chunkZ0; chunkZ <= chunkZ1; ++chunkZ) {
                if (this.hasChunk(chunkX, chunkZ)) continue;
                return false;
            }
        }
        return true;
    }

    public RegistryAccess registryAccess();

    public FeatureFlagSet enabledFeatures();

    default public <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> key) {
        HolderLookup.RegistryLookup registry = this.registryAccess().lookupOrThrow((ResourceKey)key);
        return registry.filterFeatures(this.enabledFeatures());
    }

    public EnvironmentAttributeReader environmentAttributes();
}

