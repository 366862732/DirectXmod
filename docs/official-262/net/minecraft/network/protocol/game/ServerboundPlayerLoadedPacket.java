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

public record ServerboundPlayerLoadedPacket() implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ServerboundPlayerLoadedPacket> STREAM_CODEC = StreamCodec.unit(new ServerboundPlayerLoadedPacket());

    @Override
    public PacketType<ServerboundPlayerLoadedPacket> type() {
        return GamePacketTypes.SERVERBOUND_PLAYER_LOADED;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleAcceptPlayerLoad(this);
    }
}

