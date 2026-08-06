/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.doubles.DoubleList
 */
package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CubePointRange;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.SubShape;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SliceShape
extends VoxelShape {
    private final VoxelShape delegate;
    private final Direction.Axis axis;
    private static final DoubleList SLICE_COORDS = new CubePointRange(1);

    public SliceShape(VoxelShape delegate, Direction.Axis axis, int point) {
        super(SliceShape.makeSlice(delegate.shape, axis, point));
        this.delegate = delegate;
        this.axis = axis;
    }

    private static DiscreteVoxelShape makeSlice(DiscreteVoxelShape delegate, Direction.Axis axis, int point) {
        return new SubShape(delegate, axis.choose(point, 0, 0), axis.choose(0, point, 0), axis.choose(0, 0, point), axis.choose(point + 1, delegate.xSize, delegate.xSize), axis.choose(delegate.ySize, point + 1, delegate.ySize), axis.choose(delegate.zSize, delegate.zSize, point + 1));
    }

    @Override
    public DoubleList getCoords(Direction.Axis axis) {
        if (axis == this.axis) {
            return SLICE_COORDS;
        }
        return this.delegate.getCoords(axis);
    }
}

