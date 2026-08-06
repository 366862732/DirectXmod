/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftGameRuleService;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

public class MinecraftGameRuleServiceImpl
implements MinecraftGameRuleService {
    private final NotificationManager notificationManager;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftGameRuleServiceImpl(NotificationManager notificationManager, JsonRpcLogger jsonrpcLogger) {
        this.notificationManager = notificationManager;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> update, ClientInfo clientInfo) {
        GameRule<T> gameRule = update.gameRule();
        DedicatedServer server = this.server();
        GameRules gameRules = server.getGameRules();
        T oldValue = gameRules.get(gameRule);
        T newValue = update.value();
        gameRules.set(gameRule, newValue, server);
        this.jsonrpcLogger.log(clientInfo, "Game rule '{}' updated from '{}' to '{}'", gameRule.id(), gameRule.serialize(oldValue), gameRule.serialize(newValue));
        return update;
    }

    @Override
    public <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> gameRule, T value) {
        return new GameRulesService.GameRuleUpdate<T>(gameRule, value);
    }

    @Override
    public Stream<GameRule<?>> getAvailableGameRules() {
        return this.server().getGameRules().availableRules();
    }

    @Override
    public <T> T getRuleValue(GameRule<T> gameRule) {
        return this.server().getGameRules().get(gameRule);
    }
}

