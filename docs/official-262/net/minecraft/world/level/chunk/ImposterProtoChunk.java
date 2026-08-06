/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;

public class ImposterProtoChunk
extends ProtoChunk {
    private final LevelChunk wrapped;
    private final boolean allowWrites;

    public ImposterProtoChunk(LevelChunk wrapped, boolean allowWrites) {
        super(wrapped.getPos(), UpgradeData.EMPTY, wrapped.levelHeightAccessor, wrapped.getLevel().palettedContainerFactory(), wrapped.getBlendingData());
        this.wrapped = wrapped;
        this.allowWrites = allowWrites;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.wrapped.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.wrapped.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.wrapped.getFluidState(pos);
    }

    @Override
    public LevelChunkSection getSection(int sectionIndex) {
        if (this.allowWrites) {
            return this.wrapped.getSection(sectionIndex);
        }
        return super.getSection(sectionIndex);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        if (this.allowWrites) {
            return this.wrapped.setBlockState(pos, state, flags);
        }
        return null;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        if (this.allowWrites) {
            this.wrapped.setBlockEntity(blockEntity);
        }
    }

    @Override
    public void addEntity(Entity entity) {
        if (this.allowWrites) {
            this.wrapped.addEntity(entity);
        }
    }

    @Override
    public void setPersistedStatus(ChunkStatus status) {
        if (this.allowWrites) {
            super.setPersistedStatus(status);
        }
    }

    @Override
    public LevelChunkSection[] getSections() {
        return this.wrapped.getSections();
    }

    @Override
    public void setHeightmap(Heightmap.Types key, long[] data) {
    }

    private Heightmap.Types fixType(Heightmap.Types type) {
        if (type == Heightmap.Types.WORLD_SURFACE_WG) {
            return Heightmap.Types.WORLD_SURFACE;
        }
        if (type == Heightmap.Types.OCEAN_FLOOR_WG) {
            return Heightmap.Types.OCEAN_FLOOR;
        }
        return type;
    }

    @Override
    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.wrapped.getOrCreateHeightmapUnprimed(type);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        return this.wrapped.getHeight(this.fixType(type), x, z);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return this.wrapped.getNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public ChunkPos getPos() {
        return this.wrapped.getPos();
    }

    @Override
    public @Nullable StructureStart getStartForStructure(Structure structure) {
        return this.wrapped.getStartForStructure(structure);
    }

    @Override
    public void setStartForStructure(Structure structure, StructureStart structureStart) {
    }

    @Override
    public Map<Structure, StructureStart> getAllStarts() {
        return this.wrapped.getAllStarts();
    }

    @Override
    public void setAllStarts(Map<Structure, StructureStart> starts) {
    }

    @Override
    public LongSet getReferencesForStructure(Structure structure) {
        return this.wrapped.getReferencesForStructure(structure);
    }

    @Override
    public void addReferenceForStructure(Structure structure, long reference) {
    }

    @Override
    public Map<Structure, LongSet> getAllReferences() {
        return this.wrapped.getAllReferences();
    }

    @Override
    public void setAllReferences(Map<Structure, LongSet> data) {
    }

    @Override
    public void markUnsaved() {
        this.wrapped.markUnsaved();
    }

    @Override
    public boolean canBeSerialized() {
        return false;
    }

    @Override
    public boolean tryMarkSaved() {
        return false;
    }

    @Override
    public boolean isUnsaved() {
        return false;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.wrapped.getPersistedStatus();
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
    }

    @Override
    public void markPosForPostProcessing(BlockPos blockPos) {
    }

    @Override
    public void setBlockEntityNbt(CompoundTag entityTag) {
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbt(BlockPos blockPos) {
        return this.wrapped.getBlockEntityNbt(blockPos);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos, HolderLookup.Provider registryAccess) {
        return this.wrapped.getBlockEntityNbtForSaving(blockPos, registryAccess);
    }

    @Override
    public void findBlocks(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> consumer) {
        this.wrapped.findBlocks(predicate, consumer);
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        if (this.allowWrites) {
            return this.wrapped.getBlockTicks();
        }
        return BlackholeTickAccess.emptyContainer();
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        if (this.allowWrites) {
            return this.wrapped.getFluidTicks();
        }
        return BlackholeTickAccess.emptyContainer();
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(long currentTick) {
        return this.wrapped.getTicksForSerialization(currentTick);
    }

    @Override
    public @Nullable BlendingData getBlendingData() {
        return this.wrapped.getBlendingData();
    }

    @Override
    public CarvingMask getCarvingMask() {
        if (this.allowWrites) {
            return super.getCarvingMask();
        }
        throw Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }

    @Override
    public CarvingMask getOrCreateCarvingMask() {
        if (this.allowWrites) {
            return super.getOrCreateCarvingMask();
        }
        throw Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
    }

    public LevelChunk getWrapped() {
        return this.wrapped;
    }

    @Override
    public boolean isLightCorrect() {
        return this.wrapped.isLightCorrect();
    }

    @Override
    public void setLightCorrect(boolean isLightCorrect) {
        this.wrapped.setLightCorrect(isLightCorrect);
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler) {
        if (this.allowWrites) {
            this.wrapped.fillBiomesFromNoise(biomeResolver, sampler);
        }
    }

    @Override
    public void initializeLightSources() {
        this.wrapped.initializeLightSources();
    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return this.wrapped.getSkyLightSources();
    }
}

