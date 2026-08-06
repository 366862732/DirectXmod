/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPlaceContext
extends UseOnContext {
    private final BlockPos relativePos;
    protected boolean replaceClicked = true;

    public BlockPlaceContext(Player player, InteractionHand hand, ItemStack itemInHand, BlockHitResult hitResult) {
        this(player.level(), player, hand, itemInHand, hitResult);
    }

    public BlockPlaceContext(UseOnContext context) {
        this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getHitResult());
    }

    protected BlockPlaceContext(Level level, @Nullable Player player, InteractionHand hand, ItemStack itemStackInHand, BlockHitResult hitResult) {
        super(level, player, hand, itemStackInHand, hitResult);
        this.relativePos = hitResult.getBlockPos().relative(hitResult.getDirection());
        this.replaceClicked = level.getBlockState(hitResult.getBlockPos()).canBeReplaced(this);
    }

    public static BlockPlaceContext at(BlockPlaceContext context, BlockPos pos, Direction direction) {
        return new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(new Vec3((double)pos.getX() + 0.5 + (double)direction.getStepX() * 0.5, (double)pos.getY() + 0.5 + (double)direction.getStepY() * 0.5, (double)pos.getZ() + 0.5 + (double)direction.getStepZ() * 0.5), direction, pos, false));
    }

    @Override
    public BlockPos getClickedPos() {
        return this.replaceClicked ? super.getClickedPos() : this.relativePos;
    }

    public boolean canPlace() {
        return this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
    }

    public boolean replacingClickedOnBlock() {
        return this.replaceClicked;
    }

    public Direction getNearestLookingDirection() {
        return Direction.orderedByNearest(this.getPlayer())[0];
    }

    public Direction getNearestLookingVerticalDirection() {
        return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
    }

    public Direction[] getNearestLookingDirections() {
        int index;
        Direction[] directions = Direction.orderedByNearest(this.getPlayer());
        if (this.replaceClicked) {
            return directions;
        }
        Direction clickedFace = this.getClickedFace();
        for (index = 0; index < directions.length && directions[index] != clickedFace.getOpposite(); ++index) {
        }
        if (index > 0) {
            System.arraycopy(directions, 0, directions, 1, index);
            directions[0] = clickedFace.getOpposite();
        }
        return directions;
    }
}

