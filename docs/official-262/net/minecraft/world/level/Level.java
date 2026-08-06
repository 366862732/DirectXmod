/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.serialization.Codec
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class Level
implements LevelAccessor,
AutoCloseable {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int ACROSS_THE_WHOLE_WORLD = 60000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    private static final WeightedList<ExplosionParticleInfo> DEFAULT_EXPLOSION_BLOCK_PARTICLES = WeightedList.builder().add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5f, 1.0f)).add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0f, 1.0f)).build();
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    protected final CollectingNeighborUpdater neighborUpdater;
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    private final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    protected int randValue = RandomSource.createThreadLocalInstance().nextInt();
    protected final int addend = 1013904223;
    protected float oRainLevel;
    protected float rainLevel;
    protected float oThunderLevel;
    protected float thunderLevel;
    protected final RandomSource random = RandomSource.create();
    @Deprecated
    private final RandomSource soundSeedGenerator = RandomSource.createThreadSafe();
    private final Holder<DimensionType> dimensionTypeRegistration;
    protected final WritableLevelData levelData;
    private final boolean isClientSide;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private final RegistryAccess registryAccess;
    private final DamageSources damageSources;
    private final PalettedContainerFactory palettedContainerFactory;
    private long subTickCount;

    protected Level(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        this.levelData = levelData;
        this.dimensionTypeRegistration = dimensionTypeRegistration;
        this.dimension = dimension;
        this.isClientSide = isClientSide;
        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, biomeZoomSeed);
        this.isDebug = isDebug;
        this.neighborUpdater = new CollectingNeighborUpdater(this, maxChainedNeighborUpdates);
        this.registryAccess = registryAccess;
        this.palettedContainerFactory = PalettedContainerFactory.create(registryAccess);
        this.damageSources = new DamageSources(registryAccess);
    }

    public int getNextEntityId() {
        return 0;
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return null;
    }

    public boolean isInWorldBounds(BlockPos pos) {
        return this.isInsideBuildHeight(pos) && Level.isInWorldBoundsHorizontal(pos);
    }

    public boolean isInValidBounds(BlockPos pos) {
        return this.isInsideBuildHeight(pos) && Level.isInValidBoundsHorizontal(pos);
    }

    public static boolean isInSpawnableBounds(BlockPos pos) {
        return !Level.isOutsideSpawnableHeight(pos.getY()) && Level.isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    private static boolean isInValidBoundsHorizontal(BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        return ChunkPos.isValid(chunkX, chunkZ);
    }

    private static boolean isOutsideSpawnableHeight(int y) {
        return y < -20000000 || y >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    @Override
    public LevelChunk getChunk(int chunkX, int chunkZ) {
        return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL);
    }

    @Override
    public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean loadOrGenerate) {
        ChunkAccess chunk = this.getChunkSource().getChunk(chunkX, chunkZ, status, loadOrGenerate);
        if (chunk == null && loadOrGenerate) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        }
        return chunk;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags) {
        return this.setBlock(pos, blockState, updateFlags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        if (!this.isInValidBounds(pos)) {
            return false;
        }
        if (!this.isClientSide() && this.isDebug()) {
            return false;
        }
        LevelChunk chunk = this.getChunkAt(pos);
        Block block = blockState.getBlock();
        BlockState oldState = chunk.setBlockState(pos, blockState, updateFlags);
        if (oldState != null) {
            BlockState newState = this.getBlockState(pos);
            if (newState == blockState) {
                if (oldState != newState) {
                    this.setBlocksDirty(pos, oldState, newState);
                }
                if ((updateFlags & 2) != 0 && (!this.isClientSide() || (updateFlags & 4) == 0) && (this.isClientSide() || chunk.getFullStatus() != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                    this.sendBlockUpdated(pos, oldState, blockState, updateFlags);
                }
                if ((updateFlags & 1) != 0) {
                    this.updateNeighborsAt(pos, oldState.getBlock());
                    if (!this.isClientSide() && blockState.hasAnalogOutputSignal()) {
                        this.updateNeighbourForOutputSignal(pos, block);
                    }
                }
                if ((updateFlags & 0x10) == 0 && updateLimit > 0) {
                    int neighbourUpdateFlags = updateFlags & 0xFFFFFFDE;
                    oldState.updateIndirectNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
                    blockState.updateNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
                    blockState.updateIndirectNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
                }
                this.updatePOIOnBlockStateChange(pos, oldState, newState);
            }
            return true;
        }
        return false;
    }

    public void updatePOIOnBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState) {
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        FluidState fluidState = this.getFluidState(pos);
        return this.setBlock(pos, fluidState.createLegacyBlock(), 3 | (movedByPiston ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        boolean destroyed;
        BlockState blockState = this.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }
        FluidState fluidState = this.getFluidState(pos);
        if (!(blockState.getBlock() instanceof BaseFireBlock)) {
            this.levelEvent(2001, pos, Block.getId(blockState));
        }
        if (dropResources) {
            BlockEntity blockEntity = blockState.hasBlockEntity() ? this.getBlockEntity(pos) : null;
            Block.dropResources(blockState, this, pos, blockEntity, breaker, ItemStack.EMPTY);
        }
        if (destroyed = this.setBlock(pos, fluidState.createLegacyBlock(), 3, updateLimit)) {
            this.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(breaker, blockState));
        }
        return destroyed;
    }

    public void addDestroyBlockEffect(BlockPos pos, BlockState blockState) {
    }

    public boolean setBlockAndUpdate(BlockPos pos, BlockState blockState) {
        return this.setBlock(pos, blockState, 3);
    }

    public abstract void sendBlockUpdated(BlockPos var1, BlockState var2, BlockState var3, @Block.UpdateFlags int var4);

    public void setBlocksDirty(BlockPos pos, BlockState oldState, BlockState newState) {
    }

    public void updateNeighborsAt(BlockPos pos, Block sourceBlock, @Nullable Orientation orientation) {
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block blockObject, Direction skipDirection, @Nullable Orientation orientation) {
    }

    public void neighborChanged(BlockPos pos, Block changedBlock, @Nullable Orientation orientation) {
    }

    public void neighborChanged(BlockState state, BlockPos pos, Block changedBlock, @Nullable Orientation orientation, boolean movedByPiston) {
    }

    @Override
    public void neighborShapeChanged(Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        this.neighborUpdater.shapeUpdate(direction, neighborState, pos, neighborPos, updateFlags, updateLimit);
    }

    @Override
    public int getHeight(Heightmap.Types type, int x, int z) {
        int y = x < -30000000 || z < -30000000 || x >= 30000000 || z >= 30000000 ? this.getSeaLevel() + 1 : (this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)) ? this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(type, x & 0xF, z & 0xF) + 1 : this.getMinY());
        return y;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (!this.isInValidBounds(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        LevelChunk chunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        return chunk.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (!this.isInValidBounds(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        }
        LevelChunk chunk = this.getChunkAt(pos);
        return chunk.getFluidState(pos);
    }

    public boolean isBrightOutside() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isDarkOutside() {
        return !this.dimensionType().hasFixedTime() && !this.isBrightOutside();
    }

    @Override
    public void playSound(@Nullable Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.playSound(except, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, sound, source, volume, pitch);
    }

    public abstract void playSeededSound(@Nullable Entity var1, double var2, double var4, double var6, Holder<SoundEvent> var8, SoundSource var9, float var10, float var11, long var12);

    public void playSeededSound(@Nullable Entity except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, long seed) {
        this.playSeededSound(except, x, y, z, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, seed);
    }

    public abstract void playSeededSound(@Nullable Entity var1, Entity var2, Holder<SoundEvent> var3, SoundSource var4, float var5, float var6, long var7);

    public void playSound(@Nullable Entity except, double x, double y, double z, SoundEvent sound, SoundSource source) {
        this.playSound(except, x, y, z, sound, source, 1.0f, 1.0f);
    }

    public void playSound(@Nullable Entity except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.playSeededSound(except, x, y, z, sound, source, volume, pitch, this.soundSeedGenerator.nextLong());
    }

    public void playSound(@Nullable Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        this.playSeededSound(except, x, y, z, sound, source, volume, pitch, this.soundSeedGenerator.nextLong());
    }

    public void playSound(@Nullable Entity except, Entity sourceEntity, SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.playSeededSound(except, sourceEntity, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, this.soundSeedGenerator.nextLong());
    }

    public void playLocalSound(BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        this.playLocalSound((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, sound, source, volume, pitch, distanceDelay);
    }

    public void playLocalSound(Entity sourceEntity, SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    public void playLocalSound(double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
    }

    public void playPlayerSound(SoundEvent sound, SoundSource source, float volume, float pitch) {
    }

    @Override
    public void addParticle(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
    }

    public void addParticle(ParticleOptions particle, boolean overrideLimiter, boolean alwaysShow, double x, double y, double z, double xd, double yd, double zd) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd) {
    }

    public void addAlwaysVisibleParticle(ParticleOptions particle, boolean overrideLimiter, double x, double y, double z, double xd, double yd, double zd) {
    }

    public void addBlockEntityTicker(TickingBlockEntity ticker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
    }

    public void tickBlockEntities() {
        this.tickingBlockEntities = true;
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }
        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
        boolean tickBlockEntities = this.tickRateManager().runsNormally();
        while (iterator.hasNext()) {
            TickingBlockEntity ticker = iterator.next();
            if (ticker.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (!tickBlockEntities || !this.shouldTickBlocksAt(ticker.getPos())) continue;
            ticker.tick();
        }
        this.tickingBlockEntities = false;
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> tick, T entity) {
        try {
            tick.accept(entity);
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Ticking entity");
            CrashReportCategory category = report.addCategory("Entity being ticked");
            entity.fillCrashReportCategory(category);
            throw new ReportedException(report);
        }
    }

    public boolean shouldTickDeath(Entity entity) {
        return true;
    }

    public boolean shouldTickBlocksAt(long chunkPos) {
        return true;
    }

    public boolean shouldTickBlocksAt(BlockPos pos) {
        return this.shouldTickBlocksAt(ChunkPos.pack(pos));
    }

    public void explode(@Nullable Entity source, double x, double y, double z, float r, ExplosionInteraction blockInteraction) {
        this.explode(source, Explosion.getDefaultDamageSource(this, source), null, x, y, z, r, false, blockInteraction, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, DEFAULT_EXPLOSION_BLOCK_PARTICLES, SoundEvents.GENERIC_EXPLODE);
    }

    public void explode(@Nullable Entity source, double x, double y, double z, float r, boolean fire, ExplosionInteraction blockInteraction) {
        this.explode(source, Explosion.getDefaultDamageSource(this, source), null, x, y, z, r, fire, blockInteraction, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, DEFAULT_EXPLOSION_BLOCK_PARTICLES, SoundEvents.GENERIC_EXPLODE);
    }

    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, Vec3 boomPos, float r, boolean fire, ExplosionInteraction blockInteraction) {
        this.explode(source, damageSource, damageCalculator, boomPos.x(), boomPos.y(), boomPos.z(), r, fire, blockInteraction, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, DEFAULT_EXPLOSION_BLOCK_PARTICLES, SoundEvents.GENERIC_EXPLODE);
    }

    public void explode(@Nullable Entity source, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float r, boolean fire, ExplosionInteraction interactionType) {
        this.explode(source, damageSource, damageCalculator, x, y, z, r, fire, interactionType, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, DEFAULT_EXPLOSION_BLOCK_PARTICLES, SoundEvents.GENERIC_EXPLODE);
    }

    public abstract void explode(@Nullable Entity var1, @Nullable DamageSource var2, @Nullable ExplosionDamageCalculator var3, double var4, double var6, double var8, float var10, boolean var11, ExplosionInteraction var12, ParticleOptions var13, ParticleOptions var14, WeightedList<ExplosionParticleInfo> var15, Holder<SoundEvent> var16);

    public abstract String gatherChunkSourceStats();

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        if (!this.isInValidBounds(pos)) {
            return null;
        }
        if (!this.isClientSide() && Thread.currentThread() != this.thread) {
            return null;
        }
        return this.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (!this.isInValidBounds(pos)) {
            return;
        }
        this.getChunkAt(pos).addAndRegisterBlockEntity(blockEntity);
    }

    public void removeBlockEntity(BlockPos pos) {
        if (!this.isInValidBounds(pos)) {
            return;
        }
        this.getChunkAt(pos).removeBlockEntity(pos);
    }

    public boolean isLoaded(BlockPos pos) {
        if (!this.isInValidBounds(pos)) {
            return false;
        }
        return this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos pos, Entity entity, Direction faceDirection) {
        if (!this.isInValidBounds(pos)) {
            return false;
        }
        ChunkAccess chunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
        if (chunk == null) {
            return false;
        }
        return chunk.getBlockState(pos).entityCanStandOnFace(this, pos, entity, faceDirection);
    }

    public boolean loadedAndEntityCanStandOn(BlockPos pos, Entity entity) {
        return this.loadedAndEntityCanStandOnFace(pos, entity, Direction.UP);
    }

    public void updateSkyBrightness() {
        this.skyDarken = (int)(15.0f - this.environmentAttributes().getDimensionValue(EnvironmentAttributes.SKY_LIGHT_LEVEL).floatValue());
    }

    public void setSpawnSettings(boolean spawnEnemies) {
        this.getChunkSource().setSpawnSettings(spawnEnemies);
    }

    public abstract void setRespawnData(LevelData.RespawnData var1);

    public abstract LevelData.RespawnData getRespawnData();

    public LevelData.RespawnData getWorldBorderAdjustedRespawnData(LevelData.RespawnData respawnData) {
        WorldBorder worldBorder = this.getWorldBorder();
        if (!worldBorder.isWithinBounds(respawnData.pos())) {
            BlockPos newPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(worldBorder.getCenterX(), 0.0, worldBorder.getCenterZ()));
            return LevelData.RespawnData.of(respawnData.dimension(), newPos, respawnData.yaw(), respawnData.pitch());
        }
        return respawnData;
    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Override
    public @Nullable BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AABB bb, Predicate<? super Entity> selector) {
        Profiler.get().incrementCounter("getEntities");
        ArrayList output = Lists.newArrayList();
        this.getEntities().get(bb, entity -> {
            if (entity != except && selector.test((Entity)entity)) {
                output.add(entity);
            }
        });
        for (EnderDragonPart dragonPart : this.dragonParts()) {
            if (dragonPart == except || dragonPart.parentMob == except || !selector.test(dragonPart) || !bb.intersects(dragonPart.getBoundingBox())) continue;
            output.add(dragonPart);
        }
        return output;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector) {
        ArrayList output = Lists.newArrayList();
        this.getEntities(type, bb, selector, output);
        return output;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector, List<? super T> output) {
        this.getEntities(type, bb, selector, output, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector, List<? super T> output, int maxResults) {
        Profiler.get().incrementCounter("getEntities");
        this.getEntities().get(type, bb, e -> {
            if (selector.test(e)) {
                output.add((Object)e);
                if (output.size() >= maxResults) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }
            if (e instanceof EnderDragon) {
                EnderDragon enderDragon = (EnderDragon)e;
                for (EnderDragonPart subEntity : enderDragon.getSubEntities()) {
                    Entity castSubPart = (Entity)type.tryCast(subEntity);
                    if (castSubPart == null || !selector.test(castSubPart)) continue;
                    output.add((Object)castSubPart);
                    if (output.size() < maxResults) continue;
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
    }

    public <T extends Entity> boolean hasEntities(EntityTypeTest<Entity, T> type, AABB bb, Predicate<? super T> selector) {
        Profiler.get().incrementCounter("hasEntities");
        MutableBoolean hasEntities = new MutableBoolean();
        this.getEntities().get(type, bb, e -> {
            if (selector.test(e)) {
                hasEntities.setTrue();
                return AbortableIterationConsumer.Continuation.ABORT;
            }
            if (e instanceof EnderDragon) {
                EnderDragon enderDragon = (EnderDragon)e;
                for (EnderDragonPart subEntity : enderDragon.getSubEntities()) {
                    Entity castSubPart = (Entity)type.tryCast(subEntity);
                    if (castSubPart == null || !selector.test(castSubPart)) continue;
                    hasEntities.setTrue();
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        return hasEntities.isTrue();
    }

    public List<Entity> getPushableEntities(Entity pusher, AABB boundingBox) {
        return this.getEntities(pusher, boundingBox, EntitySelector.pushableBy(pusher));
    }

    public abstract @Nullable Entity getEntity(int var1);

    public @Nullable Entity getEntity(UUID uuid) {
        return this.getEntities().get(uuid);
    }

    public @Nullable Entity getEntityInAnyDimension(UUID uuid) {
        return this.getEntity(uuid);
    }

    public @Nullable Player getPlayerInAnyDimension(UUID uuid) {
        return this.getPlayerByUUID(uuid);
    }

    public abstract Collection<EnderDragonPart> dragonParts();

    public void blockEntityChanged(BlockPos pos) {
        if (this.hasChunkAt(pos)) {
            this.getChunkAt(pos).markUnsaved();
        }
    }

    public void onBlockEntityAdded(BlockEntity blockEntity) {
    }

    public long getOverworldClockTime() {
        return this.getClockTimeTicks(this.registryAccess().get(WorldClocks.OVERWORLD));
    }

    public long getDefaultClockTime() {
        return this.getClockTimeTicks(this.dimensionType().defaultClock());
    }

    private long getClockTimeTicks(Optional<? extends Holder<WorldClock>> clock) {
        return clock.map(holder -> this.clockManager().getTotalTicks((Holder<WorldClock>)holder)).orElse(0L);
    }

    public boolean mayInteract(Entity entity, BlockPos pos) {
        return true;
    }

    public void broadcastEntityEvent(Entity entity, byte event) {
    }

    public void broadcastDamageEvent(Entity entity, DamageSource source) {
    }

    public void blockEvent(BlockPos pos, Block block, int b0, int b1) {
        this.getBlockState(pos).triggerEvent(this, pos, b0, b1);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public abstract TickRateManager tickRateManager();

    public float getThunderLevel(float a) {
        return Mth.lerp(a, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(a);
    }

    public void setThunderLevel(float thunderLevel) {
        float clampedThunderLevel;
        this.oThunderLevel = clampedThunderLevel = Mth.clamp(thunderLevel, 0.0f, 1.0f);
        this.thunderLevel = clampedThunderLevel;
    }

    public float getRainLevel(float a) {
        return Mth.lerp(a, this.oRainLevel, this.rainLevel);
    }

    public void setRainLevel(float rainLevel) {
        float clampedRainLevel;
        this.oRainLevel = clampedRainLevel = Mth.clamp(rainLevel, 0.0f, 1.0f);
        this.rainLevel = clampedRainLevel;
    }

    public boolean canHaveWeather() {
        return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() && this.dimension() != END;
    }

    public boolean isThundering() {
        return this.canHaveWeather() && (double)this.getThunderLevel(1.0f) > 0.9;
    }

    public boolean isRaining() {
        return this.canHaveWeather() && (double)this.getRainLevel(1.0f) > 0.2;
    }

    public boolean isRainingAt(BlockPos pos) {
        return this.precipitationAt(pos) == Biome.Precipitation.RAIN;
    }

    public Biome.Precipitation precipitationAt(BlockPos pos) {
        if (!this.isRaining()) {
            return Biome.Precipitation.NONE;
        }
        if (!this.canSeeSky(pos)) {
            return Biome.Precipitation.NONE;
        }
        if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return Biome.Precipitation.NONE;
        }
        Biome biome = this.getBiome(pos).value();
        return biome.getPrecipitationAt(pos, this.getSeaLevel());
    }

    public abstract @Nullable MapItemSavedData getMapData(MapId var1);

    public void globalLevelEvent(int type, BlockPos pos, int data) {
    }

    public CrashReportCategory fillReportDetails(CrashReport report) {
        CrashReportCategory category = report.addCategory("Affected level", 1);
        category.setDetail("All players", () -> {
            List<? extends Player> players = this.players();
            return players.size() + " total; " + players.stream().map(Player::debugInfo).collect(Collectors.joining(", "));
        });
        category.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        category.setDetail("Level dimension", () -> this.dimension().identifier().toString());
        category.setDetail("Level time", () -> String.format(Locale.ROOT, "%d game time, %d day time", this.getGameTime(), this.getOverworldClockTime()));
        try {
            this.levelData.fillCrashReportCategory(category, this);
        }
        catch (Throwable t) {
            category.setDetailError("Level Data Unobtainable", t);
        }
        return category;
    }

    public abstract void destroyBlockProgress(int var1, BlockPos var2, int var3);

    public void createFireworks(double x, double y, double z, double xd, double yd, double zd, List<FireworkExplosion> explosions) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateNeighbourForOutputSignal(BlockPos pos, Block changedBlock) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos relativePos = pos.relative(direction);
            if (!this.hasChunkAt(relativePos)) continue;
            BlockState state = this.getBlockState(relativePos);
            if (state.is(Blocks.COMPARATOR)) {
                this.neighborChanged(state, relativePos, changedBlock, null, false);
                continue;
            }
            if (!state.isRedstoneConductor(this, relativePos) || !(state = this.getBlockState(relativePos = relativePos.relative(direction))).is(Blocks.COMPARATOR)) continue;
            this.neighborChanged(state, relativePos, changedBlock, null, false);
        }
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int skyFlashTime) {
    }

    public void sendPacketToServer(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionTypeRegistration.value();
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(pos));
    }

    public abstract RecipeAccess recipeAccess();

    public BlockPos getBlockRandomPos(int xo, int yo, int zo, int yMask) {
        this.randValue = this.randValue * 3 + 1013904223;
        int val = this.randValue >> 2;
        return new BlockPos(xo + (val & 0xF), yo + (val >> 16 & yMask), zo + (val >> 8 & 0xF));
    }

    public boolean noSave() {
        return false;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    protected abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public long nextSubTickCount() {
        return this.subTickCount++;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public DamageSources damageSources() {
        return this.damageSources;
    }

    public abstract ClockManager clockManager();

    @Override
    public abstract EnvironmentAttributeSystem environmentAttributes();

    public abstract PotionBrewing potionBrewing();

    public abstract FuelValues fuelValues();

    public int getClientLeafTintColor(BlockPos pos) {
        return 0;
    }

    public PalettedContainerFactory palettedContainerFactory() {
        return this.palettedContainerFactory;
    }

    public static enum ExplosionInteraction implements StringRepresentable
    {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<ExplosionInteraction> CODEC;
        private final String id;

        private ExplosionInteraction(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        static {
            CODEC = StringRepresentable.fromEnum(ExplosionInteraction::values);
        }
    }
}

