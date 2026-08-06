/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.Predicate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class MeleeAttack {
    public static <T extends Mob> OneShot<T> create(int cooldownBetweenAttacks) {
        return MeleeAttack.create(body -> true, cooldownBetweenAttacks);
    }

    public static <T extends Mob> OneShot<T> create(Predicate<T> canAttackPredicate, int cooldownBetweenAttacks) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.LOOK_TARGET), i.present(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.ATTACK_COOLING_DOWN), i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply((Applicative)i, (lookTarget, attackTarget, attackCoolingDown, nearestEntities) -> (level, body, timestamp) -> {
            LivingEntity target = (LivingEntity)i.get(attackTarget);
            if (canAttackPredicate.test(body) && !MeleeAttack.isHoldingUsableNonMeleeWeapon(body) && body.isWithinMeleeAttackRange(target) && ((NearestVisibleLivingEntities)i.get(nearestEntities)).contains(target)) {
                lookTarget.set(new EntityTracker(target, true));
                body.swing(InteractionHand.MAIN_HAND);
                body.doHurtTarget(level, target);
                attackCoolingDown.setWithExpiry(true, cooldownBetweenAttacks);
                return true;
            }
            return false;
        }));
    }

    private static boolean isHoldingUsableNonMeleeWeapon(Mob body) {
        return body.isHolding(body::canUseNonMeleeWeapon);
    }
}

