/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Column {
    public static Range around(int lowest, int highest) {
        return new Range(lowest - 1, highest + 1);
    }

    public static Range inside(int floor, int ceiling) {
        return new Range(floor, ceiling);
    }

    public static Column below(int ceiling) {
        return new Ray(ceiling, false);
    }

    public static Column fromHighest(int highest) {
        return new Ray(highest + 1, false);
    }

    public static Column above(int floor) {
        return new Ray(floor, true);
    }

    public static Column fromLowest(int lowest) {
        return new Ray(lowest - 1, true);
    }

    public static Column line() {
        return Line.INSTANCE;
    }

    public static Column create(OptionalInt floor, OptionalInt ceiling) {
        if (floor.isPresent() && ceiling.isPresent()) {
            return Column.inside(floor.getAsInt(), ceiling.getAsInt());
        }
        if (floor.isPresent()) {
            return Column.above(floor.getAsInt());
        }
        if (ceiling.isPresent()) {
            return Column.below(ceiling.getAsInt());
        }
        return Column.line();
    }

    public abstract OptionalInt getCeiling();

    public abstract OptionalInt getFloor();

    public abstract OptionalInt getHeight();

    public Column withFloor(OptionalInt floor) {
        return Column.create(floor, this.getCeiling());
    }

    public Column withCeiling(OptionalInt ceiling) {
        return Column.create(this.getFloor(), ceiling);
    }

    public static Optional<Column> scan(LevelSimulatedReader level, BlockPos pos, int searchRange, Predicate<BlockState> insideColumn, Predicate<BlockState> validEdge) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        if (!level.isStateAtPosition(pos, insideColumn)) {
            return Optional.empty();
        }
        int nearestEmptyY = pos.getY();
        OptionalInt ceiling = Column.scanDirection(level, searchRange, insideColumn, validEdge, mutablePos, nearestEmptyY, Direction.UP);
        OptionalInt floor = Column.scanDirection(level, searchRange, insideColumn, validEdge, mutablePos, nearestEmptyY, Direction.DOWN);
        return Optional.of(Column.create(floor, ceiling));
    }

    private static OptionalInt scanDirection(LevelSimulatedReader level, int searchRange, Predicate<BlockState> insideColumn, Predicate<BlockState> validEdge, BlockPos.MutableBlockPos mutablePos, int nearestEmptyY, Direction direction) {
        mutablePos.setY(nearestEmptyY);
        for (int i = 1; i < searchRange && level.isStateAtPosition(mutablePos, insideColumn); ++i) {
            mutablePos.move(direction);
        }
        return level.isStateAtPosition(mutablePos, validEdge) ? OptionalInt.of(mutablePos.getY()) : OptionalInt.empty();
    }

    public static final class Range
    extends Column {
        private final int floor;
        private final int ceiling;

        public Range(int floor, int ceiling) {
            this.floor = floor;
            this.ceiling = ceiling;
            if (this.height() < 0) {
                throw new IllegalArgumentException("Column of negative height: " + String.valueOf(this));
            }
        }

        @Override
        public OptionalInt getCeiling() {
            return OptionalInt.of(this.ceiling);
        }

        @Override
        public OptionalInt getFloor() {
            return OptionalInt.of(this.floor);
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.of(this.height());
        }

        public int ceiling() {
            return this.ceiling;
        }

        public int floor() {
            return this.floor;
        }

        public int height() {
            return this.ceiling - this.floor - 1;
        }

        public String toString() {
            return "C(" + this.ceiling + "-" + this.floor + ")";
        }
    }

    public static final class Ray
    extends Column {
        private final int edge;
        private final boolean pointingUp;

        public Ray(int edge, boolean pointingUp) {
            this.edge = edge;
            this.pointingUp = pointingUp;
        }

        @Override
        public OptionalInt getCeiling() {
            return this.pointingUp ? OptionalInt.empty() : OptionalInt.of(this.edge);
        }

        @Override
        public OptionalInt getFloor() {
            return this.pointingUp ? OptionalInt.of(this.edge) : OptionalInt.empty();
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.empty();
        }

        public String toString() {
            return this.pointingUp ? "C(" + this.edge + "-)" : "C(-" + this.edge + ")";
        }
    }

    public static final class Line
    extends Column {
        private static final Line INSTANCE = new Line();

        private Line() {
        }

        @Override
        public OptionalInt getCeiling() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt getFloor() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt getHeight() {
            return OptionalInt.empty();
        }

        public String toString() {
            return "C(-)";
        }
    }
}

