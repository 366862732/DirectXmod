/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashBasedTable
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  com.google.common.collect.Table
 *  com.google.common.collect.Tables
 */
package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager
implements WaypointManager<WaypointTransmitter> {
    private final Set<WaypointTransmitter> waypoints = new HashSet<WaypointTransmitter>();
    private final Set<ServerPlayer> players = new HashSet<ServerPlayer>();
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

    @Override
    public void trackWaypoint(WaypointTransmitter waypoint) {
        this.waypoints.add(waypoint);
        for (ServerPlayer player : this.players) {
            this.createConnection(player, waypoint);
        }
    }

    @Override
    public void updateWaypoint(WaypointTransmitter waypoint) {
        if (!this.waypoints.contains(waypoint)) {
            return;
        }
        Map playerConnection = Tables.transpose(this.connections).row((Object)waypoint);
        Sets.SetView potentialPlayers = Sets.difference(this.players, playerConnection.keySet());
        for (Map.Entry waypointConnection : ImmutableSet.copyOf(playerConnection.entrySet())) {
            this.updateConnection((ServerPlayer)waypointConnection.getKey(), waypoint, (WaypointTransmitter.Connection)waypointConnection.getValue());
        }
        for (ServerPlayer player : potentialPlayers) {
            this.createConnection(player, waypoint);
        }
    }

    @Override
    public void untrackWaypoint(WaypointTransmitter waypoint) {
        this.connections.column((Object)waypoint).forEach((player, connection) -> connection.disconnect());
        Tables.transpose(this.connections).row((Object)waypoint).clear();
        this.waypoints.remove(waypoint);
    }

    public void addPlayer(ServerPlayer player) {
        this.players.add(player);
        for (WaypointTransmitter waypoint : this.waypoints) {
            this.createConnection(player, waypoint);
        }
        if (player.isTransmittingWaypoint()) {
            this.trackWaypoint(player);
        }
    }

    public void updatePlayer(ServerPlayer player) {
        Map waypointConnections = this.connections.row((Object)player);
        Sets.SetView potentialWaypoints = Sets.difference(this.waypoints, waypointConnections.keySet());
        for (Map.Entry waypointConnection : ImmutableSet.copyOf(waypointConnections.entrySet())) {
            this.updateConnection(player, (WaypointTransmitter)waypointConnection.getKey(), (WaypointTransmitter.Connection)waypointConnection.getValue());
        }
        for (WaypointTransmitter waypoint : potentialWaypoints) {
            this.createConnection(player, waypoint);
        }
    }

    public void removePlayer(ServerPlayer player) {
        this.connections.row((Object)player).values().removeIf(connection -> {
            connection.disconnect();
            return true;
        });
        this.untrackWaypoint(player);
        this.players.remove(player);
    }

    public void breakAllConnections() {
        this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
        this.connections.clear();
    }

    public void remakeConnections(WaypointTransmitter waypoint) {
        for (ServerPlayer player : this.players) {
            this.createConnection(player, waypoint);
        }
    }

    public Set<WaypointTransmitter> transmitters() {
        return this.waypoints;
    }

    private static boolean isLocatorBarEnabledFor(ServerPlayer player) {
        return player.level().getGameRules().get(GameRules.LOCATOR_BAR);
    }

    private void createConnection(ServerPlayer player, WaypointTransmitter waypoint) {
        if (player == waypoint) {
            return;
        }
        if (!ServerWaypointManager.isLocatorBarEnabledFor(player)) {
            return;
        }
        waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(connection -> {
            this.connections.put((Object)player, (Object)waypoint, connection);
            connection.connect();
        }, () -> {
            WaypointTransmitter.Connection connection = (WaypointTransmitter.Connection)this.connections.remove((Object)player, (Object)waypoint);
            if (connection != null) {
                connection.disconnect();
            }
        });
    }

    private void updateConnection(ServerPlayer player, WaypointTransmitter waypoint, WaypointTransmitter.Connection connection) {
        if (player == waypoint) {
            return;
        }
        if (!ServerWaypointManager.isLocatorBarEnabledFor(player)) {
            return;
        }
        if (!connection.isBroken()) {
            connection.update();
            return;
        }
        waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(newConnection -> {
            newConnection.connect();
            this.connections.put((Object)player, (Object)waypoint, newConnection);
        }, () -> {
            connection.disconnect();
            this.connections.remove((Object)player, (Object)waypoint);
        });
    }
}

