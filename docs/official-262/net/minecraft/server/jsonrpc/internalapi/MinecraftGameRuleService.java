/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.stream.Stream;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.world.level.gamerules.GameRule;

public interface MinecraftGameRuleService {
    public <T> GameRulesService.GameRuleUpdate<T> updateGameRule(GameRulesService.GameRuleUpdate<T> var1, ClientInfo var2);

    public <T> T getRuleValue(GameRule<T> var1);

    public <T> GameRulesService.GameRuleUpdate<T> getTypedRule(GameRule<T> var1, T var2);

    public Stream<GameRule<?>> getAvailableGameRules();
}

