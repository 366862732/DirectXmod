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
import net.minecraft.world.level.GameType;

public record ServerboundChangeGameModePacket(GameType mode) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ServerboundChangeGameModePacket> STREAM_CODEC = StreamCodec.composite(GameType.STREAM_CODEC, ServerboundChangeGameModePacket::mode, ServerboundChangeGameModePacket::new);

    @Override
    public PacketType<ServerboundChangeGameModePacket> type() {
        return GamePacketTypes.SERVERBOUND_CHANGE_GAME_MODE;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChangeGameMode(this);
    }
}

