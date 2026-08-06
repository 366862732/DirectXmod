/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.debug.DebugSubscription;

public class ServerDebugSubscribers {
    private final MinecraftServer server;
    private final Map<DebugSubscription<?>, List<ServerPlayer>> enabledSubscriptions = new HashMap();

    public ServerDebugSubscribers(MinecraftServer server) {
        this.server = server;
    }

    private List<ServerPlayer> getSubscribersFor(DebugSubscription<?> subscription) {
        return this.enabledSubscriptions.getOrDefault(subscription, List.of());
    }

    public void tick() {
        this.enabledSubscriptions.values().forEach(List::clear);
        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            for (DebugSubscription<?> subscription : player.debugSubscriptions()) {
                this.enabledSubscriptions.computeIfAbsent(subscription, s -> new ArrayList()).add(player);
            }
        }
        this.enabledSubscriptions.values().removeIf(List::isEmpty);
    }

    public void broadcastToAll(DebugSubscription<?> subscription, Packet<?> packet) {
        for (ServerPlayer player : this.getSubscribersFor(subscription)) {
            player.connection.send(packet);
        }
    }

    public Set<DebugSubscription<?>> enabledSubscriptions() {
        return Set.copyOf(this.enabledSubscriptions.keySet());
    }

    public boolean hasAnySubscriberFor(DebugSubscription<?> subscription) {
        return !this.getSubscribersFor(subscription).isEmpty();
    }

    public boolean hasRequiredPermissions(ServerPlayer player) {
        NameAndId nameAndId = player.nameAndId();
        if (SharedConstants.IS_RUNNING_IN_IDE && this.server.isSingleplayerOwner(nameAndId)) {
            return true;
        }
        return this.server.getPlayerList().isOp(nameAndId);
    }
}

