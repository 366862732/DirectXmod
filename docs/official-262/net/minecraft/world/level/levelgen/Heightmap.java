/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  it.unimi.dsi.fastutil.objects.ObjectListIterator
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Predicate<BlockState> NOT_AIR = input -> !input.isAir();
    private static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockBehaviour.BlockStateBase::blocksMotion;
    private final BitStorage data;
    private final Predicate<BlockState> isOpaque;
    private final ChunkAccess chunk;

    public Heightmap(ChunkAccess chunk, Types heightmapType) {
        this.isOpaque = heightmapType.isOpaque();
        this.chunk = chunk;
        int heightBits = Mth.ceillog2(chunk.getHeight() + 1);
        this.data = new SimpleBitStorage(heightBits, 256);
    }

    public static void primeHeightmaps(ChunkAccess chunk, Set<Types> types) {
        if (types.isEmpty()) {
            return;
        }
        int size = types.size();
        ObjectArrayList heightmaps = new ObjectArrayList(size);
        ObjectListIterator iterator = heightmaps.iterator();
        int highestSectionPosition = chunk.getHighestSectionPosition() + 16;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; ++x) {
            block1: for (int z = 0; z < 16; ++z) {
                for (Types type : types) {
                    heightmaps.add((Object)chunk.getOrCreateHeightmapUnprimed(type));
                }
                for (int y = highestSectionPosition - 1; y >= chunk.getMinY(); --y) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.is(Blocks.AIR)) continue;
                    while (iterator.hasNext()) {
                        Heightmap heightmap = (Heightmap)iterator.next();
                        if (!heightmap.isOpaque.test(state)) continue;
                        heightmap.setHeight(x, z, y + 1);
                        iterator.remove();
                    }
                    if (heightmaps.isEmpty()) continue block1;
                    iterator.back(size);
                }
            }
        }
    }

    public boolean update(int localX, int localY, int localZ, BlockState state) {
        int firstAvailable = this.getFirstAvailable(localX, localZ);
        if (localY <= firstAvailable - 2) {
            return false;
        }
        if (this.isOpaque.test(state)) {
            if (localY >= firstAvailable) {
                this.setHeight(localX, localZ, localY + 1);
                return true;
            }
        } else if (firstAvailable - 1 == localY) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = localY - 1; y >= this.chunk.getMinY(); --y) {
                pos.set(localX, y, localZ);
                if (!this.isOpaque.test(this.chunk.getBlockState(pos))) continue;
                this.setHeight(localX, localZ, y + 1);
                return true;
            }
            this.setHeight(localX, localZ, this.chunk.getMinY());
            return true;
        }
        return false;
    }

    public int getFirstAvailable(int x, int z) {
        return this.getFirstAvailable(Heightmap.getIndex(x, z));
    }

    public int getHighestTaken(int x, int z) {
        return this.getFirstAvailable(Heightmap.getIndex(x, z)) - 1;
    }

    private int getFirstAvailable(int index) {
        return this.data.get(index) + this.chunk.getMinY();
    }

    private void setHeight(int x, int z, int height) {
        this.data.set(Heightmap.getIndex(x, z), height - this.chunk.getMinY());
    }

    public void setRawData(ChunkAccess chunk, Types type, long[] data) {
        long[] rawData = this.data.getRaw();
        if (rawData.length == data.length) {
            System.arraycopy(data, 0, rawData, 0, data.length);
            return;
        }
        LOGGER.warn("Ignoring heightmap data for chunk {}, size does not match; expected: {}, got: {}", new Object[]{chunk.getPos(), rawData.length, data.length});
        Heightmap.primeHeightmaps(chunk, EnumSet.of(type));
    }

    public long[] getRawData() {
        return this.data.getRaw();
    }

    private static int getIndex(int x, int z) {
        return x + z * 16;
    }

    public static enum Types implements StringRepresentable
    {
        WORLD_SURFACE_WG(0, "WORLD_SURFACE_WG", Usage.WORLDGEN, NOT_AIR),
        WORLD_SURFACE(1, "WORLD_SURFACE", Usage.CLIENT, NOT_AIR),
        OCEAN_FLOOR_WG(2, "OCEAN_FLOOR_WG", Usage.WORLDGEN, MATERIAL_MOTION_BLOCKING),
        OCEAN_FLOOR(3, "OCEAN_FLOOR", Usage.LIVE_WORLD, MATERIAL_MOTION_BLOCKING),
        MOTION_BLOCKING(4, "MOTION_BLOCKING", Usage.CLIENT, input -> input.blocksMotion() || !input.getFluidState().isEmpty()),
        MOTION_BLOCKING_NO_LEAVES(5, "MOTION_BLOCKING_NO_LEAVES", Usage.CLIENT, input -> (input.blocksMotion() || !input.getFluidState().isEmpty()) && !(input.getBlock() instanceof LeavesBlock));

        public static final Codec<Types> CODEC;
        private static final IntFunction<Types> BY_ID;
        public static final StreamCodec<ByteBuf, Types> STREAM_CODEC;
        private final int id;
        private final String serializationKey;
        private final Usage usage;
        private final Predicate<BlockState> isOpaque;

        private Types(int id, String serializationKey, Usage usage, Predicate<BlockState> isOpaque) {
            this.id = id;
            this.serializationKey = serializationKey;
            this.usage = usage;
            this.isOpaque = isOpaque;
        }

        public String getSerializationKey() {
            return this.serializationKey;
        }

        public boolean sendToClient() {
            return this.usage == Usage.CLIENT;
        }

        public boolean keepAfterWorldgen() {
            return this.usage != Usage.WORLDGEN;
        }

        public Predicate<BlockState> isOpaque() {
            return this.isOpaque;
        }

        @Override
        public String getSerializedName() {
            return this.serializationKey;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Types::values);
            BY_ID = ByIdMap.continuous(t -> t.id, Types.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, t -> t.id);
        }
    }

    public static enum Usage {
        WORLDGEN,
        LIVE_WORLD,
        CLIENT;

    }
}

