/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.TrackedWaypointManager;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointManager;

public record ClientboundTrackedWaypointPacket(Operation operation, TrackedWaypoint waypoint) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTrackedWaypointPacket> STREAM_CODEC = StreamCodec.composite(Operation.STREAM_CODEC, ClientboundTrackedWaypointPacket::operation, TrackedWaypoint.STREAM_CODEC, ClientboundTrackedWaypointPacket::waypoint, ClientboundTrackedWaypointPacket::new);

    public static ClientboundTrackedWaypointPacket removeWaypoint(UUID identifier) {
        return new ClientboundTrackedWaypointPacket(Operation.UNTRACK, TrackedWaypoint.empty(identifier));
    }

    public static ClientboundTrackedWaypointPacket addWaypointPosition(UUID identifier, Waypoint.Icon icon, Vec3i position) {
        return new ClientboundTrackedWaypointPacket(Operation.TRACK, TrackedWaypoint.setPosition(identifier, icon, position));
    }

    public static ClientboundTrackedWaypointPacket updateWaypointPosition(UUID identifier, Waypoint.Icon icon, Vec3i position) {
        return new ClientboundTrackedWaypointPacket(Operation.UPDATE, TrackedWaypoint.setPosition(identifier, icon, position));
    }

    public static ClientboundTrackedWaypointPacket addWaypointChunk(UUID identifier, Waypoint.Icon icon, ChunkPos chunk) {
        return new ClientboundTrackedWaypointPacket(Operation.TRACK, TrackedWaypoint.setChunk(identifier, icon, chunk));
    }

    public static ClientboundTrackedWaypointPacket updateWaypointChunk(UUID identifier, Waypoint.Icon icon, ChunkPos chunk) {
        return new ClientboundTrackedWaypointPacket(Operation.UPDATE, TrackedWaypoint.setChunk(identifier, icon, chunk));
    }

    public static ClientboundTrackedWaypointPacket addWaypointAzimuth(UUID identifier, Waypoint.Icon icon, float angle) {
        return new ClientboundTrackedWaypointPacket(Operation.TRACK, TrackedWaypoint.setAzimuth(identifier, icon, angle));
    }

    public static ClientboundTrackedWaypointPacket updateWaypointAzimuth(UUID identifier, Waypoint.Icon icon, float angle) {
        return new ClientboundTrackedWaypointPacket(Operation.UPDATE, TrackedWaypoint.setAzimuth(identifier, icon, angle));
    }

    @Override
    public PacketType<ClientboundTrackedWaypointPacket> type() {
        return GamePacketTypes.CLIENTBOUND_WAYPOINT;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleWaypoint(this);
    }

    public void apply(TrackedWaypointManager manager) {
        this.operation.action.accept(manager, this.waypoint);
    }

    private static enum Operation {
        TRACK(WaypointManager::trackWaypoint),
        UNTRACK(WaypointManager::untrackWaypoint),
        UPDATE(WaypointManager::updateWaypoint);

        private final BiConsumer<TrackedWaypointManager, TrackedWaypoint> action;
        public static final IntFunction<Operation> BY_ID;
        public static final StreamCodec<ByteBuf, Operation> STREAM_CODEC;

        private Operation(BiConsumer<TrackedWaypointManager, TrackedWaypoint> action) {
            this.action = action;
        }

        static {
            BY_ID = ByIdMap.continuous(Enum::ordinal, Operation.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
        }
    }
}

