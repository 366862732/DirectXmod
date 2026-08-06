/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite {
    public static BehaviorControl<Villager> create(float speedModifier) {
        return BehaviorBuilder.create(i -> i.group(i.present(MemoryModuleType.POTENTIAL_JOB_SITE), i.absent(MemoryModuleType.JOB_SITE), i.present(MemoryModuleType.NEAREST_LIVING_ENTITIES), i.registered(MemoryModuleType.WALK_TARGET), i.registered(MemoryModuleType.LOOK_TARGET)).apply((Applicative)i, (potentialJob, jobSite, nearestEntities, walkTarget, lookTarget) -> (level, body, timestamp) -> {
            if (body.isBaby()) {
                return false;
            }
            if (!body.getVillagerData().profession().is(VillagerProfession.NONE)) {
                return false;
            }
            BlockPos poiPos = ((GlobalPos)i.get(potentialJob)).pos();
            Optional<Holder<PoiType>> poiType = level.getPoiManager().getType(poiPos);
            if (poiType.isEmpty()) {
                return true;
            }
            ((List)i.get(nearestEntities)).stream().filter(v -> v instanceof Villager && v != body).map(v -> (Villager)v).filter(LivingEntity::isAlive).filter(v -> YieldJobSite.nearbyWantsJobsite((Holder)poiType.get(), v, poiPos)).findFirst().ifPresent(nearbyVillager -> {
                walkTarget.erase();
                lookTarget.erase();
                potentialJob.erase();
                if (nearbyVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()) {
                    BehaviorUtils.setWalkAndLookTargetMemories((LivingEntity)nearbyVillager, poiPos, speedModifier, 1);
                    nearbyVillager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(level.dimension(), poiPos));
                    level.debugSynchronizers().updatePoi(poiPos);
                }
            });
            return true;
        }));
    }

    private static boolean nearbyWantsJobsite(Holder<PoiType> type, Villager nearbyVillager, BlockPos poiPos) {
        boolean nearbyHasPotentialJobSite = nearbyVillager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
        if (nearbyHasPotentialJobSite) {
            return false;
        }
        Optional<GlobalPos> nearbyVillagerJobSiteMemory = nearbyVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        Holder<VillagerProfession> nearbyProfession = nearbyVillager.getVillagerData().profession();
        if (nearbyProfession.value().heldJobSite().test(type)) {
            if (nearbyVillagerJobSiteMemory.isEmpty()) {
                return YieldJobSite.canReachPos(nearbyVillager, poiPos, type.value());
            }
            return nearbyVillagerJobSiteMemory.get().pos().equals(poiPos);
        }
        return false;
    }

    private static boolean canReachPos(PathfinderMob nearbyVillager, BlockPos poiPos, PoiType type) {
        Path path = nearbyVillager.getNavigation().createPath(poiPos, type.validRange());
        return path != null && path.canReach();
    }
}

