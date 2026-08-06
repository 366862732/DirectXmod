/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BehaviorUtils {
    private BehaviorUtils() {
    }

    public static void lockGazeAndWalkToEachOther(LivingEntity entity1, LivingEntity entity2, float speedModifier, int closeEnoughDistance) {
        BehaviorUtils.lookAtEachOther(entity1, entity2);
        BehaviorUtils.setWalkAndLookTargetMemoriesToEachOther(entity1, entity2, speedModifier, closeEnoughDistance);
    }

    public static boolean entityIsVisible(Brain<?> brain, LivingEntity targetEntity) {
        Optional<NearestVisibleLivingEntities> visibleEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return visibleEntities.isPresent() && visibleEntities.get().contains(targetEntity);
    }

    public static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memory, EntityType<?> targetType) {
        return BehaviorUtils.targetIsValid(brain, memory, (LivingEntity entity) -> entity.is(targetType));
    }

    private static boolean targetIsValid(Brain<?> brain, MemoryModuleType<? extends LivingEntity> memory, Predicate<LivingEntity> targetPredicate) {
        return brain.getMemory(memory).filter(targetPredicate).filter(LivingEntity::isAlive).filter(entity -> BehaviorUtils.entityIsVisible(brain, entity)).isPresent();
    }

    private static void lookAtEachOther(LivingEntity entity1, LivingEntity entity2) {
        BehaviorUtils.lookAtEntity(entity1, entity2);
        BehaviorUtils.lookAtEntity(entity2, entity1);
    }

    public static void lookAtEntity(LivingEntity looker, LivingEntity targetEntity) {
        looker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(targetEntity, true));
    }

    private static void setWalkAndLookTargetMemoriesToEachOther(LivingEntity entity1, LivingEntity entity2, float speedModifier, int closeEnoughDistance) {
        BehaviorUtils.setWalkAndLookTargetMemories(entity1, entity2, speedModifier, closeEnoughDistance);
        BehaviorUtils.setWalkAndLookTargetMemories(entity2, entity1, speedModifier, closeEnoughDistance);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity walker, Entity targetEntity, float speedModifier, int closeEnoughDistance) {
        BehaviorUtils.setWalkAndLookTargetMemories(walker, new EntityTracker(targetEntity, true), speedModifier, closeEnoughDistance);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity walker, BlockPos targetPos, float speedModifier, int closeEnoughDistance) {
        BehaviorUtils.setWalkAndLookTargetMemories(walker, new BlockPosTracker(targetPos), speedModifier, closeEnoughDistance);
    }

    public static void setWalkAndLookTargetMemories(LivingEntity walker, PositionTracker target, float speedModifier, int closeEnoughDistance) {
        WalkTarget walkTarget = new WalkTarget(target, speedModifier, closeEnoughDistance);
        walker.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, target);
        walker.getBrain().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static void throwItem(LivingEntity thrower, ItemStack item, Vec3 targetPos) {
        Vec3 throwVelocity = new Vec3(0.3f, 0.3f, 0.3f);
        BehaviorUtils.throwItem(thrower, item, targetPos, throwVelocity, 0.3f);
    }

    public static void throwItem(LivingEntity thrower, ItemStack item, Vec3 targetPos, Vec3 throwVelocity, float handYDistanceFromEye) {
        double yHandPos = thrower.getEyeY() - (double)handYDistanceFromEye;
        ItemEntity itemEntity = new ItemEntity(thrower.level(), thrower.getX(), yHandPos, thrower.getZ(), item);
        itemEntity.setThrower(thrower);
        Vec3 throwVector = targetPos.subtract(thrower.position());
        throwVector = throwVector.normalize().multiply(throwVelocity.x, throwVelocity.y, throwVelocity.z);
        itemEntity.setDeltaMovement(throwVector);
        itemEntity.setDefaultPickUpDelay();
        thrower.level().addFreshEntity(itemEntity);
    }

    public static SectionPos findSectionClosestToVillage(ServerLevel level, SectionPos center, int radius) {
        int distToVillage = level.sectionsToVillage(center);
        return SectionPos.cube(center, radius).filter(s -> level.sectionsToVillage((SectionPos)s) < distToVillage).min(Comparator.comparingInt(level::sectionsToVillage)).orElse(center);
    }

    public static boolean isWithinAttackRange(Mob body, LivingEntity target, int projectileAttackRangeMargin) {
        Item item = body.getMainHandItem().getItem();
        if (item instanceof ProjectileWeaponItem) {
            ProjectileWeaponItem weapon = (ProjectileWeaponItem)item;
            if (body.canUseNonMeleeWeapon(body.getMainHandItem())) {
                int maxAllowedDistance = weapon.getDefaultProjectileRange() - projectileAttackRangeMargin;
                return body.closerThan(target, maxAllowedDistance);
            }
        }
        return body.isWithinMeleeAttackRange(target);
    }

    public static boolean isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(LivingEntity body, LivingEntity otherTarget, double howMuchFurtherAway) {
        Optional<LivingEntity> currentTarget = body.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (currentTarget.isEmpty()) {
            return false;
        }
        double distSqrToCurrentTarget = body.distanceToSqr(currentTarget.get().position());
        double distSqrToOtherTarget = body.distanceToSqr(otherTarget.position());
        return distSqrToOtherTarget > distSqrToCurrentTarget + howMuchFurtherAway * howMuchFurtherAway;
    }

    public static boolean canSee(LivingEntity body, LivingEntity target) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)) {
            return false;
        }
        return brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(target);
    }

    public static LivingEntity getNearestTarget(LivingEntity body, Optional<LivingEntity> target1, LivingEntity target2) {
        if (target1.isEmpty()) {
            return target2;
        }
        return BehaviorUtils.getTargetNearestMe(body, target1.get(), target2);
    }

    public static LivingEntity getTargetNearestMe(LivingEntity body, LivingEntity target1, LivingEntity target2) {
        Vec3 pos1 = target1.position();
        Vec3 pos2 = target2.position();
        return body.distanceToSqr(pos1) < body.distanceToSqr(pos2) ? target1 : target2;
    }

    public static Optional<LivingEntity> getLivingEntityFromUUIDMemory(LivingEntity body, MemoryModuleType<UUID> memoryType) {
        Optional<UUID> uuidMemory = body.getBrain().getMemory(memoryType);
        return uuidMemory.map(uuid -> body.level().getEntity((UUID)uuid)).map(entity -> {
            LivingEntity livingEntity;
            return entity instanceof LivingEntity ? (livingEntity = (LivingEntity)entity) : null;
        });
    }

    public static @Nullable Vec3 getRandomSwimmablePos(PathfinderMob body, int maxHorizontalDistance, int maxVerticalDistance) {
        Vec3 targetPos = DefaultRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance);
        int count = 0;
        while (targetPos != null && !body.level().getBlockState(BlockPos.containing(targetPos)).isPathfindable(PathComputationType.WATER) && count++ < 10) {
            targetPos = DefaultRandomPos.getPos(body, maxHorizontalDistance, maxVerticalDistance);
        }
        return targetPos;
    }

    public static boolean isBreeding(LivingEntity body) {
        return body.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET);
    }
}

