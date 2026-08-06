/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector3i
 */
package net.minecraft.world.phys.shapes;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import org.joml.Vector3i;

public abstract class DiscreteVoxelShape {
    private static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
    protected final int xSize;
    protected final int ySize;
    protected final int zSize;

    protected DiscreteVoxelShape(int xSize, int ySize, int zSize) {
        if (xSize < 0 || ySize < 0 || zSize < 0) {
            throw new IllegalArgumentException("Need all positive sizes: x: " + xSize + ", y: " + ySize + ", z: " + zSize);
        }
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
    }

    public DiscreteVoxelShape rotate(OctahedralGroup rotation) {
        if (rotation == OctahedralGroup.IDENTITY) {
            return this;
        }
        Vector3i v = rotation.rotate(new Vector3i(this.xSize, this.ySize, this.zSize));
        int shiftX = DiscreteVoxelShape.fixupCoordinate(v, 0);
        int shiftY = DiscreteVoxelShape.fixupCoordinate(v, 1);
        int shiftZ = DiscreteVoxelShape.fixupCoordinate(v, 2);
        BitSetDiscreteVoxelShape newShape = new BitSetDiscreteVoxelShape(v.x, v.y, v.z);
        for (int x = 0; x < this.xSize; ++x) {
            for (int y = 0; y < this.ySize; ++y) {
                for (int z = 0; z < this.zSize; ++z) {
                    if (!this.isFull(x, y, z)) continue;
                    Vector3i newPos = rotation.rotate(v.set(x, y, z));
                    int newX = shiftX + newPos.x;
                    int newY = shiftY + newPos.y;
                    int newZ = shiftZ + newPos.z;
                    ((DiscreteVoxelShape)newShape).fill(newX, newY, newZ);
                }
            }
        }
        return newShape;
    }

    private static int fixupCoordinate(Vector3i v, int index) {
        int value = v.get(index);
        if (value < 0) {
            v.setComponent(index, -value);
            return -value - 1;
        }
        return 0;
    }

    public boolean isFullWide(AxisCycle transform, int x, int y, int z) {
        return this.isFullWide(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
    }

    public boolean isFullWide(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) {
            return false;
        }
        if (x >= this.xSize || y >= this.ySize || z >= this.zSize) {
            return false;
        }
        return this.isFull(x, y, z);
    }

    public boolean isFull(AxisCycle transform, int x, int y, int z) {
        return this.isFull(transform.cycle(x, y, z, Direction.Axis.X), transform.cycle(x, y, z, Direction.Axis.Y), transform.cycle(x, y, z, Direction.Axis.Z));
    }

    public abstract boolean isFull(int var1, int var2, int var3);

    public abstract void fill(int var1, int var2, int var3);

    public boolean isEmpty() {
        for (Direction.Axis axis : AXIS_VALUES) {
            if (this.firstFull(axis) < this.lastFull(axis)) continue;
            return true;
        }
        return false;
    }

    public abstract int firstFull(Direction.Axis var1);

    public abstract int lastFull(Direction.Axis var1);

    public int firstFull(Direction.Axis aAxis, int b, int c) {
        int aSize = this.getSize(aAxis);
        if (b < 0 || c < 0) {
            return aSize;
        }
        Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
        Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
        if (b >= this.getSize(bAxis) || c >= this.getSize(cAxis)) {
            return aSize;
        }
        AxisCycle transform = AxisCycle.between(Direction.Axis.X, aAxis);
        for (int a = 0; a < aSize; ++a) {
            if (!this.isFull(transform, a, b, c)) continue;
            return a;
        }
        return aSize;
    }

    public int lastFull(Direction.Axis aAxis, int b, int c) {
        if (b < 0 || c < 0) {
            return 0;
        }
        Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
        Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
        if (b >= this.getSize(bAxis) || c >= this.getSize(cAxis)) {
            return 0;
        }
        int aSize = this.getSize(aAxis);
        AxisCycle transform = AxisCycle.between(Direction.Axis.X, aAxis);
        for (int a = aSize - 1; a >= 0; --a) {
            if (!this.isFull(transform, a, b, c)) continue;
            return a + 1;
        }
        return 0;
    }

    public int getSize(Direction.Axis axis) {
        return axis.choose(this.xSize, this.ySize, this.zSize);
    }

    public int getXSize() {
        return this.getSize(Direction.Axis.X);
    }

    public int getYSize() {
        return this.getSize(Direction.Axis.Y);
    }

    public int getZSize() {
        return this.getSize(Direction.Axis.Z);
    }

