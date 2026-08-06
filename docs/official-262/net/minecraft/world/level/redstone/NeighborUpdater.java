/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.redstone;

import java.util.Locale;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public interface NeighborUpdater {
    public static final Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

    public void shapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, @Block.UpdateFlags int var5, int var6);

    public void neighborChanged(BlockPos var1, Block var2, @Nullable Orientation var3);

    public void neighborChanged(BlockState var1, BlockPos var2, Block var3, @Nullable Orientation var4, boolean var5);

    default public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block block, @Nullable Direction skipDirection, @Nullable Orientation orientation) {
        for (Direction direction : UPDATE_ORDER) {
            if (direction == skipDirection) continue;
            this.neighborChanged(pos.relative(direction), block, null);
        }
    }

    public static void executeShapeUpdate(LevelAccessor level, Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, @Block.UpdateFlags int updateFlags, int updateLimit) {
        BlockState currentState = level.getBlockState(pos);
        if ((updateFlags & 0x80) != 0 && currentState.is(Blocks.REDSTONE_WIRE)) {
            return;
        }
        try {
            BlockState newState = currentState.updateShape(level, level, pos, direction, neighborPos, neighborState, level.getRandom());
            Block.updateOrDestroy(currentState, newState, level, pos, updateFlags, updateLimit);
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exception while updating neighbour shapes");
            CrashReportCategory ownCategory = report.addCategory("Block being updated");
            CrashReportCategory.populateBlockDetails(ownCategory, level, pos, currentState);
            CrashReportCategory neighborCategory = report.addCategory("Neighbor block");
            CrashReportCategory.populateBlockDetails(neighborCategory, level, neighborPos, neighborState);
            throw new ReportedException(report);
        }
    }

    public static void executeUpdate(Level level, BlockState state, BlockPos pos, Block changedBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        try {
            state.handleNeighborChanged(level, pos, changedBlock, orientation, movedByPiston);
        }
        catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exception while updating neighbours");
            CrashReportCategory category = report.addCategory("Block being updated");
            category.setDetail("Source block type", () -> {
                try {
                    return String.format(Locale.ROOT, "ID #%s (%s // %s)", BuiltInRegistries.BLOCK.getKey(changedBlock), changedBlock.getDescriptionId(), changedBlock.getClass().getCanonicalName());
                }
                catch (Throwable ignored) {
                    return "ID #" + String.valueOf(BuiltInRegistries.BLOCK.getKey(changedBlock));
                }
            });
            CrashReportCategory.populateBlockDetails(category, level, pos, state);
            throw new ReportedException(report);
        }
    }
}

