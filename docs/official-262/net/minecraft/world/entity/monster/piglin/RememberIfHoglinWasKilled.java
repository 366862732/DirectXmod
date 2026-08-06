/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.monster.piglin;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;

public class RememberIfHoglinWasKilled {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.ATTACK_TARGET), i.registered(MemoryModuleType.HUNTED_RECENTLY)).apply((Applicative)i, (attackTarget, huntedRecently) -> (level, body, timestamp) -> {
            LivingEntity target = (LivingEntity)i.get(attackTarget);
            if (target.is(EntityTypes.HOGLIN) && target.isDeadOrDying()) {
                huntedRecently.setWithExpiry(true, PiglinAi.TIME_BETWEEN_HUNTS.sample(body.level().getRandom()));
            }
            return true;
        }));
    }
}

