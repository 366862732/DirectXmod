/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.CommonPacketTypes;

public record ClientboundTransferPacket(String host, int port) implements Packet<ClientCommonPacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ClientboundTransferPacket> STREAM_CODEC = Packet.codec(ClientboundTransferPacket::write, ClientboundTransferPacket::new);

    private ClientboundTransferPacket(FriendlyByteBuf input) {
        this(input.readUtf(), input.readVarInt());
    }

    private void write(FriendlyByteBuf output) {
        output.writeUtf(this.host);
        output.writeVarInt(this.port);
    }

    @Override
    public PacketType<ClientboundTransferPacket> type() {
        return CommonPacketTypes.CLIENTBOUND_TRANSFER;
    }

    @Override
    public void handle(ClientCommonPacketListener listener) {
        listener.handleTransfer(this);
    }
}

