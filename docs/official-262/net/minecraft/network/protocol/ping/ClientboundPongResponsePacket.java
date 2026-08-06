/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.ping;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.ping.ClientPongPacketListener;
import net.minecraft.network.protocol.ping.PingPacketTypes;

public record ClientboundPongResponsePacket(long time) implements Packet<ClientPongPacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ClientboundPongResponsePacket> STREAM_CODEC = Packet.codec(ClientboundPongResponsePacket::write, ClientboundPongResponsePacket::new);

    private ClientboundPongResponsePacket(FriendlyByteBuf input) {
        this(input.readLong());
    }

    private void write(FriendlyByteBuf output) {
        output.writeLong(this.time);
    }

    @Override
    public PacketType<ClientboundPongResponsePacket> type() {
        return PingPacketTypes.CLIENTBOUND_PONG_RESPONSE;
    }

    @Override
    public void handle(ClientPongPacketListener listener) {
        listener.handlePongResponse(this);
    }
}

