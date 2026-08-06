/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.sensing;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

public class GolemSensor
extends Sensor<LivingEntity> {
    private static final int GOLEM_SCAN_RATE = 200;
    private static final int MEMORY_TIME_TO_LIVE = 599;

    public GolemSensor() {
        this(200);
    }

    public GolemSensor(int scanRate) {
        super(scanRate);
    }

    @Override
    protected void doTick(ServerLevel level, LivingEntity body) {
        GolemSensor.checkForNearbyGolem(body);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    }

    public static void checkForNearbyGolem(LivingEntity body) {
        Optional<List<LivingEntity>> livingEntitiesMemory = body.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (livingEntitiesMemory.isEmpty()) {
            return;
        }
        boolean golemPresent = livingEntitiesMemory.get().stream().anyMatch(entity -> entity.is(EntityTypes.IRON_GOLEM));
        if (golemPresent) {
            GolemSensor.golemDetected(body);
        }
    }

    public static void golemDetected(LivingEntity body) {
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 599L);
    }
}

