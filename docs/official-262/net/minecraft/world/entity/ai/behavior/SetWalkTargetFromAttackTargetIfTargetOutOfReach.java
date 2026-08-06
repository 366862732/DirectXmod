/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromAttackTargetIfTargetOutOfReach {
    private static final int PROJECTILE_ATTACK_RANGE_BUFFER = 1;

    public static BehaviorControl<Mob> create(float speedModifier) {
        return SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(mob -> Float.valueOf(speedModifier));
    }

    public static BehaviorControl<Mob> create(Function<LivingEntity, Float> speedModifier) {
        return BehaviorBuilder.create((BehaviorBuilder.Instance<E> i) -> i.group(i.registered(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET), i.present(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply((Applicative)i, (walkTarget, lookTarget, attackTarget, nearestEntities) -> (level, body, timestamp) -> {
            LivingEntity toAttack = (LivingEntity)i.get(attackTarget);
            Optional entities = i.tryGet(nearestEntities);
            if (entities.isPresent() && ((NearestVisibleLivingEntities)entities.get()).contains(toAttack) && BehaviorUtils.isWithinAttackRange(body, toAttack, 1)) {
                walkTarget.erase();
            } else {
                lookTarget.set(new EntityTracker(toAttack, true));
                walkTarget.set(new WalkTarget(new EntityTracker(toAttack, false), ((Float)speedModifier.apply(body)).floatValue(), 0));
            }
            return true;
        }));
    }
}

