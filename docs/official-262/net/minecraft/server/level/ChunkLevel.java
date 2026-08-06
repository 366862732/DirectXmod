/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class ChunkLevel {
    private static final int FULL_CHUNK_LEVEL = 33;
    private static final int BLOCK_TICKING_LEVEL = 32;
    private static final int ENTITY_TICKING_LEVEL = 31;
    private static final ChunkStep FULL_CHUNK_STEP = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL);
    public static final int RADIUS_AROUND_FULL_CHUNK = FULL_CHUNK_STEP.accumulatedDependencies().getRadius();
    public static final int MAX_LEVEL = 33 + RADIUS_AROUND_FULL_CHUNK;

    public static @Nullable ChunkStatus generationStatus(int level) {
        return ChunkLevel.getStatusAroundFullChunk(level - 33, null);
    }

    @Contract(value="_,!null->!null;_,_->_")
    public static @Nullable ChunkStatus getStatusAroundFullChunk(int distanceToFullChunk, @Nullable ChunkStatus defaultValue) {
        if (distanceToFullChunk > RADIUS_AROUND_FULL_CHUNK) {
            return defaultValue;
        }
        if (distanceToFullChunk <= 0) {
            return ChunkStatus.FULL;
        }
        return FULL_CHUNK_STEP.accumulatedDependencies().get(distanceToFullChunk);
    }

    public static ChunkStatus getStatusAroundFullChunk(int distanceToFullChunk) {
        return ChunkLevel.getStatusAroundFullChunk(distanceToFullChunk, ChunkStatus.EMPTY);
    }

    public static int byStatus(ChunkStatus status) {
        return 33 + FULL_CHUNK_STEP.getAccumulatedRadiusOf(status);
    }

    public static FullChunkStatus fullStatus(int level) {
        if (level <= 31) {
            return FullChunkStatus.ENTITY_TICKING;
        }
        if (level <= 32) {
            return FullChunkStatus.BLOCK_TICKING;
        }
        if (level <= 33) {
            return FullChunkStatus.FULL;
        }
        return FullChunkStatus.INACCESSIBLE;
    }

    public static int byStatus(FullChunkStatus status) {
        return switch (status) {
            default -> throw new MatchException(null, null);
            case FullChunkStatus.INACCESSIBLE -> MAX_LEVEL;
            case FullChunkStatus.FULL -> 33;
            case FullChunkStatus.BLOCK_TICKING -> 32;
            case FullChunkStatus.ENTITY_TICKING -> 31;
        };
    }

    public static boolean isEntityTicking(int level) {
        return level <= 31;
    }

    public static boolean isBlockTicking(int level) {
        return level <= 32;
    }

    public static boolean isLoaded(int level) {
        return level <= MAX_LEVEL;
    }
}

