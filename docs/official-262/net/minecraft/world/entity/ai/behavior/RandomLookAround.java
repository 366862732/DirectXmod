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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

public class RandomLookAround
extends Behavior<Mob> {
    private final IntProvider interval;
    private final float maxYaw;
    private final float minPitch;
    private final float pitchRange;

    public RandomLookAround(IntProvider interval, float maxYaw, float minPitch, float maxPitch) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.GAZE_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT)));
        if (minPitch > maxPitch) {
            throw new IllegalArgumentException("Minimum pitch is larger than maximum pitch! " + minPitch + " > " + maxPitch);
        }
        this.interval = interval;
        this.maxYaw = maxYaw;
        this.minPitch = minPitch;
        this.pitchRange = maxPitch - minPitch;
    }

    @Override
    protected void start(ServerLevel level, Mob body, long timestamp) {
        RandomSource random = body.getRandom();
        float pitch = Mth.clamp(random.nextFloat() * this.pitchRange + this.minPitch, -90.0f, 90.0f);
        float rotation = Mth.wrapDegrees(body.getYRot() + 2.0f * random.nextFloat() * this.maxYaw - this.maxYaw);
        Vec3 newLookVec = Vec3.directionFromRotation(pitch, rotation);
        body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(body.getEyePosition().add(newLookVec)));
        body.getBrain().setMemory(MemoryModuleType.GAZE_COOLDOWN_TICKS, this.interval.sample(random));
    }
}

