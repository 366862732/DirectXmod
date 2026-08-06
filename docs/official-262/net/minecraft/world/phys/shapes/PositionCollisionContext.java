/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PositionCollisionContext
implements CollisionContext {
    private final double y;

    public PositionCollisionContext(double y) {
        this.y = y;
    }

    @Override
    public boolean isDescending() {
        return false;
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
        return this.y > (double)pos.getY() + shape.max(Direction.Axis.Y) - (double)1.0E-5f;
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return false;
    }

    @Override
    public boolean alwaysCollideWithFluid() {
        return false;
    }

    @Override
    public boolean canStandOnFluid(FluidState fluidStateAbove, FluidState fluid) {
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, CollisionGetter collisionGetter, BlockPos pos) {
        return state.getCollisionShape(collisionGetter, pos, this);
    }
}

