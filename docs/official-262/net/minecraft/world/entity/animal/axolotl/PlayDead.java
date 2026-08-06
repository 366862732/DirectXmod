/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.axolotl.Axolotl;

public class PlayDead
extends Behavior<Axolotl> {
    public PlayDead() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.PLAY_DEAD_TICKS, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.HURT_BY_ENTITY, (Object)((Object)MemoryStatus.VALUE_PRESENT)), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Axolotl body) {
        return body.isInWater();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Axolotl body, long timestamp) {
        return body.isInWater() && body.getBrain().hasMemoryValue(MemoryModuleType.PLAY_DEAD_TICKS);
    }

    @Override
    protected void start(ServerLevel level, Axolotl body, long timestamp) {
        Brain<Axolotl> brain = body.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        body.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
    }
}

