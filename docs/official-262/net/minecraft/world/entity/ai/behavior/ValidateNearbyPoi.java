/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Applicative
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.kinds.Applicative;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ValidateNearbyPoi {
    private static final int MAX_DISTANCE = 16;

    public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> memoryType) {
        return BehaviorBuilder.create(i -> i.group(i.present(memoryType)).apply((Applicative)i, memory -> (level, body, timestamp) -> {
            GlobalPos globalPos = (GlobalPos)i.get(memory);
            BlockPos pos = globalPos.pos();
            if (level.dimension() != globalPos.dimension() || !pos.closerToCenterThan(body.position(), 16.0)) {
                return false;
            }
            ServerLevel poiLevel = level.getServer().getLevel(globalPos.dimension());
            if (poiLevel == null || !poiLevel.getPoiManager().exists(pos, poiType)) {
                memory.erase();
            } else if (ValidateNearbyPoi.bedIsOccupied(poiLevel, pos, body)) {
                memory.erase();
                if (!ValidateNearbyPoi.bedIsOccupiedByVillager(poiLevel, pos)) {
                    level.getPoiManager().release(pos);
                    level.debugSynchronizers().updatePoi(pos);
                }
            }
            return true;
        }));
    }

    private static boolean bedIsOccupied(ServerLevel poiLevel, BlockPos poiPos, LivingEntity body) {
        BlockState blockState = poiLevel.getBlockState(poiPos);
        return blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) != false && !body.isSleeping();
    }

    private static boolean bedIsOccupiedByVillager(ServerLevel poiLevel, BlockPos poiPos) {
        List<Villager> villagers = poiLevel.getEntitiesOfClass(Villager.class, new AABB(poiPos), LivingEntity::isSleeping);
        return !villagers.isEmpty();
    }
}

