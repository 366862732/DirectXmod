/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;

public class ServerboundKeepAlivePacket
implements Packet<ServerCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundKeepAlivePacket> STREAM_CODEC = Packet.codec(ServerboundKeepAlivePacket::write, ServerboundKeepAlivePacket::new);
    private final long id;

    public ServerboundKeepAlivePacket(long id) {
        this.id = id;
    }

    private ServerboundKeepAlivePacket(FriendlyByteBuf input) {
        this.id = input.readLong();
    }

    private void write(FriendlyByteBuf output) {
        output.writeLong(this.id);
    }

    @Override
    public PacketType<ServerboundKeepAlivePacket> type() {
        return CommonPacketTypes.SERVERBOUND_KEEP_ALIVE;
    }

    @Override
    public void handle(ServerCommonPacketListener listener) {
        listener.handleKeepAlive(this);
    }

    public long getId() {
        return this.id;
    }
}

