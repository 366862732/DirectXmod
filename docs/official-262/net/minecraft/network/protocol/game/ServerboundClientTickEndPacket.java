/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public record ServerboundClientTickEndPacket() implements Packet<ServerGamePacketListener>
{
    public static final ServerboundClientTickEndPacket INSTANCE = new ServerboundClientTickEndPacket();
    public static final StreamCodec<ByteBuf, ServerboundClientTickEndPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public PacketType<ServerboundClientTickEndPacket> type() {
        return GamePacketTypes.SERVERBOUND_CLIENT_TICK_END;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleClientTickEnd(this);
    }
}

