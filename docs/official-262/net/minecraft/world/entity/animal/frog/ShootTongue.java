/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 */
package net.minecraft.world.entity.animal.frog;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.pathfinder.Path;

public class ShootTongue
extends Behavior<Frog> {
    public static final int TIME_OUT_DURATION = 100;
    public static final int CATCH_ANIMATION_DURATION = 6;
    public static final int TONGUE_ANIMATION_DURATION = 10;
    private static final float EATING_DISTANCE = 1.75f;
    private static final float EATING_MOVEMENT_FACTOR = 0.75f;
    public static final int UNREACHABLE_TONGUE_TARGETS_COOLDOWN_DURATION = 100;
    public static final int MAX_UNREACHBLE_TONGUE_TARGETS_IN_MEMORY = 5;
    private int eatAnimationTimer;
    private int calculatePathCounter;
    private final SoundEvent tongueSound;
    private final SoundEvent eatSound;
    private State state = State.DONE;

    public ShootTongue(SoundEvent tongueSound, SoundEvent eatSound) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.ATTACK_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT)), 100);
        this.tongueSound = tongueSound;
        this.eatSound = eatSound;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Frog body) {
        LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        boolean canPathfindToTarget = this.canPathfindToTarget(body, target);
        if (!canPathfindToTarget) {
            body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            this.addUnreachableTargetToMemory(body, target);
        }
        return canPathfindToTarget && body.getPose() != Pose.CROAKING && Frog.canEat(target);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Frog body, long timestamp) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && this.state != State.DONE && !body.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
    }

    @Override
    protected void start(ServerLevel level, Frog body, long timestamp) {
        LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        BehaviorUtils.lookAtEntity(body, target);
        body.setTongueTarget(target);
        body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.position(), 2.0f, 0));
        this.calculatePathCounter = 10;
        this.state = State.MOVE_TO_TARGET;
    }

    @Override
    protected void stop(ServerLevel level, Frog body, long timestamp) {
        body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        body.eraseTongueTarget();
        body.setPose(Pose.STANDING);
    }

    private void eatEntity(ServerLevel level, Frog body) {
        Entity target;
        level.playSound(null, body, this.eatSound, SoundSource.NEUTRAL, 2.0f, 1.0f);
        Optional<Entity> tongueTarget = body.getTongueTarget();
        if (tongueTarget.isPresent() && (target = tongueTarget.get()).isAlive()) {
            body.doHurtTarget(level, target);
            if (!target.isAlive()) {
                target.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected void tick(ServerLevel level, Frog body, long timestamp) {
        LivingEntity target = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        body.setTongueTarget(target);
        switch (this.state.ordinal()) {
            case 0: {
                if (target.distanceTo(body) < 1.75f) {
                    level.playSound(null, body, this.tongueSound, SoundSource.NEUTRAL, 2.0f, 1.0f);
                    body.setPose(Pose.USING_TONGUE);
                    target.setDeltaMovement(target.position().vectorTo(body.position()).normalize().scale(0.75));
                    this.eatAnimationTimer = 0;
                    this.state = State.CATCH_ANIMATION;
                    break;
                }
                if (this.calculatePathCounter <= 0) {
                    body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.position(), 2.0f, 0));
                    this.calculatePathCounter = 10;
                    break;
                }
                --this.calculatePathCounter;
                break;
            }
            case 1: {
                if (this.eatAnimationTimer++ < 6) break;
                this.state = State.EAT_ANIMATION;
                this.eatEntity(level, body);
                break;
            }
            case 2: {
                if (this.eatAnimationTimer >= 10) {
                    this.state = State.DONE;
                    break;
                }
                ++this.eatAnimationTimer;
                break;
            }
        }
    }

    private boolean canPathfindToTarget(Frog body, LivingEntity target) {
        Path path = body.getNavigation().createPath(target, 0);
        return path != null && path.getDistToTarget() < 1.75f;
    }

    private void addUnreachableTargetToMemory(Frog body, LivingEntity entity) {
        boolean shouldAddUnreachableTarget;
        List unreachableTargets = body.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
        boolean bl = shouldAddUnreachableTarget = !unreachableTargets.contains(entity.getUUID());
        if (unreachableTargets.size() == 5 && shouldAddUnreachableTarget) {
            unreachableTargets.remove(0);
        }
        if (shouldAddUnreachableTarget) {
            unreachableTargets.add(entity.getUUID());
        }
        body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS, unreachableTargets, 100L);
    }

    private static enum State {
        MOVE_TO_TARGET,
        CATCH_ANIMATION,
        EAT_ANIMATION,
        DONE;

    }
}

