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
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;

public class VillagerMakeLove
extends Behavior<Villager> {
    private long birthTimestamp;

    public VillagerMakeLove() {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.BREED_TARGET, (Object)((Object)MemoryStatus.VALUE_PRESENT), MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, (Object)((Object)MemoryStatus.VALUE_PRESENT)), 350, 350);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        return this.isBreedingPossible(body);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return timestamp <= this.birthTimestamp && this.isBreedingPossible(body);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        AgeableMob breedTarget = body.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        BehaviorUtils.lockGazeAndWalkToEachOther(body, breedTarget, 0.5f, 2);
        level.broadcastEntityEvent(breedTarget, (byte)18);
        level.broadcastEntityEvent(body, (byte)18);
        int duration = 275 + body.getRandom().nextInt(50);
        this.birthTimestamp = timestamp + (long)duration;
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        Villager target = (Villager)body.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
        if (body.distanceToSqr(target) > 5.0) {
            return;
        }
        BehaviorUtils.lockGazeAndWalkToEachOther(body, target, 0.5f, 2);
        if (timestamp >= this.birthTimestamp) {
            body.eatAndDigestFood();
            target.eatAndDigestFood();
            this.tryToGiveBirth(level, body, target);
        } else if (body.getRandom().nextInt(35) == 0) {
            level.broadcastEntityEvent(target, (byte)12);
            level.broadcastEntityEvent(body, (byte)12);
        }
    }

    private void tryToGiveBirth(ServerLevel level, Villager body, Villager target) {
        Optional<BlockPos> childsBed = this.takeVacantBed(level, body);
        if (childsBed.isEmpty()) {
            level.broadcastEntityEvent(target, (byte)13);
            level.broadcastEntityEvent(body, (byte)13);
        } else {
            Optional<Villager> child = this.breed(level, body, target);
            if (child.isPresent()) {
                this.giveBedToChild(level, child.get(), childsBed.get());
            } else {
                level.getPoiManager().release(childsBed.get());
                level.debugSynchronizers().updatePoi(childsBed.get());
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
    }

    private boolean isBreedingPossible(Villager myBody) {
        Brain<Villager> brain = myBody.getBrain();
        Optional<AgeableMob> breedTarget = brain.getMemory(MemoryModuleType.BREED_TARGET).filter(entity -> entity.is(EntityTypes.VILLAGER));
        if (breedTarget.isEmpty()) {
            return false;
        }
        return BehaviorUtils.targetIsValid(brain, MemoryModuleType.BREED_TARGET, EntityTypes.VILLAGER) && myBody.canBreed() && breedTarget.get().canBreed();
    }

    private Optional<BlockPos> takeVacantBed(ServerLevel level, Villager body) {
        return level.getPoiManager().take(p -> p.is(PoiTypes.HOME), (poiType, poiPos) -> this.canReach(body, (BlockPos)poiPos, (Holder<PoiType>)poiType), body.blockPosition(), 48);
    }

    private boolean canReach(Villager body, BlockPos poiPos, Holder<PoiType> poiType) {
        Path path = body.getNavigation().createPath(poiPos, poiType.value().validRange());
        return path != null && path.canReach();
    }

    private Optional<Villager> breed(ServerLevel level, Villager source, Villager target) {
        Villager child = source.getBreedOffspring(level, target);
        if (child == null) {
            return Optional.empty();
        }
        source.setAge(6000);
        target.setAge(6000);
        child.setAge(-24000);
        child.snapTo(source.getX(), source.getY(), source.getZ(), 0.0f, 0.0f);
        level.addFreshEntityWithPassengers(child);
        level.broadcastEntityEvent(child, (byte)12);
        return Optional.of(child);
    }

    private void giveBedToChild(ServerLevel level, Villager child, BlockPos bedPos) {
        GlobalPos globalBedPos = GlobalPos.of(level.dimension(), bedPos);
        child.getBrain().setMemory(MemoryModuleType.HOME, globalBedPos);
    }
}

