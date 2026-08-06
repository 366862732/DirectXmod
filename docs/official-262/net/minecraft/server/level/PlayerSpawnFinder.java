/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlayerSpawnFinder {
    private static final EntityDimensions PLAYER_DIMENSIONS = EntityTypes.PLAYER.getDimensions();
    private static final int ABSOLUTE_MAX_ATTEMPTS = 1024;
    private final ServerLevel level;
    private final BlockPos spawnSuggestion;
    private final int radius;
    private final int candidateCount;
    private final int coprime;
    private final int offset;
    private int nextCandidateIndex;
    private final CompletableFuture<Vec3> finishedFuture = new CompletableFuture();

    private PlayerSpawnFinder(ServerLevel level, BlockPos spawnSuggestion, int radius) {
        this.level = level;
        this.spawnSuggestion = spawnSuggestion;
        this.radius = radius;
        long squareSide = (long)radius * 2L + 1L;
        this.candidateCount = (int)Math.min(1024L, squareSide * squareSide);
        this.coprime = PlayerSpawnFinder.getCoprime(this.candidateCount);
        this.offset = RandomSource.createThreadLocalInstance().nextInt(this.candidateCount);
    }

    public static CompletableFuture<Vec3> findSpawn(ServerLevel level, BlockPos spawnSuggestion) {
        if (level.getServer().getWorldData().getGameType() == GameType.ADVENTURE) {
            return CompletableFuture.completedFuture(PlayerSpawnFinder.fixupSpawnHeight(level, spawnSuggestion));
        }
        int radius = Math.max(0, level.getGameRules().get(GameRules.RESPAWN_RADIUS));
        int distToBorder = Mth.floor(level.getWorldBorder().getDistanceToBorder(spawnSuggestion.getX(), spawnSuggestion.getZ()));
        if (distToBorder < radius) {
            radius = distToBorder;
        }
        if (distToBorder <= 1) {
            radius = 1;
        }
        PlayerSpawnFinder finder = new PlayerSpawnFinder(level, spawnSuggestion, radius);
        finder.scheduleNext();
        return finder.finishedFuture;
    }

    private void scheduleNext() {
        int candidateIndex;
        if ((candidateIndex = this.nextCandidateIndex++) < this.candidateCount) {
            int value = (this.offset + this.coprime * candidateIndex) % this.candidateCount;
            int deltaX = value % (this.radius * 2 + 1);
            int deltaZ = value / (this.radius * 2 + 1);
            int targetX = this.spawnSuggestion.getX() + deltaX - this.radius;
            int targetZ = this.spawnSuggestion.getZ() + deltaZ - this.radius;
            this.scheduleCandidate(targetX, targetZ, candidateIndex, () -> {
                BlockPos spawnPos = PlayerSpawnFinder.getLevelRespawnPos(this.level, targetX, targetZ);
                if (spawnPos != null && PlayerSpawnFinder.noCollisionNoLiquid(this.level, spawnPos)) {
                    return Optional.of(Vec3.atBottomCenterOf(spawnPos));
                }
                return Optional.empty();
            });
        } else {
            this.scheduleCandidate(this.spawnSuggestion.getX(), this.spawnSuggestion.getZ(), candidateIndex, () -> Optional.of(PlayerSpawnFinder.fixupSpawnHeight(this.level, this.spawnSuggestion)));
        }
    }

    private static Vec3 fixupSpawnHeight(CollisionGetter level, BlockPos spawnPos) {
        BlockPos.MutableBlockPos mutablePos = spawnPos.mutable();
        while (!PlayerSpawnFinder.noCollisionNoLiquid(level, mutablePos) && mutablePos.getY() < level.getMaxY()) {
            mutablePos.move(Direction.UP);
        }
        mutablePos.move(Direction.DOWN);
        while (PlayerSpawnFinder.noCollisionNoLiquid(level, mutablePos) && mutablePos.getY() > level.getMinY()) {
            mutablePos.move(Direction.DOWN);
        }
        mutablePos.move(Direction.UP);
        return Vec3.atBottomCenterOf(mutablePos);
    }

    private static boolean noCollisionNoLiquid(CollisionGetter level, BlockPos pos) {
        return level.noCollision(null, PLAYER_DIMENSIONS.makeBoundingBox(Vec3.atBottomCenterOf(pos)), true);
    }

    private static int getCoprime(int possibleOrigins) {
        return possibleOrigins <= 16 ? possibleOrigins - 1 : 17;
    }

    private void scheduleCandidate(int candidateX, int candidateZ, int candidateIndex, Supplier<Optional<Vec3>> candidateChecker) {
        if (this.finishedFuture.isDone()) {
            return;
        }
        int chunkX = SectionPos.blockToSectionCoord(candidateX);
        int chunkZ = SectionPos.blockToSectionCoord(candidateZ);
        this.level.getChunkSource().addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, new ChunkPos(chunkX, chunkZ), 0).whenCompleteAsync((ignored, throwable) -> {
            if (throwable == null) {
                try {
                    Optional spawnPos = (Optional)candidateChecker.get();
                    if (spawnPos.isPresent()) {
                        this.finishedFuture.complete((Vec3)spawnPos.get());
                    } else {
                        this.scheduleNext();
                    }
                }
                catch (Throwable t) {
                    throwable = t;
                }
            }
            if (throwable != null) {
                CrashReport report = CrashReport.forThrowable(throwable, "Searching for spawn");
                CrashReportCategory details = report.addCategory("Spawn Lookup");
                details.setDetail("Origin", this.spawnSuggestion::toString);
                details.setDetail("Radius", () -> Integer.toString(this.radius));
                details.setDetail("Candidate", () -> "[" + candidateX + "," + candidateZ + "]");
                details.setDetail("Progress", () -> candidateIndex + " out of " + this.candidateCount);
                this.finishedFuture.completeExceptionally(new ReportedException(report));
            }
        }, (Executor)this.level.getServer());
    }

    protected static @Nullable BlockPos getLevelRespawnPos(ServerLevel level, int x, int z) {
        int topY;
        boolean caveWorld = level.dimensionType().hasCeiling();
        LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int n = topY = caveWorld ? level.getChunkSource().getGenerator().getSpawnHeight(level) : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 0xF, z & 0xF);
        if (topY < level.getMinY()) {
            return null;
        }
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 0xF, z & 0xF);
        if (surface <= topY && surface > chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 0xF, z & 0xF)) {
            return null;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = topY + 1; y >= level.getMinY(); --y) {
            pos.set(x, y, z);
            BlockState blockState = level.getBlockState(pos);
            if (!blockState.getFluidState().isEmpty()) break;
            if (!Block.isFaceFull(blockState.getCollisionShape(level, pos), Direction.UP)) continue;
            return ((BlockPos)pos.above()).immutable();
        }
        return null;
    }

    public static @Nullable BlockPos getSpawnPosInChunk(ServerLevel level, ChunkPos chunkPos) {
        if (SharedConstants.debugVoidTerrain(chunkPos)) {
            return null;
        }
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); ++x) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); ++z) {
                BlockPos validSpawnPosition = PlayerSpawnFinder.getLevelRespawnPos(level, x, z);
                if (validSpawnPosition == null) continue;
                return validSpawnPosition;
            }
        }
        return null;
    }
}

