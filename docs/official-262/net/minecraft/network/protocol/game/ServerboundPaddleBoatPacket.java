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

public class ServerboundPaddleBoatPacket
implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPaddleBoatPacket> STREAM_CODEC = Packet.codec(ServerboundPaddleBoatPacket::write, ServerboundPaddleBoatPacket::new);
    private final boolean left;
    private final boolean right;

    public ServerboundPaddleBoatPacket(boolean left, boolean right) {
        this.left = left;
        this.right = right;
    }

    private ServerboundPaddleBoatPacket(FriendlyByteBuf input) {
        this.left = input.readBoolean();
        this.right = input.readBoolean();
    }

    private void write(FriendlyByteBuf output) {
        output.writeBoolean(this.left);
        output.writeBoolean(this.right);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePaddleBoat(this);
    }

    @Override
    public PacketType<ServerboundPaddleBoatPacket> type() {
        return GamePacketTypes.SERVERBOUND_PADDLE_BOAT;
    }

    public boolean getLeft() {
        return this.left;
    }

    public boolean getRight() {
        return this.right;
    }
}

