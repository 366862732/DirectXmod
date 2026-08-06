/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.LpVec3;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Vec3
implements Position {
    public static final Codec<Vec3> CODEC = Codec.DOUBLE.listOf().comapFlatMap(input -> Util.fixedSize(input, 3).map(doubles -> new Vec3((Double)doubles.get(0), (Double)doubles.get(1), (Double)doubles.get(2))), pos -> List.of(Double.valueOf(pos.x()), Double.valueOf(pos.y()), Double.valueOf(pos.z())));
    public static final StreamCodec<ByteBuf, Vec3> STREAM_CODEC = new StreamCodec<ByteBuf, Vec3>(){

        @Override
        public Vec3 decode(ByteBuf input) {
            return new Vec3(input.readDouble(), input.readDouble(), input.readDouble());
        }

        @Override
        public void encode(ByteBuf output, Vec3 value) {
            output.writeDouble(value.x());
            output.writeDouble(value.y());
            output.writeDouble(value.z());
        }
    };
    public static final StreamCodec<ByteBuf, Vec3> LP_STREAM_CODEC = StreamCodec.of(LpVec3::write, LpVec3::read);
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
    public static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
    public static final Vec3 Y_AXIS = new Vec3(0.0, 1.0, 0.0);
    public static final Vec3 Z_AXIS = new Vec3(0.0, 0.0, 1.0);
    public final double x;
    public final double y;
    public final double z;

    public static Vec3 atLowerCornerOf(Vec3i pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 atLowerCornerWithOffset(Vec3i pos, double x, double y, double z) {
        return new Vec3((double)pos.getX() + x, (double)pos.getY() + y, (double)pos.getZ() + z);
    }

    public static Vec3 atCenterOf(Vec3i pos) {
        return Vec3.atLowerCornerWithOffset(pos, 0.5, 0.5, 0.5);
    }

    public static Vec3 atBottomCenterOf(Vec3i pos) {
        return Vec3.atLowerCornerWithOffset(pos, 0.5, 0.0, 0.5);
    }

    public static Vec3 upFromBottomCenterOf(Vec3i pos, double yOffset) {
        return Vec3.atLowerCornerWithOffset(pos, 0.5, yOffset, 0.5);
    }

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vector3fc vec) {
        this(vec.x(), vec.y(), vec.z());
    }

    public Vec3(Vec3i vec) {
        this(vec.getX(), vec.getY(), vec.getZ());
    }

    public Vec3 vectorTo(Vec3 vec) {
        return new Vec3(vec.x - this.x, vec.y - this.y, vec.z - this.z);
    }

    public Vec3 normalize() {
        double dist = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (dist < (double)1.0E-5f) {
            return ZERO;
        }
        return new Vec3(this.x / dist, this.y / dist, this.z / dist);
    }

    public double dot(Vec3 vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public Vec3 cross(Vec3 vec) {
        return new Vec3(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }

    public Vec3 subtract(Vec3 vec) {
        return this.subtract(vec.x, vec.y, vec.z);
    }

    public Vec3 subtract(double value) {
        return this.subtract(value, value, value);
    }

    public Vec3 subtract(double x, double y, double z) {
        return this.add(-x, -y, -z);
    }

    public Vec3 add(double value) {
        return this.add(value, value, value);
    }

    public Vec3 add(Vec3 vec) {
        return this.add(vec.x, vec.y, vec.z);
    }

    public Vec3 add(double x, double y, double z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }

    public boolean closerThan(Position pos, double distance) {
        return this.distanceToSqr(pos.x(), pos.y(), pos.z()) < distance * distance;
    }

    public double distanceTo(Vec3 vec) {
        double xd = vec.x - this.x;
        double yd = vec.y - this.y;
        double zd = vec.z - this.z;
        return Math.sqrt(xd * xd + yd * yd + zd * zd);
    }

    public double distanceToSqr(Vec3 vec) {
        double xd = vec.x - this.x;
        double yd = vec.y - this.y;
        double zd = vec.z - this.z;
        return xd * xd + yd * yd + zd * zd;
    }

    public double distanceToSqr(double x, double y, double z) {
        double xd = x - this.x;
        double yd = y - this.y;
        double zd = z - this.z;
        return xd * xd + yd * yd + zd * zd;
    }

    public boolean closerThan(Vec3 vec, double distanceXZ, double distanceY) {
        double dx = vec.x() - this.x;
        double dy = vec.y() - this.y;
        double dz = vec.z() - this.z;
        return Mth.lengthSquared(dx, dz) < Mth.square(distanceXZ) && Math.abs(dy) < distanceY;
    }

    public Vec3 scale(double scale) {
        return this.multiply(scale, scale, scale);
    }

    public Vec3 reverse() {
        return this.scale(-1.0);
    }

    public Vec3 multiply(Vec3 scale) {
        return this.multiply(scale.x, scale.y, scale.z);
    }

    public Vec3 multiply(double xScale, double yScale, double zScale) {
        return new Vec3(this.x * xScale, this.y * yScale, this.z * zScale);
    }

    public Vec3 horizontal() {
        return new Vec3(this.x, 0.0, this.z);
    }

    public Vec3 offsetRandom(RandomSource random, float offset) {
        return this.add((random.nextFloat() - 0.5f) * offset, (random.nextFloat() - 0.5f) * offset, (random.nextFloat() - 0.5f) * offset);
    }

    public Vec3 offsetRandomXZ(RandomSource random, float offset) {
        return this.add((random.nextFloat() - 0.5f) * offset, 0.0, (random.nextFloat() - 0.5f) * offset);
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double horizontalDistance() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public double horizontalDistanceSqr() {
        return this.x * this.x + this.z * this.z;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vec3)) {
            return false;
        }
        Vec3 vec3 = (Vec3)o;
        if (Double.compare(vec3.x, this.x) != 0) {
            return false;
        }
        if (Double.compare(vec3.y, this.y) != 0) {
            return false;
        }
        return Double.compare(vec3.z, this.z) == 0;
    }

    public int hashCode() {
        long temp = Double.doubleToLongBits(this.x);
        int result = (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.y);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.z);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        return result;
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public Vec3 lerp(Vec3 vec, double a) {
        return new Vec3(Mth.lerp(a, this.x, vec.x), Mth.lerp(a, this.y, vec.y), Mth.lerp(a, this.z, vec.z));
    }

    public Vec3 xRot(float radians) {
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        double xx = this.x;
        double yy = this.y * (double)cos + this.z * (double)sin;
        double zz = this.z * (double)cos - this.y * (double)sin;
        return new Vec3(xx, yy, zz);
    }

    public Vec3 yRot(float radians) {
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        double xx = this.x * (double)cos + this.z * (double)sin;
        double yy = this.y;
        double zz = this.z * (double)cos - this.x * (double)sin;
        return new Vec3(xx, yy, zz);
    }

    public Vec3 zRot(float radians) {
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        double xx = this.x * (double)cos + this.y * (double)sin;
        double yy = this.y * (double)cos - this.x * (double)sin;
        double zz = this.z;
        return new Vec3(xx, yy, zz);
    }

    public Vec3 rotateClockwise90() {
        return new Vec3(-this.z, this.y, this.x);
    }

    public static Vec3 directionFromRotation(Vec2 rotation) {
        return Vec3.directionFromRotation(rotation.x, rotation.y);
    }

    public static Vec3 directionFromRotation(float rotX, float rotY) {
        float yCos = Mth.cos(-rotY * ((float)Math.PI / 180) - (float)Math.PI);
        float ySin = Mth.sin(-rotY * ((float)Math.PI / 180) - (float)Math.PI);
        float xCos = -Mth.cos(-rotX * ((float)Math.PI / 180));
        float xSin = Mth.sin(-rotX * ((float)Math.PI / 180));
        return new Vec3(ySin * xCos, xSin, yCos * xCos);
    }

    public Vec2 rotation() {
        float yaw = (float)Math.atan2(-this.x, this.z) * 57.295776f;
        float pitch = (float)Math.asin(-this.y / Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z)) * 57.295776f;
        return new Vec2(pitch, yaw);
    }

    public Vec3 align(EnumSet<Direction.Axis> axes) {
        double x = axes.contains(Direction.Axis.X) ? (double)Mth.floor(this.x) : this.x;
        double y = axes.contains(Direction.Axis.Y) ? (double)Mth.floor(this.y) : this.y;
        double z = axes.contains(Direction.Axis.Z) ? (double)Mth.floor(this.z) : this.z;
        return new Vec3(x, y, z);
    }

    public double get(Direction.Axis axis) {
        return axis.choose(this.x, this.y, this.z);
    }

    public Vec3 with(Direction.Axis axis, double value) {
        double x = axis == Direction.Axis.X ? value : this.x;
        double y = axis == Direction.Axis.Y ? value : this.y;
        double z = axis == Direction.Axis.Z ? value : this.z;
        return new Vec3(x, y, z);
    }

    public Vec3 relative(Direction direction, double distance) {
        Vec3i normal = direction.getUnitVec3i();
        return new Vec3(this.x + distance * (double)normal.getX(), this.y + distance * (double)normal.getY(), this.z + distance * (double)normal.getZ());
    }

    @Override
    public final double x() {
        return this.x;
    }

    @Override
    public final double y() {
        return this.y;
    }

    @Override
    public final double z() {
        return this.z;
    }

    public Vector3f toVector3f() {
        return new Vector3f((float)this.x, (float)this.y, (float)this.z);
    }

    public Vec3 projectedOn(Vec3 onto) {
        if (onto.lengthSqr() == 0.0) {
            return onto;
        }
        return onto.scale(this.dot(onto)).scale(1.0 / onto.lengthSqr());
    }

    public static Vec3 applyLocalCoordinatesToRotation(Vec2 rotation, Vec3 direction) {
        float yCos = Mth.cos((rotation.y + 90.0f) * ((float)Math.PI / 180));
        float ySin = Mth.sin((rotation.y + 90.0f) * ((float)Math.PI / 180));
        float xCos = Mth.cos(-rotation.x * ((float)Math.PI / 180));
        float xSin = Mth.sin(-rotation.x * ((float)Math.PI / 180));
        float xCosUp = Mth.cos((-rotation.x + 90.0f) * ((float)Math.PI / 180));
        float xSinUp = Mth.sin((-rotation.x + 90.0f) * ((float)Math.PI / 180));
        Vec3 forwards = new Vec3(yCos * xCos, xSin, ySin * xCos);
        Vec3 up = new Vec3(yCos * xCosUp, xSinUp, ySin * xCosUp);
        Vec3 left = forwards.cross(up).scale(-1.0);
        double xa = forwards.x * direction.z + up.x * direction.y + left.x * direction.x;
        double ya = forwards.y * direction.z + up.y * direction.y + left.y * direction.x;
        double za = forwards.z * direction.z + up.z * direction.y + left.z * direction.x;
        return new Vec3(xa, ya, za);
    }

    public Vec3 addLocalCoordinates(Vec3 direction) {
        return Vec3.applyLocalCoordinatesToRotation(this.rotation(), direction);
    }

    public boolean isFinite() {
        return Double.isFinite(this.x) && Double.isFinite(this.y) && Double.isFinite(this.z);
    }
}

