/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class StandingAndWallBlockItem
extends BlockItem {
    protected final Block wallBlock;
    private final Direction attachmentDirection;

    public StandingAndWallBlockItem(Block block, Block wallBlock, Direction attachmentDirection, Item.Properties properties) {
        super(block, properties);
        this.wallBlock = wallBlock;
        this.attachmentDirection = attachmentDirection;
    }

    protected boolean canPlace(LevelReader level, BlockState possibleState, BlockPos pos) {
        return possibleState.canSurvive(level, pos);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState wallState = this.wallBlock.getStateForPlacement(context);
        BlockState stateForPlacement = null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState possibleState;
            if (direction == this.attachmentDirection.getOpposite()) continue;
            BlockState blockState = possibleState = direction == this.attachmentDirection ? this.getBlock().getStateForPlacement(context) : wallState;
            if (possibleState == null || !this.canPlace(level, possibleState, pos)) continue;
            stateForPlacement = possibleState;
            break;
        }
        return stateForPlacement != null && level.isUnobstructed(stateForPlacement, pos, CollisionContext.empty()) ? stateForPlacement : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> map, Item item) {
        super.registerBlocks(map, item);
        map.put(this.wallBlock, item);
    }
}

