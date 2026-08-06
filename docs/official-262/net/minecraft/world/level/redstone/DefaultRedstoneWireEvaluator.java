/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.redstone;

import com.google.common.collect.Sets;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.jspecify.annotations.Nullable;

public class DefaultRedstoneWireEvaluator
extends RedstoneWireEvaluator {
    public DefaultRedstoneWireEvaluator(RedStoneWireBlock wireBlock) {
        super(wireBlock);
    }

    @Override
    public void updatePowerStrength(Level level, BlockPos pos, BlockState state, @Nullable Orientation orientation, boolean skipShapeUpdates) {
        int targetStrength = this.calculateTargetStrength(level, pos);
        if (state.getValue(RedStoneWireBlock.POWER) != targetStrength) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, (BlockState)state.setValue(RedStoneWireBlock.POWER, targetStrength), 2);
            }
            HashSet toUpdate = Sets.newHashSet();
            toUpdate.add(pos);
            for (Direction direction : Direction.values()) {
                toUpdate.add(pos.relative(direction));
            }
            for (BlockPos blockPos : toUpdate) {
                level.updateNeighborsAt(blockPos, this.wireBlock);
            }
        }
    }

    private int calculateTargetStrength(Level level, BlockPos pos) {
        int blockSignal = this.getBlockSignal(level, pos);
        if (blockSignal == 15) {
            return blockSignal;
        }
        return Math.max(blockSignal, this.getIncomingWireSignal(level, pos));
    }
}

