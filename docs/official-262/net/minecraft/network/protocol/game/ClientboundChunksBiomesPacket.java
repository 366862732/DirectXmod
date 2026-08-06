/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 */
package net.minecraft.network.protocol.game;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public record ClientboundChunksBiomesPacket(List<ChunkBiomeData> chunkBiomeData) implements Packet<ClientGamePacketListener>
{
    public static final StreamCodec<FriendlyByteBuf, ClientboundChunksBiomesPacket> STREAM_CODEC = Packet.codec(ClientboundChunksBiomesPacket::write, ClientboundChunksBiomesPacket::new);
    private static final int TWO_MEGABYTES = 0x200000;

    private ClientboundChunksBiomesPacket(FriendlyByteBuf input) {
        this(input.readList(ChunkBiomeData::new));
    }

    public static ClientboundChunksBiomesPacket forChunks(List<LevelChunk> chunks) {
        return new ClientboundChunksBiomesPacket(chunks.stream().map(ChunkBiomeData::new).toList());
    }

    private void write(FriendlyByteBuf output) {
        output.writeCollection(this.chunkBiomeData, (o, c) -> c.write((FriendlyByteBuf)((Object)o)));
    }

    @Override
    public PacketType<ClientboundChunksBiomesPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CHUNKS_BIOMES;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChunksBiomes(this);
    }

    public record ChunkBiomeData(ChunkPos pos, byte[] buffer) {
        public ChunkBiomeData(LevelChunk chunk) {
            this(chunk.getPos(), new byte[ChunkBiomeData.calculateChunkSize(chunk)]);
            ChunkBiomeData.extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), chunk);
        }

        public ChunkBiomeData(FriendlyByteBuf input) {
            this(input.readChunkPos(), input.readByteArray(0x200000));
        }

        private static int calculateChunkSize(LevelChunk chunk) {
            int total = 0;
            for (LevelChunkSection section : chunk.getSections()) {
                total += section.getBiomes().getSerializedSize();
            }
            return total;
        }

        public FriendlyByteBuf getReadBuffer() {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer((byte[])this.buffer));
        }

        private ByteBuf getWriteBuffer() {
            ByteBuf buffer = Unpooled.wrappedBuffer((byte[])this.buffer);
            buffer.writerIndex(0);
            return buffer;
        }

        public static void extractChunkData(FriendlyByteBuf buffer, LevelChunk chunk) {
            for (LevelChunkSection section : chunk.getSections()) {
                section.getBiomes().write(buffer);
            }
            if (buffer.writerIndex() != buffer.capacity()) {
                throw new IllegalStateException("Didn't fill biome buffer: expected " + buffer.capacity() + " bytes, got " + buffer.writerIndex());
            }
        }

        public void write(FriendlyByteBuf output) {
            output.writeChunkPos(this.pos);
            output.writeByteArray(this.buffer);
        }
    }
}

