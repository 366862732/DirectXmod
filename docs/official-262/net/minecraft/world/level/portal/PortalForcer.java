/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.portal;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class PortalForcer {
    public static final int TICKET_RADIUS = 3;
    private static final int NETHER_PORTAL_RADIUS = 16;
    private static final int OVERWORLD_PORTAL_RADIUS = 128;
    private static final int FRAME_HEIGHT = 5;
    private static final int FRAME_WIDTH = 4;
    private static final int FRAME_BOX = 3;
    private static final int FRAME_HEIGHT_START = -1;
    private static final int FRAME_HEIGHT_END = 4;
    private static final int FRAME_WIDTH_START = -1;
    private static final int FRAME_WIDTH_END = 3;
    private static final int FRAME_BOX_START = -1;
    private static final int FRAME_BOX_END = 2;
    private static final int NOTHING_FOUND = -1;
    private final ServerLevel level;

    public PortalForcer(ServerLevel level) {
        this.level = level;
    }

    public Optional<BlockPos> findClosestPortalPosition(BlockPos approximateExitPos, boolean toNether, WorldBorder worldBorder) {
        PoiManager poiManager = this.level.getPoiManager();
        int radius = toNether ? 16 : 128;
        poiManager.ensureLoadedAndValid(this.level, approximateExitPos, radius);
        return poiManager.getInSquare(type -> type.is(PoiTypes.NETHER_PORTAL), approximateExitPos, radius, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).filter(worldBorder::isWithinBounds).filter(pos -> this.level.getBlockState((BlockPos)pos).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)).min(Comparator.comparingDouble(p -> p.distSqr(approximateExitPos)).thenComparingInt(Vec3i::getY));
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos origin, Direction.Axis portalAxis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, portalAxis);
        double closestFullDistanceSqr = -1.0;
        BlockPos closestFullPosition = null;
        double closestPartialDistanceSqr = -1.0;
        BlockPos closestPartialPosition = null;
        WorldBorder worldBorder = this.level.getWorldBorder();
        int maxPlaceableY = Math.min(this.level.getMaxY(), this.level.getMinY() + this.level.getLogicalHeight() - 1);
        boolean edgeDistance = true;
        BlockPos.MutableBlockPos mutable = origin.mutable();
        for (BlockPos.MutableBlockPos columnPos : BlockPos.spiralAround(origin, 16, Direction.EAST, Direction.SOUTH)) {
            int height = Math.min(maxPlaceableY, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnPos.getX(), columnPos.getZ()));
            if (!worldBorder.isWithinBounds(columnPos) || !worldBorder.isWithinBounds(columnPos.move(direction, 1))) continue;
            columnPos.move(direction.getOpposite(), 1);
            for (int y = height; y >= this.level.getMinY(); --y) {
                int deltaY;
                columnPos.setY(y);
                if (!this.canPortalReplaceBlock(columnPos)) continue;
                int firstEmptyY = y;
                while (y > this.level.getMinY() && this.canPortalReplaceBlock(columnPos.move(Direction.DOWN))) {
                    --y;
                }
                if (y + 4 > maxPlaceableY || (deltaY = firstEmptyY - y) > 0 && deltaY < 3) continue;
                columnPos.setY(y);
                if (!this.canHostFrame(columnPos, mutable, direction, 0)) continue;
                double distance = origin.distSqr(columnPos);
                if (this.canHostFrame(columnPos, mutable, direction, -1) && this.canHostFrame(columnPos, mutable, direction, 1) && (closestFullDistanceSqr == -1.0 || closestFullDistanceSqr > distance)) {
                    closestFullDistanceSqr = distance;
                    closestFullPosition = columnPos.immutable();
                }
                if (closestFullDistanceSqr != -1.0 || closestPartialDistanceSqr != -1.0 && !(closestPartialDistanceSqr > distance)) continue;
                closestPartialDistanceSqr = distance;
                closestPartialPosition = columnPos.immutable();
            }
        }
        if (closestFullDistanceSqr == -1.0 && closestPartialDistanceSqr != -1.0) {
            closestFullPosition = closestPartialPosition;
            closestFullDistanceSqr = closestPartialDistanceSqr;
        }
        if (closestFullDistanceSqr == -1.0) {
            int maxStartY = maxPlaceableY - 9;
            int minStartY = Math.max(this.level.getMinY() - -1, 70);
            if (maxStartY < minStartY) {
                return Optional.empty();
            }
            closestFullPosition = new BlockPos(origin.getX() - direction.getStepX() * 1, Mth.clamp(origin.getY(), minStartY, maxStartY), origin.getZ() - direction.getStepZ() * 1).immutable();
            closestFullPosition = worldBorder.clampToBounds(closestFullPosition);
            Direction clockWise = direction.getClockWise();
            for (int box = -1; box < 2; ++box) {
                for (int width = 0; width < 2; ++width) {
                    for (int height = -1; height < 3; ++height) {
                        BlockState blockState = height < 0 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        mutable.setWithOffset(closestFullPosition, width * direction.getStepX() + box * clockWise.getStepX(), height, width * direction.getStepZ() + box * clockWise.getStepZ());
                        this.level.setBlockAndUpdate(mutable, blockState);
                    }
                }
            }
        }
        for (int width = -1; width < 3; ++width) {
            for (int height = -1; height < 4; ++height) {
                if (width != -1 && width != 2 && height != -1 && height != 3) continue;
                mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
                this.level.setBlock(mutable, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
        }
        BlockState portalBlockState = (BlockState)Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, portalAxis);
        for (int width = 0; width < 2; ++width) {
            for (int height = 0; height < 3; ++height) {
                mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), height, width * direction.getStepZ());
                this.level.setBlock(mutable, portalBlockState, 18);
            }
        }
        return Optional.of(new BlockUtil.FoundRectangle(closestFullPosition.immutable(), 2, 3));
    }

    private boolean canPortalReplaceBlock(BlockPos.MutableBlockPos pos) {
        BlockState blockState = this.level.getBlockState(pos);
        return blockState.canBeReplaced() && blockState.getFluidState().isEmpty();
    }

    private boolean canHostFrame(BlockPos origin, BlockPos.MutableBlockPos mutable, Direction direction, int offset) {
        Direction clockWise = direction.getClockWise();
        for (int width = -1; width < 3; ++width) {
            for (int height = -1; height < 4; ++height) {
                mutable.setWithOffset(origin, direction.getStepX() * width + clockWise.getStepX() * offset, height, direction.getStepZ() * width + clockWise.getStepZ() * offset);
                if (height < 0 && !this.level.getBlockState(mutable).isSolid()) {
                    return false;
                }
                if (height < 0 || this.canPortalReplaceBlock(mutable)) continue;
                return false;
            }
        }
        return true;
    }
}

