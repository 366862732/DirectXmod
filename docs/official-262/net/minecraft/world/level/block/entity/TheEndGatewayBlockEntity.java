/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity
extends TheEndPortalBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    private static final long DEFAULT_AGE = 0L;
    private static final boolean DEFAULT_EXACT_TELEPORT = false;
    private long age = 0L;
    private int teleportCooldown;
    private @Nullable BlockPos exitPortal;
    private boolean exactTeleport = false;

    public TheEndGatewayBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.END_GATEWAY, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Age", this.age);
        output.storeNullable("exit_portal", BlockPos.CODEC, this.exitPortal);
        if (this.exactTeleport) {
            output.putBoolean("ExactTeleport", true);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.age = input.getLongOr("Age", 0L);
        this.exitPortal = input.read("exit_portal", BlockPos.CODEC).filter(Level::isInSpawnableBounds).orElse(null);
        this.exactTeleport = input.getBooleanOr("ExactTeleport", false);
    }

    public static void beamAnimationTick(Level level, BlockPos pos, BlockState state, TheEndGatewayBlockEntity entity) {
        ++entity.age;
        if (entity.isCoolingDown()) {
            --entity.teleportCooldown;
        }
    }

    public static void portalTick(Level level, BlockPos pos, BlockState state, TheEndGatewayBlockEntity entity) {
        boolean spawning = entity.isSpawning();
        boolean coolingDown = entity.isCoolingDown();
        ++entity.age;
        if (coolingDown) {
            --entity.teleportCooldown;
        } else if (entity.age % 2400L == 0L) {
            TheEndGatewayBlockEntity.triggerCooldown(level, pos, state, entity);
        }
        if (spawning != entity.isSpawning() || coolingDown != entity.isCoolingDown()) {
            TheEndGatewayBlockEntity.setChanged(level, pos, state);
        }
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(float a) {
        return Mth.clamp(((float)this.age + a) / 200.0f, 0.0f, 1.0f);
    }

    public float getCooldownPercent(float a) {
        return 1.0f - Mth.clamp(((float)this.teleportCooldown - a) / 40.0f, 0.0f, 1.0f);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public static void triggerCooldown(Level level, BlockPos pos, BlockState blockState, TheEndGatewayBlockEntity entity) {
        if (!level.isClientSide()) {
            entity.teleportCooldown = 40;
            level.blockEvent(pos, blockState.getBlock(), 1, 0);
            TheEndGatewayBlockEntity.setChanged(level, pos, blockState);
        }
    }

    @Override
    public boolean triggerEvent(int b0, int b1) {
        if (b0 == 1) {
            this.teleportCooldown = 40;
            return true;
        }
        return super.triggerEvent(b0, b1);
    }

    public @Nullable Vec3 getPortalPosition(ServerLevel currentLevel, BlockPos portalEntryPos) {
        if (this.exitPortal == null && currentLevel.dimension() == Level.END) {
            BlockPos exitPortalPos = TheEndGatewayBlockEntity.findOrCreateValidTeleportPos(currentLevel, portalEntryPos);
            exitPortalPos = exitPortalPos.above(10);
            LOGGER.debug("Creating portal at {}", (Object)exitPortalPos);
            TheEndGatewayBlockEntity.spawnGatewayPortal(currentLevel, exitPortalPos, EndGatewayConfiguration.knownExit(portalEntryPos, false));
            this.setExitPosition(exitPortalPos, this.exactTeleport);
        }
        if (this.exitPortal != null) {
            BlockPos pos = this.exactTeleport ? this.exitPortal : TheEndGatewayBlockEntity.findExitPosition(currentLevel, this.exitPortal);
            return Vec3.atBottomCenterOf(pos);
        }
        return null;
    }

    private static BlockPos findExitPosition(Level level, BlockPos exitPortal) {
        BlockPos pos = TheEndGatewayBlockEntity.findTallestBlock(level, exitPortal.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", (Object)exitPortal, (Object)pos);
        return pos.above();
    }

    private static BlockPos findOrCreateValidTeleportPos(ServerLevel level, BlockPos endGatewayPos) {
        Vec3 exitPortalXZPosTentative = TheEndGatewayBlockEntity.findExitPortalXZPosTentative(level, endGatewayPos);
        LevelChunk exitPortalChunk = TheEndGatewayBlockEntity.getChunk(level, exitPortalXZPosTentative);
        BlockPos exitPortalPos = TheEndGatewayBlockEntity.findValidSpawnInChunk(exitPortalChunk);
        if (exitPortalPos == null) {
            BlockPos newExitPortalPos = BlockPos.containing(exitPortalXZPosTentative.x + 0.5, 75.0, exitPortalXZPosTentative.z + 0.5);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", (Object)newExitPortalPos);
            level.registryAccess().lookup(Registries.CONFIGURED_FEATURE).flatMap(registry -> registry.get(EndFeatures.END_ISLAND)).ifPresent(endIsland -> ((ConfiguredFeature)endIsland.value()).place(level, level.getChunkSource().getGenerator(), RandomSource.create(newExitPortalPos.asLong()), newExitPortalPos));
            exitPortalPos = newExitPortalPos;
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", (Object)exitPortalPos);
        }
        return TheEndGatewayBlockEntity.findTallestBlock(level, exitPortalPos, 16, true);
    }

    private static Vec3 findExitPortalXZPosTentative(ServerLevel level, BlockPos endGatewayPos) {
        Vec3 teleportXZDirectionVector = new Vec3(endGatewayPos.getX(), 0.0, endGatewayPos.getZ()).normalize();
        int teleportDistance = 1024;
        Vec3 exitPortalXZPosTentative = teleportXZDirectionVector.scale(1024.0);
        int chunkLimit = 16;
        while (!TheEndGatewayBlockEntity.isChunkEmpty(level, exitPortalXZPosTentative) && chunkLimit-- > 0) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", (Object)exitPortalXZPosTentative);
            exitPortalXZPosTentative = exitPortalXZPosTentative.add(teleportXZDirectionVector.scale(-16.0));
        }
        chunkLimit = 16;
        while (TheEndGatewayBlockEntity.isChunkEmpty(level, exitPortalXZPosTentative) && chunkLimit-- > 0) {
            LOGGER.debug("Skipping forward past empty chunk at {}", (Object)exitPortalXZPosTentative);
            exitPortalXZPosTentative = exitPortalXZPosTentative.add(teleportXZDirectionVector.scale(16.0));
        }
        LOGGER.debug("Found chunk at {}", (Object)exitPortalXZPosTentative);
        return exitPortalXZPosTentative;
    }

    private static boolean isChunkEmpty(ServerLevel level, Vec3 xzPos) {
        return TheEndGatewayBlockEntity.getChunk(level, xzPos).getHighestFilledSectionIndex() == -1;
    }

    private static BlockPos findTallestBlock(BlockGetter level, BlockPos around, int dist, boolean allowBedrock) {
        Vec3i tallest = null;
        for (int xd = -dist; xd <= dist; ++xd) {
            block1: for (int zd = -dist; zd <= dist; ++zd) {
                if (xd == 0 && zd == 0 && !allowBedrock) continue;
                for (int y = level.getMaxY(); y > (tallest == null ? level.getMinY() : tallest.getY()); --y) {
                    BlockPos pos = new BlockPos(around.getX() + xd, y, around.getZ() + zd);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isCollisionShapeFullBlock(level, pos) || !allowBedrock && state.is(Blocks.BEDROCK)) continue;
                    tallest = pos;
                    continue block1;
                }
            }
        }
        return tallest == null ? around : tallest;
    }

    private static LevelChunk getChunk(Level level, Vec3 pos) {
        return level.getChunk(Mth.floor(pos.x / 16.0), Mth.floor(pos.z / 16.0));
    }

    private static @Nullable BlockPos findValidSpawnInChunk(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos start = new BlockPos(chunkPos.getMinBlockX(), 30, chunkPos.getMinBlockZ());
        int maxY = chunk.getHighestSectionPosition() + 16 - 1;
        BlockPos end = new BlockPos(chunkPos.getMaxBlockX(), maxY, chunkPos.getMaxBlockZ());
        BlockPos closest = null;
        double closestDist = 0.0;
        for (BlockPos pos : BlockPos.betweenClosed(start, end)) {
            BlockState state = chunk.getBlockState(pos);
            BlockPos above = pos.above();
            BlockPos above2 = pos.above(2);
            if (!state.is(Blocks.END_STONE) || chunk.getBlockState(above).isCollisionShapeFullBlock(chunk, above) || chunk.getBlockState(above2).isCollisionShapeFullBlock(chunk, above2)) continue;
            double dist = pos.distToCenterSqr(0.0, 0.0, 0.0);
            if (closest != null && !(dist < closestDist)) continue;
            closest = pos;
            closestDist = dist;
        }
        return closest;
    }

    private static void spawnGatewayPortal(ServerLevel level, BlockPos portalPos, EndGatewayConfiguration config) {
        Feature.END_GATEWAY.place(config, level, level.getChunkSource().getGenerator(), RandomSource.create(), portalPos);
    }

    @Override
    public boolean shouldRenderFace(Direction direction) {
        return Block.shouldRenderFace(this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(direction)), direction);
    }

    public int getParticleAmount() {
        int count = 0;
        for (Direction direction : Direction.values()) {
            count += this.shouldRenderFace(direction) ? 1 : 0;
        }
        return count;
    }

    public void setExitPosition(BlockPos exactPosition, boolean exact) {
        this.exactTeleport = exact;
        this.exitPortal = exactPosition;
        this.setChanged();
    }
}

