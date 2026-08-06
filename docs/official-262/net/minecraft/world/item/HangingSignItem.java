/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HangingSignItem
extends SignItem {
    public HangingSignItem(Block hangingSign, Block wallHangingSign, Item.Properties properties) {
        super(properties, hangingSign, wallHangingSign, Direction.UP);
    }

    @Override
    protected boolean canPlace(LevelReader level, BlockState possibleState, BlockPos pos) {
        WallHangingSignBlock hangingSign;
        Block block = possibleState.getBlock();
        if (block instanceof WallHangingSignBlock && !(hangingSign = (WallHangingSignBlock)block).canPlace(possibleState, level, pos)) {
            return false;
        }
        return super.canPlace(level, possibleState, pos);
    }
}

