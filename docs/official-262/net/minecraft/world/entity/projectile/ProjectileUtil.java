/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ProjectileUtil {
    public static final float DEFAULT_ENTITY_HIT_RESULT_MARGIN = 0.3f;

    public static HitResult getHitResultOnMoveVector(Entity source, Predicate<Entity> matching) {
        Vec3 movement = source.getDeltaMovement();
        Level level = source.level();
        Vec3 from = source.position();
        return ProjectileUtil.getHitResult(from, source, matching, movement, level, ProjectileUtil.computeMargin(source), ClipContext.Block.COLLIDER);
    }

    public static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(Entity attacker, AttackRange attackRange, Predicate<Entity> matching, ClipContext.Block blockClipType) {
        Vec3 look = attacker.getHeadLookAngle();
        Vec3 eyePosition = attacker.getEyePosition();
        Vec3 from = eyePosition.add(look.scale(attackRange.effectiveMinRange(attacker)));
        double movementComponent = attacker.getKnownMovement().dot(look);
        Vec3 to = eyePosition.add(look.scale((double)attackRange.effectiveMaxRange(attacker) + Math.max(0.0, movementComponent)));
        return ProjectileUtil.getHitEntitiesAlong(attacker, eyePosition, from, matching, to, attackRange.hitboxMargin(), blockClipType);
    }

    public static HitResult getHitResultOnMoveVector(Entity source, Predicate<Entity> matching, ClipContext.Block clipType) {
        Vec3 movement = source.getDeltaMovement();
        Level level = source.level();
        Vec3 from = source.position();
        return ProjectileUtil.getHitResult(from, source, matching, movement, level, ProjectileUtil.computeMargin(source), clipType);
    }

    public static HitResult getHitResultOnViewVector(Entity source, Predicate<Entity> matching, double distance) {
        Vec3 viewVector = source.getViewVector(0.0f).scale(distance);
        Level level = source.level();
        Vec3 from = source.getEyePosition();
        return ProjectileUtil.getHitResult(from, source, matching, viewVector, level, 0.0f, ClipContext.Block.COLLIDER);
    }

    private static HitResult getHitResult(Vec3 from, Entity source, Predicate<Entity> matching, Vec3 delta, Level level, float entityMargin, ClipContext.Block clipType) {
        EntityHitResult entityHit;
        Vec3 to = from.add(delta);
        HitResult hitResult = level.clipIncludingBorder(new ClipContext(from, to, clipType, ClipContext.Fluid.NONE, source));
        if (((HitResult)hitResult).getType() != HitResult.Type.MISS) {
            to = hitResult.getLocation();
        }
        if ((entityHit = ProjectileUtil.getEntityHitResult(level, source, from, to, source.getBoundingBox().expandTowards(delta).inflate(1.0), matching, entityMargin)) != null) {
            hitResult = entityHit;
        }
        return hitResult;
    }

    private static Either<BlockHitResult, Collection<EntityHitResult>> getHitEntitiesAlong(Entity source, Vec3 origin, Vec3 from, Predicate<Entity> matching, Vec3 to, float entityMargin, ClipContext.Block clipType) {
        Level level = source.level();
        BlockHitResult hitResult = level.clipIncludingBorder(new ClipContext(origin, to, clipType, ClipContext.Fluid.NONE, source));
        if (hitResult.getType() != HitResult.Type.MISS && origin.distanceToSqr(to = hitResult.getLocation()) < origin.distanceToSqr(from)) {
            return Either.left((Object)hitResult);
        }
        AABB searchArea = AABB.ofSize(from, entityMargin, entityMargin, entityMargin).expandTowards(to.subtract(from)).inflate(1.0);
        Collection<EntityHitResult> entityHit = ProjectileUtil.getManyEntityHitResult(level, source, from, to, searchArea, matching, entityMargin, clipType, true);
        if (!entityHit.isEmpty()) {
            return Either.right(entityHit);
        }
        return Either.left((Object)hitResult);
    }

    public static @Nullable EntityHitResult getEntityHitResult(Entity except, Vec3 from, Vec3 to, AABB box, Predicate<Entity> matching, double maxValue) {
        Level level = except.level();
        double nearest = maxValue;
        Entity hovered = null;
        Vec3 hoveredPos = null;
        for (Entity entity : level.getEntities(except, box, matching)) {
            Vec3 location;
            double dd;
            AABB bb = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> clipPoint = bb.clip(from, to);
            if (bb.contains(from)) {
                if (!(nearest >= 0.0) || !entity.canBePickedFromInside()) continue;
                hovered = entity;
                hoveredPos = clipPoint.orElse(from);
                nearest = 0.0;
                continue;
            }
            if (!clipPoint.isPresent() || !((dd = from.distanceToSqr(location = clipPoint.get())) < nearest) && nearest != 0.0) continue;
            if (entity.getRootVehicle() == except.getRootVehicle()) {
                if (nearest != 0.0) continue;
                hovered = entity;
                hoveredPos = location;
                continue;
            }
            hovered = entity;
            hoveredPos = location;
            nearest = dd;
        }
        if (hovered == null) {
            return null;
        }
        return new EntityHitResult(hovered, hoveredPos);
    }

    public static @Nullable EntityHitResult getEntityHitResult(Level level, Projectile source, Vec3 from, Vec3 to, AABB targetSearchArea, Predicate<Entity> matching) {
        return ProjectileUtil.getEntityHitResult(level, source, from, to, targetSearchArea, matching, ProjectileUtil.computeMargin(source));
    }

    public static float computeMargin(Entity source) {
        return Math.max(0.0f, Math.min(0.3f, (float)(source.tickCount - 2) / 20.0f));
    }

    public static @Nullable EntityHitResult getEntityHitResult(Level level, Entity source, Vec3 from, Vec3 to, AABB targetSearchArea, Predicate<Entity> matching, float entityMargin) {
        double nearest = Double.MAX_VALUE;
        Optional<Object> nearestLocation = Optional.empty();
        Entity hitEntity = null;
        for (Entity entity : level.getEntities(source, targetSearchArea, matching)) {
            double dd;
            AABB bb = entity.getBoundingBox().inflate(entityMargin);
            Optional<Vec3> location = bb.clip(from, to);
            if (!location.isPresent() || !((dd = from.distanceToSqr(location.get())) < nearest)) continue;
            hitEntity = entity;
            nearest = dd;
            nearestLocation = location;
        }
        if (hitEntity == null) {
            return null;
        }
        return new EntityHitResult(hitEntity, (Vec3)nearestLocation.get());
    }

    public static Collection<EntityHitResult> getManyEntityHitResult(Level level, Entity source, Vec3 from, Vec3 to, AABB targetSearchArea, Predicate<Entity> matching, boolean includeFromEntity) {
        return ProjectileUtil.getManyEntityHitResult(level, source, from, to, targetSearchArea, matching, ProjectileUtil.computeMargin(source), ClipContext.Block.COLLIDER, includeFromEntity);
    }

    public static Collection<EntityHitResult> getManyEntityHitResult(Level level, Entity source, Vec3 from, Vec3 to, AABB targetSearchArea, Predicate<Entity> matching, float entityMargin, ClipContext.Block clipType, boolean includeFromEntity) {
        ArrayList<EntityHitResult> collector = new ArrayList<EntityHitResult>();
        for (Entity entity : level.getEntities(source, targetSearchArea, matching)) {
            Optional<Vec3> surfaceHit;
            Vec3 towardsTarget;
            Optional<Vec3> outsideHit;
            AABB entityBB = entity.getBoundingBox();
            if (includeFromEntity && entityBB.contains(from)) {
                collector.add(new EntityHitResult(entity, from));
                continue;
            }
            Optional<Vec3> exactHit = entityBB.clip(from, to);
            if (exactHit.isPresent()) {
                collector.add(new EntityHitResult(entity, exactHit.get()));
                continue;
            }
            if ((double)entityMargin <= 0.0 || (outsideHit = entityBB.inflate(entityMargin).clip(from, to)).isEmpty()) continue;
            Vec3 outsideHitPosition = outsideHit.get();
            BlockHitResult hitResult = level.clipIncludingBorder(new ClipContext(outsideHitPosition, towardsTarget = entityBB.getCenter(), clipType, ClipContext.Fluid.NONE, source));
            if (hitResult.getType() != HitResult.Type.MISS) {
                towardsTarget = hitResult.getLocation();
            }
            if (!(surfaceHit = entity.getBoundingBox().clip(outsideHitPosition, towardsTarget)).isPresent()) continue;
            collector.add(new EntityHitResult(entity, surfaceHit.get()));
        }
        return collector;
    }

    public static void rotateTowardsMovement(Entity projectile, float rotationSpeed) {
        Vec3 movement = projectile.getDeltaMovement();
        if (movement.lengthSqr() == 0.0) {
            return;
        }
        double sd = movement.horizontalDistance();
        projectile.setYRot((float)(Mth.atan2(movement.z, movement.x) * 57.2957763671875) + 90.0f);
        projectile.setXRot((float)(Mth.atan2(sd, movement.y) * 57.2957763671875) - 90.0f);
        while (projectile.getXRot() - projectile.xRotO < -180.0f) {
            projectile.xRotO -= 360.0f;
        }
        while (projectile.getXRot() - projectile.xRotO >= 180.0f) {
            projectile.xRotO += 360.0f;
        }
        while (projectile.getYRot() - projectile.yRotO < -180.0f) {
            projectile.yRotO -= 360.0f;
        }
        while (projectile.getYRot() - projectile.yRotO >= 180.0f) {
            projectile.yRotO += 360.0f;
        }
        projectile.setXRot(Mth.lerp(rotationSpeed, projectile.xRotO, projectile.getXRot()));
        projectile.setYRot(Mth.lerp(rotationSpeed, projectile.yRotO, projectile.getYRot()));
    }

    public static InteractionHand getWeaponHoldingHand(LivingEntity mob, Item weaponItem) {
        return mob.getMainHandItem().is(weaponItem) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static AbstractArrow getMobArrow(LivingEntity mob, ItemStack projectile, float power, @Nullable ItemStack firedFromWeapon) {
        ArrowItem arrowItem = (ArrowItem)(projectile.getItem() instanceof ArrowItem ? projectile.getItem() : Items.ARROW);
        AbstractArrow arrow = arrowItem.createArrow(mob.level(), projectile, mob, firedFromWeapon);
        arrow.setBaseDamageFromMob(power);
        return arrow;
    }
}

