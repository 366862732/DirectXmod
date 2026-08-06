/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.util.TaskChainer;
import org.slf4j.Logger;

public class FutureChain
implements TaskChainer,
AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CompletableFuture<?> head = CompletableFuture.completedFuture(null);
    private final Executor executor;
    private volatile boolean closed;

    public FutureChain(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> void append(CompletableFuture<T> preparation, Consumer<T> chainedTask) {
        this.head = ((CompletableFuture)((CompletableFuture)this.head.thenCombine(preparation, (ignored, value) -> value)).thenAcceptAsync(value -> {
            if (!this.closed) {
                chainedTask.accept(value);
            }
        }, this.executor)).exceptionally(t -> {
            RuntimeException c;
            if (t instanceof CompletionException) {
                c = (CompletionException)t;
                t = c.getCause();
            }
            if (t instanceof CancellationException) {
                c = (CancellationException)t;
                throw c;
            }
            LOGGER.error("Chain link failed, continuing to next one", t);
            return null;
        });
    }

    @Override
    public void close() {
        this.closed = true;
    }
}

