/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

public class LookAndFollowTradingPlayerSink
extends Behavior<Villager> {
    private final float speedModifier;

    public LookAndFollowTradingPlayerSink(float speedModifier) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED)), Integer.MAX_VALUE);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        Player tradingPlayer = body.getTradingPlayer();
        return body.isAlive() && tradingPlayer != null && !body.isInWater() && !body.hurtMarked && body.distanceToSqr(tradingPlayer) <= 16.0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return this.checkExtraStartConditions(level, body);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        this.followPlayer(body);
    }

    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        Brain<Villager> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        this.followPlayer(body);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    private void followPlayer(Villager body) {
        Brain<Villager> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(body.getTradingPlayer(), false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(body.getTradingPlayer(), true));
    }
}

