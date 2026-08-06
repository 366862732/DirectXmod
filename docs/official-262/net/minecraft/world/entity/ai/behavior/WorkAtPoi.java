/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;

public class WorkAtPoi
extends Behavior<Villager> {
    private static final int CHECK_COOLDOWN = 300;
    private static final double DISTANCE = 1.73;
    private long lastCheck;

    public WorkAtPoi() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.JOB_SITE, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED)));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        if (level.getGameTime() - this.lastCheck < 300L) {
            return false;
        }
        if (level.getRandom().nextInt(2) != 0) {
            return false;
        }
        this.lastCheck = level.getGameTime();
        GlobalPos target = body.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
        return target.dimension() == level.dimension() && target.pos().closerToCenterThan(body.position(), 1.73);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        Brain<Villager> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.LAST_WORKED_AT_POI, timestamp);
        brain.getMemory(MemoryModuleType.JOB_SITE).ifPresent(globalPos -> brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(globalPos.pos())));
        body.playWorkSound();
        this.useWorkstation(level, body);
        if (body.shouldRestock(level)) {
            body.restock();
        }
    }

    protected void useWorkstation(ServerLevel level, Villager body) {
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        Optional<GlobalPos> jobSiteMemory = body.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (jobSiteMemory.isEmpty()) {
            return false;
        }
        GlobalPos target = jobSiteMemory.get();
        return target.dimension() == level.dimension() && target.pos().closerToCenterThan(body.position(), 1.73);
    }
}

