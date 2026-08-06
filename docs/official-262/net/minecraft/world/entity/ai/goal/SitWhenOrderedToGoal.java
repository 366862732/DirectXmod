/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

public class SitWhenOrderedToGoal
extends Goal {
    private final TamableAnimal mob;

    public SitWhenOrderedToGoal(TamableAnimal mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.isOrderedToSit();
    }

    @Override
    public boolean canUse() {
        boolean orderedToSit = this.mob.isOrderedToSit();
        if (!orderedToSit && !this.mob.isTame()) {
            return false;
        }
        if (this.mob.isInWater()) {
            return false;
        }
        if (!this.mob.onGround()) {
            return false;
        }
        LivingEntity owner = this.mob.getOwner();
        if (owner == null || owner.level() != this.mob.level()) {
            return true;
        }
        if (this.mob.distanceToSqr(owner) < 144.0 && owner.getLastHurtByMob() != null) {
            return false;
        }
        return orderedToSit;
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
        this.mob.setInSittingPose(true);
    }

    @Override
    public void stop() {
        this.mob.setInSittingPose(false);
    }
}

