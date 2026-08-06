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

public record ServerboundAttackPacket(int entityId) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ServerboundAttackPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ServerboundAttackPacket::entityId, ServerboundAttackPacket::new);

    @Override
    public PacketType<ServerboundAttackPacket> type() {
        return GamePacketTypes.SERVERBOUND_ATTACK;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleAttack(this);
    }
}

