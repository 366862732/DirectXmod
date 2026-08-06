/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BecomePassiveIfMemoryPresent {
    public static BehaviorControl<LivingEntity> create(MemoryModuleType<?> pacifyingMemory, int pacifyDuration) {
        return BehaviorBuilder.create(i -> i.group(i.registered(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.PACIFIED), i.present(pacifyingMemory)).apply((Applicative)i, i.point(() -> "[BecomePassive if " + String.valueOf(pacifyingMemory) + " present]", (attackTarget, pacified, pacifying) -> (level, body, timestamp) -> {
            pacified.setWithExpiry(true, pacifyDuration);
            attackTarget.erase();
            return true;
        })));
    }
}

