/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface CollisionGetter
extends BlockGetter {
    public WorldBorder getWorldBorder();

    public @Nullable BlockGetter getChunkForCollisions(int var1, int var2);

    default public boolean isUnobstructed(@Nullable Entity source, VoxelShape shape) {
        return true;
    }

    default public boolean isUnobstructed(BlockState state, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getCollisionShape(this, pos, context);
        return shape.isEmpty() || this.isUnobstructed(null, shape.move(pos));
    }

    default public boolean isUnobstructed(Entity ignore) {
        return this.isUnobstructed(ignore, Shapes.create(ignore.getBoundingBox()));
    }

    default public boolean noCollision(AABB aabb) {
        return this.noCollision(null, aabb);
    }

    default public boolean noCollision(Entity source) {
        return this.noCollision(source, source.getBoundingBox());
    }

    default public boolean noCollision(@Nullable Entity entity, AABB aabb) {
        return this.noCollision(entity, aabb, false);
    }

    default public boolean noCollision(@Nullable Entity entity, AABB aabb, boolean alwaysCollideWithFluids) {
        return this.noBlockCollision(entity, aabb, alwaysCollideWithFluids) && this.noEntityCollision(entity, aabb) && this.noBorderCollision(entity, aabb);
    }

    default public boolean noBlockCollision(@Nullable Entity entity, AABB aabb) {
        return this.noBlockCollision(entity, aabb, false);
    }

    default public boolean noBlockCollision(@Nullable Entity entity, AABB aabb, boolean alwaysCollideWithFluids) {
        Iterable<VoxelShape> blockCollisions = alwaysCollideWithFluids ? this.getBlockAndLiquidCollisions(entity, aabb) : this.getBlockCollisions(entity, aabb);
        for (VoxelShape blockCollision : blockCollisions) {
            if (blockCollision.isEmpty()) continue;
            return false;
        }
        return true;
    }

    default public boolean noEntityCollision(@Nullable Entity entity, AABB aabb) {
        return this.getEntityCollisions(entity, aabb).isEmpty();
    }

    default public boolean noBorderCollision(@Nullable Entity entity, AABB aabb) {
        if (entity != null) {
            VoxelShape borderShape = this.borderCollision(entity, aabb);
            return borderShape == null || !Shapes.joinIsNotEmpty(borderShape, Shapes.create(aabb), BooleanOp.AND);
        }
        return true;
    }

    public List<VoxelShape> getEntityCollisions(@Nullable Entity var1, AABB var2);

    default public Iterable<VoxelShape> getCollisions(@Nullable Entity source, AABB box) {
        List<VoxelShape> entityCollisions = this.getEntityCollisions(source, box);
        Iterable blockCollisions = this.getBlockCollisions(source, box);
        return entityCollisions.isEmpty() ? blockCollisions : Iterables.concat(entityCollisions, blockCollisions);
    }

    default public Iterable<VoxelShape> getPreMoveCollisions(@Nullable Entity source, AABB box, Vec3 oldPos) {
        List<VoxelShape> entityCollisions = this.getEntityCollisions(source, box);
        Iterable blockCollisions = this.getBlockCollisionsFromContext(CollisionContext.withPosition(source, oldPos.y), box);
        return entityCollisions.isEmpty() ? blockCollisions : Iterables.concat(entityCollisions, blockCollisions);
    }

    default public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity source, AABB box) {
        return this.getBlockCollisionsFromContext(source == null ? CollisionContext.empty() : CollisionContext.of(source), box);
    }

    default public Iterable<VoxelShape> getBlockAndLiquidCollisions(@Nullable Entity source, AABB box) {
        return this.getBlockCollisionsFromContext(source == null ? CollisionContext.emptyWithFluidCollisions() : CollisionContext.of(source, true), box);
    }

    default public Iterable<VoxelShape> getBlockCollisionsFromContext(CollisionContext source, AABB box) {
        return () -> new BlockCollisions<VoxelShape>(this, source, box, false, (p, shape) -> shape);
    }

    private @Nullable VoxelShape borderCollision(Entity source, AABB box) {
        WorldBorder worldBorder = this.getWorldBorder();
        return worldBorder.isInsideCloseToBorder(source, box) ? worldBorder.getCollisionShape() : null;
    }

    default public BlockHitResult clipIncludingBorder(ClipContext c) {
        BlockHitResult hitResult = this.clip(c);
        WorldBorder worldBorder = this.getWorldBorder();
        if (worldBorder.isWithinBounds(c.getFrom()) && !worldBorder.isWithinBounds(hitResult.getLocation())) {
            Vec3 delta = hitResult.getLocation().subtract(c.getFrom());
            Direction deltaDirection = Direction.getApproximateNearest(delta.x, delta.y, delta.z);
            Vec3 hit = worldBorder.clampVec3ToBound(hitResult.getLocation());
            return new BlockHitResult(hit, deltaDirection, BlockPos.containing(hit), false, true);
        }
        return hitResult;
    }

    default public boolean collidesWithSuffocatingBlock(@Nullable Entity source, AABB box) {
        BlockCollisions<VoxelShape> blockCollisions = new BlockCollisions<VoxelShape>(this, source, box, true, (p, shape) -> shape);
        while (blockCollisions.hasNext()) {
            if (((VoxelShape)blockCollisions.next()).isEmpty()) continue;
            return true;
        }
        return false;
    }

    default public Optional<BlockPos> findSupportingBlock(Entity source, AABB box) {
        BlockPos mainSupport = null;
        double mainSupportDistance = Double.MAX_VALUE;
        BlockCollisions<BlockPos> blockCollisions = new BlockCollisions<BlockPos>(this, source, box, false, (pos, shape) -> pos);
        while (blockCollisions.hasNext()) {
            BlockPos pos2 = (BlockPos)blockCollisions.next();
            double distance = pos2.distToCenterSqr(source.position());
            if (!(distance < mainSupportDistance) && (distance != mainSupportDistance || mainSupport != null && mainSupport.compareTo(pos2) >= 0)) continue;
            mainSupport = pos2.immutable();
            mainSupportDistance = distance;
        }
        return Optional.ofNullable(mainSupport);
    }

    default public Optional<Vec3> findFreePosition(@Nullable Entity source, VoxelShape allowedCenters, Vec3 preferredCenter, double sizeX, double sizeY, double sizeZ) {
        if (allowedCenters.isEmpty()) {
            return Optional.empty();
        }
        AABB searchArea = allowedCenters.bounds().inflate(sizeX, sizeY, sizeZ);
        VoxelShape expandedCollisions = StreamSupport.stream(this.getBlockCollisions(source, searchArea).spliterator(), false).filter(shape -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(shape.bounds())).flatMap(shape -> shape.toAabbs().stream()).map(aabb -> aabb.inflate(sizeX / 2.0, sizeY / 2.0, sizeZ / 2.0)).map(Shapes::create).reduce(Shapes.empty(), Shapes::or);
        VoxelShape freeSpots = Shapes.join(allowedCenters, expandedCollisions, BooleanOp.ONLY_FIRST);
        return freeSpots.closestPointTo(preferredCenter);
    }
}

