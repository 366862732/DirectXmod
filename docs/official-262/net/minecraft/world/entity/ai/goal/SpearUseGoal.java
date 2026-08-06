/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpearUseGoal<T extends Monster>
extends Goal {
    private static final int MIN_REPOSITION_DISTANCE = 6;
    private static final int MAX_REPOSITION_DISTANCE = 7;
    private static final int MIN_COOLDOWN_DISTANCE = 9;
    private static final int MAX_COOLDOWN_DISTANCE = 11;
    private static final double MAX_FLEEING_TIME = SpearUseGoal.reducedTickDelay(100);
    private final T mob;
    private @Nullable SpearUseState state;
    private final double speedModifierWhenCharging;
    private final double speedModifierWhenRepositioning;
    private final float approachDistanceSq;
    private final float targetInRangeRadiusSq;

    public SpearUseGoal(T mob, double speedModifierWhenCharging, double speedModifierWhenRepositioning, float approachDistance, float targetInRangeRadius) {
        this.mob = mob;
        this.speedModifierWhenCharging = speedModifierWhenCharging;
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.targetInRangeRadiusSq = targetInRangeRadius * targetInRangeRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.ableToAttack() && !((LivingEntity)this.mob).isUsingItem();
    }

    private boolean ableToAttack() {
        return ((Mob)this.mob).getTarget() != null && ((LivingEntity)this.mob).getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    private int getKineticWeaponUseDuration() {
        int durationTicks = Optional.ofNullable(((LivingEntity)this.mob).getMainHandItem().get(DataComponents.KINETIC_WEAPON)).map(KineticWeapon::computeDamageUseDuration).orElse(0);
        return SpearUseGoal.reducedTickDelay(durationTicks);
    }

    @Override
    public boolean canContinueToUse() {
        return this.state != null && !this.state.done && this.ableToAttack();
    }

    @Override
    public void start() {
        super.start();
        ((Mob)this.mob).setAggressive(true);
        this.state = new SpearUseState();
    }

    @Override
    public void stop() {
        super.stop();
        ((Mob)this.mob).getNavigation().stop();
        ((Mob)this.mob).setAggressive(false);
        this.state = null;
        ((LivingEntity)this.mob).stopUsingItem();
    }

    @Override
    public void tick() {
        double distance;
        if (this.state == null) {
            return;
        }
        LivingEntity target = ((Mob)this.mob).getTarget();
        double targetDistSqr = ((Entity)this.mob).distanceToSqr(target.getX(), target.getY(), target.getZ());
        Entity mount = ((Entity)this.mob).getRootVehicle();
        float speedModifier = 1.0f;
        if (mount instanceof Mob) {
            Mob vehicleMob = (Mob)mount;
            speedModifier = vehicleMob.chargeSpeedModifier();
        }
        int mountDistance = ((Entity)this.mob).isPassenger() ? 2 : 0;
        ((Mob)this.mob).lookAt(target, 30.0f, 30.0f);
        ((Mob)this.mob).getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (this.state.notEngagedYet()) {
            if (targetDistSqr > (double)this.approachDistanceSq) {
                ((Mob)this.mob).getNavigation().moveTo(target, (double)speedModifier * this.speedModifierWhenRepositioning);
                return;
            }
            this.state.startEngagement(this.getKineticWeaponUseDuration());
            ((LivingEntity)this.mob).startUsingItem(InteractionHand.MAIN_HAND);
        }
        if (this.state.tickAndCheckEngagement()) {
            ((LivingEntity)this.mob).stopUsingItem();
            distance = Math.sqrt(targetDistSqr);
            this.state.awayPos = LandRandomPos.getPosAway(this.mob, Math.max(0.0, (double)(9 + mountDistance) - distance), Math.max(1.0, (double)(11 + mountDistance) - distance), 7, target.position());
            this.state.fleeingTime = 1;
        }
        if (this.state.tickAndCheckFleeing()) {
            return;
        }
        if (this.state.awayPos != null) {
            ((Mob)this.mob).getNavigation().moveTo(this.state.awayPos.x, this.state.awayPos.y, this.state.awayPos.z, (double)speedModifier * this.speedModifierWhenRepositioning);
            if (((Mob)this.mob).getNavigation().isDone()) {
                if (this.state.fleeingTime > 0) {
                    this.state.done = true;
                    return;
                }
                this.state.awayPos = null;
            }
        } else {
            ((Mob)this.mob).getNavigation().moveTo(target, (double)speedModifier * this.speedModifierWhenCharging);
            if (targetDistSqr < (double)this.targetInRangeRadiusSq || ((Mob)this.mob).getNavigation().isDone()) {
                distance = Math.sqrt(targetDistSqr);
                this.state.awayPos = LandRandomPos.getPosAway(this.mob, (double)(6 + mountDistance) - distance, (double)(7 + mountDistance) - distance, 7, target.position());
            }
        }
    }

    public static class SpearUseState {
        private int engageTime = -1;
        private int fleeingTime = -1;
        private @Nullable Vec3 awayPos;
        private boolean done = false;

        public boolean notEngagedYet() {
            return this.engageTime < 0;
        }

        public void startEngagement(int spearDownTime) {
            this.engageTime = spearDownTime;
        }

        public boolean tickAndCheckEngagement() {
            if (this.engageTime > 0) {
                --this.engageTime;
                if (this.engageTime == 0) {
                    return true;
                }
            }
            return false;
        }

        public boolean tickAndCheckFleeing() {
            if (this.fleeingTime > 0) {
                ++this.fleeingTime;
                if ((double)this.fleeingTime > MAX_FLEEING_TIME) {
                    this.done = true;
                    return true;
                }
            }
            return false;
        }
    }
}

