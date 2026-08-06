/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import java.util.OptionalInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.ServerGamePacketListener;

public record ServerboundSpectatorActionPacket(OptionalInt spectateEntityId) implements Packet<ServerGamePacketListener>
{
    public static final StreamCodec<ByteBuf, ServerboundSpectatorActionPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.OPTIONAL_VAR_INT, ServerboundSpectatorActionPacket::spectateEntityId, ServerboundSpectatorActionPacket::new);

    @Override
    public PacketType<ServerboundSpectatorActionPacket> type() {
        return GamePacketTypes.SERVERBOUND_SPECTATOR_ACTION;
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleSpectatorAction(this);
    }
}

