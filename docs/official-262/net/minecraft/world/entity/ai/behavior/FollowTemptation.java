/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation
extends Behavior<PathfinderMob> {
    public static final int TEMPTATION_COOLDOWN = 100;
    public static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.5;
    public static final double BACKED_UP_CLOSE_ENOUGH_DIST = 3.5;
    private final Function<LivingEntity, Float> speedModifier;
    private final Function<LivingEntity, Double> closeEnoughDistance;
    private final boolean lookInTheEyes;

    public FollowTemptation(Function<LivingEntity, Float> speedModifier) {
        this(speedModifier, entity -> 2.5);
    }

    public FollowTemptation(Function<LivingEntity, Float> speedModifier, Function<LivingEntity, Double> closeEnoughDistance) {
        this(speedModifier, closeEnoughDistance, false);
    }

    public FollowTemptation(Function<LivingEntity, Float> speedModifier, Function<LivingEntity, Double> closeEnoughDistance, boolean lookInTheEyes) {
        super((Map)Util.make(() -> {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            builder.put(MemoryModuleType.LOOK_TARGET, (Object)MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.WALK_TARGET, (Object)MemoryStatus.REGISTERED);
            builder.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, (Object)MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.IS_TEMPTED, (Object)MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.TEMPTING_PLAYER, (Object)MemoryStatus.VALUE_PRESENT);
            builder.put(MemoryModuleType.BREED_TARGET, (Object)MemoryStatus.VALUE_ABSENT);
            builder.put(MemoryModuleType.IS_PANICKING, (Object)MemoryStatus.VALUE_ABSENT);
            return builder.build();
        }));
        this.speedModifier = speedModifier;
        this.closeEnoughDistance = closeEnoughDistance;
        this.lookInTheEyes = lookInTheEyes;
    }

    protected float getSpeedModifier(PathfinderMob body) {
        return this.speedModifier.apply(body).floatValue();
    }

    private Optional<Player> getTemptingPlayer(PathfinderMob body) {
        return body.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PathfinderMob body, long timestamp) {
        return this.getTemptingPlayer(body).isPresent() && !body.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET) && !body.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    @Override
    protected void start(ServerLevel level, PathfinderMob body, long timestamp) {
        body.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
    }

    @Override
    protected void stop(ServerLevel level, PathfinderMob body, long timestamp) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
        brain.eraseMemory(MemoryModuleType.IS_TEMPTED);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, PathfinderMob body, long timestamp) {
        Player player = this.getTemptingPlayer(body).get();
        Brain<? extends LivingEntity> brain = body.getBrain();
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
        double closeEnough = this.closeEnoughDistance.apply(body);
        if (body.distanceToSqr(player) < Mth.square(closeEnough)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        } else {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(player, this.lookInTheEyes, this.lookInTheEyes), this.getSpeedModifier(body), 2));
        }
    }
}

