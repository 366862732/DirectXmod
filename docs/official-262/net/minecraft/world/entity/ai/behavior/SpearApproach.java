/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.SpearAttack;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public class SpearApproach
extends Behavior<PathfinderMob> {
    private final double speedModifierWhenRepositioning;
    private final float approachDistanceSq;

    public SpearApproach(double speedModifierWhenRepositioning, float approachDistance) {
        super(Map.of(MemoryModuleType.SPEAR_STATUS, MemoryStatus.VALUE_ABSENT));
        this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
        this.approachDistanceSq = approachDistance * approachDistance;
    }

    private boolean ableToAttack(PathfinderMob mob) {
        return this.getTarget(mob) != null && mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, PathfinderMob body) {
        return this.ableToAttack(body) && !body.isUsingItem();
    }

    @Override
    protected void start(ServerLevel level, PathfinderMob body, long timestamp) {
        body.setAggressive(true);
        body.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.APPROACH);
        super.start(level, body, timestamp);
    }

    private @Nullable LivingEntity getTarget(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PathfinderMob body, long timestamp) {
        return this.ableToAttack(body) && this.farEnough(body);
    }

    private boolean farEnough(PathfinderMob mob) {
        LivingEntity target = this.getTarget(mob);
        double targetDistSqr = mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        return targetDistSqr > (double)this.approachDistanceSq;
    }

    @Override
    protected void tick(ServerLevel level, PathfinderMob mob, long timestamp) {
        LivingEntity target = this.getTarget(mob);
        Entity mount = mob.getRootVehicle();
        float speedModifier = 1.0f;
        if (mount instanceof Mob) {
            Mob vehicleMob = (Mob)mount;
            speedModifier = vehicleMob.chargeSpeedModifier();
        }
        mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        mob.getNavigation().moveTo(target, (double)speedModifier * this.speedModifierWhenRepositioning);
    }

    @Override
    protected void stop(ServerLevel level, PathfinderMob body, long timestamp) {
        body.getNavigation().stop();
        body.getBrain().setMemory(MemoryModuleType.SPEAR_STATUS, SpearAttack.SpearStatus.CHARGING);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }
}

