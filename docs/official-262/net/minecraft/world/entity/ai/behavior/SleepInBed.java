/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SleepInBed
extends Behavior<LivingEntity> {
    public static final int COOLDOWN_AFTER_BEING_WOKEN = 100;
    private long nextOkStartTime;

    public SleepInBed() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.HOME, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.LAST_WOKEN, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.LAST_SLEPT, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, (Object)((Object)MemoryStatus.REGISTERED)));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, LivingEntity body) {
        long timeSinceLastWoken;
        if (body.isPassenger()) {
            return false;
        }
        Brain<? extends LivingEntity> brain = body.getBrain();
        GlobalPos target = brain.getMemory(MemoryModuleType.HOME).get();
        if (level.dimension() != target.dimension()) {
            return false;
        }
        Optional<Long> lastWokenMemory = brain.getMemory(MemoryModuleType.LAST_WOKEN);
        if (lastWokenMemory.isPresent() && (timeSinceLastWoken = level.getGameTime() - lastWokenMemory.get()) > 0L && timeSinceLastWoken < 100L) {
            return false;
        }
        BlockState blockState = level.getBlockState(target.pos());
        return target.pos().closerToCenterThan(body.position(), 2.0) && blockState.is(BlockTags.BEDS) && blockState.getValue(BedBlock.OCCUPIED) == false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, LivingEntity body, long timestamp) {
        Optional<GlobalPos> memory = body.getBrain().getMemory(MemoryModuleType.HOME);
        if (memory.isEmpty()) {
            return false;
        }
        BlockPos bedPos = memory.get().pos();
        return body.getBrain().isActive(Activity.REST) && body.getY() > (double)bedPos.getY() + 0.4 && bedPos.closerToCenterThan(body.position(), 1.14);
    }

    @Override
    protected void start(ServerLevel level, LivingEntity body, long timestamp) {
        if (timestamp > this.nextOkStartTime) {
            Brain<? extends LivingEntity> brain = body.getBrain();
            if (brain.hasMemoryValue(MemoryModuleType.DOORS_TO_CLOSE)) {
                Set<GlobalPos> doors = brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get();
                Optional<List<LivingEntity>> nearestEntities = brain.hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES) ? brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES) : Optional.empty();
                InteractWithDoor.closeDoorsThatIHaveOpenedOrPassedThrough(level, body, null, null, doors, nearestEntities);
            }
            body.startSleeping(body.getBrain().getMemory(MemoryModuleType.HOME).get().pos());
            brain.setMemory(MemoryModuleType.LAST_SLEPT, timestamp);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    @Override
    protected void stop(ServerLevel level, LivingEntity body, long timestamp) {
        if (body.isSleeping()) {
            body.stopSleeping();
            this.nextOkStartTime = timestamp + 40L;
        }
    }
}

