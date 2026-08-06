/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.configuration;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;

public class ServerboundFinishConfigurationPacket
implements Packet<ServerConfigurationPacketListener> {
    public static final ServerboundFinishConfigurationPacket INSTANCE = new ServerboundFinishConfigurationPacket();
    public static final StreamCodec<ByteBuf, ServerboundFinishConfigurationPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ServerboundFinishConfigurationPacket() {
    }

    @Override
    public PacketType<ServerboundFinishConfigurationPacket> type() {
        return ConfigurationPacketTypes.SERVERBOUND_FINISH_CONFIGURATION;
    }

    @Override
    public void handle(ServerConfigurationPacketListener listener) {
        listener.handleConfigurationFinished(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}

