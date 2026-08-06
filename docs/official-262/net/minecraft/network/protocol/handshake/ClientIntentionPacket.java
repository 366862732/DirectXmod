/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.handshake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.HandshakePacketTypes;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) implements Packet<ServerHandshakePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ClientIntentionPacket> STREAM_CODEC = Packet.codec(ClientIntentionPacket::write, ClientIntentionPacket::new);
    private static final int MAX_HOST_LENGTH = 255;

    private ClientIntentionPacket(FriendlyByteBuf input) {
        this(input.readVarInt(), input.readUtf(255), input.readUnsignedShort(), ClientIntent.byId(input.readVarInt()));
    }

    private void write(FriendlyByteBuf output) {
        output.writeVarInt(this.protocolVersion);
        output.writeUtf(this.hostName);
        output.writeShort(this.port);
        output.writeVarInt(this.intention.id());
    }

    @Override
    public PacketType<ClientIntentionPacket> type() {
        return HandshakePacketTypes.CLIENT_INTENTION;
    }

    @Override
    public void handle(ServerHandshakePacketListener listener) {
        listener.handleIntention(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}

