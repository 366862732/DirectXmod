/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation
extends PathNavigation {
    private boolean avoidSun;
    private boolean canPathToTargetsBelowSurface;

    public GroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
    }

    @Override
    public Path createPath(BlockPos pos, int reachRange) {
        LevelChunk chunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null) {
            return null;
        }
        if (!this.canPathToTargetsBelowSurface) {
            pos = this.findSurfacePosition(chunk, pos, reachRange);
        }
        return super.createPath(pos, reachRange);
    }

    private BlockPos findSurfacePosition(LevelChunk chunk, BlockPos pos, int reachRange) {
        BlockPos.MutableBlockPos columnPos;
        if (chunk.getBlockState(pos).isAir()) {
            columnPos = pos.mutable().move(Direction.DOWN);
            while (columnPos.getY() >= this.level.getMinY() && chunk.getBlockState(columnPos).isAir()) {
                columnPos.move(Direction.DOWN);
            }
            if (columnPos.getY() >= this.level.getMinY()) {
                return columnPos.above();
            }
            columnPos.setY(pos.getY() + 1);
            while (columnPos.getY() <= this.level.getMaxY() && chunk.getBlockState(columnPos).isAir()) {
                columnPos.move(Direction.UP);
            }
            pos = columnPos;
        }
        if (chunk.getBlockState(pos).isSolid()) {
            columnPos = pos.mutable().move(Direction.UP);
            while (columnPos.getY() <= this.level.getMaxY() && chunk.getBlockState(columnPos).isSolid()) {
                columnPos.move(Direction.UP);
            }
            return columnPos.immutable();
        }
        return pos;
    }

    @Override
    public Path createPath(Entity target, int reachRange) {
        return this.createPath(target.blockPosition(), reachRange);
    }

    private int getSurfaceY() {
        if (!this.mob.isInWater() || !this.canFloat()) {
            return Mth.floor(this.mob.getY() + 0.5);
        }
        int surface = this.mob.getBlockY();
        BlockState state = this.level.getBlockState(BlockPos.containing(this.mob.getX(), surface, this.mob.getZ()));
        int steps = 0;
        while (state.is(Blocks.WATER)) {
            state = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++surface, this.mob.getZ()));
            if (++steps <= 16) continue;
            return this.mob.getBlockY();
        }
        return surface;
    }

    @Override
    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
                return;
            }
            for (int i = 0; i < this.path.getNodeCount(); ++i) {
                Node node = this.path.getNode(i);
                if (!this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) continue;
                this.path.truncateNodes(i);
                return;
            }
        }
    }

    @Override
    public boolean canNavigateGround() {
        return true;
    }

    public void setAvoidSun(boolean avoidSun) {
        this.avoidSun = avoidSun;
    }

    public void setCanWalkOverFences(boolean canWalkOverFences) {
        this.nodeEvaluator.setCanWalkOverFences(canWalkOverFences);
    }

    public void setCanPathToTargetsBelowSurface(boolean canPathToTargetsBelowSurface) {
        this.canPathToTargetsBelowSurface = canPathToTargetsBelowSurface;
    }
}

