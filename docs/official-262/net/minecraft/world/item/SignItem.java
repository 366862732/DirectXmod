/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SignItem
extends StandingAndWallBlockItem {
    public SignItem(Block sign, Block wallSign, Item.Properties properties) {
        super(sign, wallSign, Direction.DOWN, properties);
    }

    public SignItem(Item.Properties properties, Block sign, Block wallSign, Direction direction) {
        super(sign, wallSign, direction, properties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack itemStack, BlockState placedState) {
        Object object;
        boolean success = super.updateCustomBlockEntityTag(pos, level, player, itemStack, placedState);
        if (!level.isClientSide() && !success && player != null && (object = level.getBlockEntity(pos)) instanceof SignBlockEntity) {
            SignBlockEntity signEntity = (SignBlockEntity)object;
            object = level.getBlockState(pos).getBlock();
            if (object instanceof SignBlock) {
                SignBlock sign = (SignBlock)object;
                sign.openTextEdit(player, signEntity, true);
            }
        }
        return success;
    }
}

