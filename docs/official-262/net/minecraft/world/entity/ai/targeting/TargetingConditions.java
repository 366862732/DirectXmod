/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.targeting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

public class TargetingConditions {
    public static final TargetingConditions DEFAULT = TargetingConditions.forCombat();
    private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0;
    private final boolean isCombat;
    private double range = -1.0;
    private boolean checkLineOfSight = true;
    private boolean testInvisible = true;
    private @Nullable Selector selector;

    private TargetingConditions(boolean isCombat) {
        this.isCombat = isCombat;
    }

    public static TargetingConditions forCombat() {
        return new TargetingConditions(true);
    }

    public static TargetingConditions forNonCombat() {
        return new TargetingConditions(false);
    }

    public TargetingConditions copy() {
        TargetingConditions clone = this.isCombat ? TargetingConditions.forCombat() : TargetingConditions.forNonCombat();
        clone.range = this.range;
        clone.checkLineOfSight = this.checkLineOfSight;
        clone.testInvisible = this.testInvisible;
        clone.selector = this.selector;
        return clone;
    }

    public TargetingConditions range(double range) {
        this.range = range;
        return this;
    }

    public TargetingConditions ignoreLineOfSight() {
        this.checkLineOfSight = false;
        return this;
    }

    public TargetingConditions ignoreInvisibilityTesting() {
        this.testInvisible = false;
        return this;
    }

    public TargetingConditions selector(@Nullable Selector selector) {
        this.selector = selector;
        return this;
    }

    public boolean test(ServerLevel level, @Nullable LivingEntity targeter, LivingEntity target) {
        if (targeter == target) {
            return false;
        }
        if (!target.canBeSeenByAnyone()) {
            return false;
        }
        if (this.selector != null && !this.selector.test(target, level)) {
            return false;
        }
        if (targeter == null) {
            if (this.isCombat && (!target.canBeSeenAsEnemy() || level.getDifficulty() == Difficulty.PEACEFUL)) {
                return false;
            }
        } else {
            Mob mob;
            if (this.isCombat && (!targeter.canAttack(target) || targeter.isAlliedTo(target))) {
                return false;
            }
            if (this.range > 0.0) {
                double modifier = this.testInvisible ? target.getVisibilityPercent(targeter) : 1.0;
                double visibilityDistance = Math.max(this.range * modifier, 2.0);
                double distanceToSqr = targeter.distanceToSqr(target.getX(), target.getY(), target.getZ());
                if (distanceToSqr > visibilityDistance * visibilityDistance) {
                    return false;
                }
            }
            if (this.checkLineOfSight && targeter instanceof Mob && !(mob = (Mob)targeter).getSensing().hasLineOfSight(target)) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    public static interface Selector {
        public boolean test(LivingEntity var1, ServerLevel var2);
    }
}

