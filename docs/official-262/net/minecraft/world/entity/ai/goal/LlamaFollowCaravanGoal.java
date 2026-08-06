/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.phys.Vec3;

public class LlamaFollowCaravanGoal
extends Goal {
    public final Llama llama;
    private double speedModifier;
    private static final int CARAVAN_LIMIT = 8;
    private int distCheckCounter;

    public LlamaFollowCaravanGoal(Llama llama, double speedModifier) {
        this.llama = llama;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        double distSquare;
        Llama candidate;
        if (this.llama.isLeashed() || this.llama.inCaravan()) {
            return false;
        }
        List<Entity> llamas = this.llama.level().getEntities(this.llama, this.llama.getBoundingBox().inflate(9.0, 4.0, 9.0), e -> e.is(EntityTypes.LLAMA) || e.is(EntityTypes.TRADER_LLAMA));
        Leashable closest = null;
        double closestDistSquare = Double.MAX_VALUE;
        for (Entity entity : llamas) {
            candidate = (Llama)entity;
            if (!candidate.inCaravan() || candidate.hasCaravanTail() || (distSquare = this.llama.distanceToSqr(candidate)) > closestDistSquare) continue;
            closestDistSquare = distSquare;
            closest = candidate;
        }
        if (closest == null) {
            for (Entity entity : llamas) {
                candidate = (Llama)entity;
                if (!candidate.isLeashed() || candidate.hasCaravanTail() || (distSquare = this.llama.distanceToSqr(candidate)) > closestDistSquare) continue;
                closestDistSquare = distSquare;
                closest = candidate;
            }
        }
        if (closest == null) {
            return false;
        }
        if (closestDistSquare < 4.0) {
            return false;
        }
        if (!closest.isLeashed() && !this.firstIsLeashed((Llama)closest, 1)) {
            return false;
        }
        this.llama.joinCaravan((Llama)closest);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(this.llama.inCaravan() && this.llama.getCaravanHead().isAlive() && this.firstIsLeashed(this.llama, 0))) {
            return false;
        }
        double distSqr = this.llama.distanceToSqr(this.llama.getCaravanHead());
        if (distSqr > 676.0) {
            if (this.speedModifier <= 3.0) {
                this.speedModifier *= 1.2;
                this.distCheckCounter = LlamaFollowCaravanGoal.reducedTickDelay(40);
                return true;
            }
            if (this.distCheckCounter == 0) {
                return false;
            }
        }
        if (this.distCheckCounter > 0) {
            --this.distCheckCounter;
        }
        return true;
    }

    @Override
    public void stop() {
        this.llama.leaveCaravan();
        this.speedModifier = 2.1;
    }

    @Override
    public void tick() {
        if (!this.llama.inCaravan()) {
            return;
        }
        if (this.llama.getLeashHolder() instanceof LeashFenceKnotEntity) {
            return;
        }
        Llama follows = this.llama.getCaravanHead();
        double distanceTo = this.llama.distanceTo(follows);
        float wantedDistance = 2.0f;
        Vec3 delta = new Vec3(follows.getX() - this.llama.getX(), follows.getY() - this.llama.getY(), follows.getZ() - this.llama.getZ()).normalize().scale(Math.max(distanceTo - 2.0, 0.0));
        this.llama.getNavigation().moveTo(this.llama.getX() + delta.x, this.llama.getY() + delta.y, this.llama.getZ() + delta.z, this.speedModifier);
    }

    private boolean firstIsLeashed(Llama currentMob, int counter) {
        if (counter > 8) {
            return false;
        }
        if (currentMob.inCaravan()) {
            if (currentMob.getCaravanHead().isLeashed()) {
                return true;
            }
            return this.firstIsLeashed(currentMob.getCaravanHead(), ++counter);
        }
        return false;
    }
}

