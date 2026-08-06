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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class LongJumpMidJump
extends Behavior<Mob> {
    public static final int TIME_OUT_DURATION = 100;
    private final UniformInt timeBetweenLongJumps;
    private final SoundEvent landingSound;

    public LongJumpMidJump(UniformInt timeBetweenLongJumps, SoundEvent landingSound) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.LONG_JUMP_MID_JUMP, (Object)((Object)MemoryStatus.VALUE_PRESENT)), 100);
        this.timeBetweenLongJumps = timeBetweenLongJumps;
        this.landingSound = landingSound;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        return !body.onGround();
    }

    @Override
    protected void start(ServerLevel level, Mob body, long timestamp) {
        body.setDiscardFriction(true);
        body.setPose(Pose.LONG_JUMPING);
    }

    @Override
    protected void stop(ServerLevel level, Mob body, long timestamp) {
        if (body.onGround()) {
            body.setDeltaMovement(body.getDeltaMovement().multiply(0.1f, 1.0, 0.1f));
            level.playSound(null, body, this.landingSound, SoundSource.NEUTRAL, 2.0f, 1.0f);
        }
        body.setDiscardFriction(false);
        body.setPose(Pose.STANDING);
        body.getBrain().eraseMemory(MemoryModuleType.LONG_JUMP_MID_JUMP);
        body.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(level.getRandom()));
    }
}

