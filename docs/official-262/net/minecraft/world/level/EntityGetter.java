/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public interface EntityGetter {
    public List<Entity> getEntities(@Nullable Entity var1, AABB var2, Predicate<? super Entity> var3);

    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> var1, AABB var2, Predicate<? super T> var3);

    default public <T extends Entity> List<T> getEntitiesOfClass(Class<T> baseClass, AABB bb, Predicate<? super T> selector) {
        return this.getEntities(EntityTypeTest.forClass(baseClass), bb, selector);
    }

    public List<? extends Player> players();

    default public List<Entity> getEntities(@Nullable Entity except, AABB bb) {
        return this.getEntities(except, bb, EntitySelector.NO_SPECTATORS);
    }

    default public boolean isUnobstructed(@Nullable Entity source, VoxelShape shape) {
        if (shape.isEmpty()) {
            return true;
        }
        for (Entity entity : this.getEntities(source, shape.bounds())) {
            if (entity.isRemoved() || !entity.blocksBuilding || source != null && entity.isPassengerOfSameVehicle(source) || !Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) continue;
            return false;
        }
        return true;
    }

    default public <T extends Entity> List<T> getEntitiesOfClass(Class<T> baseClass, AABB bb) {
        return this.getEntitiesOfClass(baseClass, bb, EntitySelector.NO_SPECTATORS);
    }

    default public List<VoxelShape> getEntityCollisions(@Nullable Entity source, AABB testArea) {
        if (testArea.getSize() < 1.0E-7) {
            return List.of();
        }
        Predicate<Entity> canCollide = source == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(source::canCollideWith);
        List<Entity> collidingEntities = this.getEntities(source, testArea.inflate(1.0E-7), canCollide);
        if (collidingEntities.isEmpty()) {
            return List.of();
        }
        ImmutableList.Builder shapes = ImmutableList.builderWithExpectedSize((int)collidingEntities.size());
        for (Entity entity : collidingEntities) {
            shapes.add((Object)Shapes.create(entity.getBoundingBox()));
        }
        return shapes.build();
    }

    default public @Nullable Player getNearestPlayer(double x, double y, double z, double range, @Nullable Predicate<Entity> predicate) {
        double best = -1.0;
        Player result = null;
        for (Player player : this.players()) {
            if (predicate != null && !predicate.test(player)) continue;
            double dist = player.distanceToSqr(x, y, z);
            if (!(range < 0.0) && !(dist < range * range) || best != -1.0 && !(dist < best)) continue;
            best = dist;
            result = player;
        }
        return result;
    }

    default public @Nullable Player getNearestPlayer(Entity source, double maxDist) {
        return this.getNearestPlayer(source.getX(), source.getY(), source.getZ(), maxDist, false);
    }

    default public @Nullable Player getNearestPlayer(double x, double y, double z, double maxDist, boolean filterOutCreative) {
        Predicate<Entity> predicate = filterOutCreative ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
        return this.getNearestPlayer(x, y, z, maxDist, predicate);
    }

    default public boolean hasNearbyAlivePlayer(double x, double y, double z, double range) {
        for (Player player : this.players()) {
            if (!EntitySelector.NO_SPECTATORS.test(player) || !EntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) continue;
            double playerDist = player.distanceToSqr(x, y, z);
            if (!(range < 0.0) && !(playerDist < range * range)) continue;
            return true;
        }
        return false;
    }

    default public @Nullable Player getPlayerByUUID(UUID uuid) {
        for (int i = 0; i < this.players().size(); ++i) {
            Player player = this.players().get(i);
            if (!uuid.equals(player.getUUID())) continue;
            return player;
        }
        return null;
    }
}

