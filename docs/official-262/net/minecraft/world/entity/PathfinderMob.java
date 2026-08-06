/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public abstract class PathfinderMob
extends Mob {
    protected static final float DEFAULT_WALK_TARGET_VALUE = 0.0f;

    protected PathfinderMob(EntityType<? extends PathfinderMob> type, Level level) {
        super((EntityType<? extends Mob>)type, level);
    }

    public float getWalkTargetValue(BlockPos pos) {
        return this.getWalkTargetValue(pos, this.level());
    }

    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0f;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason spawnReason) {
        return this.getWalkTargetValue(this.blockPosition(), level) >= 0.0f;
    }

    public boolean isPathFinding() {
        return !this.getNavigation().isDone();
    }

    public boolean isPanicking() {
        if (!this.brain.isBrainDead() && this.brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)) {
            return this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent();
        }
        for (WrappedGoal wrappedGoal : this.goalSelector.getAvailableGoals()) {
            if (!wrappedGoal.isRunning() || !(wrappedGoal.getGoal() instanceof PanicGoal)) continue;
            return true;
        }
        return false;
    }

    protected boolean shouldStayCloseToLeashHolder() {
        return true;
    }

    @Override
    public void closeRangeLeashBehaviour(Entity leashHolder) {
        super.closeRangeLeashBehaviour(leashHolder);
        if (this.shouldStayCloseToLeashHolder() && !this.isPanicking()) {
            this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            float wantedDistance = 2.0f;
            float distanceTo = this.distanceTo(leashHolder);
            Vec3 delta = new Vec3(leashHolder.getX() - this.getX(), leashHolder.getY() - this.getY(), leashHolder.getZ() - this.getZ()).normalize().scale(Math.max(distanceTo - 2.0f, 0.0f));
            this.getNavigation().moveTo(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z, this.followLeashSpeed());
        }
    }

    @Override
    public void whenLeashedTo(Entity leashHolder) {
        this.setHomeTo(leashHolder.blockPosition(), (int)this.leashElasticDistance() - 1);
        super.whenLeashedTo(leashHolder);
    }

    protected double followLeashSpeed() {
        return 1.0;
    }
}

