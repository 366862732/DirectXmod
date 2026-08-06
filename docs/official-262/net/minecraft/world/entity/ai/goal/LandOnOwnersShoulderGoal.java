/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;

public class LandOnOwnersShoulderGoal
extends Goal {
    private final ShoulderRidingEntity entity;
    private boolean isSittingOnShoulder;

    public LandOnOwnersShoulderGoal(ShoulderRidingEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean canUse() {
        LivingEntity livingEntity = this.entity.getOwner();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer owner = (ServerPlayer)livingEntity;
            boolean ownerThatCanBeSatOn = !owner.isSpectator() && !owner.getAbilities().flying && !owner.isInWater() && !owner.isInPowderSnow;
            return !this.entity.isOrderedToSit() && ownerThatCanBeSatOn && this.entity.canSitOnShoulder();
        }
        return false;
    }

    @Override
    public boolean isInterruptable() {
        return !this.isSittingOnShoulder;
    }

    @Override
    public void start() {
        this.isSittingOnShoulder = false;
    }

    @Override
    public void tick() {
        if (this.isSittingOnShoulder || this.entity.isInSittingPose() || this.entity.isLeashed()) {
            return;
        }
        LivingEntity livingEntity = this.entity.getOwner();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer owner = (ServerPlayer)livingEntity;
            if (this.entity.getBoundingBox().intersects(owner.getBoundingBox())) {
                this.isSittingOnShoulder = this.entity.setEntityOnShoulder(owner);
            }
        }
    }
}

