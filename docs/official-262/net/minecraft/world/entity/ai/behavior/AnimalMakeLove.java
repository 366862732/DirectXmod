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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.Animal;

public class AnimalMakeLove
extends Behavior<Animal> {
    private static final int BREED_RANGE = 3;
    private static final int MIN_DURATION = 60;
    private static final int MAX_DURATION = 110;
    private final EntityType<? extends Animal> partnerType;
    private final float speedModifier;
    private final int closeEnoughDistance;
    private static final int DEFAULT_CLOSE_ENOUGH_DISTANCE = 2;
    private long spawnChildAtTime;

    public AnimalMakeLove(EntityType<? extends Animal> partnerType) {
        this(partnerType, 1.0f, 2);
    }

    public AnimalMakeLove(EntityType<? extends Animal> partnerType, float speedModifier, int closeEnoughDistance) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.BREED_TARGET, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.WALK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.LOOK_TARGET, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT)), 110);
        this.partnerType = partnerType;
        this.speedModifier = speedModifier;
        this.closeEnoughDistance = closeEnoughDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Animal body) {
        return body.isInLove() && this.findValidBreedPartner(body).isPresent();
    }

    @Override
    protected void start(ServerLevel level, Animal body, long timestamp) {
        Animal partner = this.findValidBreedPartner(body).get();
        body.getBrain().setMemory(MemoryModuleType.BREED_TARGET, partner);
        partner.getBrain().setMemory(MemoryModuleType.BREED_TARGET, body);
        BehaviorUtils.lockGazeAndWalkToEachOther(body, partner, this.speedModifier, this.closeEnoughDistance);
        int duration = 60 + body.getRandom().nextInt(50);
        this.spawnChildAtTime = timestamp + (long)duration;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Animal body, long timestamp) {
        if (!this.hasBreedTargetOfRightType(body)) {
            return false;
        }
        Animal partner = this.getBreedTarget(body);
        return partner.isAlive() && body.canMate(partner) && BehaviorUtils.entityIsVisible(body.getBrain(), partner) && timestamp <= this.spawnChildAtTime && !body.isPanicking() && !partner.isPanicking();
    }

    @Override
    protected void tick(ServerLevel level, Animal body, long timestamp) {
        Animal partner = this.getBreedTarget(body);
        BehaviorUtils.lockGazeAndWalkToEachOther(body, partner, this.speedModifier, this.closeEnoughDistance);
        if (!body.closerThan(partner, 3.0)) {
            return;
        }
        if (timestamp >= this.spawnChildAtTime) {
            body.spawnChildFromBreeding(level, partner);
            body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            partner.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        }
    }

    @Override
    protected void stop(ServerLevel level, Animal body, long timestamp) {
        body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        body.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.spawnChildAtTime = 0L;
    }

    private Animal getBreedTarget(Animal body) {
        return (Animal)body.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
    }

    private boolean hasBreedTargetOfRightType(Animal body) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        return brain.hasMemoryValue(MemoryModuleType.BREED_TARGET) && brain.getMemory(MemoryModuleType.BREED_TARGET).get().is(this.partnerType);
    }

    private Optional<? extends Animal> findValidBreedPartner(Animal body) {
        return body.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().findClosest(entity -> {
            Animal animal;
            return entity.is(this.partnerType) && entity instanceof Animal && body.canMate(animal = (Animal)entity) && !animal.isPanicking();
        }).map(Animal.class::cast);
    }
}

