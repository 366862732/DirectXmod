/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class ClientboundMoveEntityPacket
implements Packet<ClientGamePacketListener> {
    protected final int entityId;
    protected final short xa;
    protected final short ya;
    protected final short za;
    protected final byte yRot;
    protected final byte xRot;
    protected final boolean onGround;
    protected final boolean hasRot;
    protected final boolean hasPos;

    protected ClientboundMoveEntityPacket(int entityId, short xa, short ya, short za, byte yRot, byte xRot, boolean onGround, boolean hasRot, boolean hasPos) {
        this.entityId = entityId;
        this.xa = xa;
        this.ya = ya;
        this.za = za;
        this.yRot = yRot;
        this.xRot = xRot;
        this.onGround = onGround;
        this.hasRot = hasRot;
        this.hasPos = hasPos;
    }

    @Override
    public abstract PacketType<? extends ClientboundMoveEntityPacket> type();

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleMoveEntity(this);
    }

    public String toString() {
        return "Entity_" + super.toString();
    }

    public @Nullable Entity getEntity(Level level) {
        return level.getEntity(this.entityId);
    }

    public short getXa() {
        return this.xa;
    }

    public short getYa() {
        return this.ya;
    }

    public short getZa() {
        return this.za;
    }

    public float getYRot() {
        return Mth.unpackDegrees(this.yRot);
    }

    public float getXRot() {
        return Mth.unpackDegrees(this.xRot);
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public static class Rot
    extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, Rot> STREAM_CODEC = Packet.codec(Rot::write, Rot::read);

        public Rot(int id, byte yRot, byte xRot, boolean onGround) {
            super(id, (short)0, (short)0, (short)0, yRot, xRot, onGround, true, false);
        }

        private static Rot read(FriendlyByteBuf input) {
            int entityId = input.readVarInt();
            byte yRot = input.readByte();
            byte xRot = input.readByte();
            boolean onGround = input.readBoolean();
            return new Rot(entityId, yRot, xRot, onGround);
        }

        private void write(FriendlyByteBuf output) {
            output.writeVarInt(this.entityId);
            output.writeByte(this.yRot);
            output.writeByte(this.xRot);
            output.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<Rot> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_ROT;
        }
    }

    public static class Pos
    extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, Pos> STREAM_CODEC = Packet.codec(Pos::write, Pos::read);

        public Pos(int id, short xa, short ya, short za, boolean onGround) {
            super(id, xa, ya, za, (byte)0, (byte)0, onGround, false, true);
        }

        private static Pos read(FriendlyByteBuf input) {
            int entityId = input.readVarInt();
            short xa = input.readShort();
            short ya = input.readShort();
            short za = input.readShort();
            boolean onGround = input.readBoolean();
            return new Pos(entityId, xa, ya, za, onGround);
        }

        private void write(FriendlyByteBuf output) {
            output.writeVarInt(this.entityId);
            output.writeShort(this.xa);
            output.writeShort(this.ya);
            output.writeShort(this.za);
            output.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<Pos> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS;
        }
    }

    public static class PosRot
    extends ClientboundMoveEntityPacket {
        public static final StreamCodec<FriendlyByteBuf, PosRot> STREAM_CODEC = Packet.codec(PosRot::write, PosRot::read);

        public PosRot(int id, short xa, short ya, short za, byte yRot, byte xRot, boolean onGround) {
            super(id, xa, ya, za, yRot, xRot, onGround, true, true);
        }

        private static PosRot read(FriendlyByteBuf input) {
            int entityId = input.readVarInt();
            short xa = input.readShort();
            short ya = input.readShort();
            short za = input.readShort();
            byte yRot = input.readByte();
            byte xRot = input.readByte();
            boolean onGround = input.readBoolean();
            return new PosRot(entityId, xa, ya, za, yRot, xRot, onGround);
        }

        private void write(FriendlyByteBuf output) {
            output.writeVarInt(this.entityId);
            output.writeShort(this.xa);
            output.writeShort(this.ya);
            output.writeShort(this.za);
            output.writeByte(this.yRot);
            output.writeByte(this.xRot);
            output.writeBoolean(this.onGround);
        }

        @Override
        public PacketType<PosRot> type() {
            return GamePacketTypes.CLIENTBOUND_MOVE_ENTITY_POS_ROT;
        }
    }
}

