/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.navigation;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.ServerDebugSubscribers;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class PathNavigation {
    private static final int MAX_TIME_RECOMPUTE = 20;
    private static final int STUCK_CHECK_INTERVAL = 100;
    private static final float STUCK_THRESHOLD_DISTANCE_FACTOR = 0.25f;
    protected final Mob mob;
    protected final Level level;
    protected @Nullable Path path;
    protected double speedModifier;
    protected int tick;
    protected int lastStuckCheck;
    protected Vec3 lastStuckCheckPos = Vec3.ZERO;
    protected Vec3i timeoutCachedNode = Vec3i.ZERO;
    protected long timeoutTimer;
    protected long lastTimeoutCheck;
    protected double timeoutLimit;
    protected float maxDistanceToWaypoint = 0.5f;
    protected boolean hasDelayedRecomputation;
    protected long timeLastRecompute;
    protected NodeEvaluator nodeEvaluator;
    private @Nullable BlockPos targetPos;
    private int reachRange;
    private float maxVisitedNodesMultiplier = 1.0f;
    private final PathFinder pathFinder;
    private boolean isStuck;
    private float requiredPathLength = 16.0f;

    public PathNavigation(Mob mob, Level level) {
        this.mob = mob;
        this.level = level;
        this.pathFinder = this.createPathFinder(Mth.floor(mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE) * 16.0));
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            ServerDebugSubscribers subscribers = serverLevel.getServer().debugSubscribers();
            this.pathFinder.setCaptureDebug(() -> subscribers.hasAnySubscriberFor(DebugSubscriptions.ENTITY_PATHS));
        }
    }

    public void updatePathfinderMaxVisitedNodes() {
        int maxVisitedNodes = Mth.floor(this.getMaxPathLength() * 16.0f);
        this.pathFinder.setMaxVisitedNodes(maxVisitedNodes);
    }

    public void setRequiredPathLength(float length) {
        this.requiredPathLength = length;
        this.updatePathfinderMaxVisitedNodes();
    }

    private float getMaxPathLength() {
        return Math.max((float)this.mob.getAttributeValue(Attributes.FOLLOW_RANGE), this.requiredPathLength);
    }

    public void resetMaxVisitedNodesMultiplier() {
        this.maxVisitedNodesMultiplier = 1.0f;
    }

    public void setMaxVisitedNodesMultiplier(float maxVisitedNodesMultiplier) {
        this.maxVisitedNodesMultiplier = maxVisitedNodesMultiplier;
    }

    public @Nullable BlockPos getTargetPos() {
        return this.targetPos;
    }

    protected abstract PathFinder createPathFinder(int var1);

    public void setSpeedModifier(double speedModifier) {
        this.speedModifier = speedModifier;
    }

    public void recomputePath() {
        if (this.level.getGameTime() - this.timeLastRecompute > 20L && this.canUpdatePath()) {
            if (this.targetPos != null) {
                this.path = null;
                this.path = this.createPath(this.targetPos, this.reachRange);
                this.timeLastRecompute = this.level.getGameTime();
                this.hasDelayedRecomputation = false;
            }
        } else {
            this.hasDelayedRecomputation = true;
        }
    }

    public final @Nullable Path createPath(double x, double y, double z, int reachRange) {
        return this.createPath(BlockPos.containing(x, y, z), reachRange);
    }

    public @Nullable Path createPath(Stream<BlockPos> positions, int reachRange) {
        return this.createPath(positions.collect(Collectors.toSet()), 8, false, reachRange);
    }

    public @Nullable Path createPath(Set<BlockPos> positions, int reachRange) {
        return this.createPath(positions, 8, false, reachRange);
    }

    public @Nullable Path createPath(BlockPos pos, int reachRange) {
        return this.createPath((Set<BlockPos>)ImmutableSet.of((Object)pos), 8, false, reachRange);
    }

    public @Nullable Path createPath(BlockPos pos, int reachRange, int maxPathLength) {
        return this.createPath((Set<BlockPos>)ImmutableSet.of((Object)pos), 8, false, reachRange, maxPathLength);
    }

    public @Nullable Path createPath(Entity target, int reachRange) {
        return this.createPath((Set<BlockPos>)ImmutableSet.of((Object)target.blockPosition()), 16, true, reachRange);
    }

    protected @Nullable Path createPath(Set<BlockPos> targets, int radiusOffset, boolean above, int reachRange) {
        return this.createPath(targets, radiusOffset, above, reachRange, this.getMaxPathLength());
    }

    protected @Nullable Path createPath(Set<BlockPos> targets, int radiusOffset, boolean above, int reachRange, float maxPathLength) {
        if (targets.isEmpty()) {
            return null;
        }
        if (this.mob.getY() < (double)this.level.getMinY()) {
            return null;
        }
        if (!this.canUpdatePath()) {
            return null;
        }
        if (this.path != null && !this.path.isDone() && targets.contains(this.targetPos)) {
            return this.path;
        }
        ProfilerFiller profiler = Profiler.get();
        profiler.push("pathfind");
        BlockPos fromPos = above ? this.mob.blockPosition().above() : this.mob.blockPosition();
        int radius = (int)(maxPathLength + (float)radiusOffset);
        PathNavigationRegion region = new PathNavigationRegion(this.level, fromPos.offset(-radius, -radius, -radius), fromPos.offset(radius, radius, radius));
        Path path = this.pathFinder.findPath(region, this.mob, targets, maxPathLength, reachRange, this.maxVisitedNodesMultiplier);
        profiler.pop();
        if (path != null && path.getTarget() != null) {
            this.targetPos = path.getTarget();
            this.reachRange = reachRange;
            this.resetStuckTimeout();
        }
        return path;
    }

    public boolean moveTo(double x, double y, double z, double speedModifier) {
        return this.moveTo(this.createPath(x, y, z, 1), speedModifier);
    }

    public boolean moveTo(double x, double y, double z, int reachRange, double speedModifier) {
        return this.moveTo(this.createPath(x, y, z, reachRange), speedModifier);
    }

    public boolean moveTo(Entity target, double speedModifier) {
        Path newPath = this.createPath(target, 1);
        return newPath != null && this.moveTo(newPath, speedModifier);
    }

    public boolean moveTo(@Nullable Path newPath, double speedModifier) {
        if (newPath == null) {
            this.path = null;
            return false;
        }
        if (!newPath.sameAs(this.path)) {
            this.path = newPath;
        }
        if (this.isDone()) {
            return false;
        }
        this.trimPath();
        if (this.path.getNodeCount() <= 0) {
            return false;
        }
        this.speedModifier = speedModifier;
        Vec3 mobPos = this.getTempMobPos();
        this.lastStuckCheck = this.tick;
        this.lastStuckCheckPos = mobPos;
        return true;
    }

    public @Nullable Path getPath() {
        return this.path;
    }

    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }
        if (this.isDone()) {
            return;
        }
        if (this.canUpdatePath()) {
            this.followThePath();
        } else if (this.path != null && !this.path.isDone()) {
            Vec3 mobPos = this.getTempMobPos();
            Vec3 pos = this.path.getNextEntityPos(this.mob);
            if (mobPos.y > pos.y && !this.mob.onGround() && Mth.floor(mobPos.x) == Mth.floor(pos.x) && Mth.floor(mobPos.z) == Mth.floor(pos.z)) {
                this.path.advance();
            }
        }
        if (this.isDone()) {
            return;
        }
        Vec3 target = this.path.getNextEntityPos(this.mob);
        this.mob.getMoveControl().setWantedPosition(target.x, this.getGroundY(target), target.z, this.speedModifier);
    }

    protected double getGroundY(Vec3 target) {
        BlockPos blockPos = BlockPos.containing(target);
        return this.level.getBlockState(blockPos.below()).isAir() ? target.y : WalkNodeEvaluator.getFloorLevel(this.level, blockPos);
    }

    protected void followThePath() {
        boolean isCloseEnoughToCurrentNode;
        Vec3 mobPos = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75f ? this.mob.getBbWidth() / 2.0f : 0.75f - this.mob.getBbWidth() / 2.0f;
        BlockPos currentNodePos = this.path.getNextNodePos();
        double xDistance = Math.abs(this.mob.getX() - ((double)currentNodePos.getX() + 0.5));
        double yDistance = Math.abs(this.mob.getY() - (double)currentNodePos.getY());
        double zDistance = Math.abs(this.mob.getZ() - ((double)currentNodePos.getZ() + 0.5));
        boolean bl = isCloseEnoughToCurrentNode = xDistance < (double)this.maxDistanceToWaypoint && zDistance < (double)this.maxDistanceToWaypoint && yDistance < (double)this.getMaxVerticalDistanceToWaypoint();
        if (isCloseEnoughToCurrentNode || this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(mobPos)) {
            this.path.advance();
        }
        this.doStuckDetection(mobPos);
    }

    private boolean shouldTargetNextNodeInDirection(Vec3 mobPosition) {
        boolean withinCurrentBlock;
        if (this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount()) {
            return false;
        }
        Vec3 currentNode = Vec3.atBottomCenterOf(this.path.getNextNodePos());
        if (!mobPosition.closerThan(currentNode, 2.0)) {
            return false;
        }
        if (this.canMoveDirectly(mobPosition, this.path.getNextEntityPos(this.mob))) {
            return true;
        }
        Vec3 nextNode = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
        Vec3 mobToCurrent = currentNode.subtract(mobPosition);
        Vec3 mobToNext = nextNode.subtract(mobPosition);
        double mobToCurrentSqr = mobToCurrent.lengthSqr();
        double mobToNextSqr = mobToNext.lengthSqr();
        boolean closerToNextThanCurrent = mobToNextSqr < mobToCurrentSqr;
        boolean bl = withinCurrentBlock = mobToCurrentSqr < 0.5;
        if (closerToNextThanCurrent || withinCurrentBlock) {
            Vec3 mobDirection = mobToCurrent.normalize();
            Vec3 pathDirection = mobToNext.normalize();
            return pathDirection.dot(mobDirection) < 0.0;
        }
        return false;
    }

    protected void doStuckDetection(Vec3 mobPos) {
        if (this.tick - this.lastStuckCheck > 100) {
            float effectiveSpeed = this.mob.getSpeed() >= 1.0f ? this.mob.getSpeed() : this.mob.getSpeed() * this.mob.getSpeed();
            float thresholdDistance = effectiveSpeed * 100.0f * 0.25f;
            if (mobPos.distanceToSqr(this.lastStuckCheckPos) < (double)(thresholdDistance * thresholdDistance)) {
                this.isStuck = true;
                this.stop();
            } else {
                this.isStuck = false;
            }
            this.lastStuckCheck = this.tick;
            this.lastStuckCheckPos = mobPos;
        }
        if (this.path != null && !this.path.isDone()) {
            BlockPos pos = this.path.getNextNodePos();
            long time = this.level.getGameTime();
            if (pos.equals(this.timeoutCachedNode)) {
                this.timeoutTimer += time - this.lastTimeoutCheck;
            } else {
                this.timeoutCachedNode = pos;
                double distToNode = mobPos.distanceTo(Vec3.atBottomCenterOf(this.timeoutCachedNode));
                double d = this.timeoutLimit = this.mob.getSpeed() > 0.0f ? distToNode / (double)this.mob.getSpeed() * 20.0 : 0.0;
            }
            if (this.timeoutLimit > 0.0 && (double)this.timeoutTimer > this.timeoutLimit * 3.0) {
                this.timeoutPath();
            }
            this.lastTimeoutCheck = time;
        }
    }

    private void timeoutPath() {
        this.resetStuckTimeout();
        this.stop();
    }

    private void resetStuckTimeout() {
        this.timeoutCachedNode = Vec3i.ZERO;
        this.timeoutTimer = 0L;
        this.timeoutLimit = 0.0;
        this.isStuck = false;
    }

    public boolean isDone() {
        return this.path == null || this.path.isDone();
    }

    public boolean isInProgress() {
        return !this.isDone();
    }

    public void stop() {
        this.path = null;
    }

    protected abstract Vec3 getTempMobPos();

    protected abstract boolean canUpdatePath();

    protected void trimPath() {
        if (this.path == null) {
            return;
        }
        for (int i = 0; i < this.path.getNodeCount(); ++i) {
            Node node = this.path.getNode(i);
            Node nextNode = i + 1 < this.path.getNodeCount() ? this.path.getNode(i + 1) : null;
            BlockState state = this.level.getBlockState(new BlockPos(node.x, node.y, node.z));
            if (!state.is(BlockTags.CAULDRONS)) continue;
            this.path.replaceNode(i, node.cloneAndMove(node.x, node.y + 1, node.z));
            if (nextNode == null || node.y < nextNode.y) continue;
            this.path.replaceNode(i + 1, node.cloneAndMove(nextNode.x, node.y + 1, nextNode.z));
        }
    }

    protected boolean canMoveDirectly(Vec3 startPos, Vec3 stopPos) {
        return false;
    }

    public boolean canCutCorner(PathType pathType) {
        return pathType != PathType.FIRE_IN_NEIGHBOR && pathType != PathType.DAMAGING_IN_NEIGHBOR && pathType != PathType.WALKABLE_DOOR;
    }

    protected static boolean isClearForMovementBetween(Mob mob, Vec3 startPos, Vec3 stopPos, boolean blockedByFluids) {
        Vec3 to = new Vec3(stopPos.x, stopPos.y + (double)mob.getBbHeight() * 0.5, stopPos.z);
        return mob.level().clip(new ClipContext(startPos, to, ClipContext.Block.COLLIDER, blockedByFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS;
    }

    public boolean isStableDestination(BlockPos pos) {
        BlockPos below = pos.below();
        return this.level.getBlockState(below).isSolidRender();
    }

    public NodeEvaluator getNodeEvaluator() {
        return this.nodeEvaluator;
    }

    public void setCanFloat(boolean canFloat) {
        this.nodeEvaluator.setCanFloat(canFloat);
    }

    public boolean canFloat() {
        return this.nodeEvaluator.canFloat();
    }

    public boolean shouldRecomputePath(BlockPos pos) {
        if (this.hasDelayedRecomputation) {
            return false;
        }
        if (this.path == null || this.path.isDone() || this.path.getNodeCount() == 0) {
            return false;
        }
        Node target = this.path.getEndNode();
        Vec3 middlePos = new Vec3(((double)target.x + this.mob.getX()) / 2.0, ((double)target.y + this.mob.getY()) / 2.0, ((double)target.z + this.mob.getZ()) / 2.0);
        return pos.closerToCenterThan(middlePos, this.path.getNodeCount() - this.path.getNextNodeIndex());
    }

    public float getMaxDistanceToWaypoint() {
        return this.maxDistanceToWaypoint;
    }

    public float getMaxVerticalDistanceToWaypoint() {
        return 1.0f;
    }

    public boolean isStuck() {
        return this.isStuck;
    }

    public abstract boolean canNavigateGround();

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.nodeEvaluator.setCanOpenDoors(canOpenDoors);
    }
}

