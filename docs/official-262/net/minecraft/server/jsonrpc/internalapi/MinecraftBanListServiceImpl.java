/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftBanListService;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public class MinecraftBanListServiceImpl
implements MinecraftBanListService {
    private final NotificationManager notificationManager;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftBanListServiceImpl(NotificationManager notificationManager, JsonRpcLogger jsonrpcLogger) {
        this.notificationManager = notificationManager;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public void addUserBan(UserBanListEntry ban, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Add player '{}' to banlist. Reason: '{}'", ban.getDisplayName(), ban.getReasonMessage().getString());
        this.server().getPlayerList().getBans().add(ban);
    }

    @Override
    public void removeUserBan(NameAndId nameAndId, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Remove player '{}' from banlist", nameAndId);
        this.server().getPlayerList().getBans().remove(nameAndId);
    }

    @Override
    public void clearUserBans(ClientInfo clientInfo) {
        this.server().getPlayerList().getBans().clear();
    }

    @Override
    public Collection<UserBanListEntry> getUserBanEntries() {
        return this.server().getPlayerList().getBans().getEntries();
    }

    @Override
    public Collection<IpBanListEntry> getIpBanEntries() {
        return this.server().getPlayerList().getIpBans().getEntries();
    }

    @Override
    public void addIpBan(IpBanListEntry ipBanEntry, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Add ip '{}' to ban list", ipBanEntry.getUser());
        this.server().getPlayerList().getIpBans().add(ipBanEntry);
    }

    @Override
    public void clearIpBans(ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Clear ip ban list", new Object[0]);
        this.server().getPlayerList().getIpBans().clear();
    }

    @Override
    public void removeIpBan(String ip, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Remove ip '{}' from ban list", ip);
        this.server().getPlayerList().getIpBans().remove(ip);
    }
}

