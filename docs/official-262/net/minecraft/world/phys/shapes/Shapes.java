/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Maps
 *  com.google.common.math.DoubleMath
 *  com.google.common.math.IntMath
 *  it.unimi.dsi.fastutil.doubles.DoubleArrayList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CubePointRange;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteCubeMerger;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.IdenticalMerger;
import net.minecraft.world.phys.shapes.IndexMerger;
import net.minecraft.world.phys.shapes.IndirectMerger;
import net.minecraft.world.phys.shapes.NonOverlappingMerger;
import net.minecraft.world.phys.shapes.SliceShape;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Shapes {
    public static final double EPSILON = 1.0E-7;
    public static final double BIG_EPSILON = 1.0E-6;
    private static final VoxelShape BLOCK = Util.make(() -> {
        BitSetDiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(1, 1, 1);
        ((DiscreteVoxelShape)shape).fill(0, 0, 0);
        return new CubeVoxelShape(shape);
    });
    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
    public static final VoxelShape INFINITY = Shapes.box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    private static final VoxelShape EMPTY = new ArrayVoxelShape((DiscreteVoxelShape)new BitSetDiscreteVoxelShape(0, 0, 0), (DoubleList)new DoubleArrayList(new double[]{0.0}), (DoubleList)new DoubleArrayList(new double[]{0.0}), (DoubleList)new DoubleArrayList(new double[]{0.0}));

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        }
        return Shapes.create(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX - minX < 1.0E-7 || maxY - minY < 1.0E-7 || maxZ - minZ < 1.0E-7) {
            return Shapes.empty();
        }
        int xBits = Shapes.findBits(minX, maxX);
        int yBits = Shapes.findBits(minY, maxY);
        int zBits = Shapes.findBits(minZ, maxZ);
        if (xBits < 0 || yBits < 0 || zBits < 0) {
            return new ArrayVoxelShape(Shapes.BLOCK.shape, (DoubleList)DoubleArrayList.wrap((double[])new double[]{minX, maxX}), (DoubleList)DoubleArrayList.wrap((double[])new double[]{minY, maxY}), (DoubleList)DoubleArrayList.wrap((double[])new double[]{minZ, maxZ}));
        }
        if (xBits == 0 && yBits == 0 && zBits == 0) {
            return Shapes.block();
        }
        int xSize = 1 << xBits;
        int ySize = 1 << yBits;
        int zSize = 1 << zBits;
        BitSetDiscreteVoxelShape voxelShape = BitSetDiscreteVoxelShape.withFilledBounds(xSize, ySize, zSize, (int)Math.round(minX * (double)xSize), (int)Math.round(minY * (double)ySize), (int)Math.round(minZ * (double)zSize), (int)Math.round(maxX * (double)xSize), (int)Math.round(maxY * (double)ySize), (int)Math.round(maxZ * (double)zSize));
        return new CubeVoxelShape(voxelShape);
    }

    public static VoxelShape create(AABB aabb) {
        return Shapes.create(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    @VisibleForTesting
    static int findBits(double min, double max) {
        if (min < -1.0E-7 || max > 1.0000001) {
            return -1;
        }
        for (int bits = 0; bits <= 3; ++bits) {
            boolean foundMax;
            int intervals = 1 << bits;
            double shMin = min * (double)intervals;
            double shMax = max * (double)intervals;
            boolean foundMin = Math.abs(shMin - (double)Math.round(shMin)) < 1.0E-7 * (double)intervals;
            boolean bl = foundMax = Math.abs(shMax - (double)Math.round(shMax)) < 1.0E-7 * (double)intervals;
            if (!foundMin || !foundMax) continue;
            return bits;
        }
        return -1;
    }

    @VisibleForTesting
    static long lcm(int first, int second) {
        return (long)first * (long)(second / IntMath.gcd((int)first, (int)second));
    }

    public static VoxelShape or(VoxelShape first, VoxelShape second) {
        return Shapes.join(first, second, BooleanOp.OR);
    }

    public static VoxelShape or(VoxelShape first, VoxelShape ... tail) {
        return Arrays.stream(tail).reduce(first, Shapes::or);
    }

    public static VoxelShape join(VoxelShape first, VoxelShape second, BooleanOp op) {
        return Shapes.joinUnoptimized(first, second, op).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) {
            throw Util.pauseInIde(new IllegalArgumentException());
        }
        if (first == second) {
            return op.apply(true, true) ? first : Shapes.empty();
        }
        boolean firstOnlyMatters = op.apply(true, false);
        boolean secondOnlyMatters = op.apply(false, true);
        if (first.isEmpty()) {
            return secondOnlyMatters ? second : Shapes.empty();
        }
        if (second.isEmpty()) {
            return firstOnlyMatters ? first : Shapes.empty();
        }
        IndexMerger xMerger = Shapes.createIndexMerger(1, first.getCoords(Direction.Axis.X), second.getCoords(Direction.Axis.X), firstOnlyMatters, secondOnlyMatters);
        IndexMerger yMerger = Shapes.createIndexMerger(xMerger.size() - 1, first.getCoords(Direction.Axis.Y), second.getCoords(Direction.Axis.Y), firstOnlyMatters, secondOnlyMatters);
        IndexMerger zMerger = Shapes.createIndexMerger((xMerger.size() - 1) * (yMerger.size() - 1), first.getCoords(Direction.Axis.Z), second.getCoords(Direction.Axis.Z), firstOnlyMatters, secondOnlyMatters);
        BitSetDiscreteVoxelShape voxelShape = BitSetDiscreteVoxelShape.join(first.shape, second.shape, xMerger, yMerger, zMerger, op);
        if (xMerger instanceof DiscreteCubeMerger && yMerger instanceof DiscreteCubeMerger && zMerger instanceof DiscreteCubeMerger) {
            return new CubeVoxelShape(voxelShape);
        }
        return new ArrayVoxelShape((DiscreteVoxelShape)voxelShape, xMerger.getList(), yMerger.getList(), zMerger.getList());
    }

    public static boolean joinIsNotEmpty(VoxelShape first, VoxelShape second, BooleanOp op) {
        if (op.apply(false, false)) {
            throw Util.pauseInIde(new IllegalArgumentException());
        }
        boolean firstEmpty = first.isEmpty();
        boolean secondEmpty = second.isEmpty();
        if (firstEmpty || secondEmpty) {
            return op.apply(!firstEmpty, !secondEmpty);
        }
        if (first == second) {
            return op.apply(true, true);
        }
        boolean firstOnlyMatters = op.apply(true, false);
        boolean secondOnlyMatters = op.apply(false, true);
        for (Direction.Axis axis : AxisCycle.AXIS_VALUES) {
            if (first.max(axis) < second.min(axis) - 1.0E-7) {
                return firstOnlyMatters || secondOnlyMatters;
            }
            if (!(second.max(axis) < first.min(axis) - 1.0E-7)) continue;
            return firstOnlyMatters || secondOnlyMatters;
        }
        IndexMerger xMerger = Shapes.createIndexMerger(1, first.getCoords(Direction.Axis.X), second.getCoords(Direction.Axis.X), firstOnlyMatters, secondOnlyMatters);
        IndexMerger yMerger = Shapes.createIndexMerger(xMerger.size() - 1, first.getCoords(Direction.Axis.Y), second.getCoords(Direction.Axis.Y), firstOnlyMatters, secondOnlyMatters);
        IndexMerger zMerger = Shapes.createIndexMerger((xMerger.size() - 1) * (yMerger.size() - 1), first.getCoords(Direction.Axis.Z), second.getCoords(Direction.Axis.Z), firstOnlyMatters, secondOnlyMatters);
        return Shapes.joinIsNotEmpty(xMerger, yMerger, zMerger, first.shape, second.shape, op);
    }

    private static boolean joinIsNotEmpty(IndexMerger xMerger, IndexMerger yMerger, IndexMerger zMerger, DiscreteVoxelShape first, DiscreteVoxelShape second, BooleanOp op) {
        return !xMerger.forMergedIndexes((x1, x2, xr) -> yMerger.forMergedIndexes((y1, y2, yr) -> zMerger.forMergedIndexes((z1, z2, zr) -> !op.apply(first.isFullWide(x1, y1, z1), second.isFullWide(x2, y2, z2)))));
    }

    public static double collide(Direction.Axis axis, AABB moving, Iterable<VoxelShape> shapes, double distance) {
        for (VoxelShape shape : shapes) {
            if (Math.abs(distance) < 1.0E-7) {
                return 0.0;
            }
            distance = shape.collide(axis, moving, distance);
        }
        return distance;
    }

    public static boolean blockOccludes(VoxelShape shape, VoxelShape occluder, Direction direction) {
        if (shape == Shapes.block() && occluder == Shapes.block()) {
            return true;
        }
        if (occluder.isEmpty()) {
            return false;
        }
        Direction.Axis axis = direction.getAxis();
        Direction.AxisDirection sign = direction.getAxisDirection();
        VoxelShape first = sign == Direction.AxisDirection.POSITIVE ? shape : occluder;
        VoxelShape second = sign == Direction.AxisDirection.POSITIVE ? occluder : shape;
        BooleanOp op = sign == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
        return DoubleMath.fuzzyEquals((double)first.max(axis), (double)1.0, (double)1.0E-7) && DoubleMath.fuzzyEquals((double)second.min(axis), (double)0.0, (double)1.0E-7) && !Shapes.joinIsNotEmpty(new SliceShape(first, axis, first.shape.getSize(axis) - 1), new SliceShape(second, axis, 0), op);
    }

    public static boolean mergedFaceOccludes(VoxelShape shape, VoxelShape occluder, Direction direction) {
        VoxelShape second;
        if (shape == Shapes.block() || occluder == Shapes.block()) {
            return true;
        }
        Direction.Axis axis = direction.getAxis();
        Direction.AxisDirection sign = direction.getAxisDirection();
        VoxelShape first = sign == Direction.AxisDirection.POSITIVE ? shape : occluder;
        VoxelShape voxelShape = second = sign == Direction.AxisDirection.POSITIVE ? occluder : shape;
        if (!DoubleMath.fuzzyEquals((double)first.max(axis), (double)1.0, (double)1.0E-7)) {
            first = Shapes.empty();
        }
        if (!DoubleMath.fuzzyEquals((double)second.min(axis), (double)0.0, (double)1.0E-7)) {
            second = Shapes.empty();
        }
        return !Shapes.joinIsNotEmpty(Shapes.block(), Shapes.joinUnoptimized(new SliceShape(first, axis, first.shape.getSize(axis) - 1), new SliceShape(second, axis, 0), BooleanOp.OR), BooleanOp.ONLY_FIRST);
    }

    public static boolean faceShapeOccludes(VoxelShape shape, VoxelShape occluder) {
        if (shape == Shapes.block() || occluder == Shapes.block()) {
            return true;
        }
        if (shape.isEmpty() && occluder.isEmpty()) {
            return false;
        }
        return !Shapes.joinIsNotEmpty(Shapes.block(), Shapes.joinUnoptimized(shape, occluder, BooleanOp.OR), BooleanOp.ONLY_FIRST);
    }

    @VisibleForTesting
    static IndexMerger createIndexMerger(int cost, DoubleList first, DoubleList second, boolean firstOnlyMatters, boolean secondOnlyMatters) {
        long size;
        int firstSize = first.size() - 1;
        int secondSize = second.size() - 1;
        if (first instanceof CubePointRange && second instanceof CubePointRange && (long)cost * (size = Shapes.lcm(firstSize, secondSize)) <= 256L) {
            return new DiscreteCubeMerger(firstSize, secondSize);
        }
        if (first.getDouble(firstSize) < second.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(first, second, false);
        }
        if (second.getDouble(secondSize) < first.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(second, first, true);
        }
        if (firstSize == secondSize && Objects.equals(first, second)) {
            return new IdenticalMerger(first);
        }
        return new IndirectMerger(first, second, firstOnlyMatters, secondOnlyMatters);
    }

    public static VoxelShape rotate(VoxelShape shape, OctahedralGroup rotation) {
        return Shapes.rotate(shape, rotation, BLOCK_CENTER);
    }

    public static VoxelShape rotate(VoxelShape shape, OctahedralGroup rotation, Vec3 rotationPoint) {
        if (rotation == OctahedralGroup.IDENTITY) {
            return shape;
        }
        DiscreteVoxelShape newDiscreteShape = shape.shape.rotate(rotation);
        if (shape instanceof CubeVoxelShape && BLOCK_CENTER.equals(rotationPoint)) {
            return new CubeVoxelShape(newDiscreteShape);
        }
        Direction.Axis newX = rotation.permutation().permuteAxis(Direction.Axis.X);
        Direction.Axis newY = rotation.permutation().permuteAxis(Direction.Axis.Y);
        Direction.Axis newZ = rotation.permutation().permuteAxis(Direction.Axis.Z);
        DoubleList newXs = shape.getCoords(newX);
        DoubleList newYs = shape.getCoords(newY);
        DoubleList newZs = shape.getCoords(newZ);
        boolean flipX = rotation.inverts(Direction.Axis.X);
        boolean flipY = rotation.inverts(Direction.Axis.Y);
        boolean flipZ = rotation.inverts(Direction.Axis.Z);
        return new ArrayVoxelShape(newDiscreteShape, Shapes.flipAxisIfNeeded(newXs, flipX, rotationPoint.get(newX), rotationPoint.x), Shapes.flipAxisIfNeeded(newYs, flipY, rotationPoint.get(newY), rotationPoint.y), Shapes.flipAxisIfNeeded(newZs, flipZ, rotationPoint.get(newZ), rotationPoint.z));
    }

    @VisibleForTesting
    static DoubleList flipAxisIfNeeded(DoubleList newAxis, boolean flip, double newRelative, double oldRelative) {
        if (!flip && newRelative == oldRelative) {
            return newAxis;
        }
        int size = newAxis.size();
        DoubleArrayList newList = new DoubleArrayList(size);
        if (flip) {
            for (int i = size - 1; i >= 0; --i) {
                newList.add(-(newAxis.getDouble(i) - newRelative) + oldRelative);
            }
        } else {
            for (int i = 0; i >= 0 && i < size; ++i) {
                newList.add(newAxis.getDouble(i) - newRelative + oldRelative);
            }
        }
        return newList;
    }

    public static boolean equal(VoxelShape first, VoxelShape second) {
        return !Shapes.joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape zAxis) {
        return Shapes.rotateHorizontalAxis(zAxis, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape zAxis, Vec3 rotationCenter) {
        return Maps.newEnumMap(Map.of(Direction.Axis.Z, zAxis, Direction.Axis.X, Shapes.rotate(zAxis, OctahedralGroup.BLOCK_ROT_Y_90, rotationCenter)));
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape north) {
        return Shapes.rotateAllAxis(north, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape north, Vec3 rotationCenter) {
        return Maps.newEnumMap(Map.of(Direction.Axis.Z, north, Direction.Axis.X, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_90, rotationCenter), Direction.Axis.Y, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_X_90, rotationCenter)));
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape north) {
        return Shapes.rotateHorizontal(north, OctahedralGroup.IDENTITY, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape north, OctahedralGroup initial) {
        return Shapes.rotateHorizontal(north, initial, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape north, OctahedralGroup initial, Vec3 rotationCenter) {
        return Maps.newEnumMap(Map.of(Direction.NORTH, Shapes.rotate(north, initial), Direction.EAST, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_90.compose(initial), rotationCenter), Direction.SOUTH, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_180.compose(initial), rotationCenter), Direction.WEST, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_270.compose(initial), rotationCenter)));
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape north) {
        return Shapes.rotateAll(north, OctahedralGroup.IDENTITY, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape north, Vec3 rotationCenter) {
        return Shapes.rotateAll(north, OctahedralGroup.IDENTITY, rotationCenter);
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape north, OctahedralGroup initial, Vec3 rotationCenter) {
        return Maps.newEnumMap(Map.of(Direction.NORTH, Shapes.rotate(north, initial), Direction.EAST, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_90.compose(initial), rotationCenter), Direction.SOUTH, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_180.compose(initial), rotationCenter), Direction.WEST, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_Y_270.compose(initial), rotationCenter), Direction.UP, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_X_270.compose(initial), rotationCenter), Direction.DOWN, Shapes.rotate(north, OctahedralGroup.BLOCK_ROT_X_90.compose(initial), rotationCenter)));
    }

    public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape north) {
        return Shapes.rotateAttachFace(north, OctahedralGroup.IDENTITY);
    }

    public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape north, OctahedralGroup initial) {
        return Map.of(AttachFace.WALL, Shapes.rotateHorizontal(north, initial), AttachFace.FLOOR, Shapes.rotateHorizontal(north, OctahedralGroup.BLOCK_ROT_X_270.compose(initial)), AttachFace.CEILING, Shapes.rotateHorizontal(north, OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_X_90).compose(initial)));
    }

    public static interface DoubleLineConsumer {
        public void consume(double var1, double var3, double var5, double var7, double var9, double var11);
    }
}

