/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.shorts.ShortList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ProtoChunk
extends ChunkAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile @Nullable LevelLightEngine lightEngine;
    private volatile ChunkStatus status = ChunkStatus.EMPTY;
    private final List<CompoundTag> entities = Lists.newArrayList();
    private @Nullable CarvingMask carvingMask;
    private @Nullable BelowZeroRetrogen belowZeroRetrogen;
    private final ProtoChunkTicks<Block> blockTicks;
    private final ProtoChunkTicks<Fluid> fluidTicks;

    public ProtoChunk(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory containerFactory, @Nullable BlendingData blendingData) {
        this(chunkPos, upgradeData, null, new ProtoChunkTicks<Block>(), new ProtoChunkTicks<Fluid>(), levelHeightAccessor, containerFactory, blendingData);
    }

    public ProtoChunk(ChunkPos chunkPos, UpgradeData upgradeData, LevelChunkSection @Nullable [] sections, ProtoChunkTicks<Block> blockTicks, ProtoChunkTicks<Fluid> fluidTicks, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory containerFactory, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, containerFactory, 0L, sections, blendingData);
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
    }

    @Override
    public TickContainerAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickContainerAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public ChunkAccess.PackedTicks getTicksForSerialization(long currentTick) {
        return new ChunkAccess.PackedTicks(this.blockTicks.pack(currentTick), this.fluidTicks.pack(currentTick));
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int y = pos.getY();
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        LevelChunkSection section = this.getSection(this.getSectionIndex(y));
        if (section.hasOnlyAir()) {
            return Blocks.AIR.defaultBlockState();
        }
        return section.getBlockState(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        int y = pos.getY();
        if (this.isOutsideBuildHeight(y)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        LevelChunkSection section = this.getSection(this.getSectionIndex(y));
        if (section.hasOnlyAir()) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return section.getFluidState(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.isOutsideBuildHeight(y)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        int sectionIndex = this.getSectionIndex(y);
        LevelChunkSection section = this.getSection(sectionIndex);
        boolean wasEmpty = section.hasOnlyAir();
        if (wasEmpty && state.is(Blocks.AIR)) {
            return state;
        }
        int localX = SectionPos.sectionRelative(x);
        int localY = SectionPos.sectionRelative(y);
        int localZ = SectionPos.sectionRelative(z);
        BlockState oldState = section.setBlockState(localX, localY, localZ, state);
        if (this.status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
            boolean isEmpty = section.hasOnlyAir();
            if (isEmpty != wasEmpty) {
                this.lightEngine.updateSectionStatus(pos, isEmpty);
            }
            if (LightEngine.hasDifferentLightProperties(oldState, state)) {
                this.skyLightSources.update(this, localX, y, localZ);
                this.lightEngine.checkBlock(pos);
            }
        }
        EnumSet<Heightmap.Types> heightmapsAfter = this.getPersistedStatus().heightmapsAfter();
        EnumSet<Heightmap.Types> toPrime = null;
        for (Heightmap.Types type : heightmapsAfter) {
            Heightmap heightmap = (Heightmap)this.heightmaps.get(type);
            if (heightmap != null) continue;
            if (toPrime == null) {
                toPrime = EnumSet.noneOf(Heightmap.Types.class);
            }
            toPrime.add(type);
        }
        if (toPrime != null) {
            Heightmap.primeHeightmaps(this, toPrime);
        }
        for (Heightmap.Types type : heightmapsAfter) {
            ((Heightmap)this.heightmaps.get(type)).update(localX, y, localZ, state);
        }
        return oldState;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        this.pendingBlockEntities.remove(blockEntity.getBlockPos());
        this.blockEntities.put(blockEntity.getBlockPos(), blockEntity);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return (BlockEntity)this.blockEntities.get(pos);
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addEntity(CompoundTag tag) {
        this.entities.add(tag);
    }

    @Override
    public void addEntity(Entity entity) {
        if (entity.isPassenger()) {
            return;
        }
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.save(output);
            this.addEntity(output.buildResult());
        }
    }

    @Override
    public void setStartForStructure(Structure structure, StructureStart structureStart) {
        BelowZeroRetrogen belowZeroRetrogen = this.getBelowZeroRetrogen();
        if (belowZeroRetrogen != null && structureStart.isValid()) {
            BoundingBox boundingBox = structureStart.getBoundingBox();
            LevelHeightAccessor heightAccessor = this.getHeightAccessorForGeneration();
            if (boundingBox.minY() < heightAccessor.getMinY() || boundingBox.maxY() > heightAccessor.getMaxY()) {
                return;
            }
        }
        super.setStartForStructure(structure, structureStart);
    }

    public List<CompoundTag> getEntities() {
        return this.entities;
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return this.status;
    }

    public void setPersistedStatus(ChunkStatus status) {
        this.status = status;
        if (this.belowZeroRetrogen != null && status.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
            this.setBelowZeroRetrogen(null);
        }
        this.markUnsaved();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        if (this.getHighestGeneratedStatus().isOrAfter(ChunkStatus.BIOMES)) {
            return super.getNoiseBiome(quartX, quartY, quartZ);
        }
        throw new IllegalStateException("Asking for biomes before we have biomes");
    }

    public static short packOffsetCoordinates(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int dx = x & 0xF;
        int dy = y & 0xF;
        int dz = z & 0xF;
        return (short)(dx | dy << 4 | dz << 8);
    }

    public static BlockPos unpackOffsetCoordinates(short packedData, int sectionY, ChunkPos chunkPos) {
        int posX = SectionPos.sectionToBlockCoord(chunkPos.x(), packedData & 0xF);
        int posY = SectionPos.sectionToBlockCoord(sectionY, packedData >>> 4 & 0xF);
        int posZ = SectionPos.sectionToBlockCoord(chunkPos.z(), packedData >>> 8 & 0xF);
        return new BlockPos(posX, posY, posZ);
    }

    @Override
    public void markPosForPostProcessing(BlockPos blockPos) {
        if (this.isInsideBuildHeight(blockPos)) {
            ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(blockPos.getY())).add(ProtoChunk.packOffsetCoordinates(blockPos));
        }
    }

    @Override
    public void addPackedPostProcess(ShortList packedOffsets, int sectionIndex) {
        ChunkAccess.getOrCreateOffsetList(this.postProcessing, sectionIndex).addAll(packedOffsets);
    }

    public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
        return Collections.unmodifiableMap(this.pendingBlockEntities);
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos, HolderLookup.Provider registryAccess) {
        BlockEntity blockEntity = this.getBlockEntity(blockPos);
        if (blockEntity != null) {
            return blockEntity.saveWithFullMetadata(registryAccess);
        }
        return (CompoundTag)this.pendingBlockEntities.get(blockPos);
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        this.blockEntities.remove(pos);
        this.pendingBlockEntities.remove(pos);
    }

    public @Nullable CarvingMask getCarvingMask() {
        return this.carvingMask;
    }

    public CarvingMask getOrCreateCarvingMask() {
        if (this.carvingMask == null) {
            this.carvingMask = new CarvingMask(this.getHeight(), this.getMinY());
        }
        return this.carvingMask;
    }

    public void setCarvingMask(CarvingMask data) {
        this.carvingMask = data;
    }

    public void setLightEngine(LevelLightEngine lightEngine) {
        this.lightEngine = lightEngine;
    }

    public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen belowZeroRetrogen) {
        this.belowZeroRetrogen = belowZeroRetrogen;
    }

    @Override
    public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
        return this.belowZeroRetrogen;
    }

    private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> ticks) {
        return new LevelChunkTicks<T>(ticks.scheduledTicks());
    }

    public LevelChunkTicks<Block> unpackBlockTicks() {
        return ProtoChunk.unpackTicks(this.blockTicks);
    }

    public LevelChunkTicks<Fluid> unpackFluidTicks() {
        return ProtoChunk.unpackTicks(this.fluidTicks);
    }

    @Override
    public LevelHeightAccessor getHeightAccessorForGeneration() {
        if (this.isUpgrading()) {
            return BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR;
        }
        return this;
    }
}

