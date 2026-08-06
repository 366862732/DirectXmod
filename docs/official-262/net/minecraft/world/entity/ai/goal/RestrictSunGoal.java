/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.goal;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;

public class RestrictSunGoal
extends Goal {
    private final PathfinderMob mob;

    public RestrictSunGoal(PathfinderMob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        return this.mob.level().isBrightOutside() && this.mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && GoalUtils.hasGroundPathNavigation(this.mob);
    }

    @Override
    public void start() {
        PathNavigation pathNavigation = this.mob.getNavigation();
        if (pathNavigation instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigation2 = (GroundPathNavigation)pathNavigation;
            pathNavigation2.setAvoidSun(true);
        }
    }

    @Override
    public void stop() {
        PathNavigation pathNavigation;
        if (GoalUtils.hasGroundPathNavigation(this.mob) && (pathNavigation = this.mob.getNavigation()) instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigation2 = (GroundPathNavigation)pathNavigation;
            pathNavigation2.setAvoidSun(false);
        }
    }
}

