/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.waypoints;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.Waypoint;

public interface WaypointTransmitter
extends Waypoint {
    public static final int REALLY_FAR_DISTANCE = 332;

    public boolean isTransmittingWaypoint();

    public Optional<Connection> makeWaypointConnectionWith(ServerPlayer var1);

    public Waypoint.Icon waypointIcon();

    public static boolean doesSourceIgnoreReceiver(LivingEntity source, ServerPlayer receiver) {
        if (receiver.isSpectator()) {
            return false;
        }
        if (source.isSpectator() || source.hasIndirectPassenger(receiver)) {
            return true;
        }
        double broadcastRange = Math.min(source.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE), receiver.getAttributeValue(Attributes.WAYPOINT_RECEIVE_RANGE));
        return (double)source.distanceTo(receiver) >= broadcastRange;
    }

    public static boolean isChunkVisible(ChunkPos chunkPos, ServerPlayer receiver) {
        return receiver.getChunkTrackingView().isInViewDistance(chunkPos.x(), chunkPos.z());
    }

    public static boolean isReallyFar(LivingEntity source, ServerPlayer receiver) {
        return source.distanceTo(receiver) > 332.0f;
    }

    public static class EntityAzimuthConnection
    implements Connection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private float lastAngle;

        public EntityAzimuthConnection(LivingEntity source, Waypoint.Icon icon, ServerPlayer receiver) {
            this.source = source;
            this.icon = icon;
            this.receiver = receiver;
            Vec3 direction = receiver.position().subtract(source.position()).rotateClockwise90();
            this.lastAngle = (float)Mth.atan2(direction.z(), direction.x());
        }

        @Override
        public boolean isBroken() {
            return WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver) || WaypointTransmitter.isChunkVisible(this.source.chunkPosition(), this.receiver) || !WaypointTransmitter.isReallyFar(this.source, this.receiver);
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointAzimuth(this.source.getUUID(), this.icon, this.lastAngle));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            Vec3 direction = this.receiver.position().subtract(this.source.position()).rotateClockwise90();
            float currentAngle = (float)Mth.atan2(direction.z(), direction.x());
            if (Mth.abs(currentAngle - this.lastAngle) > (float)Math.PI / 360) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointAzimuth(this.source.getUUID(), this.icon, currentAngle));
                this.lastAngle = currentAngle;
            }
        }
    }

    public static class EntityChunkConnection
    implements ChunkConnection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private ChunkPos lastPosition;

        public EntityChunkConnection(LivingEntity source, Waypoint.Icon icon, ServerPlayer receiver) {
            this.source = source;
            this.icon = icon;
            this.receiver = receiver;
            this.lastPosition = source.chunkPosition();
        }

        @Override
        public int distanceChessboard() {
            return this.lastPosition.getChessboardDistance(this.source.chunkPosition());
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointChunk(this.source.getUUID(), this.icon, this.lastPosition));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            ChunkPos currentPosition = this.source.chunkPosition();
            if (currentPosition.getChessboardDistance(this.lastPosition) > 0) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointChunk(this.source.getUUID(), this.icon, currentPosition));
                this.lastPosition = currentPosition;
            }
        }

        @Override
        public boolean isBroken() {
            if (ChunkConnection.super.isBroken() || WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver)) {
                return true;
            }
            return WaypointTransmitter.isChunkVisible(this.lastPosition, this.receiver);
        }
    }

    public static interface ChunkConnection
    extends Connection {
        public int distanceChessboard();

        @Override
        default public boolean isBroken() {
            return this.distanceChessboard() > 1;
        }
    }

    public static class EntityBlockConnection
    implements BlockConnection {
        private final LivingEntity source;
        private final Waypoint.Icon icon;
        private final ServerPlayer receiver;
        private BlockPos lastPosition;

        public EntityBlockConnection(LivingEntity source, Waypoint.Icon icon, ServerPlayer receiver) {
            this.source = source;
            this.receiver = receiver;
            this.icon = icon;
            this.lastPosition = source.blockPosition();
        }

        @Override
        public void connect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(this.source.getUUID(), this.icon, this.lastPosition));
        }

        @Override
        public void disconnect() {
            this.receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(this.source.getUUID()));
        }

        @Override
        public void update() {
            BlockPos currentPosition = this.source.blockPosition();
            if (currentPosition.distManhattan(this.lastPosition) > 0) {
                this.receiver.connection.send(ClientboundTrackedWaypointPacket.updateWaypointPosition(this.source.getUUID(), this.icon, currentPosition));
                this.lastPosition = currentPosition;
            }
        }

        @Override
        public int distanceManhattan() {
            return this.lastPosition.distManhattan(this.source.blockPosition());
        }

        @Override
        public boolean isBroken() {
            return BlockConnection.super.isBroken() || WaypointTransmitter.doesSourceIgnoreReceiver(this.source, this.receiver);
        }
    }

    public static interface BlockConnection
    extends Connection {
        public int distanceManhattan();

        @Override
        default public boolean isBroken() {
            return this.distanceManhattan() > 1;
        }
    }

    public static interface Connection {
        public void connect();

        public void disconnect();

        public void update();

        public boolean isBroken();
    }
}

