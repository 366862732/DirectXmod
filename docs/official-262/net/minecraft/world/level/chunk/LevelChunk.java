/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.shorts.ShortList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelChunk
extends ChunkAccess
implements DebugValueSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity(){

        @Override
        public void tick() {
        }

        @Override
        public boolean isRemoved() {
            return true;
        }

        @Override
        public BlockPos getPos() {
            return BlockPos.ZERO;
        }

        @Override
        public String getType() {
            return "<null>";
        }
    };
    private final Map<BlockPos, RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
    private boolean loaded;
    private final Level level;
    private @Nullable Supplier<FullChunkStatus> fullStatus;
    private @Nullable PostLoadProcessor postLoad;
    private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
    private final LevelChunkTicks<Block> blockTicks;
    private final LevelChunkTicks<Fluid> fluidTicks;
    private UnsavedListener unsavedListener = chunkPos -> {};

    public LevelChunk(Level level, ChunkPos pos) {
        this(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<Block>(), new LevelChunkTicks<Fluid>(), 0L, null, null, null);
    }

    public LevelChunk(Level level, ChunkPos pos, UpgradeData upgradeData, LevelChunkTicks<Block> blockTicks, LevelChunkTicks<Fluid> fluidTicks, long inhabitedTime, LevelChunkSection @Nullable [] sections, @Nullable PostLoadProcessor postLoad, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, level, level.palettedContainerFactory(), inhabitedTime, sections, blendingData);
        this.level = level;
        this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap();
        for (Heightmap.Types type : Heightmap.Types.values()) {
            if (!ChunkStatus.FULL.heightmapsAfter().contains(type)) continue;
            this.heightmaps.put(type, new Heightmap(this, type));
        }
        this.postLoad = postLoad;
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
    }

    public LevelChunk(ServerLevel level, ProtoChunk protoChunk, @Nullable PostLoadProcessor postLoad) {
        this(level, protoChunk.getPos(), protoChunk.getUpgradeData(), protoChunk.unpackBlockTicks(), protoChunk.unpackFluidTicks(), protoChunk.getInhabitedTime(), protoChunk.getSections(), postLoad, protoChunk.getBlendingData());
        if (!Collections.disjoint(protoChunk.pendingBlockEntities.keySet(), protoChunk.blockEntities.keySet())) {
            LOGGER.error("Chunk at {} contains duplicated block entities", (Object)protoChunk.getPos());
        }
        for (BlockEntity blockEntity : protoChunk.getBlockEntities().values()) {
            this.setBlockEntity(blockEntity);
        }
        this.pendingBlockEntities.putAll(protoChunk.getBlockEntityNbts());
        for (int i = 0; i < protoChunk.getPostProcessing().length; ++i) {
            this.postProcessing[i] = protoChunk.getPostProcessing()[i];
        }
        this.setAllStarts(protoChunk.getAllStarts());
        this.setAllReferences(protoChunk.getAllReferences());
        for (Map.Entry<Heightmap.Types, Heightmap> entry : protoChunk.getHeightmaps()) {
            if (!ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) continue;
            this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
        }
        this.skyLightSources = protoChunk.skyLightSources;
        this.setLightCorrect(protoChunk.isLightCorrect());
        this.markUnsaved();
    }

    public void setUnsavedListener(UnsavedListener unsavedListener) {
        this.unsavedListener = unsavedListener;
        if (this.isUnsaved()) {
            unsavedListener.setUnsaved(this.chunkPos);
        }
    }

    @Override
    public void markUnsaved() {
        boolean wasUnsaved = this.isUnsaved();
        super.markUnsaved();
        if (!wasUnsaved) {
            this.unsavedListener.setUnsaved(this.chunkPos);
        }
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
    public GameEventListenerRegistry getListenerRegistry(int section) {
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            return (GameEventListenerRegistry)this.gameEventListenerRegistrySections.computeIfAbsent(section, key -> new EuclideanGameEventListenerRegistry(serverLevel, section, this::removeGameEventListenerRegistry));
        }
        return super.getListenerRegistry(section);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (this.level.isDebug()) {
            BlockState blockState = null;
            if (y == 60) {
                blockState = Blocks.BARRIER.defaultBlockState();
            }
            if (y == 70) {
                blockState = DebugLevelSource.getBlockStateFor(x, z);
            }
            return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
        }
        try {
            LevelChunkSection currentSection;
            int sectionIndex = this.getSectionIndex(y);
            if (sectionIndex >= 0 && sectionIndex < this.sections.length && !(currentSection = this.sections[sectionIndex]).hasOnlyAir()) {
                return currentSection.getBlockState(x & 0xF, y & 0xF, z & 0xF);
            }
            return Blocks.AIR.defaultBlockState();
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Getting block state");
            CrashReportCategory category = report.addCategory("Block being got");
            category.setDetail("Location", () -> CrashReportCategory.formatLocation((LevelHeightAccessor)this, x, y, z));
            throw new ReportedException(report);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        try {
            LevelChunkSection currentSection;
            int sectionIndex = this.getSectionIndex(y);
            if (sectionIndex >= 0 && sectionIndex < this.sections.length && !(currentSection = this.sections[sectionIndex]).hasOnlyAir()) {
                return currentSection.getFluidState(x & 0xF, y & 0xF, z & 0xF);
            }
            return Fluids.EMPTY.defaultFluidState();
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Getting fluid state");
            CrashReportCategory category = report.addCategory("Block being got");
            category.setDetail("Location", () -> CrashReportCategory.formatLocation((LevelHeightAccessor)this, x, y, z));
            throw new ReportedException(report);
        }
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        Level level;
        BlockEntity blockEntity;
        boolean sideEffects;
        int localZ;
        int localY;
        int y = pos.getY();
        LevelChunkSection section = this.getSection(this.getSectionIndex(y));
        boolean wasEmpty = section.hasOnlyAir();
        if (wasEmpty && state.isAir()) {
            return null;
        }
        int localX = pos.getX() & 0xF;
        BlockState oldState = section.setBlockState(localX, localY = y & 0xF, localZ = pos.getZ() & 0xF, state);
        if (oldState == state) {
            return null;
        }
        Block newBlock = state.getBlock();
        ((Heightmap)this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING)).update(localX, y, localZ, state);
        ((Heightmap)this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)).update(localX, y, localZ, state);
        ((Heightmap)this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR)).update(localX, y, localZ, state);
        ((Heightmap)this.heightmaps.get(Heightmap.Types.WORLD_SURFACE)).update(localX, y, localZ, state);
        boolean isEmpty = section.hasOnlyAir();
        if (wasEmpty != isEmpty) {
            this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, isEmpty);
            this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x(), SectionPos.blockToSectionCoord(y), this.chunkPos.z(), isEmpty);
        }
        if (LightEngine.hasDifferentLightProperties(oldState, state)) {
            ProfilerFiller profiler = Profiler.get();
            profiler.push("updateSkyLightSources");
            this.skyLightSources.update(this, localX, y, localZ);
            profiler.popPush("queueCheckLight");
            this.level.getChunkSource().getLightEngine().checkBlock(pos);
            profiler.pop();
        }
        boolean blockChanged = !oldState.is(newBlock);
        boolean movedByPiston = (flags & 0x40) != 0;
        boolean bl = sideEffects = (flags & 0x100) == 0;
        if (blockChanged && oldState.hasBlockEntity() && !state.shouldChangedStateKeepBlockEntity(oldState)) {
            if (!this.level.isClientSide() && sideEffects && (blockEntity = this.level.getBlockEntity(pos)) != null) {
                blockEntity.preRemoveSideEffects(pos, oldState);
            }
            this.removeBlockEntity(pos);
        }
        if ((blockChanged || newBlock instanceof BaseRailBlock) && (level = this.level) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if ((flags & 1) != 0 || movedByPiston) {
                oldState.affectNeighborsAfterRemoval(serverLevel, pos, movedByPiston);
            }
        }
        if (!section.getBlockState(localX, localY, localZ).is(newBlock)) {
            return null;
        }
        if (!this.level.isClientSide() && (flags & 0x200) == 0) {
            state.onPlace(this.level, pos, oldState, movedByPiston);
        }
        if (state.hasBlockEntity() && section.getBlockState(localX, localY, localZ).is(newBlock)) {
            blockEntity = this.getBlockEntity(pos, EntityCreationType.CHECK);
            if (blockEntity != null && !blockEntity.isValidBlockState(state)) {
                LOGGER.warn("Found mismatched block entity @ {}: type = {}, state = {}", new Object[]{pos, blockEntity.typeHolder().getRegisteredName(), state});
                this.removeBlockEntity(pos);
                blockEntity = null;
            }
            if (blockEntity == null) {
                blockEntity = ((EntityBlock)((Object)newBlock)).newBlockEntity(pos, state);
                if (blockEntity != null) {
                    this.addAndRegisterBlockEntity(blockEntity);
                }
            } else {
                blockEntity.setBlockState(state);
                this.updateBlockEntityTicker(blockEntity);
            }
        }
        this.markUnsaved();
        return oldState;
    }

    @Override
    @Deprecated
    public void addEntity(Entity entity) {
    }

    private @Nullable BlockEntity createBlockEntity(BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        if (!state.hasBlockEntity()) {
            return null;
        }
        return ((EntityBlock)((Object)state.getBlock())).newBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos, EntityCreationType.CHECK);
    }

    public @Nullable BlockEntity getBlockEntity(BlockPos pos, EntityCreationType creationType) {
        BlockEntity promoted;
        CompoundTag tag;
        BlockEntity blockEntity = (BlockEntity)this.blockEntities.get(pos);
        if (blockEntity == null && (tag = (CompoundTag)this.pendingBlockEntities.remove(pos)) != null && (promoted = this.promotePendingBlockEntity(pos, tag)) != null) {
            return promoted;
        }
        if (blockEntity == null) {
            if (creationType == EntityCreationType.IMMEDIATE && (blockEntity = this.createBlockEntity(pos)) != null) {
                this.addAndRegisterBlockEntity(blockEntity);
            }
        } else if (blockEntity.isRemoved()) {
            this.blockEntities.remove(pos);
            return null;
        }
        return blockEntity;
    }

    public void addAndRegisterBlockEntity(BlockEntity blockEntity) {
        this.setBlockEntity(blockEntity);
        if (this.isInLevel()) {
            Level level = this.level;
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.addGameEventListener(blockEntity, serverLevel);
            }
            this.level.onBlockEntityAdded(blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        }
    }

    private boolean isInLevel() {
        return this.loaded || this.level.isClientSide();
    }

    private boolean isTicking(BlockPos pos) {
        if (!this.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            return this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverLevel.areEntitiesLoaded(ChunkPos.pack(pos));
        }
        return true;
    }

    @Override
    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockState blockState = this.getBlockState(pos);
        if (!blockState.hasBlockEntity()) {
            LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, pos, blockState});
            return;
        }
        BlockState cachedBlockState = blockEntity.getBlockState();
        if (blockState != cachedBlockState) {
            if (!blockEntity.getType().isValid(blockState)) {
                LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, pos, blockState});
                return;
            }
            if (blockState.getBlock() != cachedBlockState.getBlock()) {
                LOGGER.warn("Block state mismatch on block entity {} in position {}, {} != {}, updating", new Object[]{blockEntity, pos, blockState, cachedBlockState});
            }
            blockEntity.setBlockState(blockState);
        }
        blockEntity.setLevel(this.level);
        blockEntity.clearRemoved();
        BlockEntity previousEntry = this.blockEntities.put(pos.immutable(), blockEntity);
        if (previousEntry != null && previousEntry != blockEntity) {
            previousEntry.setRemoved();
        }
    }

    @Override
    public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos blockPos, HolderLookup.Provider registryAccess) {
        BlockEntity blockEntity = this.getBlockEntity(blockPos);
        if (blockEntity != null && !blockEntity.isRemoved()) {
            CompoundTag result = blockEntity.saveWithFullMetadata(this.level.registryAccess());
            result.putBoolean("keepPacked", false);
            return result;
        }
        CompoundTag result = (CompoundTag)this.pendingBlockEntities.get(blockPos);
        if (result != null) {
            result = result.copy();
            result.putBoolean("keepPacked", true);
        }
        return result;
    }

    @Override
    public void removeBlockEntity(BlockPos pos) {
        BlockEntity removeThis;
        if (this.isInLevel() && (removeThis = (BlockEntity)this.blockEntities.remove(pos)) != null) {
            Level level = this.level;
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.removeGameEventListener(removeThis, serverLevel);
                serverLevel.debugSynchronizers().dropBlockEntity(pos);
            }
            removeThis.setRemoved();
        }
        this.removeBlockEntityTicker(pos);
    }

    private <T extends BlockEntity> void removeGameEventListener(T blockEntity, ServerLevel level) {
        EntityBlock entityBlock;
        GameEventListener listener;
        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock && (listener = (entityBlock = (EntityBlock)((Object)block)).getListener(level, blockEntity)) != null) {
            int section = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
            GameEventListenerRegistry listenerRegistry = this.getListenerRegistry(section);
            listenerRegistry.unregister(listener);
        }
    }

    private void removeGameEventListenerRegistry(int sectionY) {
        this.gameEventListenerRegistrySections.remove(sectionY);
    }

    private void removeBlockEntityTicker(BlockPos pos) {
        RebindableTickingBlockEntityWrapper ticker = this.tickersInLevel.remove(pos);
        if (ticker != null) {
            ticker.rebind(NULL_TICKER);
        }
    }

    public void runPostLoad() {
        if (this.postLoad != null) {
            this.postLoad.run(this);
            this.postLoad = null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public void replaceWithPacketData(FriendlyByteBuf buffer, Map<Heightmap.Types, long[]> heightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities) {
        this.clearAllBlockEntities();
        for (LevelChunkSection section : this.sections) {
            section.read(buffer);
        }
        heightmaps.forEach(this::setHeightmap);
        this.initializeLightSources();
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER);){
            blockEntities.accept((pos, type, tag) -> {
                BlockEntity blockEntity = this.getBlockEntity(pos, EntityCreationType.IMMEDIATE);
                if (blockEntity != null && tag != null && blockEntity.getType() == type) {
                    blockEntity.loadWithComponents(TagValueInput.create(reporter.forChild(blockEntity.problemPath()), (HolderLookup.Provider)this.level.registryAccess(), tag));
                }
            });
        }
    }

    public void replaceBiomes(FriendlyByteBuf buffer) {
        for (LevelChunkSection section : this.sections) {
            section.readBiomes(buffer);
        }
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public Level getLevel() {
        return this.level;
    }

    public Map<BlockPos, BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void postProcessGeneration(ServerLevel level) {
        ChunkPos chunkPos = this.getPos();
        for (int sectionIndex = 0; sectionIndex < this.postProcessing.length; ++sectionIndex) {
            ShortList postProcessingSection = this.postProcessing[sectionIndex];
            if (postProcessingSection == null) continue;
            for (Short packedOffset : postProcessingSection) {
                BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(packedOffset, this.getSectionYFromSectionIndex(sectionIndex), chunkPos);
                BlockState blockState = this.getBlockState(blockPos);
                FluidState fluidState = blockState.getFluidState();
                if (!fluidState.isEmpty()) {
                    fluidState.tick(level, blockPos, blockState);
                }
                if (blockState.getBlock() instanceof LiquidBlock) {
                    blockState.tick(level, blockPos, level.getRandom());
                    continue;
                }
                BlockState blockStateNew = Block.updateFromNeighbourShapes(blockState, level, blockPos);
                if (blockStateNew == blockState) continue;
                level.setBlock(blockPos, blockStateNew, 276);
            }
            postProcessingSection.clear();
        }
        for (BlockPos pos : ImmutableList.copyOf(this.pendingBlockEntities.keySet())) {
            this.getBlockEntity(pos);
        }
        this.pendingBlockEntities.clear();
        this.upgradeData.upgrade(this);
    }

    private @Nullable BlockEntity promotePendingBlockEntity(BlockPos pos, CompoundTag tag) {
        BlockEntity blockEntity;
        BlockState state = this.getBlockState(pos);
        if ("DUMMY".equals(tag.getStringOr("id", ""))) {
            if (state.hasBlockEntity()) {
                blockEntity = ((EntityBlock)((Object)state.getBlock())).newBlockEntity(pos, state);
            } else {
                blockEntity = null;
                LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", (Object)pos, (Object)state);
            }
        } else {
            blockEntity = BlockEntity.loadStatic(pos, state, tag, this.level.registryAccess());
        }
        if (blockEntity != null) {
            blockEntity.setLevel(this.level);
            this.addAndRegisterBlockEntity(blockEntity);
        } else {
            LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", (Object)state, (Object)pos);
        }
        return blockEntity;
    }

    public void unpackTicks(long currentTick) {
        this.blockTicks.unpack(currentTick);
        this.fluidTicks.unpack(currentTick);
    }

    public void registerTickContainerInLevel(ServerLevel level) {
        ((LevelTicks)level.getBlockTicks()).addContainer(this.chunkPos, this.blockTicks);
        ((LevelTicks)level.getFluidTicks()).addContainer(this.chunkPos, this.fluidTicks);
    }

    public void unregisterTickContainerFromLevel(ServerLevel level) {
        ((LevelTicks)level.getBlockTicks()).removeContainer(this.chunkPos);
        ((LevelTicks)level.getFluidTicks()).removeContainer(this.chunkPos);
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        if (!this.getAllStarts().isEmpty()) {
            registration.register(DebugSubscriptions.STRUCTURES, () -> {
                ArrayList<DebugStructureInfo> structures = new ArrayList<DebugStructureInfo>();
                for (StructureStart start : this.getAllStarts().values()) {
                    BoundingBox boundingBox = start.getBoundingBox();
                    List<StructurePiece> pieces = start.getPieces();
                    ArrayList<DebugStructureInfo.Piece> pieceInfos = new ArrayList<DebugStructureInfo.Piece>(pieces.size());
                    for (int i = 0; i < pieces.size(); ++i) {
                        boolean isStart = i == 0;
                        pieceInfos.add(new DebugStructureInfo.Piece(pieces.get(i).getBoundingBox(), isStart));
                    }
                    structures.add(new DebugStructureInfo(boundingBox, pieceInfos));
                }
                return structures;
            });
        }
        registration.register(DebugSubscriptions.RAIDS, () -> level.getRaids().getRaidCentersInChunk(this.chunkPos));
    }

    @Override
    public ChunkStatus getPersistedStatus() {
        return ChunkStatus.FULL;
    }

    public FullChunkStatus getFullStatus() {
        if (this.fullStatus == null) {
            return FullChunkStatus.FULL;
        }
        return this.fullStatus.get();
    }

    public void setFullStatus(Supplier<FullChunkStatus> fullStatus) {
        this.fullStatus = fullStatus;
    }

    public void clearAllBlockEntities() {
        this.blockEntities.values().forEach(BlockEntity::setRemoved);
        this.blockEntities.clear();
        this.tickersInLevel.values().forEach(ticker -> ticker.rebind(NULL_TICKER));
        this.tickersInLevel.clear();
    }

    public void registerAllBlockEntitiesAfterLevelLoad() {
        this.blockEntities.values().forEach(blockEntity -> {
            Level patt0$temp = this.level;
            if (patt0$temp instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)patt0$temp;
                this.addGameEventListener(blockEntity, serverLevel);
            }
            this.level.onBlockEntityAdded((BlockEntity)blockEntity);
            this.updateBlockEntityTicker(blockEntity);
        });
    }

    private <T extends BlockEntity> void addGameEventListener(T blockEntity, ServerLevel level) {
        EntityBlock entityBlock;
        GameEventListener listener;
        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof EntityBlock && (listener = (entityBlock = (EntityBlock)((Object)block)).getListener(level, blockEntity)) != null) {
            this.getListenerRegistry(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY())).register(listener);
        }
    }

    private <T extends BlockEntity> void updateBlockEntityTicker(T blockEntity) {
        BlockState state = blockEntity.getBlockState();
        BlockEntityTicker<?> ticker = state.getTicker(this.level, blockEntity.getType());
        if (ticker == null) {
            this.removeBlockEntityTicker(blockEntity.getBlockPos());
        } else {
            this.tickersInLevel.compute(blockEntity.getBlockPos(), (blockPos, existingTicker) -> {
                TickingBlockEntity actualTicker = this.createTicker(blockEntity, ticker);
                if (existingTicker != null) {
                    existingTicker.rebind(actualTicker);
                    return existingTicker;
                }
                if (this.isInLevel()) {
                    RebindableTickingBlockEntityWrapper result = new RebindableTickingBlockEntityWrapper(actualTicker);
                    this.level.addBlockEntityTicker(result);
                    return result;
                }
                return null;
            });
        }
    }

    private <T extends BlockEntity> TickingBlockEntity createTicker(T blockEntity, BlockEntityTicker<T> ticker) {
        return new BoundTickingBlockEntity(this, blockEntity, ticker);
    }

    @FunctionalInterface
    public static interface PostLoadProcessor {
        public void run(LevelChunk var1);
    }

    @FunctionalInterface
    public static interface UnsavedListener {
        public void setUnsaved(ChunkPos var1);
    }

    public static enum EntityCreationType {
        IMMEDIATE,
        QUEUED,
        CHECK;

    }

    private static class RebindableTickingBlockEntityWrapper
    implements TickingBlockEntity {
        private TickingBlockEntity ticker;

        private RebindableTickingBlockEntityWrapper(TickingBlockEntity ticker) {
            this.ticker = ticker;
        }

        private void rebind(TickingBlockEntity ticker) {
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            this.ticker.tick();
        }

        @Override
        public boolean isRemoved() {
            return this.ticker.isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return this.ticker.getPos();
        }

        @Override
        public String getType() {
            return this.ticker.getType();
        }

        public String toString() {
            return String.valueOf(this.ticker) + " <wrapped>";
        }
    }

    private class BoundTickingBlockEntity<T extends BlockEntity>
    implements TickingBlockEntity {
        private final T blockEntity;
        private final BlockEntityTicker<T> ticker;
        private boolean loggedInvalidBlockState;
        final /* synthetic */ LevelChunk this$0;

        private BoundTickingBlockEntity(T blockEntity, BlockEntityTicker<T> ticker) {
            reference v0 = var1_1;
            Objects.requireNonNull(v0);
            this.this$0 = v0;
            this.blockEntity = blockEntity;
            this.ticker = ticker;
        }

        @Override
        public void tick() {
            BlockPos pos;
            if (!((BlockEntity)this.blockEntity).isRemoved() && ((BlockEntity)this.blockEntity).hasLevel() && this.this$0.isTicking(pos = ((BlockEntity)this.blockEntity).getBlockPos())) {
                try {
                    ProfilerFiller profiler = Profiler.get();
                    profiler.push(this::getType);
                    BlockState blockState = this.this$0.getBlockState(pos);
                    if (((BlockEntity)this.blockEntity).getType().isValid(blockState)) {
                        this.ticker.tick(this.this$0.level, ((BlockEntity)this.blockEntity).getBlockPos(), blockState, this.blockEntity);
                        this.loggedInvalidBlockState = false;
                    } else if (!this.loggedInvalidBlockState) {
                        this.loggedInvalidBlockState = true;
                        LOGGER.warn("Block entity {} @ {} state {} invalid for ticking:", new Object[]{LogUtils.defer(this::getType), LogUtils.defer(this::getPos), blockState});
                    }
                    profiler.pop();
                }
                catch (Throwable t) {
                    CrashReport report = CrashReport.forThrowable(t, "Ticking block entity");
                    CrashReportCategory category = report.addCategory("Block entity being ticked");
                    ((BlockEntity)this.blockEntity).fillCrashReportCategory(category);
                    throw new ReportedException(report);
                }
            }
        }

        @Override
        public boolean isRemoved() {
            return ((BlockEntity)this.blockEntity).isRemoved();
        }

        @Override
        public BlockPos getPos() {
            return ((BlockEntity)this.blockEntity).getBlockPos();
        }

        @Override
        public String getType() {
            return ((BlockEntity)this.blockEntity).typeHolder().getRegisteredName();
        }

        public String toString() {
            return "Level ticker for " + this.getType() + "@" + String.valueOf(this.getPos());
        }
    }
}

