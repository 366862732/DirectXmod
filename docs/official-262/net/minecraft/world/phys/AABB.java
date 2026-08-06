/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.phys;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class AABB {
    private static final double EPSILON = 1.0E-7;
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public AABB(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public AABB(Vec3 begin, Vec3 end) {
        this(begin.x, begin.y, begin.z, end.x, end.y, end.z);
    }

    public static AABB of(BoundingBox box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }

    public static AABB unitCubeFromLowerCorner(Vec3 pos) {
        return new AABB(pos.x, pos.y, pos.z, pos.x + 1.0, pos.y + 1.0, pos.z + 1.0);
    }

    public static AABB encapsulatingFullBlocks(BlockPos pos0, BlockPos pos1) {
        return new AABB(Math.min(pos0.getX(), pos1.getX()), Math.min(pos0.getY(), pos1.getY()), Math.min(pos0.getZ(), pos1.getZ()), Math.max(pos0.getX(), pos1.getX()) + 1, Math.max(pos0.getY(), pos1.getY()) + 1, Math.max(pos0.getZ(), pos1.getZ()) + 1);
    }

    public AABB setMinX(double minX) {
        return new AABB(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AABB setMinY(double minY) {
        return new AABB(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AABB setMinZ(double minZ) {
        return new AABB(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AABB setMaxX(double maxX) {
        return new AABB(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
    }

    public AABB setMaxY(double maxY) {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
    }

    public AABB setMaxZ(double maxZ) {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
    }

    public double min(Direction.Axis axis) {
        return axis.choose(this.minX, this.minY, this.minZ);
    }

    public double max(Direction.Axis axis) {
        return axis.choose(this.maxX, this.maxY, this.maxZ);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AABB)) {
            return false;
        }
        AABB aabb = (AABB)o;
        if (Double.compare(aabb.minX, this.minX) != 0) {
            return false;
        }
        if (Double.compare(aabb.minY, this.minY) != 0) {
            return false;
        }
        if (Double.compare(aabb.minZ, this.minZ) != 0) {
            return false;
        }
        if (Double.compare(aabb.maxX, this.maxX) != 0) {
            return false;
        }
        if (Double.compare(aabb.maxY, this.maxY) != 0) {
            return false;
        }
        return Double.compare(aabb.maxZ, this.maxZ) == 0;
    }

    public int hashCode() {
        long temp = Double.doubleToLongBits(this.minX);
        int result = (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.minY);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.minZ);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.maxX);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.maxY);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.maxZ);
        result = 31 * result + (int)(temp ^ temp >>> 32);
        return result;
    }

    public AABB contract(double xa, double ya, double za) {
        double minX = this.minX;
        double minY = this.minY;
        double minZ = this.minZ;
        double maxX = this.maxX;
        double maxY = this.maxY;
        double maxZ = this.maxZ;
        if (xa < 0.0) {
            minX -= xa;
        } else if (xa > 0.0) {
            maxX -= xa;
        }
        if (ya < 0.0) {
            minY -= ya;
        } else if (ya > 0.0) {
            maxY -= ya;
        }
        if (za < 0.0) {
            minZ -= za;
        } else if (za > 0.0) {
            maxZ -= za;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB expandTowards(Vec3 delta) {
        return this.expandTowards(delta.x, delta.y, delta.z);
    }

    public AABB expandTowards(double xa, double ya, double za) {
        double minX = this.minX;
        double minY = this.minY;
        double minZ = this.minZ;
        double maxX = this.maxX;
        double maxY = this.maxY;
        double maxZ = this.maxZ;
        if (xa < 0.0) {
            minX += xa;
        } else if (xa > 0.0) {
            maxX += xa;
        }
        if (ya < 0.0) {
            minY += ya;
        } else if (ya > 0.0) {
            maxY += ya;
        }
        if (za < 0.0) {
            minZ += za;
        } else if (za > 0.0) {
            maxZ += za;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB inflate(double xAdd, double yAdd, double zAdd) {
        double minX = this.minX - xAdd;
        double minY = this.minY - yAdd;
        double minZ = this.minZ - zAdd;
        double maxX = this.maxX + xAdd;
        double maxY = this.maxY + yAdd;
        double maxZ = this.maxZ + zAdd;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB inflate(double amountToAddInAllDirections) {
        return this.inflate(amountToAddInAllDirections, amountToAddInAllDirections, amountToAddInAllDirections);
    }

    public AABB intersect(AABB other) {
        double minX = Math.max(this.minX, other.minX);
        double minY = Math.max(this.minY, other.minY);
        double minZ = Math.max(this.minZ, other.minZ);
        double maxX = Math.min(this.maxX, other.maxX);
        double maxY = Math.min(this.maxY, other.maxY);
        double maxZ = Math.min(this.maxZ, other.maxZ);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB minmax(AABB other) {
        double minX = Math.min(this.minX, other.minX);
        double minY = Math.min(this.minY, other.minY);
        double minZ = Math.min(this.minZ, other.minZ);
        double maxX = Math.max(this.maxX, other.maxX);
        double maxY = Math.max(this.maxY, other.maxY);
        double maxZ = Math.max(this.maxZ, other.maxZ);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB move(double xa, double ya, double za) {
        return new AABB(this.minX + xa, this.minY + ya, this.minZ + za, this.maxX + xa, this.maxY + ya, this.maxZ + za);
    }

    public AABB move(BlockPos pos) {
        return new AABB(this.minX + (double)pos.getX(), this.minY + (double)pos.getY(), this.minZ + (double)pos.getZ(), this.maxX + (double)pos.getX(), this.maxY + (double)pos.getY(), this.maxZ + (double)pos.getZ());
    }

    public AABB move(Vec3 pos) {
        return this.move(pos.x, pos.y, pos.z);
    }

    public AABB move(Vector3f pos) {
        return this.move(pos.x, pos.y, pos.z);
    }

    public boolean intersects(AABB aabb) {
        return this.intersects(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
    }

    public boolean intersects(Vec3 min, Vec3 max) {
        return this.intersects(Math.min(min.x, max.x), Math.min(min.y, max.y), Math.min(min.z, max.z), Math.max(min.x, max.x), Math.max(min.y, max.y), Math.max(min.z, max.z));
    }

    public boolean intersects(BlockPos pos) {
        return this.intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public boolean contains(Vec3 vec) {
        return this.contains(vec.x, vec.y, vec.z);
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
    }

    public double getSize() {
        double xs = this.getXsize();
        double ys = this.getYsize();
        double zs = this.getZsize();
        return (xs + ys + zs) / 3.0;
    }

    public double getXsize() {
        return this.maxX - this.minX;
    }

    public double getYsize() {
        return this.maxY - this.minY;
    }

    public double getZsize() {
        return this.maxZ - this.minZ;
    }

    public AABB deflate(double xSubstract, double ySubtract, double zSubtract) {
        return this.inflate(-xSubstract, -ySubtract, -zSubtract);
    }

    public AABB deflate(double amount) {
        return this.inflate(-amount);
    }

    public Optional<Vec3> clip(Vec3 from, Vec3 to) {
        return AABB.clip(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, from, to);
    }

    public static Optional<Vec3> clip(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3 from, Vec3 to) {
        double[] scaleReference = new double[]{1.0};
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        Direction direction = AABB.getDirection(minX, minY, minZ, maxX, maxY, maxZ, from, scaleReference, null, dx, dy, dz);
        if (direction == null) {
            return Optional.empty();
        }
        double scale = scaleReference[0];
        return Optional.of(from.add(scale * dx, scale * dy, scale * dz));
    }

    public static @Nullable BlockHitResult clip(Iterable<AABB> aabBs, Vec3 from, Vec3 to, BlockPos pos) {
        double[] scaleReference = new double[]{1.0};
        Direction direction = null;
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        for (AABB aabb : aabBs) {
            direction = AABB.getDirection(aabb.move(pos), from, scaleReference, direction, dx, dy, dz);
        }
        if (direction == null) {
            return null;
        }
        double scale = scaleReference[0];
        return new BlockHitResult(from.add(scale * dx, scale * dy, scale * dz), direction, pos, false);
    }

    private static @Nullable Direction getDirection(AABB aabb, Vec3 from, double[] scaleReference, @Nullable Direction direction, double dx, double dy, double dz) {
        return AABB.getDirection(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, from, scaleReference, direction, dx, dy, dz);
    }

    private static @Nullable Direction getDirection(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3 from, double[] scaleReference, @Nullable Direction direction, double dx, double dy, double dz) {
        if (dx > 1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dx, dy, dz, minX, minY, maxY, minZ, maxZ, Direction.WEST, from.x, from.y, from.z);
        } else if (dx < -1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dx, dy, dz, maxX, minY, maxY, minZ, maxZ, Direction.EAST, from.x, from.y, from.z);
        }
        if (dy > 1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dy, dz, dx, minY, minZ, maxZ, minX, maxX, Direction.DOWN, from.y, from.z, from.x);
        } else if (dy < -1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dy, dz, dx, maxY, minZ, maxZ, minX, maxX, Direction.UP, from.y, from.z, from.x);
        }
        if (dz > 1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dz, dx, dy, minZ, minX, maxX, minY, maxY, Direction.NORTH, from.z, from.x, from.y);
        } else if (dz < -1.0E-7) {
            direction = AABB.clipPoint(scaleReference, direction, dz, dx, dy, maxZ, minX, maxX, minY, maxY, Direction.SOUTH, from.z, from.x, from.y);
        }
        return direction;
    }

    private static @Nullable Direction clipPoint(double[] scaleReference, @Nullable Direction direction, double da, double db, double dc, double point, double minB, double maxB, double minC, double maxC, Direction newDirection, double fromA, double fromB, double fromC) {
        double s = (point - fromA) / da;
        double pb = fromB + s * db;
        double pc = fromC + s * dc;
        if (0.0 < s && s < scaleReference[0] && minB - 1.0E-7 < pb && pb < maxB + 1.0E-7 && minC - 1.0E-7 < pc && pc < maxC + 1.0E-7) {
            scaleReference[0] = s;
            return newDirection;
        }
        return direction;
    }

    public boolean collidedAlongVector(Vec3 vector, List<AABB> aabbs) {
        Vec3 from = this.getCenter();
        Vec3 to = from.add(vector);
        for (AABB shapePart : aabbs) {
            AABB inflated = shapePart.inflate(this.getXsize() * 0.5 - 1.0E-7, this.getYsize() * 0.5 - 1.0E-7, this.getZsize() * 0.5 - 1.0E-7);
            if (inflated.contains(to) || inflated.contains(from)) {
                return true;
            }
            if (!inflated.clip(from, to).isPresent()) continue;
            return true;
        }
        return false;
    }

    public double distanceToSqr(Vec3 point) {
        double dx = Math.max(Math.max(this.minX - point.x, point.x - this.maxX), 0.0);
        double dy = Math.max(Math.max(this.minY - point.y, point.y - this.maxY), 0.0);
        double dz = Math.max(Math.max(this.minZ - point.z, point.z - this.maxZ), 0.0);
        return Mth.lengthSquared(dx, dy, dz);
    }

    public double distanceToSqr(AABB boundingBox) {
        double dx = Math.max(Math.max(this.minX - boundingBox.maxX, boundingBox.minX - this.maxX), 0.0);
        double dy = Math.max(Math.max(this.minY - boundingBox.maxY, boundingBox.minY - this.maxY), 0.0);
        double dz = Math.max(Math.max(this.minZ - boundingBox.maxZ, boundingBox.minZ - this.maxZ), 0.0);
        return Mth.lengthSquared(dx, dy, dz);
    }

    public String toString() {
        return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }

    public boolean hasNaN() {
        return Double.isNaN(this.minX) || Double.isNaN(this.minY) || Double.isNaN(this.minZ) || Double.isNaN(this.maxX) || Double.isNaN(this.maxY) || Double.isNaN(this.maxZ);
    }

    public Vec3 getCenter() {
        return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), Mth.lerp(0.5, this.minY, this.maxY), Mth.lerp(0.5, this.minZ, this.maxZ));
    }

    public Vec3 getBottomCenter() {
        return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), this.minY, Mth.lerp(0.5, this.minZ, this.maxZ));
    }

    public Vec3 getMinPosition() {
        return new Vec3(this.minX, this.minY, this.minZ);
    }

    public Vec3 getMaxPosition() {
        return new Vec3(this.maxX, this.maxY, this.maxZ);
    }

    public static AABB ofSize(Vec3 center, double sizeX, double sizeY, double sizeZ) {
        return new AABB(center.x - sizeX / 2.0, center.y - sizeY / 2.0, center.z - sizeZ / 2.0, center.x + sizeX / 2.0, center.y + sizeY / 2.0, center.z + sizeZ / 2.0);
    }

    public static class Builder {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private boolean defined;

        public void include(Vector3fc v) {
            this.minX = Math.min(this.minX, v.x());
            this.minY = Math.min(this.minY, v.y());
            this.minZ = Math.min(this.minZ, v.z());
            this.maxX = Math.max(this.maxX, v.x());
            this.maxY = Math.max(this.maxY, v.y());
            this.maxZ = Math.max(this.maxZ, v.z());
            this.defined = true;
        }

        public boolean isDefined() {
            return this.defined;
        }

        public AABB build() {
            if (!this.defined) {
                throw new IllegalStateException("Cannot build an undefined AABB. Include at least one point.");
            }
            return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        }
    }
}

