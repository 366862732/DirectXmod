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
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;

public class ClientboundFinishConfigurationPacket
implements Packet<ClientConfigurationPacketListener> {
    public static final ClientboundFinishConfigurationPacket INSTANCE = new ClientboundFinishConfigurationPacket();
    public static final StreamCodec<ByteBuf, ClientboundFinishConfigurationPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClientboundFinishConfigurationPacket() {
    }

    @Override
    public PacketType<ClientboundFinishConfigurationPacket> type() {
        return ConfigurationPacketTypes.CLIENTBOUND_FINISH_CONFIGURATION;
    }

    @Override
    public void handle(ClientConfigurationPacketListener listener) {
        listener.handleConfigurationFinished(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}

