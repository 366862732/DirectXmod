/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class LightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>>
implements LayerLightEventListener {
    public static final int MAX_LEVEL = 15;
    protected static final int MIN_OPACITY = 1;
    protected static final long PULL_LIGHT_IN_ENTRY = QueueEntry.decreaseAllDirections(1);
    private static final int MIN_QUEUE_SIZE = 512;
    protected static final Direction[] PROPAGATION_DIRECTIONS = Direction.values();
    protected final LightChunkGetter chunkSource;
    protected final S storage;
    private final LongOpenHashSet blockNodesToCheck = new LongOpenHashSet(512, 0.5f);
    private final LongArrayFIFOQueue decreaseQueue = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue increaseQueue = new LongArrayFIFOQueue();
    private static final int CACHE_SIZE = 2;
    private final long[] lastChunkPos = new long[2];
    private final LightChunk[] lastChunk = new LightChunk[2];

    protected LightEngine(LightChunkGetter chunkSource, S storage) {
        this.chunkSource = chunkSource;
        this.storage = storage;
        this.clearChunkCache();
    }

    public static boolean hasDifferentLightProperties(BlockState oldState, BlockState newState) {
        if (newState == oldState) {
            return false;
        }
        return newState.getLightDampening() != oldState.getLightDampening() || newState.getLightEmission() != oldState.getLightEmission() || newState.useShapeForLightOcclusion() || oldState.useShapeForLightOcclusion();
    }

    public static int getLightDampeningInto(BlockState fromState, BlockState toState, Direction direction, int simpleOpacity) {
        VoxelShape toShape;
        boolean fromEmpty = LightEngine.isEmptyShape(fromState);
        boolean toEmpty = LightEngine.isEmptyShape(toState);
        if (fromEmpty && toEmpty) {
            return simpleOpacity;
        }
        VoxelShape fromShape = fromEmpty ? Shapes.empty() : fromState.getOcclusionShape();
        VoxelShape voxelShape = toShape = toEmpty ? Shapes.empty() : toState.getOcclusionShape();
        if (Shapes.mergedFaceOccludes(fromShape, toShape, direction)) {
            return 16;
        }
        return simpleOpacity;
    }

    public static VoxelShape getOcclusionShape(BlockState state, Direction direction) {
        return LightEngine.isEmptyShape(state) ? Shapes.empty() : state.getFaceOcclusionShape(direction);
    }

    protected static boolean isEmptyShape(BlockState state) {
        return !state.canOcclude() || !state.useShapeForLightOcclusion();
    }

    protected BlockState getState(BlockPos pos) {
        int chunkZ;
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        LightChunk chunk = this.getChunk(chunkX, chunkZ = SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        return chunk.getBlockState(pos);
    }

    protected int getOpacity(BlockState state) {
        return Math.max(1, state.getLightDampening());
    }

    protected boolean shapeOccludes(BlockState fromState, BlockState toState, Direction direction) {
        VoxelShape fromShape = LightEngine.getOcclusionShape(fromState, direction);
        VoxelShape toShape = LightEngine.getOcclusionShape(toState, direction.getOpposite());
        return Shapes.faceShapeOccludes(fromShape, toShape);
    }

    protected @Nullable LightChunk getChunk(int chunkX, int chunkZ) {
        long pos = ChunkPos.pack(chunkX, chunkZ);
        for (int i = 0; i < 2; ++i) {
            if (pos != this.lastChunkPos[i]) continue;
            return this.lastChunk[i];
        }
        LightChunk chunk = this.chunkSource.getChunkForLighting(chunkX, chunkZ);
        for (int i = 1; i > 0; --i) {
            this.lastChunkPos[i] = this.lastChunkPos[i - 1];
            this.lastChunk[i] = this.lastChunk[i - 1];
        }
        this.lastChunkPos[0] = pos;
        this.lastChunk[0] = chunk;
        return chunk;
    }

    private void clearChunkCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunk, null);
    }

    @Override
    public void checkBlock(BlockPos pos) {
        this.blockNodesToCheck.add(pos.asLong());
    }

    public void queueSectionData(long pos, @Nullable DataLayer data) {
        ((LayerLightSectionStorage)this.storage).queueSectionData(pos, data);
    }

    public void retainData(ChunkPos pos, boolean retain) {
        ((LayerLightSectionStorage)this.storage).retainData(SectionPos.getZeroNode(pos.x(), pos.z()), retain);
    }

    @Override
    public void updateSectionStatus(SectionPos pos, boolean sectionEmpty) {
        ((LayerLightSectionStorage)this.storage).updateSectionStatus(pos.asLong(), sectionEmpty);
    }

    @Override
    public void setLightEnabled(ChunkPos pos, boolean enable) {
        ((LayerLightSectionStorage)this.storage).setLightEnabled(SectionPos.getZeroNode(pos.x(), pos.z()), enable);
    }

    @Override
    public int runLightUpdates() {
        LongIterator iterator = this.blockNodesToCheck.iterator();
        while (iterator.hasNext()) {
            this.checkNode(iterator.nextLong());
        }
        this.blockNodesToCheck.clear();
        this.blockNodesToCheck.trim(512);
        int count = 0;
        count += this.propagateDecreases();
        this.clearChunkCache();
        ((LayerLightSectionStorage)this.storage).markNewInconsistencies(this);
        ((LayerLightSectionStorage)this.storage).swapSectionMap();
        return count += this.propagateIncreases();
    }

    private int propagateIncreases() {
        int count = 0;
        while (!this.increaseQueue.isEmpty()) {
            long fromNode = this.increaseQueue.dequeueLong();
            long increaseData = this.increaseQueue.dequeueLong();
            int fromLevel = ((LayerLightSectionStorage)this.storage).getStoredLevel(fromNode);
            int fromTargetLevel = QueueEntry.getFromLevel(increaseData);
            if (QueueEntry.isIncreaseFromEmission(increaseData) && fromLevel < fromTargetLevel) {
                ((LayerLightSectionStorage)this.storage).setStoredLevel(fromNode, fromTargetLevel);
                fromLevel = fromTargetLevel;
            }
            if (fromLevel == fromTargetLevel) {
                this.propagateIncrease(fromNode, increaseData, fromLevel);
            }
            ++count;
        }
        return count;
    }

    private int propagateDecreases() {
        int count = 0;
        while (!this.decreaseQueue.isEmpty()) {
            long fromNode = this.decreaseQueue.dequeueLong();
            long decreaseData = this.decreaseQueue.dequeueLong();
            this.propagateDecrease(fromNode, decreaseData);
            ++count;
        }
        return count;
    }

    protected void enqueueDecrease(long fromNode, long decreaseData) {
        this.decreaseQueue.enqueue(fromNode);
        this.decreaseQueue.enqueue(decreaseData);
    }

    protected void enqueueIncrease(long fromNode, long increaseData) {
        this.increaseQueue.enqueue(fromNode);
        this.increaseQueue.enqueue(increaseData);
    }

    @Override
    public boolean hasLightWork() {
        return ((LayerLightSectionStorage)this.storage).hasInconsistencies() || !this.blockNodesToCheck.isEmpty() || !this.decreaseQueue.isEmpty() || !this.increaseQueue.isEmpty();
    }

    @Override
    public @Nullable DataLayer getDataLayerData(SectionPos pos) {
        return ((LayerLightSectionStorage)this.storage).getDataLayerData(pos.asLong());
    }

    @Override
    public int getLightValue(BlockPos pos) {
        return ((LayerLightSectionStorage)this.storage).getLightValue(pos.asLong());
    }

    public String getDebugData(long sectionNode) {
        return this.getDebugSectionType(sectionNode).display();
    }

    public LayerLightSectionStorage.SectionType getDebugSectionType(long sectionNode) {
        return ((LayerLightSectionStorage)this.storage).getDebugSectionType(sectionNode);
    }

    protected abstract void checkNode(long var1);

    protected abstract void propagateIncrease(long var1, long var3, int var5);

    protected abstract void propagateDecrease(long var1, long var3);

    public static class QueueEntry {
        private static final int FROM_LEVEL_BITS = 4;
        private static final int DIRECTION_BITS = 6;
        private static final long LEVEL_MASK = 15L;
        private static final long DIRECTIONS_MASK = 1008L;
        private static final long FLAG_FROM_EMPTY_SHAPE = 1024L;
        private static final long FLAG_INCREASE_FROM_EMISSION = 2048L;

        public static long decreaseSkipOneDirection(int oldFromLevel, Direction skipDirection) {
            long decreaseData = QueueEntry.withoutDirection(1008L, skipDirection);
            return QueueEntry.withLevel(decreaseData, oldFromLevel);
        }

        public static long decreaseAllDirections(int oldFromLevel) {
            return QueueEntry.withLevel(1008L, oldFromLevel);
        }

        public static long increaseLightFromEmission(int newFromLevel, boolean fromEmptyShape) {
            long increaseData = 1008L;
            increaseData |= 0x800L;
            if (fromEmptyShape) {
                increaseData |= 0x400L;
            }
            return QueueEntry.withLevel(increaseData, newFromLevel);
        }

        public static long increaseSkipOneDirection(int newFromLevel, boolean fromEmptyShape, Direction skipDirection) {
            long increaseData = QueueEntry.withoutDirection(1008L, skipDirection);
            if (fromEmptyShape) {
                increaseData |= 0x400L;
            }
            return QueueEntry.withLevel(increaseData, newFromLevel);
        }

        public static long increaseOnlyOneDirection(int newFromLevel, boolean fromEmptyShape, Direction direction) {
            long increaseData = 0L;
            if (fromEmptyShape) {
                increaseData |= 0x400L;
            }
            increaseData = QueueEntry.withDirection(increaseData, direction);
            return QueueEntry.withLevel(increaseData, newFromLevel);
        }

        public static long increaseSkySourceInDirections(boolean down, boolean north, boolean south, boolean west, boolean east) {
            long increaseData = QueueEntry.withLevel(0L, 15);
            if (down) {
                increaseData = QueueEntry.withDirection(increaseData, Direction.DOWN);
            }
            if (north) {
                increaseData = QueueEntry.withDirection(increaseData, Direction.NORTH);
            }
            if (south) {
                increaseData = QueueEntry.withDirection(increaseData, Direction.SOUTH);
            }
            if (west) {
                increaseData = QueueEntry.withDirection(increaseData, Direction.WEST);
            }
            if (east) {
                increaseData = QueueEntry.withDirection(increaseData, Direction.EAST);
            }
            return increaseData;
        }

        public static int getFromLevel(long entry) {
            return (int)(entry & 0xFL);
        }

        public static boolean isFromEmptyShape(long entry) {
            return (entry & 0x400L) != 0L;
        }

        public static boolean isIncreaseFromEmission(long entry) {
            return (entry & 0x800L) != 0L;
        }

        public static boolean shouldPropagateInDirection(long entry, Direction direction) {
            return (entry & 1L << direction.ordinal() + 4) != 0L;
        }

        private static long withLevel(long entry, int level) {
            return entry & 0xFFFFFFFFFFFFFFF0L | (long)level & 0xFL;
        }

        private static long withDirection(long entry, Direction direction) {
            return entry | 1L << direction.ordinal() + 4;
        }

        private static long withoutDirection(long entry, Direction direction) {
            return entry & (1L << direction.ordinal() + 4 ^ 0xFFFFFFFFFFFFFFFFL);
        }
    }
}

