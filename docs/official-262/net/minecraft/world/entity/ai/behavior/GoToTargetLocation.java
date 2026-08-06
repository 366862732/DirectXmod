/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class GoToTargetLocation {
    private static BlockPos getNearbyPos(Mob body, BlockPos pos) {
        RandomSource random = body.level().getRandom();
        return pos.offset(GoToTargetLocation.getRandomOffset(random), 0, GoToTargetLocation.getRandomOffset(random));
    }

    private static int getRandomOffset(RandomSource random) {
        return random.nextInt(3) - 1;
    }

    public static <E extends Mob> OneShot<E> create(MemoryModuleType<BlockPos> locationMemory, int closeEnoughDist, float speedModifier) {
        return BehaviorBuilder.create(i -> i.group(i.present(locationMemory), i.absent(MemoryModuleType.ATTACK_TARGET), i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET)).apply((Applicative)i, (location, attackTarget, walkTarget, lookTarget) -> (level, body, timestamp) -> {
            BlockPos celebrateLocation = (BlockPos)i.get(location);
            boolean closeEnoughToTarget = celebrateLocation.closerThan(body.blockPosition(), closeEnoughDist);
            if (!closeEnoughToTarget) {
                BehaviorUtils.setWalkAndLookTargetMemories(body, GoToTargetLocation.getNearbyPos(body, celebrateLocation), speedModifier, closeEnoughDist);
            }
            return true;
        }));
    }
}

