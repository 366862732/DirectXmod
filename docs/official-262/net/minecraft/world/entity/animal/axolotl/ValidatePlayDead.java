/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.animal.axolotl;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class ValidatePlayDead {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.PLAY_DEAD_TICKS), i.registered(MemoryModuleType.HURT_BY_ENTITY)).apply((Applicative)i, (playDeadTicks, hurtBy) -> (level, body, timestamp) -> {
            int ticks = (Integer)i.get(playDeadTicks);
            if (ticks <= 0) {
                playDeadTicks.erase();
                hurtBy.erase();
                body.getBrain().useDefaultActivity();
            } else {
                playDeadTicks.set(ticks - 1);
            }
            return true;
        }));
    }
}

