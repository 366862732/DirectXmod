/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;

public class TriggerGate {
    public static <E extends LivingEntity> OneShot<E> triggerOneShuffled(List<Pair<? extends Trigger<? super E>, Integer>> weightedTriggers) {
        return TriggerGate.triggerGate(weightedTriggers, GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE);
    }

    public static <E extends LivingEntity> OneShot<E> triggerGate(List<Pair<? extends Trigger<? super E>, Integer>> weightedBehaviors, GateBehavior.OrderPolicy orderPolicy, GateBehavior.RunningPolicy runningPolicy) {
        ShufflingList behaviors = new ShufflingList();
        weightedBehaviors.forEach(entry -> behaviors.add((Trigger)entry.getFirst(), (Integer)entry.getSecond()));
        return BehaviorBuilder.create(i -> i.point((level, body, timestamp) -> {
            Trigger behavior;
            if (orderPolicy == GateBehavior.OrderPolicy.SHUFFLED) {
                behaviors.shuffle();
            }
            Iterator i$ = behaviors.iterator();
            while (i$.hasNext() && (!(behavior = (Trigger)i$.next()).trigger(level, body, timestamp) || runningPolicy != GateBehavior.RunningPolicy.RUN_ONE)) {
            }
            return true;
        }));
    }
}

