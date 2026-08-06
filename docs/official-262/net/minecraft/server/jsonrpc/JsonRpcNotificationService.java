/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc;

import net.minecraft.core.Holder;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.jsonrpc.OutgoingRpcMethod;
import net.minecraft.server.jsonrpc.OutgoingRpcMethods;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.methods.BanlistService;
import net.minecraft.server.jsonrpc.methods.GameRulesService;
import net.minecraft.server.jsonrpc.methods.IpBanlistService;
import net.minecraft.server.jsonrpc.methods.OperatorService;
import net.minecraft.server.jsonrpc.methods.ServerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class JsonRpcNotificationService
implements NotificationService {
    private final ManagementServer managementServer;
    private final MinecraftApi minecraftApi;

    public JsonRpcNotificationService(MinecraftApi minecraftApi, ManagementServer managementServer) {
        this.minecraftApi = minecraftApi;
        this.managementServer = managementServer;
    }

    @Override
    public void playerJoined(ServerPlayer player) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_JOINED, PlayerDto.from(player));
    }

    @Override
    public void playerLeft(ServerPlayer player) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_LEFT, PlayerDto.from(player));
    }

    @Override
    public void serverStarted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_STARTED);
    }

    @Override
    public void serverShuttingDown() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SHUTTING_DOWN);
    }

    @Override
    public void serverSaveStarted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_STARTED);
    }

    @Override
    public void serverSaveCompleted() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_SAVE_COMPLETED);
    }

    @Override
    public void serverActivityOccured() {
        this.broadcastNotification(OutgoingRpcMethods.SERVER_ACTIVITY_OCCURRED);
    }

    @Override
    public void playerOped(ServerOpListEntry operator) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_OPED, OperatorService.OperatorDto.from(operator));
    }

    @Override
    public void playerDeoped(ServerOpListEntry operator) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_DEOPED, OperatorService.OperatorDto.from(operator));
    }

    @Override
    public void playerAddedToAllowlist(NameAndId player) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_ADDED_TO_ALLOWLIST, PlayerDto.from(player));
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId player) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_REMOVED_FROM_ALLOWLIST, PlayerDto.from(player));
    }

    @Override
    public void ipBanned(IpBanListEntry ban) {
        this.broadcastNotification(OutgoingRpcMethods.IP_BANNED, IpBanlistService.IpBanDto.from(ban));
    }

    @Override
    public void ipUnbanned(String ip) {
        this.broadcastNotification(OutgoingRpcMethods.IP_UNBANNED, ip);
    }

    @Override
    public void playerBanned(UserBanListEntry ban) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_BANNED, BanlistService.UserBanDto.from(ban));
    }

    @Override
    public void playerUnbanned(NameAndId player) {
        this.broadcastNotification(OutgoingRpcMethods.PLAYER_UNBANNED, PlayerDto.from(player));
    }

    @Override
    public <T> void onGameRuleChanged(GameRule<T> gameRule, T value) {
        this.broadcastNotification(OutgoingRpcMethods.GAMERULE_CHANGED, GameRulesService.getTypedRule(this.minecraftApi, gameRule, value));
    }

    @Override
    public void statusHeartbeat() {
        this.broadcastNotification(OutgoingRpcMethods.STATUS_HEARTBEAT, ServerStateService.status(this.minecraftApi));
    }

    private void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Void, ?>> method) {
        this.managementServer.forEachConnection(connection -> connection.sendNotification(method));
    }

    private <Params> void broadcastNotification(Holder.Reference<? extends OutgoingRpcMethod<Params, ?>> method, Params params) {
        this.managementServer.forEachConnection(connection -> connection.sendNotification(method, params));
    }
}

