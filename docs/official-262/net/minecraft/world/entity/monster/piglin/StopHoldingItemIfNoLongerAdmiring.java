/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.monster.piglin;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;

public class StopHoldingItemIfNoLongerAdmiring {
    public static BehaviorControl<Piglin> create() {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.ADMIRING_ITEM)).apply((Applicative)i, admiring -> (level, body, timestamp) -> {
            if (body.getOffhandItem().isEmpty() || body.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
                return false;
            }
            PiglinAi.stopHoldingOffHandItem(level, body, true);
            return true;
        }));
    }
}

