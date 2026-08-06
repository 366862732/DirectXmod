/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class GoToClosestVillage {
    public static BehaviorControl<Villager> create(float speedModifier, int closeEnoughDistance) {
        return BehaviorBuilder.create(i -> i.group(i.absent(MemoryModuleType.WALK_TARGET)).apply((Applicative)i, walkTarget -> (level, body, timestamp) -> {
            if (level.isVillage(body.blockPosition())) {
                return false;
            }
            PoiManager poiManager = level.getPoiManager();
            int sectionsToVillage = poiManager.sectionsToVillage(SectionPos.of(body.blockPosition()));
            Vec3 targetPos = null;
            for (int j = 0; j < 5; ++j) {
                Vec3 landPos = LandRandomPos.getPos(body, 15, 7, p -> -poiManager.sectionsToVillage(SectionPos.of(p)));
                if (landPos == null) continue;
                int landPosSectionsToVillage = poiManager.sectionsToVillage(SectionPos.of(BlockPos.containing(landPos)));
                if (landPosSectionsToVillage < sectionsToVillage) {
                    targetPos = landPos;
                    break;
                }
                if (landPosSectionsToVillage != sectionsToVillage) continue;
                targetPos = landPos;
            }
            if (targetPos != null) {
                walkTarget.set(new WalkTarget(targetPos, speedModifier, closeEnoughDistance));
            }
            return true;
        }));
    }
}

