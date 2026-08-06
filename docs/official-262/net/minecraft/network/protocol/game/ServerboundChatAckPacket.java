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

public record ServerboundChatAckPacket(int offset) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundChatAckPacket> STREAM_CODEC = Packet.codec(ServerboundChatAckPacket::write, ServerboundChatAckPacket::new);

    private ServerboundChatAckPacket(FriendlyByteBuf input) {
        this(input.readVarInt());
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.offset);
    }

    @Override
    public PacketType<ServerboundChatAckPacket> type() {
        return GamePacketTypes.SERVERBOUND_CHAT_ACK;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChatAck(this);
    }
}

