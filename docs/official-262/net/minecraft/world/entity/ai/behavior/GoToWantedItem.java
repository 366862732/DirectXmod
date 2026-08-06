/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem {
    public static BehaviorControl<LivingEntity> create(float speedModifier, boolean interruptOngoingWalk, int maxDistToWalk) {
        return GoToWantedItem.create(body -> true, speedModifier, interruptOngoingWalk, maxDistToWalk);
    }

    public static <E extends LivingEntity> BehaviorControl<E> create(Predicate<E> predicate, float speedModifier, boolean interruptOngoingWalk, int maxDistToWalk) {
        return BehaviorBuilder.create(i -> {
            BehaviorBuilder walkCondition = interruptOngoingWalk ? i.registered(MemoryModuleType.WALK_TARGET) : i.absent(MemoryModuleType.WALK_TARGET);
            return i.group(i.registered(MemoryModuleType.LOOK_TARGET), walkCondition, i.present(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM), i.registered(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)).apply((Applicative)i, (lookTarget, walkTarget, wantedItem, cooldown) -> (level, body, timestamp) -> {
                ItemEntity item = (ItemEntity)i.get(wantedItem);
                if (i.tryGet(cooldown).isEmpty() && predicate.test(body) && item.closerThan(body, maxDistToWalk) && body.level().getWorldBorder().isWithinBounds(item.blockPosition()) && body.canPickUpLoot()) {
                    WalkTarget target = new WalkTarget(new EntityTracker(item, false), speedModifier, 0);
                    lookTarget.set(new EntityTracker(item, true));
                    walkTarget.set(target);
                    return true;
                }
                return false;
            });
        });
    }
}

