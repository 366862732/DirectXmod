/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftOperatorListService;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public class MinecraftOperatorListServiceImpl
implements MinecraftOperatorListService {
    private final NotificationManager notificationManager;
    private final JsonRpcLogger jsonrpcLogger;

    public MinecraftOperatorListServiceImpl(NotificationManager notificationManager, JsonRpcLogger jsonrpcLogger) {
        this.notificationManager = notificationManager;
        this.jsonrpcLogger = jsonrpcLogger;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public Collection<ServerOpListEntry> getEntries() {
        return this.server().getPlayerList().getOps().getEntries();
    }

    @Override
    public void op(NameAndId nameAndId, Optional<PermissionLevel> permissionLevel, Optional<Boolean> canBypassPlayerLimit, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Op '{}'", nameAndId);
        this.server().getPlayerList().op(nameAndId, permissionLevel.map(LevelBasedPermissionSet::forLevel), canBypassPlayerLimit);
    }

    @Override
    public void op(NameAndId nameAndId, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Op '{}'", nameAndId);
        this.server().getPlayerList().op(nameAndId);
    }

    @Override
    public void deop(NameAndId nameAndId, ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Deop '{}'", nameAndId);
        this.server().getPlayerList().deop(nameAndId);
    }

    @Override
    public void clear(ClientInfo clientInfo) {
        this.jsonrpcLogger.log(clientInfo, "Clear operator list", new Object[0]);
        this.server().getPlayerList().getOps().clear();
    }
}

