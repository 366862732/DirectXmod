/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkSkyLightSources {
    private static final int SIZE = 16;
    public static final int NEGATIVE_INFINITY = Integer.MIN_VALUE;
    private final int minY;
    private final BitStorage heightmap;
    private final BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos mutablePos2 = new BlockPos.MutableBlockPos();

    public ChunkSkyLightSources(LevelHeightAccessor level) {
        this.minY = level.getMinY() - 1;
        int maxY = level.getMaxY() + 1;
        int bits = Mth.ceillog2(maxY - this.minY + 1);
        this.heightmap = new SimpleBitStorage(bits, 256);
    }

    public void fillFrom(ChunkAccess chunk) {
        int maxSectionIndex = chunk.getHighestFilledSectionIndex();
        if (maxSectionIndex == -1) {
            this.fill(this.minY);
            return;
        }
        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                int initialEdgeY = Math.max(this.findLowestSourceY(chunk, maxSectionIndex, x, z), this.minY);
                this.set(ChunkSkyLightSources.index(x, z), initialEdgeY);
            }
        }
    }

    private int findLowestSourceY(ChunkAccess chunk, int topSectionIndex, int x, int z) {
        int topY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(topSectionIndex) + 1);
        BlockPos.MutableBlockPos topPos = this.mutablePos1.set(x, topY, z);
        BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset((Vec3i)topPos, Direction.DOWN);
        BlockState topState = Blocks.AIR.defaultBlockState();
        for (int sectionIndex = topSectionIndex; sectionIndex >= 0; --sectionIndex) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section.hasOnlyAir()) {
                topState = Blocks.AIR.defaultBlockState();
                int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                topPos.setY(SectionPos.sectionToBlockCoord(sectionY));
                bottomPos.setY(topPos.getY() - 1);
                continue;
            }
            for (int y = 15; y >= 0; --y) {
                BlockState bottomState = section.getBlockState(x, y, z);
                if (ChunkSkyLightSources.isEdgeOccluded(topState, bottomState)) {
                    return topPos.getY();
                }
                topState = bottomState;
                topPos.set(bottomPos);
                bottomPos.move(Direction.DOWN);
            }
        }
        return this.minY;
    }

    public boolean update(BlockGetter level, int x, int y, int z) {
        BlockState middleState;
        BlockPos.MutableBlockPos middlePos;
        BlockState topState;
        int upperEdgeY = y + 1;
        int index = ChunkSkyLightSources.index(x, z);
        int currentLowestSourceY = this.get(index);
        if (upperEdgeY < currentLowestSourceY) {
            return false;
        }
        BlockPos.MutableBlockPos topPos = this.mutablePos1.set(x, y + 1, z);
        if (this.updateEdge(level, index, currentLowestSourceY, topPos, topState = level.getBlockState(topPos), middlePos = this.mutablePos2.set(x, y, z), middleState = level.getBlockState(middlePos))) {
            return true;
        }
        BlockPos.MutableBlockPos bottomPos = this.mutablePos1.set(x, y - 1, z);
        BlockState bottomState = level.getBlockState(bottomPos);
        return this.updateEdge(level, index, currentLowestSourceY, middlePos, middleState, bottomPos, bottomState);
    }

    private boolean updateEdge(BlockGetter level, int index, int oldTopEdgeY, BlockPos topPos, BlockState topState, BlockPos bottomPos, BlockState bottomState) {
        int checkedEdgeY = topPos.getY();
        if (ChunkSkyLightSources.isEdgeOccluded(topState, bottomState)) {
            if (checkedEdgeY > oldTopEdgeY) {
                this.set(index, checkedEdgeY);
                return true;
            }
        } else if (checkedEdgeY == oldTopEdgeY) {
            this.set(index, this.findLowestSourceBelow(level, bottomPos, bottomState));
            return true;
        }
        return false;
    }

    private int findLowestSourceBelow(BlockGetter level, BlockPos startPos, BlockState startState) {
        BlockPos.MutableBlockPos topPos = this.mutablePos1.set(startPos);
        BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset((Vec3i)startPos, Direction.DOWN);
        BlockState topState = startState;
        while (bottomPos.getY() >= this.minY) {
            BlockState bottomState = level.getBlockState(bottomPos);
            if (ChunkSkyLightSources.isEdgeOccluded(topState, bottomState)) {
                return topPos.getY();
            }
            topState = bottomState;
            topPos.set(bottomPos);
            bottomPos.move(Direction.DOWN);
        }
        return this.minY;
    }

    private static boolean isEdgeOccluded(BlockState topState, BlockState bottomState) {
        if (bottomState.getLightDampening() != 0) {
            return true;
        }
        VoxelShape topShape = LightEngine.getOcclusionShape(topState, Direction.DOWN);
        VoxelShape bottomShape = LightEngine.getOcclusionShape(bottomState, Direction.UP);
        return Shapes.faceShapeOccludes(topShape, bottomShape);
    }

    public int getLowestSourceY(int x, int z) {
        int value = this.get(ChunkSkyLightSources.index(x, z));
        return this.extendSourcesBelowWorld(value);
    }

    public int getHighestLowestSourceY() {
        int maxValue = Integer.MIN_VALUE;
        for (int i = 0; i < this.heightmap.getSize(); ++i) {
            int value = this.heightmap.get(i);
            if (value <= maxValue) continue;
            maxValue = value;
        }
        return this.extendSourcesBelowWorld(maxValue + this.minY);
    }

    private void fill(int lowestSourceY) {
        int value = lowestSourceY - this.minY;
        for (int i = 0; i < this.heightmap.getSize(); ++i) {
            this.heightmap.set(i, value);
        }
    }

    private void set(int index, int value) {
        this.heightmap.set(index, value - this.minY);
    }

    private int get(int index) {
        return this.heightmap.get(index) + this.minY;
    }

    private int extendSourcesBelowWorld(int value) {
        if (value == this.minY) {
            return Integer.MIN_VALUE;
        }
        return value;
    }

    private static int index(int x, int z) {
        return x + z * 16;
    }
}

