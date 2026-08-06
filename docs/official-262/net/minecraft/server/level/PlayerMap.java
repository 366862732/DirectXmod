/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2BooleanMap
 *  it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
 */
package net.minecraft.server.level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerMap {
    private final Object2BooleanMap<ServerPlayer> players = new Object2BooleanOpenHashMap();

    public Set<ServerPlayer> getAllPlayers() {
        return this.players.keySet();
    }

    public void addPlayer(ServerPlayer player, boolean ignored) {
        this.players.put((Object)player, ignored);
    }

    public void removePlayer(ServerPlayer player) {
        this.players.removeBoolean((Object)player);
    }

    public void ignorePlayer(ServerPlayer player) {
        this.players.replace((Object)player, true);
    }

    public void unIgnorePlayer(ServerPlayer player) {
        this.players.replace((Object)player, false);
    }

    public boolean ignoredOrUnknown(ServerPlayer player) {
        return this.players.getOrDefault((Object)player, true);
    }

    public boolean ignored(ServerPlayer player) {
        return this.players.getBoolean((Object)player);
    }
}

