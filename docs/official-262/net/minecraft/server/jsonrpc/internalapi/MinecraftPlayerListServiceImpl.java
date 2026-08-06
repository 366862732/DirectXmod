/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.jsonrpc.internalapi;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.internalapi.MinecraftPlayerListService;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;

public class MinecraftPlayerListServiceImpl
implements MinecraftPlayerListService {
    private final NotificationManager notificationManager;
    private final JsonRpcLogger jsonRpcLogger;

    public MinecraftPlayerListServiceImpl(NotificationManager notificationManager, JsonRpcLogger jsonRpcLogger) {
        this.notificationManager = notificationManager;
        this.jsonRpcLogger = jsonRpcLogger;
    }

    private DedicatedServer server() {
        return Objects.requireNonNull(this.notificationManager.server());
    }

    @Override
    public List<ServerPlayer> getPlayers() {
        return this.server().getPlayerList().getPlayers();
    }

    @Override
    public @Nullable ServerPlayer getPlayer(UUID uuid) {
        return this.server().getPlayerList().getPlayer(uuid);
    }

    @Override
    public Optional<NameAndId> fetchUserByName(String name) {
        return this.server().services().nameToIdCache().get(name);
    }

    @Override
    public Optional<NameAndId> fetchUserById(UUID id) {
        return Optional.ofNullable(this.server().services().sessionService().fetchProfile(id, true)).map(profile -> new NameAndId(profile.profile()));
    }

    @Override
    public Optional<NameAndId> getCachedUserById(UUID id) {
        return this.server().services().nameToIdCache().get(id);
    }

    @Override
    public Optional<ServerPlayer> getPlayer(Optional<UUID> id, Optional<String> name) {
        if (id.isPresent()) {
            return Optional.ofNullable(this.server().getPlayerList().getPlayer(id.get()));
        }
        if (name.isPresent()) {
            return Optional.ofNullable(this.server().getPlayerList().getPlayerByName(name.get()));
        }
        return Optional.empty();
    }

    @Override
    public List<ServerPlayer> getPlayersWithAddress(String ip) {
        return this.server().getPlayerList().getPlayersWithAddress(ip);
    }

    @Override
    public void remove(ServerPlayer serverPlayer, ClientInfo clientInfo) {
        this.server().getPlayerList().remove(serverPlayer);
        this.jsonRpcLogger.log(clientInfo, "Remove player '{}'", serverPlayer.getPlainTextName());
    }

    @Override
    public @Nullable ServerPlayer getPlayerByName(String name) {
        return this.server().getPlayerList().getPlayerByName(name);
    }
}

