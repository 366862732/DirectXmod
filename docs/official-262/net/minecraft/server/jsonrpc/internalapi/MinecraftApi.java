/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftAllowListService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftAllowListServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftBanListService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftBanListServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftExecutorService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftExecutorServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftGameRuleService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftGameRuleServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftOperatorListService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftOperatorListServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftPlayerListService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftPlayerListServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftServerSettingsService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftServerSettingsServiceImpl;
import net.minecraft.server.jsonrpc.internalapi.MinecraftServerStateService;
import net.minecraft.server.jsonrpc.internalapi.MinecraftServerStateServiceImpl;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftApi {
    private final NotificationManager notificationManager;
    private final MinecraftAllowListService allowListService;
    private final MinecraftBanListService banListService;
    private final MinecraftPlayerListService minecraftPlayerListService;
    private final MinecraftGameRuleService gameRuleService;
    private final MinecraftOperatorListService minecraftOperatorListService;
    private final MinecraftServerSettingsService minecraftServerSettingsService;
    private final MinecraftServerStateService minecraftServerStateService;
    private final MinecraftExecutorService executorService;

    public MinecraftApi(NotificationManager notificationManager, MinecraftAllowListService allowListService, MinecraftBanListService banListService, MinecraftPlayerListService minecraftPlayerListService, MinecraftGameRuleService gameRuleService, MinecraftOperatorListService minecraftOperatorListService, MinecraftServerSettingsService minecraftServerSettingsService, MinecraftServerStateService minecraftServerStateService, MinecraftExecutorService executorService) {
        this.notificationManager = notificationManager;
        this.allowListService = allowListService;
        this.banListService = banListService;
        this.minecraftPlayerListService = minecraftPlayerListService;
        this.gameRuleService = gameRuleService;
        this.minecraftOperatorListService = minecraftOperatorListService;
        this.minecraftServerSettingsService = minecraftServerSettingsService;
        this.minecraftServerStateService = minecraftServerStateService;
        this.executorService = executorService;
    }

    public <V> CompletableFuture<V> submit(Supplier<V> supplier) {
        return this.executorService.submit(supplier);
    }

    public CompletableFuture<Void> submit(Runnable runnable) {
        return this.executorService.submit(runnable);
    }

    public MinecraftAllowListService allowListService() {
        return this.allowListService;
    }

    public MinecraftBanListService banListService() {
        return this.banListService;
    }

    public MinecraftPlayerListService playerListService() {
        return this.minecraftPlayerListService;
    }

    public MinecraftGameRuleService gameRuleService() {
        return this.gameRuleService;
    }

    public MinecraftOperatorListService operatorListService() {
        return this.minecraftOperatorListService;
    }

    public MinecraftServerSettingsService serverSettingsService() {
        return this.minecraftServerSettingsService;
    }

    public MinecraftServerStateService serverStateService() {
        return this.minecraftServerStateService;
    }

    public NotificationManager notificationManager() {
        return this.notificationManager;
    }

    public static MinecraftApi of(NotificationManager notificationManager) {
        JsonRpcLogger jsonrpcLogger = new JsonRpcLogger();
        MinecraftAllowListServiceImpl allowListService = new MinecraftAllowListServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftBanListServiceImpl banListService = new MinecraftBanListServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftPlayerListServiceImpl playerListService = new MinecraftPlayerListServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftGameRuleServiceImpl gameRuleService = new MinecraftGameRuleServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftOperatorListServiceImpl operatorListService = new MinecraftOperatorListServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftServerSettingsServiceImpl serverSettingsService = new MinecraftServerSettingsServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftServerStateServiceImpl serverStateService = new MinecraftServerStateServiceImpl(notificationManager, jsonrpcLogger);
        MinecraftExecutorServiceImpl executorService = new MinecraftExecutorServiceImpl(notificationManager);
        return new MinecraftApi(notificationManager, allowListService, banListService, playerListService, gameRuleService, operatorListService, serverSettingsService, serverStateService, executorService);
    }
}

