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

public class StopAttackingIfTargetInvalid {
    private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

    public static <E extends Mob> BehaviorControl<E> create(TargetErasedCallback<E> onTargetErased) {
        return StopAttackingIfTargetInvalid.create((level, entity) -> false, onTargetErased, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(StopAttackCondition stopAttackingWhen) {
        return StopAttackingIfTargetInvalid.create(stopAttackingWhen, (level, body, target) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create() {
        return StopAttackingIfTargetInvalid.create((level, entity) -> false, (level, body, target) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(StopAttackCondition stopAttackingWhen, TargetErasedCallback<E> onTargetErased, boolean canGrowTiredOfTryingToReachTarget) {
        return BehaviorBuilder.create((BehaviorBuilder.Instance<E> i) -> i.group(i.present(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)).apply((Applicative)i, (attackTarget, cantReachSince) -> (level, body, timestamp) -> {
            LivingEntity target = (LivingEntity)i.get(attackTarget);
            if (!body.canAttack(target) || canGrowTiredOfTryingToReachTarget && StopAttackingIfTargetInvalid.isTiredOfTryingToReachTarget(body, i.tryGet(cantReachSince)) || !target.isAlive() || target.level() != body.level() || stopAttackingWhen.test(level, target)) {
                onTargetErased.accept(level, body, target);
                attackTarget.erase();
                return true;
            }
            return true;
        }));
    }

    private static boolean isTiredOfTryingToReachTarget(LivingEntity body, Optional<Long> cantReachSince) {
        return cantReachSince.isPresent() && body.level().getGameTime() - cantReachSince.get() > 200L;
    }

    @FunctionalInterface
    public static interface StopAttackCondition {
        public boolean test(ServerLevel var1, LivingEntity var2);
    }

    @FunctionalInterface
    public static interface TargetErasedCallback<E> {
        public void accept(ServerLevel var1, E var2, LivingEntity var3);
    }
}

