/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.Unpooled
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class ClientboundLevelChunkPacketData {
    private static final StreamCodec<ByteBuf, Map<Heightmap.Types, long[]>> HEIGHTMAPS_STREAM_CODEC = ByteBufCodecs.map(size -> new EnumMap(Heightmap.Types.class), Heightmap.Types.STREAM_CODEC, ByteBufCodecs.LONG_ARRAY);
    private static final int TWO_MEGABYTES = 0x200000;
    private final Map<Heightmap.Types, long[]> heightmaps;
    private final byte[] buffer;
    private final List<BlockEntityInfo> blockEntitiesData;

    public ClientboundLevelChunkPacketData(LevelChunk levelChunk) {
        this.heightmaps = levelChunk.getHeightmaps().stream().filter(entry -> ((Heightmap.Types)entry.getKey()).sendToClient()).collect(Collectors.toMap(Map.Entry::getKey, entry -> (long[])((Heightmap)entry.getValue()).getRawData().clone()));
        this.buffer = new byte[ClientboundLevelChunkPacketData.calculateChunkSize(levelChunk)];
        ClientboundLevelChunkPacketData.extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), levelChunk);
        this.blockEntitiesData = Lists.newArrayList();
        for (Map.Entry<BlockPos, BlockEntity> entry2 : levelChunk.getBlockEntities().entrySet()) {
            this.blockEntitiesData.add(BlockEntityInfo.create(entry2.getValue()));
        }
    }

    public ClientboundLevelChunkPacketData(RegistryFriendlyByteBuf input, int x, int z) {
        this.heightmaps = (Map)HEIGHTMAPS_STREAM_CODEC.decode(input);
        int size = input.readVarInt();
        if (size > 0x200000) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        }
        this.buffer = new byte[size];
        input.readBytes(this.buffer);
        this.blockEntitiesData = (List)BlockEntityInfo.LIST_STREAM_CODEC.decode(input);
    }

    public void write(RegistryFriendlyByteBuf output) {
        HEIGHTMAPS_STREAM_CODEC.encode(output, this.heightmaps);
        output.writeVarInt(this.buffer.length);
        output.writeBytes(this.buffer);
        BlockEntityInfo.LIST_STREAM_CODEC.encode(output, this.blockEntitiesData);
    }

    private static int calculateChunkSize(LevelChunk chunk) {
        int total = 0;
        for (LevelChunkSection section : chunk.getSections()) {
            total += section.getSerializedSize();
        }
        return total;
    }

    private ByteBuf getWriteBuffer() {
        ByteBuf buffer = Unpooled.wrappedBuffer((byte[])this.buffer);
        buffer.writerIndex(0);
        return buffer;
    }

    public static void extractChunkData(FriendlyByteBuf buffer, LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            section.write(buffer);
        }
        if (buffer.writerIndex() != buffer.capacity()) {
            throw new IllegalStateException("Didn't fill chunk buffer: expected " + buffer.capacity() + " bytes, got " + buffer.writerIndex());
        }
    }

    public Consumer<BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int x, int z) {
        return output -> this.getBlockEntitiesTags((BlockEntityTagOutput)output, x, z);
    }

    private void getBlockEntitiesTags(BlockEntityTagOutput output, int x, int z) {
        int baseX = 16 * x;
        int baseZ = 16 * z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (BlockEntityInfo data : this.blockEntitiesData) {
            int unpackedX = baseX + SectionPos.sectionRelative(data.packedXZ >> 4);
            int unpackedZ = baseZ + SectionPos.sectionRelative(data.packedXZ);
            pos.set(unpackedX, data.y, unpackedZ);
            output.accept(pos, data.type, data.tag);
        }
    }

    public FriendlyByteBuf getReadBuffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer((byte[])this.buffer));
    }

    public Map<Heightmap.Types, long[]> getHeightmaps() {
        return this.heightmaps;
    }

    private static class BlockEntityInfo {
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(BlockEntityInfo::write, BlockEntityInfo::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, List<BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());
        private final int packedXZ;
        private final int y;
        private final BlockEntityType<?> type;
        private final @Nullable CompoundTag tag;

        private BlockEntityInfo(int packedXZ, int y, BlockEntityType<?> type, @Nullable CompoundTag tag) {
            this.packedXZ = packedXZ;
            this.y = y;
            this.type = type;
            this.tag = tag;
        }

        private BlockEntityInfo(RegistryFriendlyByteBuf input) {
            this.packedXZ = input.readByte();
            this.y = input.readShort();
            this.type = (BlockEntityType)ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(input);
            this.tag = input.readNbt();
        }

        private void write(RegistryFriendlyByteBuf output) {
            output.writeByte(this.packedXZ);
            output.writeShort(this.y);
            ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(output, this.type);
            output.writeNbt(this.tag);
        }

        private static BlockEntityInfo create(BlockEntity blockEntity) {
            CompoundTag tag = blockEntity.getUpdateTag(blockEntity.getLevel().registryAccess());
            BlockPos pos = blockEntity.getBlockPos();
            int xz = SectionPos.sectionRelative(pos.getX()) << 4 | SectionPos.sectionRelative(pos.getZ());
            return new BlockEntityInfo(xz, pos.getY(), blockEntity.getType(), tag.isEmpty() ? null : tag);
        }
    }

    @FunctionalInterface
    public static interface BlockEntityTagOutput {
        public void accept(BlockPos var1, BlockEntityType<?> var2, @Nullable CompoundTag var3);
    }
}

