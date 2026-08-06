/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public interface LevelHeightAccessor {
    public int getHeight();

    public int getMinY();

    default public int getMaxY() {
        return this.getMinY() + this.getHeight() - 1;
    }

    default public int getSectionsCount() {
        return this.getMaxSectionY() - this.getMinSectionY() + 1;
    }

    default public int getMinSectionY() {
        return SectionPos.blockToSectionCoord(this.getMinY());
    }

    default public int getMaxSectionY() {
        return SectionPos.blockToSectionCoord(this.getMaxY());
    }

    default public boolean isInsideBuildHeight(BlockPos pos) {
        return this.isInsideBuildHeight(pos.getY());
    }

    default public boolean isInsideBuildHeight(int blockY) {
        return blockY >= this.getMinY() && blockY <= this.getMaxY();
    }

    default public boolean isOutsideBuildHeight(BlockPos pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    default public boolean isOutsideBuildHeight(int blockY) {
        return blockY < this.getMinY() || blockY > this.getMaxY();
    }

    default public int getSectionIndex(int blockY) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(blockY));
    }

    default public int getSectionIndexFromSectionY(int sectionY) {
        return sectionY - this.getMinSectionY();
    }

    default public int getSectionYFromSectionIndex(int sectionIndex) {
        return sectionIndex + this.getMinSectionY();
    }

    public static LevelHeightAccessor create(final int minY, final int height) {
        return new LevelHeightAccessor(){

            @Override
            public int getHeight() {
                return height;
            }

            @Override
            public int getMinY() {
                return minY;
            }
        };
    }
}