    public void forAllEdges(IntLineConsumer consumer, boolean mergeNeighbors) {
        this.forAllAxisEdges(consumer, AxisCycle.NONE, mergeNeighbors);
        this.forAllAxisEdges(consumer, AxisCycle.FORWARD, mergeNeighbors);
        this.forAllAxisEdges(consumer, AxisCycle.BACKWARD, mergeNeighbors);
    }

    private void forAllAxisEdges(IntLineConsumer consumer, AxisCycle transform, boolean mergeNeighbors) {
        AxisCycle inverse = transform.inverse();
        int aSize = this.getSize(inverse.cycle(Direction.Axis.X));
        int bSize = this.getSize(inverse.cycle(Direction.Axis.Y));
        int cSize = this.getSize(inverse.cycle(Direction.Axis.Z));
        for (int a = 0; a <= aSize; ++a) {
            for (int b = 0; b <= bSize; ++b) {
                int lastStart = -1;
                for (int c = 0; c <= cSize; ++c) {
                    int fullSectors = 0;
                    int oddSectors = 0;
                    for (int da = 0; da <= 1; ++da) {
                        for (int db = 0; db <= 1; ++db) {
                            if (!this.isFullWide(inverse, a + da - 1, b + db - 1, c)) continue;
                            ++fullSectors;
                            oddSectors ^= da ^ db;
                        }
                    }
                    if (fullSectors == 1 || fullSectors == 3 || fullSectors == 2 && !(oddSectors & true)) {
                        if (mergeNeighbors) {
                            if (lastStart != -1) continue;
                            lastStart = c;
                            continue;
                        }
                        consumer.consume(inverse.cycle(a, b, c, Direction.Axis.X), inverse.cycle(a, b, c, Direction.Axis.Y), inverse.cycle(a, b, c, Direction.Axis.Z), inverse.cycle(a, b, c + 1, Direction.Axis.X), inverse.cycle(a, b, c + 1, Direction.Axis.Y), inverse.cycle(a, b, c + 1, Direction.Axis.Z));
                        continue;
                    }
                    if (lastStart == -1) continue;
                    consumer.consume(inverse.cycle(a, b, lastStart, Direction.Axis.X), inverse.cycle(a, b, lastStart, Direction.Axis.Y), inverse.cycle(a, b, lastStart, Direction.Axis.Z), inverse.cycle(a, b, c, Direction.Axis.X), inverse.cycle(a, b, c, Direction.Axis.Y), inverse.cycle(a, b, c, Direction.Axis.Z));
                    lastStart = -1;
                }
            }
        }
    }

    public void forAllBoxes(IntLineConsumer consumer, boolean mergeNeighbors) {
        BitSetDiscreteVoxelShape.forAllBoxes(this, consumer, mergeNeighbors);
    }

    public void forAllFaces(IntFaceConsumer consumer) {
        this.forAllAxisFaces(consumer, AxisCycle.NONE);
        this.forAllAxisFaces(consumer, AxisCycle.FORWARD);
        this.forAllAxisFaces(consumer, AxisCycle.BACKWARD);
    }

    private void forAllAxisFaces(IntFaceConsumer consumer, AxisCycle transform) {
        AxisCycle inverse = transform.inverse();
        Direction.Axis cAxis = inverse.cycle(Direction.Axis.Z);
        int aSize = this.getSize(inverse.cycle(Direction.Axis.X));
        int bSize = this.getSize(inverse.cycle(Direction.Axis.Y));
        int cSize = this.getSize(cAxis);
        Direction negative = Direction.fromAxisAndDirection(cAxis, Direction.AxisDirection.NEGATIVE);
        Direction positive = Direction.fromAxisAndDirection(cAxis, Direction.AxisDirection.POSITIVE);
        for (int a = 0; a < aSize; ++a) {
            for (int b = 0; b < bSize; ++b) {
                boolean lastFull = false;
                for (int c = 0; c <= cSize; ++c) {
                    boolean full;
                    boolean bl = full = c != cSize && this.isFull(inverse, a, b, c);
                    if (!lastFull && full) {
                        consumer.consume(negative, inverse.cycle(a, b, c, Direction.Axis.X), inverse.cycle(a, b, c, Direction.Axis.Y), inverse.cycle(a, b, c, Direction.Axis.Z));
                    }
                    if (lastFull && !full) {
                        consumer.consume(positive, inverse.cycle(a, b, c - 1, Direction.Axis.X), inverse.cycle(a, b, c - 1, Direction.Axis.Y), inverse.cycle(a, b, c - 1, Direction.Axis.Z));
                    }
                    lastFull = full;
                }
            }
        }
    }

    public static interface IntLineConsumer {
        public void consume(int var1, int var2, int var3, int var4, int var5, int var6);
    }

    public static interface IntFaceConsumer {
        public void consume(Direction var1, int var2, int var3, int var4);
    }
}

