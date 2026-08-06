/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ContiguousSet
 *  com.google.common.collect.DiscreteDomain
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Range
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.dimension.end;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockPredicate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class EnderDragonFight
extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_DRAGON_RESPAWN = 1200;
    private static final int TIME_BETWEEN_CRYSTAL_SCANS = 100;
    public static final int TIME_BETWEEN_PLAYER_SCANS = 20;
    private static final int ARENA_SIZE_CHUNKS = 8;
    public static final int ARENA_TICKET_LEVEL = 9;
    private static final int GATEWAY_COUNT = 20;
    private static final int GATEWAY_DISTANCE = 96;
    public static final int DRAGON_SPAWN_Y = 128;
    private static final Component EVENT_DISPLAY_NAME = Component.translatable("entity.minecraft.ender_dragon");
    private Predicate<Entity> validPlayer;
    private ServerBossEvent dragonEvent;
    private ServerLevel level;
    private BlockPos origin;
    private final List<Integer> gateways;
    private final BlockPattern exitPortalPattern = BlockPatternBuilder.start().aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("  ###  ", " #   # ", "#     #", "#  #  #", "#     #", " #   # ", "  ###  ").aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ").where('#', BlockInWorld.hasState(BlockPredicate.forBlock(Blocks.BEDROCK))).build();
    private int ticksSinceDragonSeen;
    private int aliveCrystals;
    private int ticksSinceCrystalsScanned;
    private int ticksSinceLastPlayerScan = 21;
    private boolean dragonKilled;
    private boolean hasPreviouslyKilledDragon;
    private boolean skipArenaLoadedCheck = false;
    private @Nullable UUID dragonUUID;
    private boolean needsStateScanning;
    private @Nullable BlockPos exitPortalLocation;
    private @Nullable DragonRespawnStage respawnStage;
    private int respawnTime;
    private List<EntityReference<EndCrystal>> respawnCrystals;
    public static final Codec<EnderDragonFight> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "needs_state_scanning", true).forGetter(fight -> fight.needsStateScanning), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "dragon_killed", false).forGetter(fight -> fight.dragonKilled), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "previously_killed", false).forGetter(fight -> fight.hasPreviouslyKilledDragon), (App)DragonRespawnStage.CODEC.optionalFieldOf("respawn_stage").forGetter(fight -> Optional.ofNullable(fight.respawnStage)), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.INT, "respawn_time", 0).forGetter(fight -> fight.respawnTime), (App)UUIDUtil.CODEC.lenientOptionalFieldOf("dragon_uuid").forGetter(fight -> Optional.ofNullable(fight.dragonUUID)), (App)BlockPos.CODEC.lenientOptionalFieldOf("exit_portal_location").forGetter(fight -> Optional.ofNullable(fight.exitPortalLocation)), (App)Codec.list((Codec)Codec.INT).lenientOptionalFieldOf("gateways", new ArrayList()).forGetter(fight -> fight.gateways), (App)Codec.list(EntityReference.codec()).optionalFieldOf("respawn_crystals", List.of()).forGetter(fight -> fight.respawnCrystals)).apply((Applicative)i, EnderDragonFight::new));
    public static final SavedDataType<EnderDragonFight> TYPE = new SavedDataType<EnderDragonFight>(Identifier.withDefaultNamespace("ender_dragon_fight"), EnderDragonFight::createDefault, CODEC, DataFixTypes.SAVED_DATA_ENDER_DRAGON_FIGHT);

    public static EnderDragonFight createDefault() {
        return new EnderDragonFight(true, false, false, Optional.empty(), 0, Optional.empty(), Optional.empty(), (List<Integer>)new ObjectArrayList(), List.of());
    }

    public EnderDragonFight(boolean needsStateScanning, boolean dragonKilled, boolean previouslyKilled, Optional<DragonRespawnStage> respawnStage, int respawnTime, Optional<UUID> dragonUUID, Optional<BlockPos> exitPortalLocation, List<Integer> gateways, List<EntityReference<EndCrystal>> respawnCrystals) {
        this.needsStateScanning = needsStateScanning;
        this.dragonUUID = dragonUUID.orElse(null);
        this.dragonKilled = dragonKilled;
        this.hasPreviouslyKilledDragon = previouslyKilled;
        this.respawnStage = respawnStage.orElse(null);
        this.respawnTime = respawnTime;
        this.exitPortalLocation = exitPortalLocation.orElse(null);
        this.gateways = new ObjectArrayList(gateways);
        this.respawnCrystals = respawnCrystals;
    }

    public void init(ServerLevel level, long seed, BlockPos origin) {
        this.level = level;
        this.origin = origin;
        this.dragonEvent = new ServerBossEvent(Mth.createInsecureUUID(level.getRandom()), EVENT_DISPLAY_NAME, BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
        this.dragonEvent.setPlayBossMusic(true).setCreateWorldFog(true);
        this.validPlayer = EntitySelector.ENTITY_STILL_ALIVE.and(EntitySelector.withinDistance(origin.getX(), 128 + origin.getY(), origin.getZ(), 192.0));
        if (this.gateways.isEmpty()) {
            ObjectArrayList newGateways = new ObjectArrayList((Collection)ContiguousSet.create((Range)Range.closedOpen((Comparable)Integer.valueOf(0), (Comparable)Integer.valueOf(20)), (DiscreteDomain)DiscreteDomain.integers()));
            Util.shuffle(newGateways, RandomSource.createThreadLocalInstance(seed));
            this.gateways.addAll((Collection<Integer>)newGateways);
            this.setDirty();
        }
    }

    @Deprecated
    @VisibleForTesting
    public void skipArenaLoadedCheck() {
        this.skipArenaLoadedCheck = true;
    }

    public void tick() {
        this.dragonEvent.setVisible(!this.dragonKilled);
        if (++this.ticksSinceLastPlayerScan >= 20) {
            this.updatePlayers();
            this.ticksSinceLastPlayerScan = 0;
        }
        if (!this.dragonEvent.getPlayers().isEmpty()) {
            this.level.getChunkSource().addTicketWithRadius(TicketType.DRAGON, new ChunkPos(0, 0), 9);
            if (!this.isArenaLoaded()) {
                return;
            }
            if (this.needsStateScanning) {
                this.scanState();
                this.needsStateScanning = false;
                this.setDirty();
            }
            if (this.respawnStage != null) {
                List<EndCrystal> respawnCrystals = this.respawnCrystals.stream().map(e -> e.getEntity(this.level, EndCrystal.class)).filter(Objects::nonNull).toList();
                if (respawnCrystals.isEmpty()) {
                    this.abortRespawnSequence();
                    return;
                }
                this.respawnStage.tick(this.level, this, respawnCrystals, this.respawnTime++);
                this.setDirty();
            }
            if (!this.dragonKilled) {
                if (this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) {
                    this.findOrCreateDragon();
                    this.ticksSinceDragonSeen = 0;
                }
                if (++this.ticksSinceCrystalsScanned >= 100) {
                    this.updateCrystalCount();
                    this.ticksSinceCrystalsScanned = 0;
                }
            }
        } else {
            this.level.getChunkSource().removeTicketWithRadius(TicketType.DRAGON, new ChunkPos(0, 0), 9);
        }
    }

    private void scanState() {
        LOGGER.info("Scanning for legacy world dragon fight...");
        boolean activePortalExists = this.hasActiveExitPortal();
        if (activePortalExists) {
            LOGGER.info("Found that the dragon has been killed in this world already.");
            this.hasPreviouslyKilledDragon = true;
        } else {
            LOGGER.info("Found that the dragon has not yet been killed in this world.");
            this.hasPreviouslyKilledDragon = false;
            if (this.findExitPortal() == null) {
                this.spawnExitPortal(false);
            }
        }
        List<? extends EnderDragon> entities = this.level.getDragons();
        if (entities.isEmpty()) {
            this.dragonKilled = true;
        } else {
            EnderDragon dragon = entities.get(0);
            this.dragonUUID = dragon.getUUID();
            LOGGER.info("Found that there's a dragon still alive ({})", (Object)dragon);
            this.dragonKilled = false;
            if (!activePortalExists) {
                LOGGER.info("But we didn't have a portal, let's remove it.");
                dragon.discard();
                this.dragonUUID = null;
            }
        }
        if (!this.hasPreviouslyKilledDragon && this.dragonKilled) {
            this.dragonKilled = false;
        }
        this.setDirty();
    }

    private void findOrCreateDragon() {
        List<? extends EnderDragon> entities = this.level.getDragons();
        if (entities.isEmpty()) {
            LOGGER.debug("Haven't seen the dragon, respawning it");
            this.createNewDragon();
        } else {
            LOGGER.debug("Haven't seen our dragon, but found another one to use.");
            this.dragonUUID = entities.get(0).getUUID();
            this.setDirty();
        }
    }

    protected void setRespawnStage(DragonRespawnStage stage) {
        if (this.respawnStage == null) {
            throw new IllegalStateException("Dragon respawn isn't in progress, can't skip ahead in the animation.");
        }
        this.respawnTime = 0;
        if (stage == DragonRespawnStage.END) {
            this.respawnStage = null;
            this.dragonKilled = false;
            EnderDragon dragon = this.createNewDragon();
            if (dragon != null) {
                for (ServerPlayer player : this.dragonEvent.getPlayers()) {
                    CriteriaTriggers.SUMMONED_ENTITY.trigger(player, dragon);
                }
            }
        } else {
            this.respawnStage = stage;
        }
        this.setDirty();
    }

    private boolean hasActiveExitPortal() {
        for (int x = -8; x <= 8; ++x) {
            for (int z = -8; z <= 8; ++z) {
                LevelChunk chunk = this.level.getChunk(x, z);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof TheEndPortalBlockEntity)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private @Nullable BlockPattern.BlockPatternMatch findExitPortal() {
        int maxY;
        ChunkPos chunkOrigin = ChunkPos.containing(this.origin);
        for (int x = -8 + chunkOrigin.x(); x <= 8 + chunkOrigin.x(); ++x) {
            for (int z = -8 + chunkOrigin.z(); z <= 8 + chunkOrigin.z(); ++z) {
                LevelChunk chunk = this.level.getChunk(x, z);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPattern.BlockPatternMatch match;
                    if (!(blockEntity instanceof TheEndPortalBlockEntity) || (match = this.exitPortalPattern.find(this.level, blockEntity.getBlockPos())) == null) continue;
                    BlockPos posInWorld = match.getBlock(3, 3, 3).getPos();
                    if (this.exitPortalLocation == null) {
                        this.exitPortalLocation = posInWorld;
                        this.setDirty();
                    }
                    return match;
                }
            }
        }
        BlockPos endPodiumLocation = EndPodiumFeature.getLocation(this.origin);
        for (int y = maxY = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, endPodiumLocation).getY(); y >= this.level.getMinY(); --y) {
            BlockPattern.BlockPatternMatch match = this.exitPortalPattern.find(this.level, new BlockPos(endPodiumLocation.getX(), y, endPodiumLocation.getZ()));
            if (match == null) continue;
            if (this.exitPortalLocation == null) {
                this.exitPortalLocation = match.getBlock(3, 3, 3).getPos();
                this.setDirty();
            }
            return match;
        }
        return null;
    }

    private boolean isArenaLoaded() {
        if (this.skipArenaLoadedCheck) {
            return true;
        }
        ChunkPos chunkOrigin = ChunkPos.containing(this.origin);
        for (int x = -8 + chunkOrigin.x(); x <= 8 + chunkOrigin.x(); ++x) {
            for (int z = 8 + chunkOrigin.z(); z <= 8 + chunkOrigin.z(); ++z) {
                ChunkAccess chunk = this.level.getChunk(x, z, ChunkStatus.FULL, false);
                if (!(chunk instanceof LevelChunk)) {
                    return false;
                }
                LevelChunk levelChunk = (LevelChunk)chunk;
                FullChunkStatus status = levelChunk.getFullStatus();
                if (status.isOrAfter(FullChunkStatus.BLOCK_TICKING)) continue;
                return false;
            }
        }
        return ChunkPos.rangeClosed(chunkOrigin, 1).allMatch(pos -> this.level.areEntitiesLoaded(pos.pack()));
    }

    private void updatePlayers() {
        HashSet newPlayers = Sets.newHashSet();
        for (ServerPlayer player : this.level.getPlayers(this.validPlayer)) {
            this.dragonEvent.addPlayer(player);
            newPlayers.add(player);
        }
        HashSet toRemove = Sets.newHashSet(this.dragonEvent.getPlayers());
        toRemove.removeAll(newPlayers);
        for (ServerPlayer player : toRemove) {
            this.dragonEvent.removePlayer(player);
        }
    }

    private void updateCrystalCount() {
        this.ticksSinceCrystalsScanned = 0;
        this.aliveCrystals = 0;
        for (EndSpikeFeature.EndSpike spike : EndSpikeFeature.getSpikesForLevel(this.level)) {
            this.aliveCrystals += this.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox()).size();
        }
        LOGGER.debug("Found {} end crystals still alive", (Object)this.aliveCrystals);
    }

    public void setDragonKilled(EnderDragon dragon) {
        if (dragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(0.0f);
            this.dragonEvent.setVisible(false);
            this.spawnExitPortal(true);
            this.spawnNewGateway();
            if (!this.hasPreviouslyKilledDragon) {
                this.level.setBlockAndUpdate(this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(this.origin)), Blocks.DRAGON_EGG.defaultBlockState());
            }
            this.hasPreviouslyKilledDragon = true;
            this.dragonKilled = true;
            this.setDirty();
        }
    }

    @Deprecated
    @VisibleForTesting
    public void removeAllGateways() {
        this.gateways.clear();
        this.setDirty();
    }

    private void spawnNewGateway() {
        if (this.gateways.isEmpty()) {
            return;
        }
        int gateway = this.gateways.remove(this.gateways.size() - 1);
        int x = Mth.floor(96.0 * Math.cos(2.0 * (-Math.PI + 0.15707963267948966 * (double)gateway)));
        int z = Mth.floor(96.0 * Math.sin(2.0 * (-Math.PI + 0.15707963267948966 * (double)gateway)));
        this.spawnNewGateway(new BlockPos(x, 75, z));
        this.setDirty();
    }

    private void spawnNewGateway(BlockPos pos) {
        this.level.levelEvent(3000, pos, 0);
        this.level.registryAccess().lookup(Registries.CONFIGURED_FEATURE).flatMap(registry -> registry.get(EndFeatures.END_GATEWAY_DELAYED)).ifPresent(endGateway -> ((ConfiguredFeature)endGateway.value()).place(this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), pos));
    }

    private void spawnExitPortal(boolean activated) {
        EndPodiumFeature feature = new EndPodiumFeature(activated);
        if (this.exitPortalLocation == null) {
            this.exitPortalLocation = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.origin)).below();
            while (this.level.getBlockState(this.exitPortalLocation).is(Blocks.BEDROCK) && this.exitPortalLocation.getY() > 63) {
                this.exitPortalLocation = this.exitPortalLocation.below();
            }
            this.exitPortalLocation = this.exitPortalLocation.atY(Math.max(this.level.getMinY() + 1, this.exitPortalLocation.getY()));
            this.setDirty();
        }
        if (feature.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), this.exitPortalLocation)) {
            int chunkRadius = Mth.positiveCeilDiv(4, 16);
            this.level.getChunkSource().chunkMap.waitForLightBeforeSending(ChunkPos.containing(this.exitPortalLocation), chunkRadius);
        }
    }

    private @Nullable EnderDragon createNewDragon() {
        this.level.getChunkAt(new BlockPos(this.origin.getX(), 128 + this.origin.getY(), this.origin.getZ()));
        EnderDragon dragon = EntityTypes.ENDER_DRAGON.create((Level)this.level, EntitySpawnReason.EVENT);
        if (dragon != null) {
            dragon.setDragonFight(this);
            dragon.setFightOrigin(this.origin);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            dragon.snapTo(this.origin.getX(), 128 + this.origin.getY(), this.origin.getZ(), this.level.getRandom().nextFloat() * 360.0f, 0.0f);
            this.level.addFreshEntity(dragon);
            this.dragonUUID = dragon.getUUID();
            this.setDirty();
        }
        return dragon;
    }

    public void updateDragon(EnderDragon dragon) {
        if (dragon.getUUID().equals(this.dragonUUID)) {
            this.dragonEvent.setProgress(dragon.getHealth() / dragon.getMaxHealth());
            this.ticksSinceDragonSeen = 0;
            if (dragon.hasCustomName()) {
                this.dragonEvent.setName(dragon.getDisplayName());
            }
        }
    }

    public int aliveCrystals() {
        return this.aliveCrystals;
    }

    public void onCrystalDestroyed(EndCrystal crystal, DamageSource source) {
        if (this.respawnStage != null && this.respawnCrystals.stream().anyMatch(ref -> ref.matches(crystal))) {
            this.abortRespawnSequence();
        } else {
            this.updateCrystalCount();
            Entity dragon = this.level.getEntity(this.dragonUUID);
            if (dragon instanceof EnderDragon) {
                EnderDragon actuallyDragon = (EnderDragon)dragon;
                actuallyDragon.onCrystalDestroyed(this.level, crystal, crystal.blockPosition(), source);
            }
        }
    }

    private void abortRespawnSequence() {
        LOGGER.debug("Aborting respawn sequence");
        this.respawnStage = null;
        this.respawnTime = 0;
        this.resetSpikeCrystals();
        this.spawnExitPortal(true);
        this.setDirty();
    }

    public boolean hasPreviouslyKilledDragon() {
        return this.hasPreviouslyKilledDragon;
    }

    public void tryRespawn() {
        if (this.dragonKilled && this.respawnStage == null) {
            BlockPos location = this.exitPortalLocation;
            if (location == null) {
                LOGGER.debug("Tried to respawn, but need to find the portal first.");
                BlockPattern.BlockPatternMatch match = this.findExitPortal();
                if (match == null) {
                    LOGGER.debug("Couldn't find a portal, so we made one.");
                    this.spawnExitPortal(true);
                } else {
                    LOGGER.debug("Found the exit portal & saved its location for next time.");
                }
                location = this.exitPortalLocation;
            }
            ArrayList crystals = Lists.newArrayList();
            BlockPos center = location.above(1);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                List<EndCrystal> found = this.level.getEntitiesOfClass(EndCrystal.class, new AABB(center.relative(direction, 3)));
                if (found.isEmpty()) {
                    return;
                }
                crystals.addAll(found);
            }
            LOGGER.debug("Found all crystals, respawning dragon.");
            this.respawnDragon(crystals);
        }
    }

    private void respawnDragon(List<EndCrystal> crystals) {
        if (this.dragonKilled && this.respawnStage == null) {
            BlockPattern.BlockPatternMatch portal = this.findExitPortal();
            while (portal != null) {
                for (int x = 0; x < this.exitPortalPattern.getWidth(); ++x) {
                    for (int y = 0; y < this.exitPortalPattern.getHeight(); ++y) {
                        for (int z = 0; z < this.exitPortalPattern.getDepth(); ++z) {
                            BlockInWorld block = portal.getBlock(x, y, z);
                            if (!block.getState().is(Blocks.BEDROCK) && !block.getState().is(Blocks.END_PORTAL)) continue;
                            this.level.setBlockAndUpdate(block.getPos(), Blocks.END_STONE.defaultBlockState());
                        }
                    }
                }
                portal = this.findExitPortal();
            }
            this.respawnStage = DragonRespawnStage.START;
            this.respawnTime = 0;
            this.spawnExitPortal(false);
            this.respawnCrystals = crystals.stream().map(EntityReference::of).toList();
            this.setDirty();
        }
    }

    public void resetSpikeCrystals() {
        for (EndSpikeFeature.EndSpike spike : EndSpikeFeature.getSpikesForLevel(this.level)) {
            List<EndCrystal> spikeCrystals = this.level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox());
            for (EndCrystal crystal : spikeCrystals) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }
    }

    public @Nullable UUID dragonUUID() {
        return this.dragonUUID;
    }
}

