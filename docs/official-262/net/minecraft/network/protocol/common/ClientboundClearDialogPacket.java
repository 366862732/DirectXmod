/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.CommonPacketTypes;

public class ClientboundClearDialogPacket
implements Packet<ClientCommonPacketListener> {
    public static final ClientboundClearDialogPacket INSTANCE = new ClientboundClearDialogPacket();
    public static final StreamCodec<ByteBuf, ClientboundClearDialogPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClientboundClearDialogPacket() {
    }

    @Override
    public PacketType<ClientboundClearDialogPacket> type() {
        return CommonPacketTypes.CLIENTBOUND_CLEAR_DIALOG;
    }

    @Override
    public void handle(ClientCommonPacketListener listener) {
        listener.handleClearDialog(this);
    }
}

