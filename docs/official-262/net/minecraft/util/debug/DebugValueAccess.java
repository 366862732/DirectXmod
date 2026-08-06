/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.debug;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.jspecify.annotations.Nullable;

public interface DebugValueAccess {
    public <T> void forEachChunk(DebugSubscription<T> var1, BiConsumer<ChunkPos, T> var2);

    public <T> @Nullable T getChunkValue(DebugSubscription<T> var1, ChunkPos var2);

    public <T> void forEachBlock(DebugSubscription<T> var1, BiConsumer<BlockPos, T> var2);

    public <T> @Nullable T getBlockValue(DebugSubscription<T> var1, BlockPos var2);

    public <T> void forEachEntity(DebugSubscription<T> var1, BiConsumer<Entity, T> var2);

    public <T> @Nullable T getEntityValue(DebugSubscription<T> var1, Entity var2);

    public <T> void forEachEvent(DebugSubscription<T> var1, EventVisitor<T> var2);

    @FunctionalInterface
    public static interface EventVisitor<T> {
        public void accept(T var1, int var2, int var3);
    }
}

