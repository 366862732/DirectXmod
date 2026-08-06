/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;

public abstract class MoveToBlockGoal
extends Goal {
    private static final int GIVE_UP_TICKS = 1200;
    private static final int STAY_TICKS = 1200;
    private static final int INTERVAL_TICKS = 200;
    protected final PathfinderMob mob;
    public final double speedModifier;
    protected int nextStartTick;
    protected int tryTicks;
    private int maxStayTicks;
    protected BlockPos blockPos = BlockPos.ZERO;
    private boolean reachedTarget;
    private final int searchRange;
    private final int verticalSearchRange;
    protected int verticalSearchStart;

    public MoveToBlockGoal(PathfinderMob mob, double speedModifier, int searchRange) {
        this(mob, speedModifier, searchRange, 1);
    }

    public MoveToBlockGoal(PathfinderMob mob, double speedModifier, int searchRange, int verticalSearchRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;
        this.verticalSearchStart = 0;
        this.verticalSearchRange = verticalSearchRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }
        this.nextStartTick = this.nextStartTick(this.mob);
        return this.findNearestBlock();
    }

    protected int nextStartTick(PathfinderMob mob) {
        return MoveToBlockGoal.reducedTickDelay(200 + mob.getRandom().nextInt(200));
    }

    @Override
    public boolean canContinueToUse() {
        return this.tryTicks >= -this.maxStayTicks && this.tryTicks <= 1200 && this.isValidTarget(this.mob.level(), this.blockPos);
    }

    @Override
    public void start() {
        this.moveMobToBlock();
        this.tryTicks = 0;
        this.maxStayTicks = this.mob.getRandom().nextInt(this.mob.getRandom().nextInt(1200) + 1200) + 1200;
    }

    protected void moveMobToBlock() {
        this.mob.getNavigation().moveTo((double)this.blockPos.getX() + 0.5, this.blockPos.getY() + 1, (double)this.blockPos.getZ() + 0.5, this.speedModifier);
    }

    public double acceptedDistance() {
        return 1.0;
    }

    protected BlockPos getMoveToTarget() {
        return this.blockPos.above();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos moveToTarget = this.getMoveToTarget();
        if (!moveToTarget.closerToCenterThan(this.mob.position(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.shouldRecalculatePath()) {
                this.mob.getNavigation().moveTo((double)moveToTarget.getX() + 0.5, moveToTarget.getY(), (double)moveToTarget.getZ() + 0.5, this.speedModifier);
            }
        } else {
            this.reachedTarget = true;
            --this.tryTicks;
        }
    }

    public boolean shouldRecalculatePath() {
        return this.tryTicks % 40 == 0;
    }

    protected boolean isReachedTarget() {
        return this.reachedTarget;
    }

    protected boolean findNearestBlock() {
        int horizontalSearch = this.searchRange;
        int verticalSearch = this.verticalSearchRange;
        BlockPos mobPos = this.mob.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y = this.verticalSearchStart;
        while (y <= verticalSearch) {
            for (int r = 0; r < horizontalSearch; ++r) {
                int x = 0;
                while (x <= r) {
                    int z;
                    int n = z = x < r && x > -r ? r : 0;
                    while (z <= r) {
                        pos.setWithOffset(mobPos, x, y - 1, z);
                        if (this.mob.isWithinHome(pos) && this.isValidTarget(this.mob.level(), pos)) {
                            this.blockPos = pos;
                            return true;
                        }
                        z = z > 0 ? -z : 1 - z;
                    }
                    x = x > 0 ? -x : 1 - x;
                }
            }
            y = y > 0 ? -y : 1 - y;
        }
        return false;
    }

    protected abstract boolean isValidTarget(LevelReader var1, BlockPos var2);
}

