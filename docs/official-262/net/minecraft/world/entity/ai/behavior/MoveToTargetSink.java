/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MoveToTargetSink
extends Behavior<Mob> {
    private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
    private int remainingCooldown;
    private @Nullable Path path;
    private @Nullable BlockPos lastTargetPos;
    private float speedModifier;

    public MoveToTargetSink() {
        this(150, 250);
    }

    public MoveToTargetSink(int minTimeout, int maxTimeout) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.PATH, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT)), minTimeout, maxTimeout);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
        if (this.remainingCooldown > 0) {
            --this.remainingCooldown;
            return false;
        }
        Brain<? extends LivingEntity> brain = body.getBrain();
        WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
        boolean reachedTarget = this.reachedTarget(body, walkTarget);
        if (!reachedTarget && this.tryComputePath(body, walkTarget, level.getGameTime())) {
            this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
            return true;
        }
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        if (reachedTarget) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        if (this.path == null || this.lastTargetPos == null) {
            return false;
        }
        Optional<WalkTarget> walkTarget = body.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
        boolean isSpectator = walkTarget.map(MoveToTargetSink::isWalkTargetSpectator).orElse(false);
        PathNavigation navigation = body.getNavigation();
        return !navigation.isDone() && walkTarget.isPresent() && !this.reachedTarget(body, walkTarget.get()) && !isSpectator;
    }

    @Override
    protected void stop(ServerLevel level, Mob body, long timestamp) {
        if (body.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) && !this.reachedTarget(body, body.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get()) && body.getNavigation().isStuck()) {
            this.remainingCooldown = level.getRandom().nextInt(40);
        }
        body.getNavigation().stop();
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.path = null;
    }

    @Override
    protected void start(ServerLevel level, Mob body, long timestamp) {
        body.getBrain().setMemory(MemoryModuleType.PATH, this.path);
        body.getNavigation().moveTo(this.path, (double)this.speedModifier);
    }

    @Override
    protected void tick(ServerLevel level, Mob body, long timestamp) {
        Path newPath = body.getNavigation().getPath();
        Brain<? extends LivingEntity> brain = body.getBrain();
        if (this.path != newPath) {
            this.path = newPath;
            brain.setMemory(MemoryModuleType.PATH, newPath);
        }
        if (newPath == null || this.lastTargetPos == null) {
            return;
        }
        WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
        if (walkTarget.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0 && this.tryComputePath(body, walkTarget, level.getGameTime())) {
            this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
            this.start(level, body, timestamp);
        }
    }

    private boolean tryComputePath(Mob body, WalkTarget walkTarget, long timestamp) {
        BlockPos targetPos = walkTarget.getTarget().currentBlockPosition();
        this.path = body.getNavigation().createPath(targetPos, 0);
        this.speedModifier = walkTarget.getSpeedModifier();
        Brain<? extends LivingEntity> brain = body.getBrain();
        if (this.reachedTarget(body, walkTarget)) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        } else {
            boolean canReach;
            boolean bl = canReach = this.path != null && this.path.canReach();
            if (canReach) {
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else if (!brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, timestamp);
            }
            if (this.path != null) {
                return true;
            }
            Vec3 partialStep = DefaultRandomPos.getPosTowards((PathfinderMob)body, 10, 7, Vec3.atBottomCenterOf(targetPos), 1.5707963705062866);
            if (partialStep != null) {
                this.path = body.getNavigation().createPath(partialStep.x, partialStep.y, partialStep.z, 0);
                return this.path != null;
            }
        }
        return false;
    }

    private boolean reachedTarget(Mob body, WalkTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(body.blockPosition()) <= walkTarget.getCloseEnoughDist();
    }

    private static boolean isWalkTargetSpectator(WalkTarget walkTarget) {
        PositionTracker target = walkTarget.getTarget();
        if (target instanceof EntityTracker) {
            EntityTracker entityTracker = (EntityTracker)target;
            return entityTracker.getEntity().isSpectator();
        }
        return false;
    }
}

