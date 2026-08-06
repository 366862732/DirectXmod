/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestVisibleLivingEntitySensor;

public class VillagerHostilesSensor
extends NearestVisibleLivingEntitySensor {
    private static final ImmutableMap<EntityType<?>, Float> ACCEPTABLE_DISTANCE_FROM_HOSTILES = ImmutableMap.builder().put(EntityTypes.DROWNED, (Object)Float.valueOf(8.0f)).put(EntityTypes.EVOKER, (Object)Float.valueOf(12.0f)).put(EntityTypes.HUSK, (Object)Float.valueOf(8.0f)).put(EntityTypes.ILLUSIONER, (Object)Float.valueOf(12.0f)).put(EntityTypes.PILLAGER, (Object)Float.valueOf(15.0f)).put(EntityTypes.RAVAGER, (Object)Float.valueOf(12.0f)).put(EntityTypes.VEX, (Object)Float.valueOf(8.0f)).put(EntityTypes.VINDICATOR, (Object)Float.valueOf(10.0f)).put(EntityTypes.ZOGLIN, (Object)Float.valueOf(10.0f)).put(EntityTypes.ZOMBIE, (Object)Float.valueOf(8.0f)).put(EntityTypes.ZOMBIE_VILLAGER, (Object)Float.valueOf(8.0f)).build();

    @Override
    protected boolean isMatchingEntity(ServerLevel level, LivingEntity body, LivingEntity mob) {
        return this.isHostile(mob) && this.isClose(body, mob);
    }

    private boolean isClose(LivingEntity body, LivingEntity mob) {
        float distThreshold = ((Float)ACCEPTABLE_DISTANCE_FROM_HOSTILES.get(mob.getType())).floatValue();
        return mob.distanceToSqr(body) <= (double)(distThreshold * distThreshold);
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemoryToSet() {
        return MemoryModuleType.NEAREST_HOSTILE;
    }

    private boolean isHostile(LivingEntity entity) {
        return ACCEPTABLE_DISTANCE_FROM_HOSTILES.containsKey(entity.getType());
    }
}

