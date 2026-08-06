/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Iterators
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  io.netty.buffer.ByteBuf
 *  org.jetbrains.annotations.Contract
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public enum Direction implements StringRepresentable
{
    DOWN(0, 1, -1, "down", AxisDirection.NEGATIVE, Axis.Y, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, "up", AxisDirection.POSITIVE, Axis.Y, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, "north", AxisDirection.NEGATIVE, Axis.Z, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, "south", AxisDirection.POSITIVE, Axis.Z, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, "west", AxisDirection.NEGATIVE, Axis.X, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, "east", AxisDirection.POSITIVE, Axis.X, new Vec3i(1, 0, 0));

    public static final StringRepresentable.EnumCodec<Direction> CODEC;
    public static final Codec<Direction> VERTICAL_CODEC;
    public static final IntFunction<Direction> BY_ID;
    public static final StreamCodec<ByteBuf, Direction> STREAM_CODEC;
    @Deprecated
    public static final Codec<Direction> LEGACY_ID_CODEC;
    @Deprecated
    public static final Codec<Direction> LEGACY_ID_CODEC_2D;
    private static final ImmutableList<Axis> YXZ_AXIS_ORDER;
    private static final ImmutableList<Axis> YZX_AXIS_ORDER;
    private final int data3d;
    private final int oppositeIndex;
    private final int data2d;
    private final String name;
    private final Axis axis;
    private final AxisDirection axisDirection;
    private final Vec3i normal;
    private final Vec3 normalVec3;
    private final Vector3fc normalVec3f;
    private static final Direction[] VALUES;
    private static final Direction[] BY_3D_DATA;
    private static final Direction[] BY_2D_DATA;

    private Direction(int data3d, int oppositeIndex, int data2d, String name, AxisDirection axisDirection, Axis axis, Vec3i normal) {
        this.data3d = data3d;
        this.data2d = data2d;
        this.oppositeIndex = oppositeIndex;
        this.name = name;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.normal = normal;
        this.normalVec3 = Vec3.atLowerCornerOf(normal);
        this.normalVec3f = new Vector3f((float)normal.getX(), (float)normal.getY(), (float)normal.getZ());
    }

    public static Direction[] orderedByNearest(Entity entity) {
        Direction axisZ;
        float pitch = entity.getViewXRot(1.0f) * ((float)Math.PI / 180);
        float yaw = -entity.getViewYRot(1.0f) * ((float)Math.PI / 180);
        float pitchSin = Mth.sin(pitch);
        float pitchCos = Mth.cos(pitch);
        float yawSin = Mth.sin(yaw);
        float yawCos = Mth.cos(yaw);
        boolean xPos = yawSin > 0.0f;
        boolean yPos = pitchSin < 0.0f;
        boolean zPos = yawCos > 0.0f;
        float xYaw = xPos ? yawSin : -yawSin;
        float yMag = yPos ? -pitchSin : pitchSin;
        float zYaw = zPos ? yawCos : -yawCos;
        float xMag = xYaw * pitchCos;
        float zMag = zYaw * pitchCos;
        Direction axisX = xPos ? EAST : WEST;
        Direction axisY = yPos ? UP : DOWN;
        Direction direction = axisZ = zPos ? SOUTH : NORTH;
        if (xYaw > zYaw) {
            if (yMag > xMag) {
                return Direction.makeDirectionArray(axisY, axisX, axisZ);
            }
            if (zMag > yMag) {
                return Direction.makeDirectionArray(axisX, axisZ, axisY);
            }
            return Direction.makeDirectionArray(axisX, axisY, axisZ);
        }
        if (yMag > zMag) {
            return Direction.makeDirectionArray(axisY, axisZ, axisX);
        }
        if (xMag > yMag) {
            return Direction.makeDirectionArray(axisZ, axisX, axisY);
        }
        return Direction.makeDirectionArray(axisZ, axisY, axisX);
    }

    private static Direction[] makeDirectionArray(Direction axis1, Direction axis2, Direction axis3) {
        return new Direction[]{axis1, axis2, axis3, axis3.getOpposite(), axis2.getOpposite(), axis1.getOpposite()};
    }

    public static Direction rotate(Matrix4fc matrix, Direction facing) {
        Vector3f vec = matrix.transformDirection(facing.normalVec3f, new Vector3f());
        return Direction.getApproximateNearest(vec.x(), vec.y(), vec.z());
    }

    public static Collection<Direction> allShuffled(RandomSource random) {
        return Util.shuffledCopy(Direction.values(), random);
    }

    public static Stream<Direction> stream() {
        return Stream.of(VALUES);
    }

    public static float getYRot(Direction direction) {
        return switch (direction.ordinal()) {
            case 2 -> 180.0f;
            case 3 -> 0.0f;
            case 4 -> 90.0f;
            case 5 -> -90.0f;
            default -> throw new IllegalStateException("No y-Rot for vertical axis: " + String.valueOf(direction));
        };
    }

    public Quaternionf getRotation() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> new Quaternionf().rotationX((float)Math.PI);
            case 1 -> new Quaternionf();
            case 2 -> new Quaternionf().rotationXYZ(1.5707964f, 0.0f, (float)Math.PI);
            case 3 -> new Quaternionf().rotationX(1.5707964f);
            case 4 -> new Quaternionf().rotationXYZ(1.5707964f, 0.0f, 1.5707964f);
            case 5 -> new Quaternionf().rotationXYZ(1.5707964f, 0.0f, -1.5707964f);
        };
    }

    public int get3DDataValue() {
        return this.data3d;
    }

    public int get2DDataValue() {
        return this.data2d;
    }

    public AxisDirection getAxisDirection() {
        return this.axisDirection;
    }

    public static Direction getFacingAxis(Entity entity, Axis axis) {
        return switch (axis.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (EAST.isFacingAngle(entity.getViewYRot(1.0f))) {
                    yield EAST;
                }
                yield WEST;
            }
            case 2 -> {
                if (SOUTH.isFacingAngle(entity.getViewYRot(1.0f))) {
                    yield SOUTH;
                }
                yield NORTH;
            }
            case 1 -> entity.getViewXRot(1.0f) < 0.0f ? UP : DOWN;
        };
    }

    public Direction getOpposite() {
        return Direction.from3DDataValue(this.oppositeIndex);
    }

    public Direction getClockWise(Axis axis) {
        return switch (axis.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (this == WEST || this == EAST) {
                    yield this;
                }
                yield this.getClockWiseX();
            }
            case 1 -> {
                if (this == UP || this == DOWN) {
                    yield this;
                }
                yield this.getClockWise();
            }
            case 2 -> this == NORTH || this == SOUTH ? this : this.getClockWiseZ();
        };
    }

    public Direction getCounterClockWise(Axis axis) {
        return switch (axis.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (this == WEST || this == EAST) {
                    yield this;
                }
                yield this.getCounterClockWiseX();
            }
            case 1 -> {
                if (this == UP || this == DOWN) {
                    yield this;
                }
                yield this.getCounterClockWise();
            }
            case 2 -> this == NORTH || this == SOUTH ? this : this.getCounterClockWiseZ();
        };
    }

    public Direction getClockWise() {
        return switch (this.ordinal()) {
            case 2 -> EAST;
            case 5 -> SOUTH;
            case 3 -> WEST;
            case 4 -> NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + String.valueOf(this));
        };
    }

    private Direction getClockWiseX() {
        return switch (this.ordinal()) {
            case 1 -> NORTH;
            case 2 -> DOWN;
            case 0 -> SOUTH;
            case 3 -> UP;
            default -> throw new IllegalStateException("Unable to get X-rotated facing of " + String.valueOf(this));
        };
    }

    private Direction getCounterClockWiseX() {
        return switch (this.ordinal()) {
            case 1 -> SOUTH;
            case 3 -> DOWN;
            case 0 -> NORTH;
            case 2 -> UP;
            default -> throw new IllegalStateException("Unable to get X-rotated facing of " + String.valueOf(this));
        };
    }

    private Direction getClockWiseZ() {
        return switch (this.ordinal()) {
            case 1 -> EAST;
            case 5 -> DOWN;
            case 0 -> WEST;
            case 4 -> UP;
            default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + String.valueOf(this));
        };
    }

    private Direction getCounterClockWiseZ() {
        return switch (this.ordinal()) {
            case 1 -> WEST;
            case 4 -> DOWN;
            case 0 -> EAST;
            case 5 -> UP;
            default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + String.valueOf(this));
        };
    }

    public Direction getCounterClockWise() {
        return switch (this.ordinal()) {
            case 2 -> WEST;
            case 5 -> NORTH;
            case 3 -> EAST;
            case 4 -> SOUTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + String.valueOf(this));
        };
    }

    public int getStepX() {
        return this.normal.getX();
    }

    public int getStepY() {
        return this.normal.getY();
    }

    public int getStepZ() {
        return this.normal.getZ();
    }

    public Vector3f step() {
        return new Vector3f(this.normalVec3f);
    }

    public String getName() {
        return this.name;
    }

    public Axis getAxis() {
        return this.axis;
    }

    public static @Nullable Direction byName(String name) {
        return CODEC.byName(name);
    }

    public static Direction from3DDataValue(int data) {
        return BY_3D_DATA[Mth.abs(data % BY_3D_DATA.length)];
    }

    public static Direction from2DDataValue(int data) {
        return BY_2D_DATA[Mth.abs(data % BY_2D_DATA.length)];
    }

    public static Direction fromYRot(double yRot) {
        return Direction.from2DDataValue(Mth.floor(yRot / 90.0 + 0.5) & 3);
    }

    public static Direction fromAxisAndDirection(Axis axis, AxisDirection direction) {
        return switch (axis.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (direction == AxisDirection.POSITIVE) {
                    yield EAST;
                }
                yield WEST;
            }
            case 1 -> {
                if (direction == AxisDirection.POSITIVE) {
                    yield UP;
                }
                yield DOWN;
            }
            case 2 -> direction == AxisDirection.POSITIVE ? SOUTH : NORTH;
        };
    }

    public float toYRot() {
        return (this.data2d & 3) * 90;
    }

    public static Direction getRandom(RandomSource random) {
        return Util.getRandom(VALUES, random);
    }

    public static Direction getApproximateNearest(double dx, double dy, double dz) {
        return Direction.getApproximateNearest((float)dx, (float)dy, (float)dz);
    }

    public static Direction getApproximateNearest(float dx, float dy, float dz) {
        Direction result = NORTH;
        float highestDot = Float.MIN_VALUE;
        for (Direction direction : VALUES) {
            float dot = dx * (float)direction.normal.getX() + dy * (float)direction.normal.getY() + dz * (float)direction.normal.getZ();
            if (!(dot > highestDot)) continue;
            highestDot = dot;
            result = direction;
        }
        return result;
    }

    public static Direction getApproximateNearest(Vec3 vec) {
        return Direction.getApproximateNearest(vec.x, vec.y, vec.z);
    }

    @Contract(value="_,_,_,!null->!null;_,_,_,_->_")
    public static @Nullable Direction getNearest(int x, int y, int z, @Nullable Direction orElse) {
        int absX = Math.abs(x);
        int absY = Math.abs(y);
        int absZ = Math.abs(z);
        if (absX > absZ && absX > absY) {
            return x < 0 ? WEST : EAST;
        }
        if (absZ > absX && absZ > absY) {
            return z < 0 ? NORTH : SOUTH;
        }
        if (absY > absX && absY > absZ) {
            return y < 0 ? DOWN : UP;
        }
        return orElse;
    }

    @Contract(value="_,!null->!null;_,_->_")
    public static @Nullable Direction getNearest(Vec3i vec, @Nullable Direction orElse) {
        return Direction.getNearest(vec.getX(), vec.getY(), vec.getZ(), orElse);
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    private static DataResult<Direction> verifyVertical(Direction v) {
        return v.getAxis().isVertical() ? DataResult.success((Object)v) : DataResult.error(() -> "Expected a vertical direction");
    }

    public static Direction get(AxisDirection axisDirection, Axis axis) {
        for (Direction direction : VALUES) {
            if (direction.getAxisDirection() != axisDirection || direction.getAxis() != axis) continue;
            return direction;
        }
        throw new IllegalArgumentException("No such direction: " + String.valueOf((Object)axisDirection) + " " + String.valueOf(axis));
    }

    public static ImmutableList<Axis> axisStepOrder(Vec3 movement) {
        if (Math.abs(movement.x) < Math.abs(movement.z)) {
            return YZX_AXIS_ORDER;
        }
        return YXZ_AXIS_ORDER;
    }

    public Vec3i getUnitVec3i() {
        return this.normal;
    }

    public Vec3 getUnitVec3() {
        return this.normalVec3;
    }

    public Vector3fc getUnitVec3f() {
        return this.normalVec3f;
    }

    public boolean isFacingAngle(float yAngle) {
        float radians = yAngle * ((float)Math.PI / 180);
        float dx = -Mth.sin(radians);
        float dz = Mth.cos(radians);
        return (float)this.normal.getX() * dx + (float)this.normal.getZ() * dz > 0.0f;
    }

    static {
        CODEC = StringRepresentable.fromEnum(Direction::values);
        VERTICAL_CODEC = CODEC.validate(Direction::verifyVertical);
        BY_ID = ByIdMap.continuous(Direction::get3DDataValue, Direction.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Direction::get3DDataValue);
        LEGACY_ID_CODEC = Codec.BYTE.xmap(Direction::from3DDataValue, d -> (byte)d.get3DDataValue());
        LEGACY_ID_CODEC_2D = Codec.BYTE.xmap(Direction::from2DDataValue, d -> (byte)d.get2DDataValue());
        YXZ_AXIS_ORDER = ImmutableList.of((Object)Axis.Y, (Object)Axis.X, (Object)Axis.Z);
        YZX_AXIS_ORDER = ImmutableList.of((Object)Axis.Y, (Object)Axis.Z, (Object)Axis.X);
        VALUES = Direction.values();
        BY_3D_DATA = (Direction[])Arrays.stream(VALUES).sorted(Comparator.comparingInt(d -> d.data3d)).toArray(Direction[]::new);
        BY_2D_DATA = (Direction[])Arrays.stream(VALUES).filter(d -> d.getAxis().isHorizontal()).sorted(Comparator.comparingInt(d -> d.data2d)).toArray(Direction[]::new);
    }

    public static enum Axis implements Predicate<Direction>,
    StringRepresentable
    {
        X("x"){

            @Override
            public int choose(int x, int y, int z) {
                return x;
            }

            @Override
            public boolean choose(boolean x, boolean y, boolean z) {
                return x;
            }

            @Override
            public double choose(double x, double y, double z) {
                return x;
            }

            @Override
            public Direction getPositive() {
                return EAST;
            }

            @Override
            public Direction getNegative() {
                return WEST;
            }
        }
        ,
        Y("y"){

            @Override
            public int choose(int x, int y, int z) {
                return y;
            }

            @Override
            public double choose(double x, double y, double z) {
                return y;
            }

            @Override
            public boolean choose(boolean x, boolean y, boolean z) {
                return y;
            }

            @Override
            public Direction getPositive() {
                return UP;
            }

            @Override
            public Direction getNegative() {
                return DOWN;
            }
        }
        ,
        Z("z"){

            @Override
            public int choose(int x, int y, int z) {
                return z;
            }

            @Override
            public double choose(double x, double y, double z) {
                return z;
            }

            @Override
            public boolean choose(boolean x, boolean y, boolean z) {
                return z;
            }

            @Override
            public Direction getPositive() {
                return SOUTH;
            }

            @Override
            public Direction getNegative() {
                return NORTH;
            }
        };

        public static final Axis[] VALUES;
        public static final StringRepresentable.EnumCodec<Axis> CODEC;
        private final String name;

        private Axis(String name) {
            this.name = name;
        }

        public static @Nullable Axis byName(String name) {
            return CODEC.byName(name);
        }

        public String getName() {
            return this.name;
        }

        public boolean isVertical() {
            return this == Y;
        }

        public boolean isHorizontal() {
            return this == X || this == Z;
        }

        public abstract Direction getPositive();

        public abstract Direction getNegative();

        public Direction[] getDirections() {
            return new Direction[]{this.getPositive(), this.getNegative()};
        }

        public String toString() {
            return this.name;
        }

        public static Axis getRandom(RandomSource random) {
            return Util.getRandom(VALUES, random);
        }

        @Override
        public boolean test(@Nullable Direction input) {
            return input != null && input.getAxis() == this;
        }

        public Plane getPlane() {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0, 2 -> Plane.HORIZONTAL;
                case 1 -> Plane.VERTICAL;
            };
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract int choose(int var1, int var2, int var3);

        public abstract double choose(double var1, double var3, double var5);

        public abstract boolean choose(boolean var1, boolean var2, boolean var3);

        static {
            VALUES = Axis.values();
            CODEC = StringRepresentable.fromEnum(Axis::values);
        }
    }

    public static enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int step;
        private final String name;

        private AxisDirection(int step, String name) {
            this.step = step;
            this.name = name;
        }

        public int getStep() {
            return this.step;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return this.name;
        }

        public AxisDirection opposite() {
            return this == POSITIVE ? NEGATIVE : POSITIVE;
        }
    }

    public static enum Plane implements Predicate<Direction>,
    Iterable<Direction>
    {
        HORIZONTAL(new Direction[]{NORTH, EAST, SOUTH, WEST}, new Axis[]{Axis.X, Axis.Z}),
        VERTICAL(new Direction[]{UP, DOWN}, new Axis[]{Axis.Y});

        private final Direction[] faces;
        private final Axis[] axis;

        private Plane(Direction[] faces, Axis[] axis) {
            this.faces = faces;
            this.axis = axis;
        }

        public Direction getRandomDirection(RandomSource random) {
            return Util.getRandom(this.faces, random);
        }

        public Axis getRandomAxis(RandomSource random) {
            return Util.getRandom(this.axis, random);
        }

        @Override
        public boolean test(@Nullable Direction input) {
            return input != null && input.getAxis().getPlane() == this;
        }

        @Override
        public Iterator<Direction> iterator() {
            return Iterators.forArray((Object[])this.faces);
        }

        public Stream<Direction> stream() {
            return Arrays.stream(this.faces);
        }

        public List<Direction> shuffledCopy(RandomSource random) {
            return Util.shuffledCopy(this.faces, random);
        }

        public int length() {
            return this.faces.length;
        }
    }
}

