/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class PathNavigationRegion
implements CollisionGetter {
    protected final int centerX;
    protected final int centerZ;
    protected final ChunkAccess[][] chunks;
    protected boolean allEmpty;
    protected final Level level;
    private final Supplier<Holder<Biome>> plains;

    public PathNavigationRegion(Level level, BlockPos start, BlockPos end) {
        int zc;
        int xc;
        this.level = level;
        this.plains = Suppliers.memoize(() -> level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
        this.centerX = SectionPos.blockToSectionCoord(start.getX());
        this.centerZ = SectionPos.blockToSectionCoord(start.getZ());
        int xc2 = SectionPos.blockToSectionCoord(end.getX());
        int zc2 = SectionPos.blockToSectionCoord(end.getZ());
        this.chunks = new ChunkAccess[xc2 - this.centerX + 1][zc2 - this.centerZ + 1];
        ChunkSource chunkSource = level.getChunkSource();
        this.allEmpty = true;
        for (xc = this.centerX; xc <= xc2; ++xc) {
            for (zc = this.centerZ; zc <= zc2; ++zc) {
                this.chunks[xc - this.centerX][zc - this.centerZ] = chunkSource.getChunkNow(xc, zc);
            }
        }
        for (xc = SectionPos.blockToSectionCoord(start.getX()); xc <= SectionPos.blockToSectionCoord(end.getX()); ++xc) {
            for (zc = SectionPos.blockToSectionCoord(start.getZ()); zc <= SectionPos.blockToSectionCoord(end.getZ()); ++zc) {
                ChunkAccess chunk = this.chunks[xc - this.centerX][zc - this.centerZ];
                if (chunk == null || chunk.isYSpaceEmpty(start.getY(), end.getY())) continue;
                this.allEmpty = false;
                return;
            }
        }
    }

    private ChunkAccess getChunk(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private ChunkAccess getChunk(int chunkX, int chunkZ) {
        int xc = chunkX - this.centerX;
        int zc = chunkZ - this.centerZ;
        if (xc < 0 || xc >= this.chunks.length || zc < 0 || zc >= this.chunks[xc].length) {
            return new EmptyLevelChunk(this.level, new ChunkPos(chunkX, chunkZ), this.plains.get());
        }
        ChunkAccess chunk = this.chunks[xc][zc];
        return chunk != null ? chunk : new EmptyLevelChunk(this.level, new ChunkPos(chunkX, chunkZ), this.plains.get());
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        return List.of();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        ChunkAccess chunk = this.getChunk(pos);
        return chunk.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        ChunkAccess chunk = this.getChunk(pos);
        return chunk.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        ChunkAccess chunk = this.getChunk(pos);
        return chunk.getFluidState(pos);
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }
}

