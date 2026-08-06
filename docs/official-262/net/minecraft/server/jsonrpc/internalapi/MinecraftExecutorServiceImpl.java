/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftExecutorService;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftExecutorServiceImpl
implements MinecraftExecutorService {
    private final NotificationManager notificationManager;

    public MinecraftExecutorServiceImpl(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public <V> CompletableFuture<V> submit(Supplier<V> supplier) {
        return this.server().submit(supplier);
    }

    @Override
    public CompletableFuture<Void> submit(Runnable runnable) {
        return this.server().submit(runnable);
    }
}

