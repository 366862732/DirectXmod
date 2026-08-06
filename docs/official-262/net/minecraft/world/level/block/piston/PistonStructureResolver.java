/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 */
package net.minecraft.world.level.block.piston;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class PistonStructureResolver {
    public static final int MAX_PUSH_DEPTH = 12;
    private final Level level;
    private final BlockPos pistonPos;
    private final boolean extending;
    private final BlockPos startPos;
    private final Direction pushDirection;
    private final List<BlockPos> toPush = Lists.newArrayList();
    private final List<BlockPos> toDestroy = Lists.newArrayList();
    private final Direction pistonDirection;

    public PistonStructureResolver(Level level, BlockPos pistonPos, Direction direction, boolean extending) {
        this.level = level;
        this.pistonPos = pistonPos;
        this.pistonDirection = direction;
        this.extending = extending;
        if (extending) {
            this.pushDirection = direction;
            this.startPos = pistonPos.relative(direction);
        } else {
            this.pushDirection = direction.getOpposite();
            this.startPos = pistonPos.relative(direction, 2);
        }
    }

    public boolean resolve() {
        this.toPush.clear();
        this.toDestroy.clear();
        BlockState nextState = this.level.getBlockState(this.startPos);
        if (!PistonBaseBlock.isPushable(nextState, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending && nextState.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(this.startPos);
                return true;
            }
            return false;
        }
        if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
        }
        for (int i = 0; i < this.toPush.size(); ++i) {
            BlockPos pos = this.toPush.get(i);
            if (!PistonStructureResolver.isSticky(this.level.getBlockState(pos)) || this.addBranchingBlocks(pos)) continue;
            return false;
        }
        return true;
    }

    private static boolean isSticky(BlockState state) {
        return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
    }

    private static boolean canStickToEachOther(BlockState state1, BlockState state2) {
        if (state1.is(Blocks.HONEY_BLOCK) && state2.is(Blocks.SLIME_BLOCK)) {
            return false;
        }
        if (state1.is(Blocks.SLIME_BLOCK) && state2.is(Blocks.HONEY_BLOCK)) {
            return false;
        }
        return PistonStructureResolver.isSticky(state1) || PistonStructureResolver.isSticky(state2);
    }

    private boolean addBlockLine(BlockPos start, Direction direction) {
        int i;
        BlockState nextState = this.level.getBlockState(start);
        if (nextState.isAir()) {
            return true;
        }
        if (!PistonBaseBlock.isPushable(nextState, this.level, start, this.pushDirection, false, direction)) {
            return true;
        }
        if (start.equals(this.pistonPos)) {
            return true;
        }
        if (this.toPush.contains(start)) {
            return true;
        }
        int blockCount = 1;
        if (blockCount + this.toPush.size() > 12) {
            return false;
        }
        while (PistonStructureResolver.isSticky(nextState)) {
            BlockPos pos = start.relative(this.pushDirection.getOpposite(), blockCount);
            BlockState previousState = nextState;
            nextState = this.level.getBlockState(pos);
            if (nextState.isAir() || !PistonStructureResolver.canStickToEachOther(previousState, nextState) || !PistonBaseBlock.isPushable(nextState, this.level, pos, this.pushDirection, false, this.pushDirection.getOpposite()) || pos.equals(this.pistonPos)) break;
            if (++blockCount + this.toPush.size() <= 12) continue;
            return false;
        }
        int blocksAdded = 0;
        for (i = blockCount - 1; i >= 0; --i) {
            this.toPush.add(start.relative(this.pushDirection.getOpposite(), i));
            ++blocksAdded;
        }
        i = 1;
        while (true) {
            BlockPos pos;
            int collisionPos;
            if ((collisionPos = this.toPush.indexOf(pos = start.relative(this.pushDirection, i))) > -1) {
                this.reorderListAtCollision(blocksAdded, collisionPos);
                for (int j = 0; j <= collisionPos + blocksAdded; ++j) {
                    BlockPos blockPos = this.toPush.get(j);
                    if (!PistonStructureResolver.isSticky(this.level.getBlockState(blockPos)) || this.addBranchingBlocks(blockPos)) continue;
                    return false;
                }
                return true;
            }
            nextState = this.level.getBlockState(pos);
            if (nextState.isAir()) {
                return true;
            }
            if (!PistonBaseBlock.isPushable(nextState, this.level, pos, this.pushDirection, true, this.pushDirection) || pos.equals(this.pistonPos)) {
                return false;
            }
            if (nextState.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(pos);
                return true;
            }
            if (this.toPush.size() >= 12) {
                return false;
            }
            this.toPush.add(pos);
            ++blocksAdded;
            ++i;
        }
    }

    private void reorderListAtCollision(int blocksAdded, int collisionPos) {
        ArrayList head = Lists.newArrayList();
        ArrayList lastLineAdded = Lists.newArrayList();
        ArrayList collisionToLine = Lists.newArrayList();
        head.addAll(this.toPush.subList(0, collisionPos));
        lastLineAdded.addAll(this.toPush.subList(this.toPush.size() - blocksAdded, this.toPush.size()));
        collisionToLine.addAll(this.toPush.subList(collisionPos, this.toPush.size() - blocksAdded));
        this.toPush.clear();
        this.toPush.addAll(head);
        this.toPush.addAll(lastLineAdded);
        this.toPush.addAll(collisionToLine);
    }

    private boolean addBranchingBlocks(BlockPos fromPos) {
        BlockState fromState = this.level.getBlockState(fromPos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos;
            BlockState neighbourState;
            if (direction.getAxis() == this.pushDirection.getAxis() || !PistonStructureResolver.canStickToEachOther(neighbourState = this.level.getBlockState(neighbourPos = fromPos.relative(direction)), fromState) || this.addBlockLine(neighbourPos, direction)) continue;
            return false;
        }
        return true;
    }

    public Direction getPushDirection() {
        return this.pushDirection;
    }

    public List<BlockPos> getToPush() {
        return this.toPush;
    }

    public List<BlockPos> getToDestroy() {
        return this.toDestroy;
    }
}

