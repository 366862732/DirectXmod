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
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;

public class ClientboundChunkBatchStartPacket
implements Packet<ClientGamePacketListener> {
    public static final ClientboundChunkBatchStartPacket INSTANCE = new ClientboundChunkBatchStartPacket();
    public static final StreamCodec<ByteBuf, ClientboundChunkBatchStartPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private ClientboundChunkBatchStartPacket() {
    }

    @Override
    public PacketType<ClientboundChunkBatchStartPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CHUNK_BATCH_START;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChunkBatchStart(this);
    }
}

