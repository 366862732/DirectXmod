/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public class OcelotAttackGoal
extends Goal {
    private final Mob mob;
    private LivingEntity target;
    private int attackTime;

    public OcelotAttackGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity bestTarget = this.mob.getTarget();
        if (bestTarget == null) {
            return false;
        }
        this.target = bestTarget;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.target.isAlive()) {
            return false;
        }
        if (this.mob.distanceToSqr(this.target) > 225.0) {
            return false;
        }
        return !this.mob.getNavigation().isDone() || this.canUse();
    }

    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.mob.getLookControl().setLookAt(this.target, 30.0f, 30.0f);
        double meleeRadiusSqr = this.mob.getBbWidth() * 2.0f * (this.mob.getBbWidth() * 2.0f);
        double distSqr = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        double speedModifier = 0.8;
        if (distSqr > meleeRadiusSqr && distSqr < 16.0) {
            speedModifier = 1.33;
        } else if (distSqr < 225.0) {
            speedModifier = 0.6;
        }
        this.mob.getNavigation().moveTo(this.target, speedModifier);
        this.attackTime = Math.max(this.attackTime - 1, 0);
        if (distSqr > meleeRadiusSqr) {
            return;
        }
        if (this.attackTime > 0) {
            return;
        }
        this.attackTime = 20;
        this.mob.doHurtTarget(OcelotAttackGoal.getServerLevel(this.mob), this.target);
    }
}

