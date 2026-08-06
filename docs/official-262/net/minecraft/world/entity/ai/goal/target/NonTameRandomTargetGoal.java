/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.goal.target;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.jspecify.annotations.Nullable;

public class NonTameRandomTargetGoal<T extends LivingEntity>
extends NearestAttackableTargetGoal<T> {
    private final TamableAnimal tamableMob;

    public NonTameRandomTargetGoal(TamableAnimal mob, Class<T> targetType, boolean mustSee,  @Nullable TargetingConditions.Selector subselector) {
        super(mob, targetType, 10, mustSee, false, subselector);
        this.tamableMob = mob;
    }

    @Override
    public boolean canUse() {
        return !this.tamableMob.isTame() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetConditions != null) {
            return this.targetConditions.test(NonTameRandomTargetGoal.getServerLevel(this.mob), this.mob, this.target);
        }
        return super.canContinueToUse();
    }
}

