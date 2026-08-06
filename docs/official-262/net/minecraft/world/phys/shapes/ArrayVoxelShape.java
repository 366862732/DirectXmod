/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleArrayList
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ArrayVoxelShape
extends VoxelShape {
    private final DoubleList xs;
    private final DoubleList ys;
    private final DoubleList zs;

    ArrayVoxelShape(DiscreteVoxelShape shape, double[] xs, double[] ys, double[] zs) {
        this(shape, (DoubleList)DoubleArrayList.wrap((double[])Arrays.copyOf(xs, shape.getXSize() + 1)), (DoubleList)DoubleArrayList.wrap((double[])Arrays.copyOf(ys, shape.getYSize() + 1)), (DoubleList)DoubleArrayList.wrap((double[])Arrays.copyOf(zs, shape.getZSize() + 1)));
    }

    ArrayVoxelShape(DiscreteVoxelShape shape, DoubleList xs, DoubleList ys, DoubleList zs) {
        super(shape);
        int xSize = shape.getXSize() + 1;
        int ySize = shape.getYSize() + 1;
        int zSize = shape.getZSize() + 1;
        if (xSize != xs.size() || ySize != ys.size() || zSize != zs.size()) {
            throw Util.pauseInIde(new IllegalArgumentException("Lengths of point arrays must be consistent with the size of the VoxelShape."));
        }
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
    }

    @Override
    public DoubleList getCoords(Direction.Axis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> this.xs;
            case Direction.Axis.Y -> this.ys;
            case Direction.Axis.Z -> this.zs;
        };
    }
}

