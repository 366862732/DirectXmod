/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.methods;

import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class ServerSettingsService {
    public static boolean autosave(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().isAutoSave();
    }

    public static boolean setAutosave(MinecraftApi minecraftApi, boolean enabled, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setAutoSave(enabled, clientInfo);
    }

    public static Difficulty difficulty(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getDifficulty();
    }

    public static Difficulty setDifficulty(MinecraftApi minecraftApi, Difficulty difficulty, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setDifficulty(difficulty, clientInfo);
    }

    public static boolean enforceAllowlist(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().isEnforceWhitelist();
    }

    public static boolean setEnforceAllowlist(MinecraftApi minecraftApi, boolean enforce, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setEnforceWhitelist(enforce, clientInfo);
    }

    public static boolean usingAllowlist(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().isUsingWhitelist();
    }

    public static boolean setUsingAllowlist(MinecraftApi minecraftApi, boolean use, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setUsingWhitelist(use, clientInfo);
    }

    public static int maxPlayers(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getMaxPlayers();
    }

    public static int setMaxPlayers(MinecraftApi minecraftApi, int maxPlayers, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setMaxPlayers(maxPlayers, clientInfo);
    }

    public static int pauseWhenEmpty(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getPauseWhenEmptySeconds();
    }

    public static int setPauseWhenEmpty(MinecraftApi minecraftApi, int emptySeconds, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setPauseWhenEmptySeconds(emptySeconds, clientInfo);
    }

    public static int playerIdleTimeout(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getPlayerIdleTimeout();
    }

    public static int setPlayerIdleTimeout(MinecraftApi minecraftApi, int idleTime, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setPlayerIdleTimeout(idleTime, clientInfo);
    }

    public static boolean allowFlight(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().allowFlight();
    }

    public static boolean setAllowFlight(MinecraftApi minecraftApi, boolean allow, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setAllowFlight(allow, clientInfo);
    }

    public static int spawnProtection(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getSpawnProtectionRadius();
    }

    public static int setSpawnProtection(MinecraftApi minecraftApi, int spawnProtection, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setSpawnProtectionRadius(spawnProtection, clientInfo);
    }

    public static String motd(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getMotd();
    }

    public static String setMotd(MinecraftApi minecraftApi, String motd, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setMotd(motd, clientInfo);
    }

    public static boolean forceGameMode(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().forceGameMode();
    }

    public static boolean setForceGameMode(MinecraftApi minecraftApi, boolean force, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setForceGameMode(force, clientInfo);
    }

    public static GameType gameMode(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getGameMode();
    }

    public static GameType setGameMode(MinecraftApi minecraftApi, GameType gameMode, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setGameMode(gameMode, clientInfo);
    }

    public static int viewDistance(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getViewDistance();
    }

    public static int setViewDistance(MinecraftApi minecraftApi, int viewDistance, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setViewDistance(viewDistance, clientInfo);
    }

    public static int simulationDistance(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getSimulationDistance();
    }

    public static int setSimulationDistance(MinecraftApi minecraftApi, int simulationDistance, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setSimulationDistance(simulationDistance, clientInfo);
    }

    public static boolean acceptTransfers(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().acceptsTransfers();
    }

    public static boolean setAcceptTransfers(MinecraftApi minecraftApi, boolean accept, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setAcceptsTransfers(accept, clientInfo);
    }

    public static int statusHeartbeatInterval(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getStatusHeartbeatInterval();
    }

    public static int setStatusHeartbeatInterval(MinecraftApi minecraftApi, int statusHeartbeatInterval, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setStatusHeartbeatInterval(statusHeartbeatInterval, clientInfo);
    }

    public static PermissionLevel operatorUserPermissionLevel(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getOperatorUserPermissions().level();
    }

    public static PermissionLevel setOperatorUserPermissionLevel(MinecraftApi minecraftApi, PermissionLevel level, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setOperatorUserPermissions(LevelBasedPermissionSet.forLevel(level), clientInfo).level();
    }

    public static boolean hidesOnlinePlayers(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().hidesOnlinePlayers();
    }

    public static boolean setHidesOnlinePlayers(MinecraftApi minecraftApi, boolean hide, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setHidesOnlinePlayers(hide, clientInfo);
    }

    public static boolean repliesToStatus(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().repliesToStatus();
    }

    public static boolean setRepliesToStatus(MinecraftApi minecraftApi, boolean enable, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setRepliesToStatus(enable, clientInfo);
    }

    public static int entityBroadcastRangePercentage(MinecraftApi minecraftApi) {
        return minecraftApi.serverSettingsService().getEntityBroadcastRangePercentage();
    }

    public static int setEntityBroadcastRangePercentage(MinecraftApi minecraftApi, int percentage, ClientInfo clientInfo) {
        return minecraftApi.serverSettingsService().setEntityBroadcastRangePercentage(percentage, clientInfo);
    }
}

