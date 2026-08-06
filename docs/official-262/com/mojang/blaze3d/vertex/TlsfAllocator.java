/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.vertex;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TlsfAllocator {
    private static final int SECOND_LEVEL_BIN_LOG2 = 3;
    private static final int SECOND_LEVEL_BIN_COUNT = 8;
    private static final int FIRST_LEVEL_INDEX_SHIFT = 8;
    private static final int FIRST_LEVEL_BIN_COUNT = 32;
    private static final int SMALL_BLOCK_SIZE = 256;
    private static final long MAX_ALLOCATION_SIZE = 0x8000000000L;
    private static final int ALIGN_SIZE = 32;
    private int firstLevelBitmap = 0;
    private final int[] secondLevelBitmap = new int[32];
    private final @Nullable Block[] freeLists = new Block[256];
    private final long totalMemorySize;
    private static final Logger LOGGER = LogUtils.getLogger();

    public TlsfAllocator(Heap heap) {
        long alignedHeapSize = TlsfAllocator.alignDown(heap.size, 32L);
        Block freeBlock = new Block(alignedHeapSize, heap, 0L, null, null, null, null);
        freeBlock.setFree();
        this.insertFreeBlock(freeBlock);
        long remainingHeapSize = heap.size - alignedHeapSize;
        Block sentinelBlock = new Block(remainingHeapSize, heap, alignedHeapSize, null, null, null, freeBlock);
        sentinelBlock.setUsed();
        freeBlock.nextPhysicalBlock = sentinelBlock;
        this.totalMemorySize = alignedHeapSize;
    }

    private @Nullable Block getBlockFromFreeList(int firstLevelIndex, int secondLevelIndex) {
        return this.freeLists[firstLevelIndex * 8 + secondLevelIndex];
    }

    private void setBlockFreeList(int firstLevelIndex, int secondLevelIndex, @Nullable Block block) {
        this.freeLists[firstLevelIndex * 8 + secondLevelIndex] = block;
    }

    private static long alignUp(long x, long align) {
        return x + (align - 1L) & (align - 1L ^ 0xFFFFFFFFFFFFFFFFL);
    }

    private static long alignDown(long x, long align) {
        return x - (x & align - 1L);
    }

    private static int findLastSignificantBit(long x) {
        return 63 - Long.numberOfLeadingZeros(x);
    }

    private static int findFirstSignificantBit(int x) {
        return Integer.numberOfTrailingZeros(x);
    }

    private IndexPair getLevelIndex(long size) {
        if (Long.compareUnsigned(size, 256L) < 0) {
            boolean firstLevelIndex = false;
            int secondLevelIndex = (int)Long.divideUnsigned(size, 32L);
            return new IndexPair(0, secondLevelIndex);
        }
        int firstLevelIndex = TlsfAllocator.findLastSignificantBit(size);
        int secondLevelIndex = (int)(size >>> firstLevelIndex - 3) ^ 8;
        return new IndexPair(firstLevelIndex -= 7, secondLevelIndex);
    }

    private IndexPair mappingSearch(long size) {
        long roundedSize = size;
        if (Long.compareUnsigned(size, 256L) >= 0) {
            long round = (1L << TlsfAllocator.findLastSignificantBit(size) - 3) - 1L;
            roundedSize += round;
        }
        return this.getLevelIndex(roundedSize);
    }

    private void insertFreeBlock(Block block) {
        IndexPair levelIndex = this.getLevelIndex(block.getSize());
        Block currentBlock = this.getBlockFromFreeList(levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
        if (currentBlock != null) {
            currentBlock.previousFreeBlock = block;
        }
        block.nextFreeBlock = currentBlock;
        this.firstLevelBitmap |= 1 << levelIndex.firstLevelIndex;
        int n = levelIndex.firstLevelIndex;
        this.secondLevelBitmap[n] = this.secondLevelBitmap[n] | 1 << levelIndex.secondLevelIndex;
        this.setBlockFreeList(levelIndex.firstLevelIndex, levelIndex.secondLevelIndex, block);
    }

    private void removeFreeBlock(Block block, int firstLevel, int secondLevel) {
        Block next = block.nextFreeBlock;
        Block previous = block.previousFreeBlock;
        if (previous != null) {
            previous.nextFreeBlock = next;
        }
        if (next != null) {
            next.previousFreeBlock = previous;
        }
        if (this.getBlockFromFreeList(firstLevel, secondLevel) == block) {
            this.setBlockFreeList(firstLevel, secondLevel, next);
            if (next == null) {
                int n = firstLevel;
                this.secondLevelBitmap[n] = this.secondLevelBitmap[n] & ~(1 << secondLevel);
                if (this.secondLevelBitmap[firstLevel] == 0) {
                    this.firstLevelBitmap &= ~(1 << firstLevel);
                }
            }
        }
    }

    private void trimBlock(Block block, long size) {
        if (Long.compareUnsigned(block.getSize(), size + 256L) > 0) {
            long remaining = block.getSize() - size;
            Block remainingBlock = new Block(remaining, block.heap, block.offsetFromHeap + size, null, null, block.nextPhysicalBlock, block);
            remainingBlock.setFree();
            Block next = block.nextPhysicalBlock;
            if (next != null) {
                assert (next.previousPhysicalBlock == block);
                next.previousPhysicalBlock = remainingBlock;
            }
            block.nextPhysicalBlock = remainingBlock;
            block.setSize(size);
            this.mergeBlockWithNext(remainingBlock);
            this.insertFreeBlock(remainingBlock);
        }
    }

    public @Nullable Allocation allocate(long size, int align) {
        boolean isPowerOfTwo = Mth.isPowerOfTwo(align);
        int sizePadding = !isPowerOfTwo || align > 32 ? align : 0;
        long alignedSize = TlsfAllocator.alignUp(size + (long)sizePadding, 32L);
        assert (alignedSize <= 0x8000000000L);
        IndexPair levelIndex = this.mappingSearch(alignedSize);
        int firstLevelIndex = levelIndex.firstLevelIndex;
        int secondLevelIndex = levelIndex.secondLevelIndex;
        long firstLevelSize = 1L << firstLevelIndex + 8 - 1;
        long secondLevelInterval = firstLevelSize / 8L;
        long slotSize = firstLevelSize + (long)secondLevelIndex * secondLevelInterval;
        if (firstLevelIndex < 32) {
            int slBitmap = this.secondLevelBitmap[firstLevelIndex] & -1 << secondLevelIndex;
            if (slBitmap == 0) {
                int flBitmap = this.firstLevelBitmap & -1 << firstLevelIndex + 1;
                if (flBitmap == 0) {
                    return null;
                }
                firstLevelIndex = TlsfAllocator.findFirstSignificantBit(flBitmap);
                slBitmap = this.secondLevelBitmap[firstLevelIndex];
            }
            secondLevelIndex = TlsfAllocator.findFirstSignificantBit(slBitmap);
            Block block = this.getBlockFromFreeList(firstLevelIndex, secondLevelIndex);
            assert (block != null && block.getSize() >= alignedSize);
            this.removeFreeBlock(block, firstLevelIndex, secondLevelIndex);
            this.trimBlock(block, slotSize);
            block.setUsed();
            long gap = Long.remainderUnsigned(block.offsetFromHeap, align);
            long alignmentOffset = gap == 0L ? 0L : (long)align - gap;
            long allocationOffset = block.offsetFromHeap + alignmentOffset;
            return new Allocation(block, allocationOffset);
        }
        return null;
    }

    private void mergeBlockWithPrevious(Block block) {
        Block previous = block.previousPhysicalBlock;
        if (previous != null && previous.isFree()) {
            assert (previous.offsetFromHeap + previous.getSize() == block.offsetFromHeap);
            assert (previous.nextPhysicalBlock == block);
            IndexPair levelIndex = this.getLevelIndex(previous.getSize());
            this.removeFreeBlock(previous, levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
            Block prevprev = previous.previousPhysicalBlock;
            if (prevprev != null) {
                assert (prevprev.nextPhysicalBlock == previous);
                prevprev.nextPhysicalBlock = block;
            }
            block.previousPhysicalBlock = prevprev;
            block.setSize(block.getSize() + previous.getSize());
            block.offsetFromHeap = previous.offsetFromHeap;
            previous.previousPhysicalBlock = null;
            previous.nextPhysicalBlock = null;
            previous.nextFreeBlock = null;
            previous.previousFreeBlock = null;
        }
    }

    private void mergeBlockWithNext(Block block) {
        Block next = block.nextPhysicalBlock;
        if (next != null && next.isFree()) {
            assert (next.offsetFromHeap - block.getSize() == block.offsetFromHeap);
            assert (next.previousPhysicalBlock == block);
            IndexPair levelIndex = this.getLevelIndex(next.getSize());
            this.removeFreeBlock(next, levelIndex.firstLevelIndex, levelIndex.secondLevelIndex);
            Block nextnext = next.nextPhysicalBlock;
            if (nextnext != null) {
                assert (nextnext.previousPhysicalBlock == next);
                nextnext.previousPhysicalBlock = block;
            }
            block.nextPhysicalBlock = nextnext;
            block.setSize(block.getSize() + next.getSize());
            next.previousPhysicalBlock = null;
            next.nextPhysicalBlock = null;
            next.nextFreeBlock = null;
            next.previousFreeBlock = null;
        }
    }

    public void free(Allocation allocation) {
        if (!allocation.freed) {
            Block block = allocation.block;
            block.setFree();
            this.mergeBlockWithPrevious(block);
            this.mergeBlockWithNext(block);
            this.insertFreeBlock(block);
            allocation.freed = true;
        }
    }

    public boolean isCompletelyFree() {
        int firstLevelIndex;
        int slBitmap;
        if (Long.bitCount(this.firstLevelBitmap) == 1 && Long.bitCount(slBitmap = this.secondLevelBitmap[firstLevelIndex = TlsfAllocator.findFirstSignificantBit(this.firstLevelBitmap)]) == 1) {
            int secondLevelIndex = TlsfAllocator.findFirstSignificantBit(slBitmap);
            Block freeBlock = this.getBlockFromFreeList(firstLevelIndex, secondLevelIndex);
            return freeBlock != null && freeBlock.getSize() == this.totalMemorySize;
        }
        return false;
    }

    @VisibleForDebug
    public void printAllocatorStatistics(String name) {
        int freeBlockCount = 0;
        long freeMemorySize = 0L;
        int levelFreeBlockCount = 0;
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 8; ++j) {
                levelFreeBlockCount = 0;
                Block block = this.getBlockFromFreeList(i, j);
                while (block != null) {
                    ++levelFreeBlockCount;
                    freeMemorySize += block.getSize();
                    block = block.nextFreeBlock;
                }
                freeBlockCount += levelFreeBlockCount;
            }
        }
        double unusedPercent = (double)freeMemorySize / (double)this.totalMemorySize * 100.0;
        LOGGER.debug("Uber buffer {}, size: {} -- free-listed memory: {} -- free-block count:{}", new Object[]{name, this.totalMemorySize, unusedPercent, freeBlockCount});
    }

    @Environment(value=EnvType.CLIENT)
    private static class Block {
        private long size = 0L;
        public final Heap heap;
        public long offsetFromHeap;
        public @Nullable Block nextFreeBlock;
        public @Nullable Block previousFreeBlock;
        public @Nullable Block nextPhysicalBlock;
        public @Nullable Block previousPhysicalBlock;
        private static final int BLOCK_HEADER_FREE_BIT = 1;

        private Block(long size, Heap heap, long offsetFromHeap, @Nullable Block nextFreeBlock, @Nullable Block previousFreeBlock, @Nullable Block nextPhysicalBlock, @Nullable Block previousPhysicalBlock) {
            this.heap = heap;
            this.offsetFromHeap = offsetFromHeap;
            this.nextFreeBlock = nextFreeBlock;
            this.previousFreeBlock = previousFreeBlock;
            this.nextPhysicalBlock = nextPhysicalBlock;
            this.previousPhysicalBlock = previousPhysicalBlock;
            this.setSize(size);
        }

        private boolean isFree() {
            return (this.size & 1L) == 1L;
        }

        private void setFree() {
            this.size |= 1L;
        }

        private void setUsed() {
            this.size &= 0xFFFFFFFFFFFFFFFEL;
        }

        private long getSize() {
            return this.size & 0xFFFFFFFFFFFFFFFEL;
        }

        private void setSize(long size) {
            long oldSize = this.size;
            this.size = size | oldSize & 1L;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Heap {
        private final long size;

        public Heap(long size) {
            this.size = size;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record IndexPair(int firstLevelIndex, int secondLevelIndex) {
    }

    @Environment(value=EnvType.CLIENT)
    public static class Allocation {
        private final Block block;
        private final long offsetFromHeap;
        private boolean freed = false;

        private Allocation(Block block, long offsetFromHeap) {
            this.block = block;
            this.offsetFromHeap = offsetFromHeap;
        }

        public long getSize() {
            return this.block.getSize();
        }

        public Heap getHeap() {
            return this.block.heap;
        }

        public long getOffsetFromHeap() {
            return this.offsetFromHeap;
        }

        public boolean isFreed() {
            return this.freed;
        }
    }
}

