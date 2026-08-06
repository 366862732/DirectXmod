/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.longs.LongConsumer
 */
package net.minecraft.core;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.entity.EntityAccess;

public class SectionPos
extends Vec3i {
    public static final int SECTION_BITS = 4;
    public static final int SECTION_SIZE = 16;
    public static final int SECTION_BLOCK_COUNT = 4096;
    public static final int SECTION_MASK = 15;
    public static final int SECTION_HALF_SIZE = 8;
    public static final int SECTION_MAX_INDEX = 15;
    private static final int PACKED_X_LENGTH = 22;
    private static final int PACKED_Y_LENGTH = 20;
    private static final int PACKED_Z_LENGTH = 22;
    private static final long PACKED_X_MASK = 0x3FFFFFL;
    private static final long PACKED_Y_MASK = 1048575L;
    private static final long PACKED_Z_MASK = 0x3FFFFFL;
    private static final int Y_OFFSET = 0;
    private static final int Z_OFFSET = 20;
    private static final int X_OFFSET = 42;
    private static final int RELATIVE_X_SHIFT = 8;
    private static final int RELATIVE_Y_SHIFT = 0;
    private static final int RELATIVE_Z_SHIFT = 4;
    public static final StreamCodec<ByteBuf, SectionPos> STREAM_CODEC = ByteBufCodecs.LONG.map(SectionPos::of, SectionPos::asLong);

    private SectionPos(int x, int y, int z) {
        super(x, y, z);
    }

    public static SectionPos of(int x, int y, int z) {
        return new SectionPos(x, y, z);
    }

    public static SectionPos of(BlockPos pos) {
        return new SectionPos(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static SectionPos of(ChunkPos pos, int sectionY) {
        return new SectionPos(pos.x(), sectionY, pos.z());
    }

    public static SectionPos of(EntityAccess entity) {
        return SectionPos.of(entity.blockPosition());
    }

    public static SectionPos of(Position pos) {
        return new SectionPos(SectionPos.blockToSectionCoord(pos.x()), SectionPos.blockToSectionCoord(pos.y()), SectionPos.blockToSectionCoord(pos.z()));
    }

    public static SectionPos of(long sectionNode) {
        return new SectionPos(SectionPos.x(sectionNode), SectionPos.y(sectionNode), SectionPos.z(sectionNode));
    }

    public static SectionPos bottomOf(ChunkAccess chunk) {
        return SectionPos.of(chunk.getPos(), chunk.getMinSectionY());
    }

    public static long offset(long sectionNode, Direction offset) {
        return SectionPos.offset(sectionNode, offset.getStepX(), offset.getStepY(), offset.getStepZ());
    }

    public static long offset(long sectionNode, int stepX, int stepY, int stepZ) {
        return SectionPos.asLong(SectionPos.x(sectionNode) + stepX, SectionPos.y(sectionNode) + stepY, SectionPos.z(sectionNode) + stepZ);
    }

    public static int posToSectionCoord(double pos) {
        return SectionPos.blockToSectionCoord(Mth.floor(pos));
    }

    public static int blockToSectionCoord(int blockCoord) {
        return blockCoord >> 4;
    }

    public static int blockToSectionCoord(double coord) {
        return Mth.floor(coord) >> 4;
    }

    public static int sectionRelative(int blockCoord) {
        return blockCoord & 0xF;
    }

    public static short sectionRelativePos(BlockPos pos) {
        int x = SectionPos.sectionRelative(pos.getX());
        int y = SectionPos.sectionRelative(pos.getY());
        int z = SectionPos.sectionRelative(pos.getZ());
        return (short)(x << 8 | z << 4 | y << 0);
    }

    public static int sectionRelativeX(short relative) {
        return relative >>> 8 & 0xF;
    }

    public static int sectionRelativeY(short relative) {
        return relative >>> 0 & 0xF;
    }

    public static int sectionRelativeZ(short relative) {
        return relative >>> 4 & 0xF;
    }

    public int relativeToBlockX(short relative) {
        return this.minBlockX() + SectionPos.sectionRelativeX(relative);
    }

    public int relativeToBlockY(short relative) {
        return this.minBlockY() + SectionPos.sectionRelativeY(relative);
    }

    public int relativeToBlockZ(short relative) {
        return this.minBlockZ() + SectionPos.sectionRelativeZ(relative);
    }

    public BlockPos relativeToBlockPos(short relative) {
        return new BlockPos(this.relativeToBlockX(relative), this.relativeToBlockY(relative), this.relativeToBlockZ(relative));
    }

    public static int sectionToBlockCoord(int sectionCoord) {
        return sectionCoord << 4;
    }

    public static int sectionToBlockCoord(int sectionCoord, int offset) {
        return SectionPos.sectionToBlockCoord(sectionCoord) + offset;
    }

    public static int x(long sectionNode) {
        return (int)(sectionNode << 0 >> 42);
    }

    public static int y(long sectionNode) {
        return (int)(sectionNode << 44 >> 44);
    }

    public static int z(long sectionNode) {
        return (int)(sectionNode << 22 >> 42);
    }

    public int x() {
        return this.getX();
    }

    public int y() {
        return this.getY();
    }

    public int z() {
        return this.getZ();
    }

    public int minBlockX() {
        return SectionPos.sectionToBlockCoord(this.x());
    }

    public int minBlockY() {
        return SectionPos.sectionToBlockCoord(this.y());
    }

    public int minBlockZ() {
        return SectionPos.sectionToBlockCoord(this.z());
    }

    public int maxBlockX() {
        return SectionPos.sectionToBlockCoord(this.x(), 15);
    }

    public int maxBlockY() {
        return SectionPos.sectionToBlockCoord(this.y(), 15);
    }

    public int maxBlockZ() {
        return SectionPos.sectionToBlockCoord(this.z(), 15);
    }

    public static long blockToSection(long blockNode) {
        return SectionPos.asLong(SectionPos.blockToSectionCoord(BlockPos.getX(blockNode)), SectionPos.blockToSectionCoord(BlockPos.getY(blockNode)), SectionPos.blockToSectionCoord(BlockPos.getZ(blockNode)));
    }

    public static long getZeroNode(int x, int z) {
        return SectionPos.getZeroNode(SectionPos.asLong(x, 0, z));
    }

    public static long getZeroNode(long sectionNode) {
        return sectionNode & 0xFFFFFFFFFFF00000L;
    }

    public static long sectionToChunk(long sectionNode) {
        return ChunkPos.pack(SectionPos.x(sectionNode), SectionPos.z(sectionNode));
    }

    public BlockPos origin() {
        return new BlockPos(SectionPos.sectionToBlockCoord(this.x()), SectionPos.sectionToBlockCoord(this.y()), SectionPos.sectionToBlockCoord(this.z()));
    }

    public BlockPos center() {
        int delta = 8;
        return this.origin().offset(8, 8, 8);
    }

    public ChunkPos chunk() {
        return new ChunkPos(this.x(), this.z());
    }

    public static long asLong(BlockPos pos) {
        return SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static long asLong(int x, int y, int z) {
        long node = 0L;
        node |= ((long)x & 0x3FFFFFL) << 42;
        node |= ((long)y & 0xFFFFFL) << 0;
        return node |= ((long)z & 0x3FFFFFL) << 20;
    }

    public long asLong() {
        return SectionPos.asLong(this.x(), this.y(), this.z());
    }

    @Override
    public SectionPos offset(int x, int y, int z) {
        if (x == 0 && y == 0 && z == 0) {
            return this;
        }
        return new SectionPos(this.x() + x, this.y() + y, this.z() + z);
    }

    public Stream<BlockPos> blocksInside() {
        return BlockPos.betweenClosedStream(this.minBlockX(), this.minBlockY(), this.minBlockZ(), this.maxBlockX(), this.maxBlockY(), this.maxBlockZ());
    }

    public static Stream<SectionPos> cube(SectionPos center, int radius) {
        int x = center.x();
        int y = center.y();
        int z = center.z();
        return SectionPos.betweenClosedStream(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
    }

    public static Stream<SectionPos> aroundChunk(ChunkPos center, int radius, int minSection, int maxSection) {
        int x = center.x();
        int z = center.z();
        return SectionPos.betweenClosedStream(x - radius, minSection, z - radius, x + radius, maxSection, z + radius);
    }

    public static Stream<SectionPos> betweenClosedStream(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<SectionPos>((long)((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1)), 64){
            private final Cursor3D cursor;
            {
                super(est, additionalCharacteristics);
                this.cursor = new Cursor3D(minX, minY, minZ, maxX, maxY, maxZ);
            }

            @Override
            public boolean tryAdvance(Consumer<? super SectionPos> action) {
                if (this.cursor.advance()) {
                    action.accept(new SectionPos(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
                    return true;
                }
                return false;
            }
        }, false);
    }

    public static void aroundAndAtBlockPos(BlockPos blockPos, LongConsumer sectionConsumer) {
        SectionPos.aroundAndAtBlockPos(blockPos.getX(), blockPos.getY(), blockPos.getZ(), sectionConsumer);
    }

    public static void aroundAndAtBlockPos(long blockPos, LongConsumer sectionConsumer) {
        SectionPos.aroundAndAtBlockPos(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos), sectionConsumer);
    }

    public static void aroundAndAtBlockPos(int blockX, int blockY, int blockZ, LongConsumer sectionConsumer) {
        int minSectionX = SectionPos.blockToSectionCoord(blockX - 1);
        int maxSectionX = SectionPos.blockToSectionCoord(blockX + 1);
        int minSectionY = SectionPos.blockToSectionCoord(blockY - 1);
        int maxSectionY = SectionPos.blockToSectionCoord(blockY + 1);
        int minSectionZ = SectionPos.blockToSectionCoord(blockZ - 1);
        int maxSectionZ = SectionPos.blockToSectionCoord(blockZ + 1);
        if (minSectionX == maxSectionX && minSectionY == maxSectionY && minSectionZ == maxSectionZ) {
            sectionConsumer.accept(SectionPos.asLong(minSectionX, minSectionY, minSectionZ));
        } else {
            for (int sectionX = minSectionX; sectionX <= maxSectionX; ++sectionX) {
                for (int sectionY = minSectionY; sectionY <= maxSectionY; ++sectionY) {
                    for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; ++sectionZ) {
                        sectionConsumer.accept(SectionPos.asLong(sectionX, sectionY, sectionZ));
                    }
                }
            }
        }
    }
}

