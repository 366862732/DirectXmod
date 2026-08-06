/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;

public class TheEndPortalBlockEntity
extends BlockEntity {
    protected TheEndPortalBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }

    public TheEndPortalBlockEntity(BlockPos worldPosition, BlockState blockState) {
        this(BlockEntityTypes.END_PORTAL, worldPosition, blockState);
    }

    public boolean shouldRenderFace(Direction direction) {
        return direction.getAxis() == Direction.Axis.Y;
    }
}

