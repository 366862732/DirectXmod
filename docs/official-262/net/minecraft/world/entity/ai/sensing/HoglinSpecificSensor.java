/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Lists
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;

public class HoglinSpecificSensor
extends Sensor<Hoglin> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, (Object[])new MemoryModuleType[0]);
    }

    @Override
    protected void doTick(ServerLevel level, Hoglin body) {
        Brain<Hoglin> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, this.findNearestRepellent(level, body));
        Optional<Object> adultPiglin = Optional.empty();
        int adultPiglinCount = 0;
        ArrayList adultHoglins = Lists.newArrayList();
        NearestVisibleLivingEntities visibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());
        for (LivingEntity entity2 : visibleLivingEntities.findAll(entity -> !entity.isBaby() && (entity instanceof Piglin || entity instanceof Hoglin))) {
            if (entity2 instanceof Piglin) {
                Piglin piglin = (Piglin)entity2;
                ++adultPiglinCount;
                if (adultPiglin.isEmpty()) {
                    adultPiglin = Optional.of(piglin);
                }
            }
            if (!(entity2 instanceof Hoglin)) continue;
            Hoglin hoglin = (Hoglin)entity2;
            adultHoglins.add(hoglin);
        }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, adultPiglin);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, adultHoglins);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, adultPiglinCount);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, adultHoglins.size());
    }

    private Optional<BlockPos> findNearestRepellent(ServerLevel level, Hoglin body) {
        return BlockPos.findClosestMatch(body.blockPosition(), 8, 4, pos -> level.getBlockState((BlockPos)pos).is(BlockTags.HOGLIN_REPELLENTS));
    }
}

