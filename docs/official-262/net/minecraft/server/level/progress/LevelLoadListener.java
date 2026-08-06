/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level.progress;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public interface LevelLoadListener {
    public static LevelLoadListener compose(final LevelLoadListener first, final LevelLoadListener second) {
        return new LevelLoadListener(){

            @Override
            public void start(Stage stage, int totalChunks) {
                first.start(stage, totalChunks);
                second.start(stage, totalChunks);
            }

            @Override
            public void update(Stage stage, int currentChunks, int totalChunks) {
                first.update(stage, currentChunks, totalChunks);
                second.update(stage, currentChunks, totalChunks);
            }

            @Override
            public void finish(Stage stage) {
                first.finish(stage);
                second.finish(stage);
            }

            @Override
            public void updateFocus(ResourceKey<Level> dimension, ChunkPos chunkPos) {
                first.updateFocus(dimension, chunkPos);
                second.updateFocus(dimension, chunkPos);
            }
        };
    }

    public void start(Stage var1, int var2);

    public void update(Stage var1, int var2, int var3);

    public void finish(Stage var1);

    public void updateFocus(ResourceKey<Level> var1, ChunkPos var2);

    public static enum Stage {
        START_SERVER,
        PREPARE_GLOBAL_SPAWN,
        LOAD_INITIAL_CHUNKS,
        LOAD_PLAYER_CHUNKS;

    }
}

