/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.BiPredicate;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.gamerules.GameRules;

public class StartCelebratingIfTargetDead {
    public static BehaviorControl<LivingEntity> create(int celebrateDuration, BiPredicate<LivingEntity, LivingEntity> dancePredicate) {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.ANGRY_AT), i.absent(MemoryModuleType.CELEBRATE_LOCATION), i.registered(MemoryModuleType.DANCING)).apply((Applicative)i, (attackTarget, angryAt, celebrateAt, dancing) -> (level, body, timestamp) -> {
            LivingEntity target = (LivingEntity)i.get(attackTarget);
            if (!target.isDeadOrDying()) {
                return false;
            }
            if (dancePredicate.test(body, target)) {
                dancing.setWithExpiry(true, celebrateDuration);
            }
            celebrateAt.setWithExpiry(target.blockPosition(), celebrateDuration);
            if (!target.is(EntityTypes.PLAYER) || level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS).booleanValue()) {
                attackTarget.erase();
                angryAt.erase();
            }
            return true;
        }));
    }
}

