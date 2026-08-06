/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public record ServerboundPickItemFromEntityPacket(int id, boolean includeData) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ServerboundPickItemFromEntityPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ServerboundPickItemFromEntityPacket::id, ByteBufCodecs.BOOL, ServerboundPickItemFromEntityPacket::includeData, ServerboundPickItemFromEntityPacket::new);

    @Override
    public PacketType<ServerboundPickItemFromEntityPacket> type() {
        return GamePacketTypes.SERVERBOUND_PICK_ITEM_FROM_ENTITY;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handlePickItemFromEntity(this);
    }
}

