/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal.target;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class HurtByTargetGoal
extends TargetGoal {
    private static final TargetingConditions HURT_BY_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting();
    private static final int ALERT_RANGE_Y = 10;
    private boolean alertSameType;
    private int timestamp;
    private final Class<?>[] toIgnoreDamage;
    private Class<?> @Nullable [] toIgnoreAlert;

    public HurtByTargetGoal(PathfinderMob mob, Class<?> ... ignoreDamageFromTheseTypes) {
        super(mob, true);
        this.toIgnoreDamage = ignoreDamageFromTheseTypes;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        int timestamp = this.mob.getLastHurtByMobTimestamp();
        LivingEntity lastHurtByMob = this.mob.getLastHurtByMob();
        if (timestamp == this.timestamp || lastHurtByMob == null) {
            return false;
        }
        if (lastHurtByMob.is(EntityTypes.PLAYER) && HurtByTargetGoal.getServerLevel(this.mob).getGameRules().get(GameRules.UNIVERSAL_ANGER).booleanValue()) {
            return false;
        }
        for (Class<?> ignoreClass : this.toIgnoreDamage) {
            if (!ignoreClass.isAssignableFrom(lastHurtByMob.getClass())) continue;
            return false;
        }
        return this.canAttack(lastHurtByMob, HURT_BY_TARGETING);
    }

    public HurtByTargetGoal setAlertOthers(Class<?> ... exceptTheseTypes) {
        this.alertSameType = true;
        this.toIgnoreAlert = exceptTheseTypes;
        return this;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.mob.getLastHurtByMob());
        this.targetMob = this.mob.getTarget();
        this.timestamp = this.mob.getLastHurtByMobTimestamp();
        this.unseenMemoryTicks = 300;
        if (this.alertSameType) {
            this.alertOthers();
        }
        super.start();
    }

    protected void alertOthers() {
        double within = this.getFollowDistance();
        AABB searchAabb = AABB.unitCubeFromLowerCorner(this.mob.position()).inflate(within, 10.0, within);
        List<Entity> nearby = this.mob.level().getEntitiesOfClass(this.mob.getClass(), searchAabb, EntitySelector.NO_SPECTATORS);
        for (Mob mob : nearby) {
            TamableAnimal tamableAnimal;
            Class<?>[] classArray;
            if (this.mob == mob || mob.getTarget() != null || (classArray = this.mob) instanceof TamableAnimal && (tamableAnimal = (TamableAnimal)classArray).getOwner() != ((TamableAnimal)mob).getOwner() || mob.isAlliedTo(this.mob.getLastHurtByMob())) continue;
            if (this.toIgnoreAlert != null) {
                boolean ignore = false;
                classArray = this.toIgnoreAlert;
                int n = classArray.length;
                for (int i = 0; i < n; ++i) {
                    Class<?> ignoreClass = classArray[i];
                    if (mob.getClass() != ignoreClass) continue;
                    ignore = true;
                    break;
                }
                if (ignore) continue;
            }
            this.alertOther(mob, this.mob.getLastHurtByMob());
        }
    }

    protected void alertOther(Mob other, LivingEntity hurtByMob) {
        other.setTarget(hurtByMob);
    }
}

