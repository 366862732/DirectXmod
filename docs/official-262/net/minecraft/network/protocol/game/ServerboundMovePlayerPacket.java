/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.world.phys.Vec3;

public abstract class ServerboundMovePlayerPacket
implements Packet<ServerGamePacketListener> {
    private static final int FLAG_ON_GROUND = 1;
    private static final int FLAG_HORIZONTAL_COLLISION = 2;
    protected final double x;
    protected final double y;
    protected final double z;
    protected final float yRot;
    protected final float xRot;
    protected final boolean onGround;
    protected final boolean horizontalCollision;
    protected final boolean hasPos;
    protected final boolean hasRot;

    private static int packFlags(boolean onGround, boolean horizontalCollision) {
        int flags = 0;
        if (onGround) {
            flags |= 1;
        }
        if (horizontalCollision) {
            flags |= 2;
        }
        return flags;
    }

    private static boolean unpackOnGround(int flags) {
        return (flags & 1) != 0;
    }

    private static boolean unpackHorizontalCollision(int flags) {
        return (flags & 2) != 0;
    }

    protected ServerboundMovePlayerPacket(double x, double y, double z, float yRot, float xRot, boolean onGround, boolean horizontalCollision, boolean hasPos, boolean hasRot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yRot;
        this.xRot = xRot;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.hasPos = hasPos;
        this.hasRot = hasRot;
    }

    @Override
    public abstract PacketType<? extends ServerboundMovePlayerPacket> type();

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleMovePlayer(this);
    }

    public double getX(double fallback) {
        return this.hasPos ? this.x : fallback;
    }

    public double getY(double fallback) {
        return this.hasPos ? this.y : fallback;
    }

    public double getZ(double fallback) {
        return this.hasPos ? this.z : fallback;
    }

    public float getYRot(float fallback) {
        return this.hasRot ? this.yRot : fallback;
    }

    public float getXRot(float fallback) {
        return this.hasRot ? this.xRot : fallback;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class StatusOnly
    extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, StatusOnly> STREAM_CODEC = Packet.codec(StatusOnly::write, StatusOnly::read);

        public StatusOnly(boolean onGround, boolean horizontalCollision) {
            super(0.0, 0.0, 0.0, 0.0f, 0.0f, onGround, horizontalCollision, false, false);
        }

        private static StatusOnly read(FriendlyByteBuf input) {
            short flags = input.readUnsignedByte();
            boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
            boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
            return new StatusOnly(onGround, horizontalCollision);
        }

        private void write(FriendlyByteBuf output) {
            output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<StatusOnly> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_STATUS_ONLY;
        }
    }

    public static class Rot
    extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, Rot> STREAM_CODEC = Packet.codec(Rot::write, Rot::read);

        public Rot(float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(0.0, 0.0, 0.0, yRot, xRot, onGround, horizontalCollision, false, true);
        }

        private static Rot read(FriendlyByteBuf input) {
            float yRot = input.readFloat();
            float xRot = input.readFloat();
            short flags = input.readUnsignedByte();
            boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
            boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
            return new Rot(yRot, xRot, onGround, horizontalCollision);
        }

        private void write(FriendlyByteBuf output) {
            output.writeFloat(this.yRot);
            output.writeFloat(this.xRot);
            output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<Rot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_ROT;
        }
    }

    public static class Pos
    extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, Pos> STREAM_CODEC = Packet.codec(Pos::write, Pos::read);

        public Pos(Vec3 pos, boolean onGround, boolean horizontalCollision) {
            super(pos.x, pos.y, pos.z, 0.0f, 0.0f, onGround, horizontalCollision, true, false);
        }

        public Pos(double x, double y, double z, boolean onGround, boolean horizontalCollision) {
            super(x, y, z, 0.0f, 0.0f, onGround, horizontalCollision, true, false);
        }

        private static Pos read(FriendlyByteBuf input) {
            double x = input.readDouble();
            double y = input.readDouble();
            double z = input.readDouble();
            short flags = input.readUnsignedByte();
            boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
            boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
            return new Pos(x, y, z, onGround, horizontalCollision);
        }

        private void write(FriendlyByteBuf output) {
            output.writeDouble(this.x);
            output.writeDouble(this.y);
            output.writeDouble(this.z);
            output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<Pos> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS;
        }
    }

    public static class PosRot
    extends ServerboundMovePlayerPacket {
        public static final StreamCodec<FriendlyByteBuf, PosRot> STREAM_CODEC = Packet.codec(PosRot::write, PosRot::read);

        public PosRot(Vec3 pos, float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(pos.x, pos.y, pos.z, yRot, xRot, onGround, horizontalCollision, true, true);
        }

        public PosRot(double x, double y, double z, float yRot, float xRot, boolean onGround, boolean horizontalCollision) {
            super(x, y, z, yRot, xRot, onGround, horizontalCollision, true, true);
        }

        private static PosRot read(FriendlyByteBuf input) {
            double x = input.readDouble();
            double y = input.readDouble();
            double z = input.readDouble();
            float yRot = input.readFloat();
            float xRot = input.readFloat();
            short flags = input.readUnsignedByte();
            boolean onGround = ServerboundMovePlayerPacket.unpackOnGround(flags);
            boolean horizontalCollision = ServerboundMovePlayerPacket.unpackHorizontalCollision(flags);
            return new PosRot(x, y, z, yRot, xRot, onGround, horizontalCollision);
        }

        private void write(FriendlyByteBuf output) {
            output.writeDouble(this.x);
            output.writeDouble(this.y);
            output.writeDouble(this.z);
            output.writeFloat(this.yRot);
            output.writeFloat(this.xRot);
            output.writeByte(ServerboundMovePlayerPacket.packFlags(this.onGround, this.horizontalCollision));
        }

        @Override
        public PacketType<PosRot> type() {
            return GamePacketTypes.SERVERBOUND_MOVE_PLAYER_POS_ROT;
        }
    }
}

