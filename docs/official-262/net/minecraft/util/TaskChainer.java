/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;

@FunctionalInterface
public interface TaskChainer {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static TaskChainer immediate(final Executor executor) {
        return new TaskChainer(){

            @Override
            public <T> void append(CompletableFuture<T> preparation, Consumer<T> chainedTask) {
                ((CompletableFuture)preparation.thenAcceptAsync((Consumer)chainedTask, executor)).exceptionally(e -> {
                    LOGGER.error("Task failed", e);
                    return null;
                });
            }
        };
    }

    default public void append(Runnable task) {
        this.append(CompletableFuture.completedFuture(null), ignored -> task.run());
    }

    public <T> void append(CompletableFuture<T> var1, Consumer<T> var2);
}

