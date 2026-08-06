/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.core;

import net.minecraft.core.Direction;

public enum AxisCycle {
    NONE{

        @Override
        public int cycle(int x, int y, int z, Direction.Axis axis) {
            return axis.choose(x, y, z);
        }

        @Override
        public double cycle(double x, double y, double z, Direction.Axis axis) {
            return axis.choose(x, y, z);
        }

        @Override
        public Direction.Axis cycle(Direction.Axis axis) {
            return axis;
        }

        @Override
        public AxisCycle inverse() {
            return this;
        }
    }
    ,
    FORWARD{

        @Override
        public int cycle(int x, int y, int z, Direction.Axis axis) {
            return axis.choose(z, x, y);
        }

        @Override
        public double cycle(double x, double y, double z, Direction.Axis axis) {
            return axis.choose(z, x, y);
        }

        @Override
        public Direction.Axis cycle(Direction.Axis axis) {
            return AXIS_VALUES[Math.floorMod(axis.ordinal() + 1, 3)];
        }

        @Override
        public AxisCycle inverse() {
            return BACKWARD;
        }
    }
    ,
    BACKWARD{

        @Override
        public int cycle(int x, int y, int z, Direction.Axis axis) {
            return axis.choose(y, z, x);
        }

        @Override
        public double cycle(double x, double y, double z, Direction.Axis axis) {
            return axis.choose(y, z, x);
        }

        @Override
        public Direction.Axis cycle(Direction.Axis axis) {
            return AXIS_VALUES[Math.floorMod(axis.ordinal() - 1, 3)];
        }

        @Override
        public AxisCycle inverse() {
            return FORWARD;
        }
    };

    public static final Direction.Axis[] AXIS_VALUES;
    public static final AxisCycle[] VALUES;

    public abstract int cycle(int var1, int var2, int var3, Direction.Axis var4);

    public abstract double cycle(double var1, double var3, double var5, Direction.Axis var7);

    public abstract Direction.Axis cycle(Direction.Axis var1);

    public abstract AxisCycle inverse();

    public static AxisCycle between(Direction.Axis from, Direction.Axis to) {
        return VALUES[Math.floorMod(to.ordinal() - from.ordinal(), 3)];
    }

    static {
        AXIS_VALUES = Direction.Axis.values();
        VALUES = AxisCycle.values();
    }
}

