/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 *  org.apache.commons.lang3.mutable.MutableInt
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.apache.commons.lang3.mutable.MutableInt;

public class SetHiddenState {
    private static final int HIDE_TIMEOUT = 300;

    public static BehaviorControl<LivingEntity> create(int seconds, int closeEnoughDist) {
        int stayHiddenTicks = seconds * 20;
        MutableInt ticksHidden = new MutableInt(0);
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.HIDING_PLACE), i.present(MemoryModuleType.HEARD_BELL_TIME)).apply((Applicative)i, (hidingPlace, heardBellTime) -> (level, body, timestamp) -> {
            boolean timedOutTryingToHide;
            long timeTriggered = (Long)i.get(heardBellTime);
            boolean bl = timedOutTryingToHide = timeTriggered + 300L <= timestamp;
            if (ticksHidden.intValue() > stayHiddenTicks || timedOutTryingToHide) {
                heardBellTime.erase();
                hidingPlace.erase();
                body.getBrain().updateActivityFromSchedule(level.environmentAttributes(), level.getGameTime(), body.position());
                ticksHidden.setValue(0);
                return true;
            }
            BlockPos hidePos = ((GlobalPos)i.get(hidingPlace)).pos();
            if (hidePos.closerThan(body.blockPosition(), closeEnoughDist)) {
                ticksHidden.increment();
            }
            return true;
        }));
    }
}

