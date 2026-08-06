/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class FollowPlayerRiddenEntityGoal
extends Goal {
    private int timeToRecalcPath;
    private final PathfinderMob mob;
    private final Class<? extends Entity> entityTypeToFollow;
    private @Nullable Player following;
    private FollowEntityGoal currentGoal;

    public FollowPlayerRiddenEntityGoal(PathfinderMob mob, Class<? extends Entity> entityTypeToFollow) {
        this.mob = mob;
        this.entityTypeToFollow = entityTypeToFollow;
    }

    @Override
    public boolean canUse() {
        if (this.following != null && this.following.hasMovedHorizontallyRecently()) {
            return true;
        }
        List<? extends Entity> entities = this.mob.level().getEntitiesOfClass(this.entityTypeToFollow, this.mob.getBoundingBox().inflate(5.0));
        for (Entity entity : entities) {
            Player controllingPlayer;
            LivingEntity livingEntity = entity.getControllingPassenger();
            if (!(livingEntity instanceof Player) || !(controllingPlayer = (Player)livingEntity).hasMovedHorizontallyRecently()) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.following != null && this.following.isPassenger() && this.following.hasMovedHorizontallyRecently();
    }

    @Override
    public void start() {
        List<? extends Entity> entities = this.mob.level().getEntitiesOfClass(this.entityTypeToFollow, this.mob.getBoundingBox().inflate(5.0));
        for (Entity entity : entities) {
            Player player;
            LivingEntity livingEntity = entity.getControllingPassenger();
            if (!(livingEntity instanceof Player)) continue;
            this.following = player = (Player)livingEntity;
            break;
        }
        this.timeToRecalcPath = 0;
        this.currentGoal = FollowEntityGoal.GO_TO_ENTITY;
    }

    @Override
    public void stop() {
        this.following = null;
    }

    @Override
    public void tick() {
        float speed = this.currentGoal == FollowEntityGoal.GO_IN_ENTITY_DIRECTION ? 0.01f : 0.015f;
        this.mob.moveRelative(speed, new Vec3(this.mob.xxa, this.mob.yya, this.mob.zza));
        this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
        if (--this.timeToRecalcPath > 0) {
            return;
        }
        this.timeToRecalcPath = this.adjustedTickDelay(10);
        if (this.currentGoal == FollowEntityGoal.GO_TO_ENTITY) {
            BlockPos behindEntityPos = this.following.blockPosition().relative(this.following.getDirection().getOpposite());
            behindEntityPos = behindEntityPos.offset(0, -1, 0);
            this.mob.getNavigation().moveTo(behindEntityPos.getX(), behindEntityPos.getY(), behindEntityPos.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) < 4.0f) {
                this.timeToRecalcPath = 0;
                this.currentGoal = FollowEntityGoal.GO_IN_ENTITY_DIRECTION;
            }
        } else if (this.currentGoal == FollowEntityGoal.GO_IN_ENTITY_DIRECTION) {
            Direction direction = this.following.getMotionDirection();
            BlockPos goTo = this.following.blockPosition().relative(direction, 10);
            this.mob.getNavigation().moveTo(goTo.getX(), goTo.getY() - 1, goTo.getZ(), 1.0);
            if (this.mob.distanceTo(this.following) > 12.0f) {
                this.timeToRecalcPath = 0;
                this.currentGoal = FollowEntityGoal.GO_TO_ENTITY;
            }
        }
    }

    private static enum FollowEntityGoal {
        GO_TO_ENTITY,
        GO_IN_ENTITY_DIRECTION;

    }
}

