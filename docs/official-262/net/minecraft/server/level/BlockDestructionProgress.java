/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.level;

import net.minecraft.core.BlockPos;

public class BlockDestructionProgress
implements Comparable<BlockDestructionProgress> {
    private final int id;
    private final BlockPos pos;
    private int progress;
    private long updatedRenderTick;

    public BlockDestructionProgress(int id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    public int getId() {
        return this.id;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void setProgress(int progress) {
        if (progress > 10) {
            progress = 10;
        }
        this.progress = progress;
    }

    public int getProgress() {
        return this.progress;
    }

    public void updateTick(long tick) {
        this.updatedRenderTick = tick;
    }

    public long getUpdatedRenderTick() {
        return this.updatedRenderTick;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        BlockDestructionProgress that = (BlockDestructionProgress)o;
        return this.id == that.id;
    }

    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    @Override
    public int compareTo(BlockDestructionProgress o) {
        if (this.progress != o.progress) {
            return Integer.compare(this.progress, o.progress);
        }
        return Integer.compare(this.id, o.id);
    }
}

