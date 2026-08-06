/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class LocateHidingPlace {
    public static OneShot<LivingEntity> create(int radius, float speedModifier, int closeEnoughDist) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.HOME), i.registered(MemoryModuleType.HIDING_PLACE), i.registered(MemoryModuleType.PATH), i.registered(MemoryModuleType.LOOK_TARGET), i.registered(MemoryModuleType.BREED_TARGET), i.registered(MemoryModuleType.INTERACTION_TARGET)).apply((Applicative)i, (walkTarget, home, hidingPlace, path, lookTarget, breedTarget, interactionTarget) -> (level, body, timestamp) -> {
            level.getPoiManager().find(p -> p.is(PoiTypes.HOME), blockPos -> true, body.blockPosition(), closeEnoughDist + 1, PoiManager.Occupancy.ANY).filter(p -> p.closerToCenterThan(body.position(), closeEnoughDist)).or(() -> level.getPoiManager().getRandom(p -> p.is(PoiTypes.HOME), blockPos -> true, PoiManager.Occupancy.ANY, body.blockPosition(), radius, body.getRandom())).or(() -> i.tryGet(home).map(GlobalPos::pos)).ifPresent(pos -> {
                path.erase();
                lookTarget.erase();
                breedTarget.erase();
                interactionTarget.erase();
                hidingPlace.set(GlobalPos.of(level.dimension(), pos));
                if (!pos.closerToCenterThan(body.position(), closeEnoughDist)) {
                    walkTarget.set(new WalkTarget((BlockPos)pos, speedModifier, closeEnoughDist));
                }
            });
            return true;
        }));
    }
}

