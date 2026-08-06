/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.core.dispenser;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.slf4j.Logger;

public class ShulkerBoxDispenseBehavior
extends OptionalDispenseItemBehavior {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    protected ItemStack execute(BlockSource source, ItemStack dispensed) {
        this.setSuccess(false);
        Item item = dispensed.getItem();
        if (item instanceof BlockItem) {
            BlockItem blockItem = (BlockItem)item;
            Direction facing = source.state().getValue(DispenserBlock.FACING);
            BlockPos relativePos = source.pos().relative(facing);
            Direction clickedFace = source.level().isEmptyBlock(relativePos.below()) ? facing : Direction.UP;
            try {
                this.setSuccess(blockItem.place(new DirectionalPlaceContext((Level)source.level(), relativePos, facing, dispensed, clickedFace)).consumesAction());
            }
            catch (Exception e) {
                LOGGER.error("Error trying to place shulker box at {}", (Object)relativePos, (Object)e);
            }
        }
        return dispensed;
    }
}

