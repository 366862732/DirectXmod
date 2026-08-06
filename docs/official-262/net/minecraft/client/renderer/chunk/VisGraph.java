/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class VisGraph {
    private static final int SIZE_IN_BITS = 4;
    private static final int LEN = 16;
    private static final int MASK = 15;
    private static final int SIZE = 4096;
    private static final int X_SHIFT = 0;
    private static final int Z_SHIFT = 4;
    private static final int Y_SHIFT = 8;
    private static final int DX = (int)Math.pow(16.0, 0.0);
    private static final int DZ = (int)Math.pow(16.0, 1.0);
    private static final int DY = (int)Math.pow(16.0, 2.0);
    private static final int INVALID_INDEX = -1;
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BitSet bitSet = new BitSet(4096);
    private static final int[] INDEX_OF_EDGES = Util.make(new int[1352], map -> {
        boolean min = false;
        int max = 15;
        int index = 0;
        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                for (int z = 0; z < 16; ++z) {
                    if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) continue;
                    map[index++] = VisGraph.getIndex(x, y, z);
                }
            }
        }
    });
    private int empty = 4096;

    public void setOpaque(BlockPos pos) {
        this.bitSet.set(VisGraph.getIndex(pos), true);
        --this.empty;
    }

    private static int getIndex(BlockPos pos) {
        return VisGraph.getIndex(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
    }

    private static int getIndex(int x, int y, int z) {
        return x << 0 | y << 8 | z << 4;
    }

    public VisibilitySet resolve() {
        VisibilitySet visibilitySet = new VisibilitySet();
        if (4096 - this.empty < 256) {
            visibilitySet.setAll(true);
        } else if (this.empty == 0) {
            visibilitySet.setAll(false);
        } else {
            for (int i : INDEX_OF_EDGES) {
                if (this.bitSet.get(i)) continue;
                visibilitySet.add(this.floodFill(i));
            }
        }
        return visibilitySet;
    }

    private Set<Direction> floodFill(int startIndex) {
        EnumSet<Direction> edges = EnumSet.noneOf(Direction.class);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        queue.enqueue(startIndex);
        this.bitSet.set(startIndex, true);
        while (!queue.isEmpty()) {
            int index = queue.dequeueInt();
            this.addEdges(index, edges);
            for (Direction direction : DIRECTIONS) {
                int neighborIndex = this.getNeighborIndexAtFace(index, direction);
                if (neighborIndex < 0 || this.bitSet.get(neighborIndex)) continue;
                this.bitSet.set(neighborIndex, true);
                queue.enqueue(neighborIndex);
            }
        }
        return edges;
    }

    private void addEdges(int index, Set<Direction> edges) {
        int x = index >> 0 & 0xF;
        if (x == 0) {
            edges.add(Direction.WEST);
        } else if (x == 15) {
            edges.add(Direction.EAST);
        }
        int y = index >> 8 & 0xF;
        if (y == 0) {
            edges.add(Direction.DOWN);
        } else if (y == 15) {
            edges.add(Direction.UP);
        }
        int z = index >> 4 & 0xF;
        if (z == 0) {
            edges.add(Direction.NORTH);
        } else if (z == 15) {
            edges.add(Direction.SOUTH);
        }
    }

    private int getNeighborIndexAtFace(int index, Direction direction) {
        return switch (direction) {
            default -> throw new MatchException(null, null);
            case Direction.DOWN -> {
                if ((index >> 8 & 0xF) == 0) {
                    yield -1;
                }
                yield index - DY;
            }
            case Direction.UP -> {
                if ((index >> 8 & 0xF) == 15) {
                    yield -1;
                }
                yield index + DY;
            }
            case Direction.NORTH -> {
                if ((index >> 4 & 0xF) == 0) {
                    yield -1;
                }
                yield index - DZ;
            }
            case Direction.SOUTH -> {
                if ((index >> 4 & 0xF) == 15) {
                    yield -1;
                }
                yield index + DZ;
            }
            case Direction.WEST -> {
                if ((index >> 0 & 0xF) == 0) {
                    yield -1;
                }
                yield index - DX;
            }
            case Direction.EAST -> (index >> 0 & 0xF) == 15 ? -1 : index + DX;
        };
    }
}

