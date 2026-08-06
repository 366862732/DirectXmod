/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.Predicate;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class CopyMemoryWithExpiry {
    public static <E extends LivingEntity, T> BehaviorControl<E> create(Predicate<E> copyIfTrue, MemoryModuleType<? extends T> sourceMemory, MemoryModuleType<T> targetMemory, UniformInt durationOfCopy) {
        return BehaviorBuilder.create(i -> i.group(i.present(sourceMemory), i.absent(targetMemory)).apply((Applicative)i, (source, target) -> (level, body, timestamp) -> {
            if (!copyIfTrue.test(body)) {
                return false;
            }
            target.setWithExpiry(i.get(source), durationOfCopy.sample(level.getRandom()));
            return true;
        }));
    }
}

