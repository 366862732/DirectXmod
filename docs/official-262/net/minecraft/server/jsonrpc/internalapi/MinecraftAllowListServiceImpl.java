/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftAllowListService;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public class MinecraftAllowListServiceImpl
implements MinecraftAllowListService {
    private final NotificationManager notificationManager;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftAllowListServiceImpl(NotificationManager notificationManager, JsonRpcLogger jsonrpcLogger) {
        this.notificationManager = notificationManager;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public Collection<UserWhiteListEntry> getEntries() {
        return this.server().getPlayerList().getWhiteList().getEntries();
    }

    @Override
    public boolean add(UserWhiteListEntry infos, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Add player '{}' to allowlist", infos.getUser());
        return this.server().getPlayerList().getWhiteList().add(infos);
    }

    @Override
    public void clear(ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Clear allowlist", new Object[0]);
        this.server().getPlayerList().getWhiteList().clear();
    }

    @Override
    public void remove(NameAndId nameAndId, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Remove player '{}' from allowlist", nameAndId);
        this.server().getPlayerList().getWhiteList().remove(nameAndId);
    }

    @Override
    public void kickUnlistedPlayers(ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Kick unlisted players", new Object[0]);
        this.server().kickUnlistedPlayers();
    }
}

