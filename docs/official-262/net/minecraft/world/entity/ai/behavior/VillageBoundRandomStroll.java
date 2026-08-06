/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;

    public static OneShot<PathfinderMob> create(float speedModifier) {
        return VillageBoundRandomStroll.create(speedModifier, 10, 7);
    }

    public static OneShot<PathfinderMob> create(float speedModifier, int maxXyDist, int maxYDist) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, walkTarget -> (level, body, timestamp) -> {
            SectionPos sectionPos;
            SectionPos optimalSectionPos;
            BlockPos bodyPos = body.blockPosition();
            Vec3 landPos = level.isVillage(bodyPos) ? LandRandomPos.getPos(body, maxXyDist, maxYDist) : ((optimalSectionPos = BehaviorUtils.findSectionClosestToVillage(level, sectionPos = SectionPos.of(bodyPos), 2)) != sectionPos ? DefaultRandomPos.getPosTowards(body, maxXyDist, maxYDist, Vec3.atBottomCenterOf(optimalSectionPos.center()), 1.5707963705062866) : LandRandomPos.getPos(body, maxXyDist, maxYDist));
            walkTarget.setOrErase(Optional.ofNullable(landPos).map(pos -> new WalkTarget((Vec3)pos, speedModifier, 0)));
            return true;
        }));
    }
}

