/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.login;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;

public class ServerboundLoginAcknowledgedPacket
implements Packet<ServerLoginPacketListener> {
    public static final ServerboundLoginAcknowledgedPacket INSTANCE = new ServerboundLoginAcknowledgedPacket();
    public static final StreamCodec<ByteBuf, ServerboundLoginAcknowledgedPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ServerboundLoginAcknowledgedPacket() {
    }

    @Override
    public PacketType<ServerboundLoginAcknowledgedPacket> type() {
        return LoginPacketTypes.SERVERBOUND_LOGIN_ACKNOWLEDGED;
    }

    @Override
    public void handle(ServerLoginPacketListener listener) {
        listener.handleLoginAcknowledgement(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}

