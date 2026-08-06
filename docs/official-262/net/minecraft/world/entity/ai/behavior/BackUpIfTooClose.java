/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BackUpIfTooClose {
    public static OneShot<Mob> create(int tooCloseDistance, float strafeSpeed) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET), i.present(MemoryModuleType.ATTACK_TARGET), i.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)).apply((Applicative)i, (walkTarget, lookTarget, attackTarget, nearestVisible) -> (level, body, timestamp) -> {
            LivingEntity target = (LivingEntity)i.get(attackTarget);
            if (target.closerThan(body, tooCloseDistance) && ((NearestVisibleLivingEntities)i.get(nearestVisible)).contains(target)) {
                lookTarget.set(new EntityTracker(target, true));
                body.getMoveControl().strafe(-strafeSpeed, 0.0f);
                body.setYRot(Mth.rotateIfNecessary(body.getYRot(), body.yHeadRot, 0.0f));
                return true;
            }
            return false;
        }));
    }
}

