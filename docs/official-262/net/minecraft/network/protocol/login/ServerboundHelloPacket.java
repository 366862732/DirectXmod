/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.network.protocol.login;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;

public record ServerboundHelloPacket(String name, UUID profileId) implements Packet<ServerLoginPacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ServerboundHelloPacket> STREAM_CODEC = Packet.codec(ServerboundHelloPacket::write, ServerboundHelloPacket::new);

    private ServerboundHelloPacket(FriendlyByteBuf input) {
        this(input.readUtf(16), input.readUUID());
    }

    private void write(FriendlyByteBuf output) {
        output.writeUtf(this.name, 16);
        output.writeUUID(this.profileId);
    }

    @Override
    public PacketType<ServerboundHelloPacket> type() {
        return LoginPacketTypes.SERVERBOUND_HELLO;
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleHello(this);
    }
}

