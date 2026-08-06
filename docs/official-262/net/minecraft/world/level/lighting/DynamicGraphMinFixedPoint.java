/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ByteMap
 *  it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap
 *  it.unimi.dsi.fastutil.longs.LongArrayList
 *  it.unimi.dsi.fastutil.longs.LongList
 */
package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Objects;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;

public abstract class DynamicGraphMinFixedPoint {
    public static final long SOURCE = Long.MAX_VALUE;
    private static final int NO_COMPUTED_LEVEL = 255;
    protected final int levelCount;
    private final LeveledPriorityQueue priorityQueue;
    private final Long2ByteMap computedLevels;
    private volatile boolean hasWork;

    protected DynamicGraphMinFixedPoint(int levelCount, int minQueueSize, final int minMapSize) {
        if (levelCount >= 254) {
            throw new IllegalArgumentException("Level count must be < 254.");
        }
        this.levelCount = levelCount;
        this.priorityQueue = new LeveledPriorityQueue(levelCount, minQueueSize);
        this.computedLevels = new Long2ByteOpenHashMap(this, minMapSize, 0.5f){
            final /* synthetic */ DynamicGraphMinFixedPoint this$0;
            {
                DynamicGraphMinFixedPoint dynamicGraphMinFixedPoint = this$0;
                Objects.requireNonNull(dynamicGraphMinFixedPoint);
                this.this$0 = dynamicGraphMinFixedPoint;
                super(expected, f);
            }

            protected void rehash(int newN) {
                if (newN > minMapSize) {
                    super.rehash(newN);
                }
            }
        };
        this.computedLevels.defaultReturnValue((byte)-1);
    }

    protected void removeFromQueue(long node) {
        int computedLevel = this.computedLevels.remove(node) & 0xFF;
        if (computedLevel == 255) {
            return;
        }
        int level = this.getLevel(node);
        int priority = this.calculatePriority(level, computedLevel);
        this.priorityQueue.dequeue(node, priority, this.levelCount);
        this.hasWork = !this.priorityQueue.isEmpty();
    }

    public void removeIf(LongPredicate pred) {
        LongArrayList nodesToRemove = new LongArrayList();
        this.computedLevels.keySet().forEach(arg_0 -> DynamicGraphMinFixedPoint.lambda$removeIf$0(pred, (LongList)nodesToRemove, arg_0));
        nodesToRemove.forEach(this::removeFromQueue);
    }

    private int calculatePriority(int level, int computedLevel) {
        return Math.min(Math.min(level, computedLevel), this.levelCount - 1);
    }

    protected void checkNode(long node) {
        this.checkEdge(node, node, this.levelCount - 1, false);
    }

    protected void checkEdge(long from, long to, int newLevelFrom, boolean onlyDecreased) {
        this.checkEdge(from, to, newLevelFrom, this.getLevel(to), this.computedLevels.get(to) & 0xFF, onlyDecreased);
        this.hasWork = !this.priorityQueue.isEmpty();
    }

    private void checkEdge(long from, long to, int newLevelFrom, int levelTo, int oldComputedLevel, boolean onlyDecreased) {
        boolean wasConsistent;
        if (this.isSource(to)) {
            return;
        }
        newLevelFrom = Mth.clamp(newLevelFrom, 0, this.levelCount - 1);
        levelTo = Mth.clamp(levelTo, 0, this.levelCount - 1);
        boolean bl = wasConsistent = oldComputedLevel == 255;
        if (wasConsistent) {
            oldComputedLevel = levelTo;
        }
        int newComputedLevel = onlyDecreased ? Math.min(oldComputedLevel, newLevelFrom) : Mth.clamp(this.getComputedLevel(to, from, newLevelFrom), 0, this.levelCount - 1);
        int oldPriority = this.calculatePriority(levelTo, oldComputedLevel);
        if (levelTo != newComputedLevel) {
            int newPriority = this.calculatePriority(levelTo, newComputedLevel);
            if (oldPriority != newPriority && !wasConsistent) {
                this.priorityQueue.dequeue(to, oldPriority, newPriority);
            }
            this.priorityQueue.enqueue(to, newPriority);
            this.computedLevels.put(to, (byte)newComputedLevel);
        } else if (!wasConsistent) {
            this.priorityQueue.dequeue(to, oldPriority, this.levelCount);
            this.computedLevels.remove(to);
        }
    }

    protected final void checkNeighbor(long from, long to, int level, boolean onlyDecreased) {
        int storedOldComputedLevel = this.computedLevels.get(to) & 0xFF;
        int levelFrom = Mth.clamp(this.computeLevelFromNeighbor(from, to, level), 0, this.levelCount - 1);
        if (onlyDecreased) {
            this.checkEdge(from, to, levelFrom, this.getLevel(to), storedOldComputedLevel, onlyDecreased);
        } else {
            boolean wasConsistent = storedOldComputedLevel == 255;
            int oldComputedLevel = wasConsistent ? Mth.clamp(this.getLevel(to), 0, this.levelCount - 1) : storedOldComputedLevel;
            if (levelFrom == oldComputedLevel) {
                this.checkEdge(from, to, this.levelCount - 1, wasConsistent ? oldComputedLevel : this.getLevel(to), storedOldComputedLevel, onlyDecreased);
            }
        }
    }

    protected final boolean hasWork() {
        return this.hasWork;
    }

    protected final int runUpdates(int count) {
        if (this.priorityQueue.isEmpty()) {
            return count;
        }
        while (!this.priorityQueue.isEmpty() && count > 0) {
            --count;
            long node = this.priorityQueue.removeFirstLong();
            int level = Mth.clamp(this.getLevel(node), 0, this.levelCount - 1);
            int computedLevel = this.computedLevels.remove(node) & 0xFF;
            if (computedLevel < level) {
                this.setLevel(node, computedLevel);
                this.checkNeighborsAfterUpdate(node, computedLevel, true);
                continue;
            }
            if (computedLevel <= level) continue;
            this.setLevel(node, this.levelCount - 1);
            if (computedLevel != this.levelCount - 1) {
                this.priorityQueue.enqueue(node, this.calculatePriority(this.levelCount - 1, computedLevel));
                this.computedLevels.put(node, (byte)computedLevel);
            }
            this.checkNeighborsAfterUpdate(node, level, false);
        }
        this.hasWork = !this.priorityQueue.isEmpty();
        return count;
    }

    public int getQueueSize() {
        return this.computedLevels.size();
    }

    protected boolean isSource(long node) {
        return node == Long.MAX_VALUE;
    }

    protected abstract int getComputedLevel(long var1, long var3, int var5);

    protected abstract void checkNeighborsAfterUpdate(long var1, int var3, boolean var4);

    protected abstract int getLevel(long var1);

    protected abstract void setLevel(long var1, int var3);

    protected abstract int computeLevelFromNeighbor(long var1, long var3, int var5);

    private static /* synthetic */ void lambda$removeIf$0(LongPredicate pred, LongList nodesToRemove, long node) {
        if (pred.test(node)) {
            nodesToRemove.add(node);
        }
    }
}

