/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.monster.piglin;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;

public class StartHuntingHoglin {
    public static OneShot<Piglin> create() {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN), i.absent(MemoryModuleType.ANGRY_AT), i.absent(MemoryModuleType.HUNTED_RECENTLY), i.registered(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS)).apply((Applicative)i, (huntable, angryAt, huntedRecently, nearestPiglins) -> (level, body, timestamp) -> {
            if (body.isBaby() || i.tryGet(nearestPiglins).filter(p -> p.stream().anyMatch(StartHuntingHoglin::hasHuntedRecently)).isPresent()) {
                return false;
            }
            Hoglin target = (Hoglin)i.get(huntable);
            PiglinAi.setAngerTarget(level, body, target);
            PiglinAi.dontKillAnyMoreHoglinsForAWhile(body);
            PiglinAi.broadcastAngerTarget(level, body, target);
            i.tryGet(nearestPiglins).ifPresent(p -> p.forEach(PiglinAi::dontKillAnyMoreHoglinsForAWhile));
            return true;
        }));
    }

    private static boolean hasHuntedRecently(AbstractPiglin otherPiglin) {
        return otherPiglin.getBrain().hasMemoryValue(MemoryModuleType.HUNTED_RECENTLY);
    }
}

