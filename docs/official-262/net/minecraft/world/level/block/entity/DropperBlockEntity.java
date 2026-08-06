/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DropperBlockEntity
extends DispenserBlockEntity {
    private static final Component DEFAULT_NAME = Component.translatable("container.dropper");

    public DropperBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.DROPPER, worldPosition, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return DEFAULT_NAME;
    }
}

