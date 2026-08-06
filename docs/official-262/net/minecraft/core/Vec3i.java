/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.concurrent.Immutable
 *  org.joml.Vector3i
 */
package net.minecraft.core;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.stream.IntStream;
import javax.annotation.concurrent.Immutable;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Vector3i;

@Immutable
public class Vec3i
implements Comparable<Vec3i> {
    public static final Codec<Vec3i> CODEC = Codec.INT_STREAM.comapFlatMap(input -> Util.fixedSize(input, 3).map(ints -> new Vec3i(ints[0], ints[1], ints[2])), pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ()));
    public static final StreamCodec<ByteBuf, Vec3i> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Vec3i::getX, ByteBufCodecs.VAR_INT, Vec3i::getY, ByteBufCodecs.VAR_INT, Vec3i::getZ, Vec3i::new);
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    private int x;
    private int y;
    private int z;

    public static Codec<Vec3i> offsetCodec(int maxOffsetPerAxis) {
        return CODEC.validate(value -> {
            if (Math.abs(value.getX()) < maxOffsetPerAxis && Math.abs(value.getY()) < maxOffsetPerAxis && Math.abs(value.getZ()) < maxOffsetPerAxis) {
                return DataResult.success((Object)value);
            }
            return DataResult.error(() -> "Position out of range, expected at most " + maxOffsetPerAxis + ": " + String.valueOf(value));
        });
    }

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vec3i)) {
            return false;
        }
        Vec3i vec3i = (Vec3i)o;
        return this.getX() == vec3i.getX() && this.getY() == vec3i.getY() && this.getZ() == vec3i.getZ();
    }

    public int hashCode() {
        return (this.getY() + this.getZ() * 31) * 31 + this.getX();
    }

    @Override
    public int compareTo(Vec3i pos) {
        if (this.getY() == pos.getY()) {
            if (this.getZ() == pos.getZ()) {
                return this.getX() - pos.getX();
            }
            return this.getZ() - pos.getZ();
        }
        return this.getY() - pos.getY();
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    protected Vec3i setX(int x) {
        this.x = x;
        return this;
    }

    protected Vec3i setY(int y) {
        this.y = y;
        return this;
    }

    protected Vec3i setZ(int z) {
        this.z = z;
        return this;
    }

    public Vec3i offset(int x, int y, int z) {
        if (x == 0 && y == 0 && z == 0) {
            return this;
        }
        return new Vec3i(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    public Vec3i offset(Vec3i vec) {
        return this.offset(vec.getX(), vec.getY(), vec.getZ());
    }

    public Vec3i subtract(Vec3i vec) {
        return this.offset(-vec.getX(), -vec.getY(), -vec.getZ());
    }

    public Vec3i multiply(int scale) {
        if (scale == 1) {
            return this;
        }
        if (scale == 0) {
            return ZERO;
        }
        return new Vec3i(this.getX() * scale, this.getY() * scale, this.getZ() * scale);
    }

    public Vec3i multiply(int xScale, int yScale, int zScale) {
        return new Vec3i(this.getX() * xScale, this.getY() * yScale, this.getZ() * zScale);
    }

    public Vec3i above() {
        return this.above(1);
    }

    public Vec3i above(int steps) {
        return this.relative(Direction.UP, steps);
    }

    public Vec3i below() {
        return this.below(1);
    }

    public Vec3i below(int steps) {
        return this.relative(Direction.DOWN, steps);
    }

    public Vec3i north() {
        return this.north(1);
    }

    public Vec3i north(int steps) {
        return this.relative(Direction.NORTH, steps);
    }

    public Vec3i south() {
        return this.south(1);
    }

    public Vec3i south(int steps) {
        return this.relative(Direction.SOUTH, steps);
    }

    public Vec3i west() {
        return this.west(1);
    }

    public Vec3i west(int steps) {
        return this.relative(Direction.WEST, steps);
    }

    public Vec3i east() {
        return this.east(1);
    }

    public Vec3i east(int steps) {
        return this.relative(Direction.EAST, steps);
    }

    public Vec3i relative(Direction direction) {
        return this.relative(direction, 1);
    }

    public Vec3i relative(Direction direction, int steps) {
        if (steps == 0) {
            return this;
        }
        return new Vec3i(this.getX() + direction.getStepX() * steps, this.getY() + direction.getStepY() * steps, this.getZ() + direction.getStepZ() * steps);
    }

    public Vec3i relative(Direction.Axis axis, int steps) {
        if (steps == 0) {
            return this;
        }
        int xStep = axis == Direction.Axis.X ? steps : 0;
        int yStep = axis == Direction.Axis.Y ? steps : 0;
        int zStep = axis == Direction.Axis.Z ? steps : 0;
        return new Vec3i(this.getX() + xStep, this.getY() + yStep, this.getZ() + zStep);
    }

    public Vec3i cross(Vec3i upVector) {
        return new Vec3i(this.getY() * upVector.getZ() - this.getZ() * upVector.getY(), this.getZ() * upVector.getX() - this.getX() * upVector.getZ(), this.getX() * upVector.getY() - this.getY() * upVector.getX());
    }

    public boolean closerThan(Vec3i pos, double distance) {
        return this.distSqr(pos) < Mth.square(distance);
    }

    public boolean closerToCenterThan(Position pos, double distance) {
        return this.distToCenterSqr(pos) < Mth.square(distance);
    }

    public double distSqr(Vec3i pos) {
        return this.distToLowCornerSqr(pos.getX(), pos.getY(), pos.getZ());
    }

    public double distToCenterSqr(Position pos) {
        return this.distToCenterSqr(pos.x(), pos.y(), pos.z());
    }

    public double distToCenterSqr(double x, double y, double z) {
        double dx = (double)this.getX() + 0.5 - x;
        double dy = (double)this.getY() + 0.5 - y;
        double dz = (double)this.getZ() + 0.5 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distToLowCornerSqr(double x, double y, double z) {
        double dx = (double)this.getX() - x;
        double dy = (double)this.getY() - y;
        double dz = (double)this.getZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public int distManhattan(Vec3i pos) {
        float xd = Math.abs(pos.getX() - this.getX());
        float yd = Math.abs(pos.getY() - this.getY());
        float zd = Math.abs(pos.getZ() - this.getZ());
        return (int)(xd + yd + zd);
    }

    public int distChessboard(Vec3i pos) {
        int xd = Math.abs(this.getX() - pos.getX());
        int yd = Math.abs(this.getY() - pos.getY());
        int zd = Math.abs(this.getZ() - pos.getZ());
        return Math.max(Math.max(xd, yd), zd);
    }

    public int get(Direction.Axis axis) {
        return axis.choose(this.x, this.y, this.z);
    }

    public Vector3i toMutable() {
        return new Vector3i(this.x, this.y, this.z);
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object)this).add("x", this.getX()).add("y", this.getY()).add("z", this.getZ()).toString();
    }

    public String toShortString() {
        return this.getX() + ", " + this.getY() + ", " + this.getZ();
    }
}

