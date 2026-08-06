/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.level.progress;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.level.progress.LevelLoadProgressTracker;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class LoggingLevelLoadListener
implements LevelLoadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final boolean includePlayerChunks;
    private final LevelLoadProgressTracker progressTracker;
    private boolean closed;
    private long startTime = Long.MAX_VALUE;
    private long nextLogTime = Long.MAX_VALUE;

    public LoggingLevelLoadListener(boolean includePlayerChunks) {
        this.includePlayerChunks = includePlayerChunks;
        this.progressTracker = new LevelLoadProgressTracker(includePlayerChunks);
    }

    public static LoggingLevelLoadListener forDedicatedServer() {
        return new LoggingLevelLoadListener(false);
    }

    public static LoggingLevelLoadListener forSingleplayer() {
        return new LoggingLevelLoadListener(true);
    }

    @Override
    public void start(LevelLoadListener.Stage stage, int totalChunks) {
        if (this.closed) {
            return;
        }
        if (this.startTime == Long.MAX_VALUE) {
            long now;
            this.startTime = now = Util.getMillis();
            this.nextLogTime = now;
        }
        this.progressTracker.start(stage, totalChunks);
        switch (stage) {
            case PREPARE_GLOBAL_SPAWN: {
                LOGGER.info("Selecting global world spawn...");
                break;
            }
            case LOAD_INITIAL_CHUNKS: {
                LOGGER.info("Loading {} persistent chunks...", (Object)totalChunks);
                break;
            }
            case LOAD_PLAYER_CHUNKS: {
                LOGGER.info("Loading {} chunks for player spawn...", (Object)totalChunks);
            }
        }
    }

    @Override
    public void update(LevelLoadListener.Stage stage, int currentChunks, int totalChunks) {
        if (this.closed) {
            return;
        }
        this.progressTracker.update(stage, currentChunks, totalChunks);
        if (Util.getMillis() > this.nextLogTime) {
            this.nextLogTime += 500L;
            int percent = Mth.floor(this.progressTracker.get() * 100.0f);
            LOGGER.info(Component.translatable("menu.preparingSpawn", percent).getString());
        }
    }

    @Override
    public void finish(LevelLoadListener.Stage stage) {
        LevelLoadListener.Stage finalStage;
        if (this.closed) {
            return;
        }
        this.progressTracker.finish(stage);
        LevelLoadListener.Stage stage2 = finalStage = this.includePlayerChunks ? LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS : LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS;
        if (stage == finalStage) {
            LOGGER.info("Time elapsed: {} ms", (Object)(Util.getMillis() - this.startTime));
            this.nextLogTime = Long.MAX_VALUE;
            this.closed = true;
        }
    }

    @Override
    public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
    }
}

