/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.shorts.ShortIterator
 *  it.unimi.dsi.fastutil.shorts.ShortSet
 */
package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ClientboundSectionBlocksUpdatePacket
implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSectionBlocksUpdatePacket> STREAM_CODEC = Packet.codec(ClientboundSectionBlocksUpdatePacket::write, ClientboundSectionBlocksUpdatePacket::new);
    private static final int POS_IN_SECTION_BITS = 12;
    private final SectionPos sectionPos;
    private final short[] positions;
    private final BlockState[] states;

    public ClientboundSectionBlocksUpdatePacket(SectionPos sectionPos, ShortSet changes, LevelChunkSection section) {
        this.sectionPos = sectionPos;
        int count = changes.size();
        this.positions = new short[count];
        this.states = new BlockState[count];
        int i = 0;
        ShortIterator shortIterator = changes.iterator();
        while (shortIterator.hasNext()) {
            short packedPos;
            this.positions[i] = packedPos = ((Short)shortIterator.next()).shortValue();
            this.states[i] = section.getBlockState(SectionPos.sectionRelativeX(packedPos), SectionPos.sectionRelativeY(packedPos), SectionPos.sectionRelativeZ(packedPos));
            ++i;
        }
    }

    private ClientboundSectionBlocksUpdatePacket(FriendlyByteBuf input) {
        this.sectionPos = (SectionPos)SectionPos.STREAM_CODEC.decode(input);
        int count = input.readVarInt();
        this.positions = new short[count];
        this.states = new BlockState[count];
        for (int i = 0; i < count; ++i) {
            long packedChange = input.readVarLong();
            this.positions[i] = (short)(packedChange & 0xFFFL);
            this.states[i] = Block.BLOCK_STATE_REGISTRY.byId((int)(packedChange >>> 12));
        }
    }

    private void write(FriendlyByteBuf output) {
        SectionPos.STREAM_CODEC.encode(output, this.sectionPos);
        output.writeVarInt(this.positions.length);
        for (int i = 0; i < this.positions.length; ++i) {
            output.writeVarLong((long)Block.getId(this.states[i]) << 12 | (long)this.positions[i]);
        }
    }

    @Override
    public PacketType<ClientboundSectionBlocksUpdatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SECTION_BLOCKS_UPDATE;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChunkBlocksUpdate(this);
    }

    public void runUpdates(BiConsumer<BlockPos, BlockState> updateFunction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < this.positions.length; ++i) {
            short packedPos = this.positions[i];
            cursor.set(this.sectionPos.relativeToBlockX(packedPos), this.sectionPos.relativeToBlockY(packedPos), this.sectionPos.relativeToBlockZ(packedPos));
            updateFunction.accept(cursor, this.states[i]);
        }
    }
}

