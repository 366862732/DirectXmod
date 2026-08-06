/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.jspecify.annotations.Nullable;

public class JumpOnBed
extends Behavior<Mob> {
    private static final int MAX_TIME_TO_REACH_BED = 100;
    private static final int MIN_JUMPS = 3;
    private static final int MAX_JUMPS = 6;
    private static final int COOLDOWN_BETWEEN_JUMPS = 5;
    private final float speedModifier;
    private @Nullable BlockPos targetBed;
    private int remainingTimeToReachBed;
    private int remainingJumps;
    private int remainingCooldownUntilNextJump;

    public JumpOnBed(float speedModifier) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.NEAREST_BED, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT)));
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Mob body) {
        return body.isBaby() && this.nearBed(level, body);
    }

    @Override
    protected void start(ServerLevel level, Mob body, long timestamp) {
        super.start(level, body, timestamp);
        this.getNearestBed(body).ifPresent(targetBed -> {
            this.targetBed = targetBed;
            this.remainingTimeToReachBed = 100;
            this.remainingJumps = 3 + level.getRandom().nextInt(4);
            this.remainingCooldownUntilNextJump = 0;
            this.startWalkingTowardsBed(body, (BlockPos)targetBed);
        });
    }

    @Override
    protected void stop(ServerLevel level, Mob body, long timestamp) {
        super.stop(level, body, timestamp);
        this.targetBed = null;
        this.remainingTimeToReachBed = 0;
        this.remainingJumps = 0;
        this.remainingCooldownUntilNextJump = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Mob body, long timestamp) {
        return body.isBaby() && this.targetBed != null && this.isBed(level, this.targetBed) && !this.tiredOfWalking(level, body) && !this.tiredOfJumping(level, body);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, Mob body, long timestamp) {
        if (!this.onOrOverBed(level, body)) {
            --this.remainingTimeToReachBed;
            return;
        }
        if (this.remainingCooldownUntilNextJump > 0) {
            --this.remainingCooldownUntilNextJump;
            return;
        }
        if (this.onBedSurface(level, body)) {
            body.getJumpControl().jump();
            --this.remainingJumps;
            this.remainingCooldownUntilNextJump = 5;
        }
    }

    private void startWalkingTowardsBed(Mob body, BlockPos bedPos) {
        body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bedPos, this.speedModifier, 0));
    }

    private boolean nearBed(ServerLevel level, Mob body) {
        return this.onOrOverBed(level, body) || this.getNearestBed(body).isPresent();
    }

    private boolean onOrOverBed(ServerLevel level, Mob body) {
        BlockPos bodyPos = body.blockPosition();
        BlockPos oneBelow = bodyPos.below();
        return this.isBed(level, bodyPos) || this.isBed(level, oneBelow);
    }

    private boolean onBedSurface(ServerLevel level, Mob body) {
        return this.isBed(level, body.blockPosition());
    }

    private boolean isBed(ServerLevel level, BlockPos bodyPos) {
        return level.getBlockState(bodyPos).is(BlockTags.BEDS);
    }

    private Optional<BlockPos> getNearestBed(Mob body) {
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_BED);
    }

    private boolean tiredOfWalking(ServerLevel level, Mob body) {
        return !this.onOrOverBed(level, body) && this.remainingTimeToReachBed <= 0;
    }

    private boolean tiredOfJumping(ServerLevel level, Mob body) {
        return this.onOrOverBed(level, body) && this.remainingJumps <= 0;
    }
}

