/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface ServerEntityGetter
extends EntityGetter {
    public ServerLevel getLevel();

    default public @Nullable Player getNearestPlayer(TargetingConditions targetConditions, LivingEntity source) {
        return this.getNearestEntity(this.players(), targetConditions, source, source.getX(), source.getY(), source.getZ());
    }

    default public @Nullable Player getNearestPlayer(TargetingConditions targetConditions, LivingEntity source, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetConditions, source, x, y, z);
    }

    default public @Nullable Player getNearestPlayer(TargetingConditions targetConditions, double x, double y, double z) {
        return this.getNearestEntity(this.players(), targetConditions, null, x, y, z);
    }

    default public <T extends LivingEntity> @Nullable T getNearestEntity(Class<? extends T> type, TargetingConditions targetConditions, @Nullable LivingEntity source, double x, double y, double z, AABB bb) {
        return (T)this.getNearestEntity(this.getEntitiesOfClass(type, bb, entity -> true), targetConditions, source, x, y, z);
    }

    default public @Nullable LivingEntity getNearestEntity(TagKey<EntityType<?>> tag, TargetingConditions targetConditions, @Nullable LivingEntity source, double x, double y, double z, AABB bb) {
        double bestDistance = Double.MAX_VALUE;
        LivingEntity nearestEntity = null;
        for (LivingEntity entity : this.getEntitiesOfClass(LivingEntity.class, bb, e -> e.is(tag))) {
            double distance;
            if (!targetConditions.test(this.getLevel(), source, entity) || !((distance = entity.distanceToSqr(x, y, z)) < bestDistance)) continue;
            bestDistance = distance;
            nearestEntity = entity;
        }
        return nearestEntity;
    }

    default public <T extends Entity> @Nullable T getNearestEntity(List<? extends T> entities, double x, double y, double z) {
        double best = -1.0;
        Entity result = null;
        for (Entity entity : entities) {
            double dist = entity.distanceToSqr(x, y, z);
            if (best != -1.0 && !(dist < best)) continue;
            best = dist;
            result = entity;
        }
        return (T)result;
    }

    default public <T extends LivingEntity> @Nullable T getNearestEntity(List<? extends T> entities, TargetingConditions targetConditions, @Nullable LivingEntity source, double x, double y, double z) {
        double best = -1.0;
        LivingEntity result = null;
        for (LivingEntity entity : entities) {
            if (!targetConditions.test(this.getLevel(), source, entity)) continue;
            double dist = entity.distanceToSqr(x, y, z);
            if (best != -1.0 && !(dist < best)) continue;
            best = dist;
            result = entity;
        }
        return (T)result;
    }

    default public List<Player> getNearbyPlayers(TargetingConditions targetConditions, LivingEntity source, AABB bb) {
        ArrayList<Player> foundPlayers = new ArrayList<Player>();
        for (Player player : this.players()) {
            if (!bb.contains(player.getX(), player.getY(), player.getZ()) || !targetConditions.test(this.getLevel(), source, player)) continue;
            foundPlayers.add(player);
        }
        return foundPlayers;
    }

    default public <T extends LivingEntity> List<T> getNearbyEntities(Class<T> type, TargetingConditions targetConditions, LivingEntity source, AABB bb) {
        List<LivingEntity> nearby = this.getEntitiesOfClass(type, bb, entity -> true);
        ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
        for (LivingEntity entity2 : nearby) {
            if (!targetConditions.test(this.getLevel(), source, entity2)) continue;
            entities.add(entity2);
        }
        return entities;
    }
}

