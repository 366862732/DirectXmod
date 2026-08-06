/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.math.DoubleMath
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.phys.shapes;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.OffsetDoubleList;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.SliceShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public abstract class VoxelShape {
    protected final DiscreteVoxelShape shape;
    private @Nullable VoxelShape @Nullable [] faces;

    protected VoxelShape(DiscreteVoxelShape shape) {
        this.shape = shape;
    }

    public double min(Direction.Axis axis) {
        int i = this.shape.firstFull(axis);
        if (i >= this.shape.getSize(axis)) {
            return Double.POSITIVE_INFINITY;
        }
        return this.get(axis, i);
    }

    public double max(Direction.Axis axis) {
        int i = this.shape.lastFull(axis);
        if (i <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return this.get(axis, i);
    }

    public AABB bounds() {
        if (this.isEmpty()) {
            throw Util.pauseInIde(new UnsupportedOperationException("No bounds for empty shape."));
        }
        return new AABB(this.min(Direction.Axis.X), this.min(Direction.Axis.Y), this.min(Direction.Axis.Z), this.max(Direction.Axis.X), this.max(Direction.Axis.Y), this.max(Direction.Axis.Z));
    }

    public VoxelShape singleEncompassing() {
        if (this.isEmpty()) {
            return Shapes.empty();
        }
        return Shapes.box(this.min(Direction.Axis.X), this.min(Direction.Axis.Y), this.min(Direction.Axis.Z), this.max(Direction.Axis.X), this.max(Direction.Axis.Y), this.max(Direction.Axis.Z));
    }

    protected double get(Direction.Axis axis, int i) {
        return this.getCoords(axis).getDouble(i);
    }

    public abstract DoubleList getCoords(Direction.Axis var1);

    public boolean isEmpty() {
        return this.shape.isEmpty();
    }

    public VoxelShape move(Vec3 delta) {
        return this.move(delta.x, delta.y, delta.z);
    }

    public VoxelShape move(Vec3i delta) {
        return this.move(delta.getX(), delta.getY(), delta.getZ());
    }

    public VoxelShape move(double dx, double dy, double dz) {
        if (this.isEmpty()) {
            return Shapes.empty();
        }
        return new ArrayVoxelShape(this.shape, (DoubleList)new OffsetDoubleList(this.getCoords(Direction.Axis.X), dx), (DoubleList)new OffsetDoubleList(this.getCoords(Direction.Axis.Y), dy), (DoubleList)new OffsetDoubleList(this.getCoords(Direction.Axis.Z), dz));
    }

    public VoxelShape optimize() {
        VoxelShape[] result = new VoxelShape[]{Shapes.empty()};
        this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            result[0] = Shapes.joinUnoptimized(result[0], Shapes.box(x1, y1, z1, x2, y2, z2), BooleanOp.OR);
        });
        return result[0];
    }

    public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
        this.shape.forAllEdges((xi1, yi1, zi1, xi2, yi2, zi2) -> consumer.consume(this.get(Direction.Axis.X, xi1), this.get(Direction.Axis.Y, yi1), this.get(Direction.Axis.Z, zi1), this.get(Direction.Axis.X, xi2), this.get(Direction.Axis.Y, yi2), this.get(Direction.Axis.Z, zi2)), true);
    }

    public void forAllBoxes(Shapes.DoubleLineConsumer consumer) {
        DoubleList xCoords = this.getCoords(Direction.Axis.X);
        DoubleList yCoords = this.getCoords(Direction.Axis.Y);
        DoubleList zCoords = this.getCoords(Direction.Axis.Z);
        this.shape.forAllBoxes((xi1, yi1, zi1, xi2, yi2, zi2) -> consumer.consume(xCoords.getDouble(xi1), yCoords.getDouble(yi1), zCoords.getDouble(zi1), xCoords.getDouble(xi2), yCoords.getDouble(yi2), zCoords.getDouble(zi2)), true);
    }

    public List<AABB> toAabbs() {
        ArrayList list = Lists.newArrayList();
        this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> list.add(new AABB(x1, y1, z1, x2, y2, z2)));
        return list;
    }

    public double min(Direction.Axis aAxis, double b, double c) {
        int ci;
        Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
        Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
        int bi = this.findIndex(bAxis, b);
        int i = this.shape.firstFull(aAxis, bi, ci = this.findIndex(cAxis, c));
        if (i >= this.shape.getSize(aAxis)) {
            return Double.POSITIVE_INFINITY;
        }
        return this.get(aAxis, i);
    }

    public double max(Direction.Axis aAxis, double b, double c) {
        int ci;
        Direction.Axis bAxis = AxisCycle.FORWARD.cycle(aAxis);
        Direction.Axis cAxis = AxisCycle.BACKWARD.cycle(aAxis);
        int bi = this.findIndex(bAxis, b);
        int i = this.shape.lastFull(aAxis, bi, ci = this.findIndex(cAxis, c));
        if (i <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return this.get(aAxis, i);
    }

    protected int findIndex(Direction.Axis axis, double coord) {
        return Mth.binarySearch(0, this.shape.getSize(axis) + 1, index -> coord < this.get(axis, index)) - 1;
    }

    public @Nullable BlockHitResult clip(Vec3 from, Vec3 to, BlockPos pos) {
        if (this.isEmpty()) {
            return null;
        }
        Vec3 diff = to.subtract(from);
        if (diff.lengthSqr() < 1.0E-7) {
            return null;
        }
        Vec3 testPoint = from.add(diff.scale(0.001));
        if (this.shape.isFullWide(this.findIndex(Direction.Axis.X, testPoint.x - (double)pos.getX()), this.findIndex(Direction.Axis.Y, testPoint.y - (double)pos.getY()), this.findIndex(Direction.Axis.Z, testPoint.z - (double)pos.getZ()))) {
            return new BlockHitResult(testPoint, Direction.getApproximateNearest(diff.x, diff.y, diff.z).getOpposite(), pos, true);
        }
        return AABB.clip(this.toAabbs(), from, to, pos);
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    public Optional<Vec3> closestPointTo(Vec3 point) {
        if (this.isEmpty()) {
            return Optional.empty();
        }
        @Nullable MutableObject closest = new MutableObject();
        this.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double x = Mth.clamp(point.x(), x1, x2);
            double y = Mth.clamp(point.y(), y1, y2);
            double z = Mth.clamp(point.z(), z1, z2);
            Vec3 currentClosest = (Vec3)closest.get();
            if (currentClosest == null || point.distanceToSqr(x, y, z) < point.distanceToSqr(currentClosest)) {
                closest.setValue((Object)new Vec3(x, y, z));
            }
        });
        return Optional.of(Objects.requireNonNull((Vec3)closest.get()));
    }

    public VoxelShape getFaceShape(Direction direction) {
        VoxelShape face;
        if (this.isEmpty() || this == Shapes.block()) {
            return this;
        }
        if (this.faces != null) {
            face = this.faces[direction.ordinal()];
            if (face != null) {
                return face;
            }
        } else {
            this.faces = new VoxelShape[6];
        }
        this.faces[direction.ordinal()] = face = this.calculateFace(direction);
        return face;
    }

    private VoxelShape calculateFace(Direction direction) {
        Direction.Axis axis = direction.getAxis();
        if (this.isCubeLikeAlong(axis)) {
            return this;
        }
        Direction.AxisDirection sign = direction.getAxisDirection();
        int index = this.findIndex(axis, sign == Direction.AxisDirection.POSITIVE ? 0.9999999 : 1.0E-7);
        SliceShape slice = new SliceShape(this, axis, index);
        if (slice.isEmpty()) {
            return Shapes.empty();
        }
        if (slice.isCubeLike()) {
            return Shapes.block();
        }
        return slice;
    }

    protected boolean isCubeLike() {
        for (Direction.Axis axis : Direction.Axis.VALUES) {
            if (this.isCubeLikeAlong(axis)) continue;
            return false;
        }
        return true;
    }

    private boolean isCubeLikeAlong(Direction.Axis axis) {
        DoubleList coords = this.getCoords(axis);
        return coords.size() == 2 && DoubleMath.fuzzyEquals((double)coords.getDouble(0), (double)0.0, (double)1.0E-7) && DoubleMath.fuzzyEquals((double)coords.getDouble(1), (double)1.0, (double)1.0E-7);
    }

    public double collide(Direction.Axis axis, AABB moving, double distance) {
        return this.collideX(AxisCycle.between(axis, Direction.Axis.X), moving, distance);
    }

    protected double collideX(AxisCycle transform, AABB moving, double distance) {
        block11: {
            int cMax;
            int bMax;
            double minA;
            Direction.Axis aAxis;
            AxisCycle inverse;
            block10: {
                if (this.isEmpty()) {
                    return distance;
                }
                if (Math.abs(distance) < 1.0E-7) {
                    return 0.0;
                }
                inverse = transform.inverse();
                aAxis = inverse.cycle(Direction.Axis.X);
                Direction.Axis bAxis = inverse.cycle(Direction.Axis.Y);
                Direction.Axis cAxis = inverse.cycle(Direction.Axis.Z);
                double maxA = moving.max(aAxis);
                minA = moving.min(aAxis);
                int aMin = this.findIndex(aAxis, minA + 1.0E-7);
                int aMax = this.findIndex(aAxis, maxA - 1.0E-7);
                int bMin = Math.max(0, this.findIndex(bAxis, moving.min(bAxis) + 1.0E-7));
                bMax = Math.min(this.shape.getSize(bAxis), this.findIndex(bAxis, moving.max(bAxis) - 1.0E-7) + 1);
                int cMin = Math.max(0, this.findIndex(cAxis, moving.min(cAxis) + 1.0E-7));
                cMax = Math.min(this.shape.getSize(cAxis), this.findIndex(cAxis, moving.max(cAxis) - 1.0E-7) + 1);
                int aSize = this.shape.getSize(aAxis);
                if (!(distance > 0.0)) break block10;
                for (int a = aMax + 1; a < aSize; ++a) {
                    for (int b = bMin; b < bMax; ++b) {
                        for (int c = cMin; c < cMax; ++c) {
                            if (!this.shape.isFullWide(inverse, a, b, c)) continue;
                            double newDistance = this.get(aAxis, a) - maxA;
                            if (newDistance >= -1.0E-7) {
                                distance = Math.min(distance, newDistance);
                            }
                            return distance;
                        }
                    }
                }
                break block11;
            }
            if (!(distance < 0.0)) break block11;
            for (int a = aMin - 1; a >= 0; --a) {
                for (int b = bMin; b < bMax; ++b) {
                    for (int c = cMin; c < cMax; ++c) {
                        if (!this.shape.isFullWide(inverse, a, b, c)) continue;
                        double newDistance = this.get(aAxis, a + 1) - minA;
                        if (newDistance <= 1.0E-7) {
                            distance = Math.max(distance, newDistance);
                        }
                        return distance;
                    }
                }
            }
        }
        return distance;
    }

    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public String toString() {
        return this.isEmpty() ? "EMPTY" : "VoxelShape[" + String.valueOf(this.bounds()) + "]";
    }
}

