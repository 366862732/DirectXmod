/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;

public class RangedBowAttackGoal<T extends Monster>
extends Goal {
    private final T mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public RangedBowAttackGoal(T mob, double speedModifier, int attackIntervalMin, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void setMinAttackInterval(int ticks) {
        this.attackIntervalMin = ticks;
    }

    @Override
    public boolean canUse() {
        if (((Mob)this.mob).getTarget() == null) {
            return false;
        }
        return this.isHoldingBow();
    }

    protected boolean isHoldingBow() {
        return ((LivingEntity)this.mob).isHolding(Items.BOW);
    }

    @Override
    public boolean canContinueToUse() {
        return (this.canUse() || !((Mob)this.mob).getNavigation().isDone()) && this.isHoldingBow();
    }

    @Override
    public void start() {
        super.start();
        ((Mob)this.mob).setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        ((Mob)this.mob).setAggressive(false);
        this.seeTime = 0;
        this.attackTime = -1;
        ((LivingEntity)this.mob).stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        boolean hadLineOfSight;
        LivingEntity target = ((Mob)this.mob).getTarget();
        if (target == null) {
            return;
        }
        double targetDistSqr = ((Entity)this.mob).distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean hasLineOfSight = ((Mob)this.mob).getSensing().hasLineOfSight(target);
        boolean bl = hadLineOfSight = this.seeTime > 0;
        if (hasLineOfSight != hadLineOfSight) {
            this.seeTime = 0;
        }
        this.seeTime = hasLineOfSight ? ++this.seeTime : --this.seeTime;
        if (targetDistSqr > (double)this.attackRadiusSqr || this.seeTime < 20) {
            ((Mob)this.mob).getNavigation().moveTo(target, this.speedModifier);
            this.strafingTime = -1;
        } else {
            ((Mob)this.mob).getNavigation().stop();
            ++this.strafingTime;
        }
        if (this.strafingTime >= 20) {
            if ((double)((Entity)this.mob).getRandom().nextFloat() < 0.3) {
                boolean bl2 = this.strafingClockwise = !this.strafingClockwise;
            }
            if ((double)((Entity)this.mob).getRandom().nextFloat() < 0.3) {
                this.strafingBackwards = !this.strafingBackwards;
            }
            this.strafingTime = 0;
        }
        if (this.strafingTime > -1) {
            if (targetDistSqr > (double)(this.attackRadiusSqr * 0.75f)) {
                this.strafingBackwards = false;
            } else if (targetDistSqr < (double)(this.attackRadiusSqr * 0.25f)) {
                this.strafingBackwards = true;
            }
            ((Mob)this.mob).getMoveControl().strafe(this.strafingBackwards ? -0.5f : 0.5f, this.strafingClockwise ? 0.5f : -0.5f);
            Entity entity = ((Entity)this.mob).getControlledVehicle();
            if (entity instanceof Mob) {
                Mob vehicle = (Mob)entity;
                vehicle.lookAt(target, 30.0f, 30.0f);
            }
            ((Mob)this.mob).lookAt(target, 30.0f, 30.0f);
        } else {
            ((Mob)this.mob).getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
        if (((LivingEntity)this.mob).isUsingItem()) {
            int pullTime;
            if (!hasLineOfSight && this.seeTime < -60) {
                ((LivingEntity)this.mob).stopUsingItem();
            } else if (hasLineOfSight && (pullTime = ((LivingEntity)this.mob).getTicksUsingItem()) >= 20) {
                ((LivingEntity)this.mob).stopUsingItem();
                ((RangedAttackMob)this.mob).performRangedAttack(target, BowItem.getPowerForTime(pullTime));
                this.attackTime = this.attackIntervalMin;
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            ((LivingEntity)this.mob).startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.BOW));
        }
    }
}

