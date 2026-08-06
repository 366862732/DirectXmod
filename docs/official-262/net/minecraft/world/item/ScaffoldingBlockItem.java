/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ScaffoldingBlockItem
extends BlockItem {
    public ScaffoldingBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Block block;
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState replacedState = level.getBlockState(pos);
        if (replacedState.is(block = this.getBlock())) {
            Direction direction = context.isSecondaryUseActive() ? (context.isInside() ? context.getClickedFace().getOpposite() : context.getClickedFace()) : (context.getClickedFace() == Direction.UP ? context.getHorizontalDirection() : Direction.UP);
            int horizontalDistance = 0;
            BlockPos.MutableBlockPos placementPos = pos.mutable().move(direction);
            while (horizontalDistance < 7) {
                if (!level.isClientSide() && !level.isInWorldBounds(placementPos)) {
                    Player player = context.getPlayer();
                    int maxY = level.getMaxY();
                    if (!(player instanceof ServerPlayer)) break;
                    ServerPlayer serverPlayer = (ServerPlayer)player;
                    if (placementPos.getY() <= maxY) break;
                    serverPlayer.sendBuildLimitMessage(true, maxY);
                    break;
                }
                replacedState = level.getBlockState(placementPos);
                if (!replacedState.is(this.getBlock())) {
                    if (!replacedState.canBeReplaced(context)) break;
                    return BlockPlaceContext.at(context, placementPos, direction);
                }
                placementPos.move(direction);
                if (!direction.getAxis().isHorizontal()) continue;
                ++horizontalDistance;
            }
            return null;
        }
        if (ScaffoldingBlock.getDistance(level, pos) == 7) {
            return null;
        }
        return context;
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }
}

