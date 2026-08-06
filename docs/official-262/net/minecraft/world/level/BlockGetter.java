/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface BlockGetter
extends LevelHeightAccessor {
    public @Nullable BlockEntity getBlockEntity(BlockPos var1);

    default public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        BlockEntity blockEntity = this.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.getType() != type) {
            return Optional.empty();
        }
        return Optional.of(blockEntity);
    }

    public BlockState getBlockState(BlockPos var1);

    public FluidState getFluidState(BlockPos var1);

    default public int getLightEmission(BlockPos pos) {
        return this.getBlockState(pos).getLightEmission();
    }

    default public Stream<BlockState> getBlockStates(AABB box) {
        return BlockPos.betweenClosedStream(box).map(this::getBlockState);
    }

    default public BlockHitResult isBlockInLine(ClipBlockStateContext c) {
        return BlockGetter.traverseBlocks(c.getFrom(), c.getTo(), c, (context, pos) -> {
            BlockState blockState = this.getBlockState((BlockPos)pos);
            Vec3 delta = context.getFrom().subtract(context.getTo());
            return context.isTargetBlock().test(blockState) ? new BlockHitResult(context.getTo(), Direction.getApproximateNearest(delta.x, delta.y, delta.z), BlockPos.containing(context.getTo()), false) : null;
        }, context -> {
            Vec3 delta = context.getFrom().subtract(context.getTo());
            return BlockHitResult.miss(context.getTo(), Direction.getApproximateNearest(delta.x, delta.y, delta.z), BlockPos.containing(context.getTo()));
        });
    }

    default public BlockHitResult clip(ClipContext c) {
        return BlockGetter.traverseBlocks(c.getFrom(), c.getTo(), c, (context, pos) -> {
            BlockState blockState = this.getBlockState((BlockPos)pos);
            FluidState fluidState = this.getFluidState((BlockPos)pos);
            Vec3 from = context.getFrom();
            Vec3 to = context.getTo();
            VoxelShape blockShape = context.getBlockShape(blockState, this, (BlockPos)pos);
            BlockHitResult blockResult = this.clipWithInteractionOverride(from, to, (BlockPos)pos, blockShape, blockState);
            VoxelShape fluidShape = context.getFluidShape(fluidState, this, (BlockPos)pos);
            BlockHitResult liquidResult = fluidShape.clip(from, to, (BlockPos)pos);
            double blockDistanceSquared = blockResult == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(blockResult.getLocation());
            double liquidDistanceSquared = liquidResult == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(liquidResult.getLocation());
            return blockDistanceSquared <= liquidDistanceSquared ? blockResult : liquidResult;
        }, context -> {
            Vec3 delta = context.getFrom().subtract(context.getTo());
            return BlockHitResult.miss(context.getTo(), Direction.getApproximateNearest(delta.x, delta.y, delta.z), BlockPos.containing(context.getTo()));
        });
    }

    default public @Nullable BlockHitResult clipWithInteractionOverride(Vec3 from, Vec3 to, BlockPos pos, VoxelShape blockShape, BlockState blockState) {
        BlockHitResult hitOverride;
        BlockHitResult result = blockShape.clip(from, to, pos);
        if (result != null && (hitOverride = blockState.getInteractionShape(this, pos).clip(from, to, pos)) != null && hitOverride.getLocation().subtract(from).lengthSqr() < result.getLocation().subtract(from).lengthSqr()) {
            return result.withDirection(hitOverride.getDirection());
        }
        return result;
    }

    default public double getBlockFloorHeight(VoxelShape blockShape, Supplier<VoxelShape> belowBlockShape) {
        if (!blockShape.isEmpty()) {
            return blockShape.max(Direction.Axis.Y);
        }
        double belowFloor = belowBlockShape.get().max(Direction.Axis.Y);
        if (belowFloor >= 1.0) {
            return belowFloor - 1.0;
        }
        return Double.NEGATIVE_INFINITY;
    }

    default public double getBlockFloorHeight(BlockPos pos) {
        return this.getBlockFloorHeight(this.getBlockState(pos).getCollisionShape(this, pos), () -> {
            BlockPos below = pos.below();
            return this.getBlockState(below).getCollisionShape(this, below);
        });
    }

    public static <T, C> T traverseBlocks(Vec3 from, Vec3 to, C context, BiFunction<C, BlockPos, @Nullable T> consumer, Function<C, T> missFactory) {
        int currentBlockZ;
        int currentBlockY;
        if (from.equals(to)) {
            return missFactory.apply(context);
        }
        double toX = Mth.lerp(-1.0E-7, to.x, from.x);
        double toY = Mth.lerp(-1.0E-7, to.y, from.y);
        double toZ = Mth.lerp(-1.0E-7, to.z, from.z);
        double fromX = Mth.lerp(-1.0E-7, from.x, to.x);
        double fromY = Mth.lerp(-1.0E-7, from.y, to.y);
        double fromZ = Mth.lerp(-1.0E-7, from.z, to.z);
        int currentBlockX = Mth.floor(fromX);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(currentBlockX, currentBlockY = Mth.floor(fromY), currentBlockZ = Mth.floor(fromZ));
        T first = consumer.apply(context, pos);
        if (first != null) {
            return first;
        }
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        int signX = Mth.sign(dx);
        int signY = Mth.sign(dy);
        int signZ = Mth.sign(dz);
        double tDeltaX = signX == 0 ? Double.MAX_VALUE : (double)signX / dx;
        double tDeltaY = signY == 0 ? Double.MAX_VALUE : (double)signY / dy;
        double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : (double)signZ / dz;
        double tX = tDeltaX * (signX > 0 ? 1.0 - Mth.frac(fromX) : Mth.frac(fromX));
        double tY = tDeltaY * (signY > 0 ? 1.0 - Mth.frac(fromY) : Mth.frac(fromY));
        double tZ = tDeltaZ * (signZ > 0 ? 1.0 - Mth.frac(fromZ) : Mth.frac(fromZ));
        while (tX <= 1.0 || tY <= 1.0 || tZ <= 1.0) {
            T result;
            if (tX < tY) {
                if (tX < tZ) {
                    currentBlockX += signX;
                    tX += tDeltaX;
                } else {
                    currentBlockZ += signZ;
                    tZ += tDeltaZ;
                }
            } else if (tY < tZ) {
                currentBlockY += signY;
                tY += tDeltaY;
            } else {
                currentBlockZ += signZ;
                tZ += tDeltaZ;
            }
            if ((result = consumer.apply(context, pos.set(currentBlockX, currentBlockY, currentBlockZ))) == null) continue;
            return result;
        }
        return missFactory.apply(context);
    }

    public static boolean forEachBlockIntersectedBetween(Vec3 from, Vec3 to, AABB aabbAtTarget, BlockStepVisitor visitor) {
        Vec3 travel = to.subtract(from);
        if (travel.lengthSqr() < (double)Mth.square(1.0E-5f)) {
            for (BlockPos blockPos : BlockPos.betweenClosed(aabbAtTarget)) {
                if (visitor.visit(blockPos, 0)) continue;
                return false;
            }
            return true;
        }
        LongOpenHashSet visitedBlocks = new LongOpenHashSet();
        for (BlockPos blockPos : BlockPos.betweenCornersInDirection(aabbAtTarget.move(travel.scale(-1.0)), travel)) {
            if (!visitor.visit(blockPos, 0)) {
                return false;
            }
            visitedBlocks.add(blockPos.asLong());
        }
        int iterations = BlockGetter.addCollisionsAlongTravel((LongSet)visitedBlocks, travel, aabbAtTarget, visitor);
        if (iterations < 0) {
            return false;
        }
        for (BlockPos blockPos : BlockPos.betweenCornersInDirection(aabbAtTarget, travel)) {
            if (!visitedBlocks.add(blockPos.asLong()) || visitor.visit(blockPos, iterations + 1)) continue;
            return false;
        }
        return true;
    }

    private static int addCollisionsAlongTravel(LongSet visitedBlocks, Vec3 deltaMove, AABB aabbAtTarget, BlockStepVisitor visitor) {
        double boxSizeX = aabbAtTarget.getXsize();
        double boxSizeY = aabbAtTarget.getYsize();
        double boxSizeZ = aabbAtTarget.getZsize();
        Vec3i cornerDir = BlockGetter.getFurthestCorner(deltaMove);
        Vec3 toCenter = aabbAtTarget.getCenter();
        Vec3 toCorner = new Vec3(toCenter.x() + boxSizeX * 0.5 * (double)cornerDir.getX(), toCenter.y() + boxSizeY * 0.5 * (double)cornerDir.getY(), toCenter.z() + boxSizeZ * 0.5 * (double)cornerDir.getZ());
        Vec3 fromCorner = toCorner.subtract(deltaMove);
        int cornerVisitedBlockX = Mth.floor(fromCorner.x);
        int cornerVisitedBlockY = Mth.floor(fromCorner.y);
        int cornerVisitedBlockZ = Mth.floor(fromCorner.z);
        int signX = Mth.sign(deltaMove.x);
        int signY = Mth.sign(deltaMove.y);
        int signZ = Mth.sign(deltaMove.z);
        double tDeltaX = signX == 0 ? Double.MAX_VALUE : (double)signX / deltaMove.x;
        double tDeltaY = signY == 0 ? Double.MAX_VALUE : (double)signY / deltaMove.y;
        double tDeltaZ = signZ == 0 ? Double.MAX_VALUE : (double)signZ / deltaMove.z;
        double tX = tDeltaX * (signX > 0 ? 1.0 - Mth.frac(fromCorner.x) : Mth.frac(fromCorner.x));
        double tY = tDeltaY * (signY > 0 ? 1.0 - Mth.frac(fromCorner.y) : Mth.frac(fromCorner.y));
        double tZ = tDeltaZ * (signZ > 0 ? 1.0 - Mth.frac(fromCorner.z) : Mth.frac(fromCorner.z));
        int iterations = 0;
        while (tX <= 1.0 || tY <= 1.0 || tZ <= 1.0) {
            if (tX < tY) {
                if (tX < tZ) {
                    cornerVisitedBlockX += signX;
                    tX += tDeltaX;
                } else {
                    cornerVisitedBlockZ += signZ;
                    tZ += tDeltaZ;
                }
            } else if (tY < tZ) {
                cornerVisitedBlockY += signY;
                tY += tDeltaY;
            } else {
                cornerVisitedBlockZ += signZ;
                tZ += tDeltaZ;
            }
            Optional<Vec3> hitPointOpt = AABB.clip(cornerVisitedBlockX, cornerVisitedBlockY, cornerVisitedBlockZ, cornerVisitedBlockX + 1, cornerVisitedBlockY + 1, cornerVisitedBlockZ + 1, fromCorner, toCorner);
            if (hitPointOpt.isEmpty()) continue;
            Vec3 hitPoint = hitPointOpt.get();
            double cornerHitX = Mth.clamp(hitPoint.x, (double)cornerVisitedBlockX + (double)1.0E-5f, (double)cornerVisitedBlockX + 1.0 - (double)1.0E-5f);
            double cornerHitY = Mth.clamp(hitPoint.y, (double)cornerVisitedBlockY + (double)1.0E-5f, (double)cornerVisitedBlockY + 1.0 - (double)1.0E-5f);
            double cornerHitZ = Mth.clamp(hitPoint.z, (double)cornerVisitedBlockZ + (double)1.0E-5f, (double)cornerVisitedBlockZ + 1.0 - (double)1.0E-5f);
            int oppositeCornerX = Mth.floor(cornerHitX - boxSizeX * (double)cornerDir.getX());
            int oppositeCornerY = Mth.floor(cornerHitY - boxSizeY * (double)cornerDir.getY());
            int oppositeCornerZ = Mth.floor(cornerHitZ - boxSizeZ * (double)cornerDir.getZ());
            int currentIteration = ++iterations;
            for (BlockPos pos : BlockPos.betweenCornersInDirection(cornerVisitedBlockX, cornerVisitedBlockY, cornerVisitedBlockZ, oppositeCornerX, oppositeCornerY, oppositeCornerZ, deltaMove)) {
                if (!visitedBlocks.add(pos.asLong()) || visitor.visit(pos, currentIteration)) continue;
                return -1;
            }
        }
        return iterations;
    }

    private static Vec3i getFurthestCorner(Vec3 direction) {
        int zSign;
        double xDot = Math.abs(Vec3.X_AXIS.dot(direction));
        double yDot = Math.abs(Vec3.Y_AXIS.dot(direction));
        double zDot = Math.abs(Vec3.Z_AXIS.dot(direction));
        int xSign = direction.x >= 0.0 ? 1 : -1;
        int ySign = direction.y >= 0.0 ? 1 : -1;
        int n = zSign = direction.z >= 0.0 ? 1 : -1;
        if (xDot <= yDot && xDot <= zDot) {
            return new Vec3i(-xSign, -zSign, ySign);
        }
        if (yDot <= zDot) {
            return new Vec3i(zSign, -ySign, -xSign);
        }
        return new Vec3i(-ySign, xSign, -zSign);
    }

    @FunctionalInterface
    public static interface BlockStepVisitor {
        public boolean visit(BlockPos var1, int var2);
    }
}

