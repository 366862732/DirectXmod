/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class WorldGenRegion
implements WorldGenLevel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final StaticCache2D<GenerationChunkHolder> cache;
    private final ChunkAccess center;
    private final ServerLevel level;
    private final long seed;
    private final LevelData levelData;
    private final RandomSource random;
    private final DimensionType dimensionType;
    private final WorldGenTickAccess<Block> blockTicks = new WorldGenTickAccess(pos -> this.getChunk((BlockPos)pos).getBlockTicks());
    private final WorldGenTickAccess<Fluid> fluidTicks = new WorldGenTickAccess(pos -> this.getChunk((BlockPos)pos).getFluidTicks());
    private final BiomeManager biomeManager;
    private final ChunkStep generatingStep;
    private @Nullable Supplier<String> currentlyGenerating;
    private final AtomicLong subTickCount = new AtomicLong();
    private static final Identifier WORLDGEN_REGION_RANDOM = Identifier.withDefaultNamespace("worldgen_region_random");
    private final int centerChunkX;
    private final int centerChunkZ;
    private final int writeRadius;

    public WorldGenRegion(ServerLevel level, StaticCache2D<GenerationChunkHolder> cache, ChunkStep generatingStep, ChunkAccess center) {
        this.generatingStep = generatingStep;
        this.cache = cache;
        this.center = center;
        this.level = level;
        this.seed = level.getSeed();
        this.levelData = level.getLevelData();
        this.random = level.getChunkSource().randomState().getOrCreateRandomFactory(WORLDGEN_REGION_RANDOM).at(this.center.getPos().getWorldPosition());
        this.dimensionType = level.dimensionType();
        this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed));
        ChunkPos centerPos = center.getPos();
        this.centerChunkX = centerPos.x();
        this.centerChunkZ = centerPos.z();
        this.writeRadius = generatingStep.blockStateWriteRadius();
    }

    public boolean isOldChunkAround(ChunkPos pos, int range) {
        return this.level.getChunkSource().chunkMap.isOldChunkAround(pos, range);
    }

    public ChunkPos getCenter() {
        return this.center.getPos();
    }

    @Override
    public void setCurrentlyGenerating(@Nullable Supplier<String> currentlyGenerating) {
        this.currentlyGenerating = currentlyGenerating;
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY);
    }

    @Override
    public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate) {
        GenerationChunkHolder chunkHolder;
        ChunkStatus maxAllowedStatus;
        int distance = this.center.getPos().getChessboardDistance(chunkX, chunkZ);
        ChunkStatus chunkStatus = maxAllowedStatus = distance >= this.generatingStep.directDependencies().size() ? null : this.generatingStep.directDependencies().get(distance);
        if (maxAllowedStatus != null) {
            ChunkAccess chunk;
            chunkHolder = this.cache.get(chunkX, chunkZ);
            if (targetStatus.isOrBefore(maxAllowedStatus) && (chunk = chunkHolder.getChunkIfPresentUnchecked(maxAllowedStatus)) != null) {
                return chunk;
            }
        } else {
            chunkHolder = null;
        }
        CrashReport report = CrashReport.forThrowable(new IllegalStateException("Requested chunk unavailable during world generation"), "Exception generating new chunk");
        CrashReportCategory category = report.addCategory("Chunk request details");
        category.setDetail("Requested chunk", String.format(Locale.ROOT, "%d, %d", chunkX, chunkZ));
        category.setDetail("Generating status", () -> this.generatingStep.targetStatus().getName());
        category.setDetail("Requested status", targetStatus::getName);
        category.setDetail("Actual status", () -> chunkHolder == null ? "[out of cache bounds]" : chunkHolder.getPersistedStatus().getName());
        category.setDetail("Maximum allowed status", () -> maxAllowedStatus == null ? "null" : maxAllowedStatus.getName());
        category.setDetail("Dependencies", this.generatingStep.directDependencies()::toString);
        category.setDetail("Requested distance", distance);
        category.setDetail("Generating chunk", this.center.getPos()::toString);
        throw new ReportedException(report);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        int distance = this.center.getPos().getChessboardDistance(chunkX, chunkZ);
        return distance < this.generatingStep.directDependencies().size();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        this.warnIfReadOutsideWriteZone(chunkX, chunkZ);
        return this.getChunk(chunkX, chunkZ).getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        this.warnIfReadOutsideWriteZone(chunkX, chunkZ);
        return this.getChunk(chunkX, chunkZ).getFluidState(pos);
    }

    @Override
    public @Nullable Player getNearestPlayer(double x, double y, double z, double maxDist, @Nullable Predicate<Entity> predicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return this.level.getUncachedNoiseBiome(quartX, quartY, quartZ);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        BlockState blockState = this.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3, updateLimit);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        ChunkAccess chunk = this.getChunk(pos);
        BlockEntity blockEntity = chunk.getBlockEntity(pos);
        if (blockEntity != null) {
            return blockEntity;
        }
        CompoundTag tag = chunk.getBlockEntityNbt(pos);
        BlockState state = chunk.getBlockState(pos);
        if (tag != null) {
            if ("DUMMY".equals(tag.getStringOr("id", ""))) {
                if (!state.hasBlockEntity()) {
                    return null;
                }
                blockEntity = ((EntityBlock)((Object)state.getBlock())).newBlockEntity(pos, state);
            } else {
                blockEntity = BlockEntity.loadStatic(pos, state, tag, this.level.registryAccess());
            }
            if (blockEntity != null) {
                chunk.setBlockEntity(blockEntity);
                return blockEntity;
            }
        }
        if (state.hasBlockEntity()) {
            LOGGER.warn("Tried to access a block entity before it was created. {}", (Object)pos);
        }
        return null;
    }

    private void warnIfReadOutsideWriteZone(int chunkX, int chunkZ) {
        if (this.centerChunkX == chunkX && this.centerChunkZ == chunkZ) {
            return;
        }
        if (!this.isWithinWriteZone(chunkX, chunkZ)) {
            int readDistance = Math.max(Math.abs(this.centerChunkX - chunkX), Math.abs(this.centerChunkZ - chunkZ));
            String warning = "Detected unsafe terrain read during worldgen: reading from chunk [" + chunkX + ", " + chunkZ + "] while generating chunk [" + this.centerChunkX + ", " + this.centerChunkZ + "] (distance: " + readDistance + ", write radius: " + this.writeRadius + "), step: " + this.generatingStep.targetStatus().getName() + (String)(this.currentlyGenerating == null ? "" : ", currently generating: " + this.currentlyGenerating.get());
            Util.logAndPauseIfInIde(warning);
        }
    }

    public boolean isWithinWriteZone(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return this.isWithinWriteZone(chunkX, chunkZ);
    }

    private boolean isWithinWriteZone(int chunkX, int chunkZ) {
        return Math.abs(this.centerChunkX - chunkX) <= this.writeRadius && Math.abs(this.centerChunkZ - chunkZ) <= this.writeRadius;
    }

    @Override
    public boolean ensureCanWrite(BlockPos pos) {
        if (!this.isWithinWriteZone(pos)) {
            int chunkX = SectionPos.blockToSectionCoord(pos.getX());
            int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
            Util.logAndPauseIfInIde("Detected setBlock in a far chunk [" + chunkX + ", " + chunkZ + "], pos: " + String.valueOf(pos) + ", status: " + String.valueOf(this.generatingStep.targetStatus()) + (String)(this.currentlyGenerating == null ? "" : ", currently generating: " + this.currentlyGenerating.get()));
            return false;
        }
        if (this.center.isUpgrading()) {
            LevelHeightAccessor levelHeightAccessor = this.center.getHeightAccessorForGeneration();
            return !levelHeightAccessor.isOutsideBuildHeight(pos.getY());
        }
        return true;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        BlockPos postProcessPos;
        if (!this.ensureCanWrite(pos)) {
            return false;
        }
        ChunkAccess chunk = this.getChunk(pos);
        BlockState oldState = chunk.setBlockState(pos, blockState, updateFlags);
        if (oldState != null) {
            this.level.updatePOIOnBlockStateChange(pos, oldState, blockState);
        }
        if (blockState.hasBlockEntity()) {
            if (chunk.getPersistedStatus().getChunkType() == ChunkType.LEVELCHUNK) {
                BlockEntity blockEntity = ((EntityBlock)((Object)blockState.getBlock())).newBlockEntity(pos, blockState);
                if (blockEntity != null) {
                    chunk.setBlockEntity(blockEntity);
                } else {
                    chunk.removeBlockEntity(pos);
                }
            } else {
                CompoundTag tag = new CompoundTag();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                tag.putString("id", "DUMMY");
                chunk.setBlockEntityNbt(tag);
            }
        } else if (oldState != null && oldState.hasBlockEntity()) {
            chunk.removeBlockEntity(pos);
        }
        if ((updateFlags & 0x10) == 0 && (postProcessPos = blockState.getPostProcessPos(this, pos)) != null) {
            this.markPosForPostProcessing(postProcessPos);
        }
        return true;
    }

    private void markPosForPostProcessing(BlockPos blockPos) {
        this.getChunk(blockPos).markPosForPostProcessing(blockPos);
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        int xc = SectionPos.blockToSectionCoord(entity.getBlockX());
        int zc = SectionPos.blockToSectionCoord(entity.getBlockZ());
        this.getChunk(xc, zc).addEntity(entity);
        return true;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    @Deprecated
    public ServerLevel getLevel() {
        return this.level;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.level.enabledFeatures();
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        if (!this.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        }
        return new DifficultyInstance(this.level.getDifficulty(), this.level.getOverworldClockTime(), 0L, this.level.getMoonBrightness(pos));
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return this.level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return this.level.getChunkSource();
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        int chunkX = SectionPos.blockToSectionCoord(x);
        int chunkZ = SectionPos.blockToSectionCoord(z);
        this.warnIfReadOutsideWriteZone(chunkX, chunkZ);
        return this.getChunk(chunkX, chunkZ).getHeight(type, x & 0xF, z & 0xF) + 1;
    }

    @Override
    public void playSound(@Nullable Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    @Override
    public void addParticle(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
    }

    @Override
    public void levelEvent(@Nullable Entity source, int type, BlockPos pos, int data) {
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionType;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(pos));
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB bb, @Nullable Predicate<? super Entity> selector) {
        return Collections.emptyList();
    }

    public List<Player> players() {
        return Collections.emptyList();
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public long nextSubTickCount() {
        return this.subTickCount.getAndIncrement();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return EnvironmentAttributeReader.EMPTY;
    }
}

