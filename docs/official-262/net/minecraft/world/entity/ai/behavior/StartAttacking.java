/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StartAttacking {
    public static <E extends Mob> BehaviorControl<E> create(TargetFinder<E> targetFinderFunction) {
        return StartAttacking.create((level, body) -> true, targetFinderFunction);
    }

    public static <E extends Mob> BehaviorControl<E> create(StartAttackingCondition<E> canAttackPredicate, TargetFinder<E> targetFinderFunction) {
        return BehaviorBuilder.create((BehaviorBuilder.Instance<E> i) -> i.group(i.absent(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)).apply((Applicative)i, (attackTarget, cantReachSince) -> (level, body, timestamp) -> {
            if (!canAttackPredicate.test(level, body)) {
                return false;
            }
            Optional<LivingEntity> target = targetFinderFunction.get(level, body);
            if (target.isEmpty()) {
                return false;
            }
            LivingEntity targetEntity = target.get();
            if (!body.canAttack(targetEntity)) {
                return false;
            }
            attackTarget.set(targetEntity);
            cantReachSince.erase();
            return true;
        }));
    }

    @FunctionalInterface
    public static interface StartAttackingCondition<E> {
        public boolean test(ServerLevel var1, E var2);
    }

    @FunctionalInterface
    public static interface TargetFinder<E> {
        public Optional<? extends LivingEntity> get(ServerLevel var1, E var2);
    }
}

