/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record AttackRange(float minReach, float maxReach, float minCreativeReach, float maxCreativeReach, float hitboxMargin, float mobFactor) {
    public static final Codec<AttackRange> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.floatRange(0.0f, 64.0f).optionalFieldOf("min_reach", (Object)Float.valueOf(0.0f)).forGetter(AttackRange::minReach), (App)ExtraCodecs.floatRange(0.0f, 64.0f).optionalFieldOf("max_reach", (Object)Float.valueOf(3.0f)).forGetter(AttackRange::maxReach), (App)ExtraCodecs.floatRange(0.0f, 64.0f).optionalFieldOf("min_creative_reach", (Object)Float.valueOf(0.0f)).forGetter(AttackRange::minCreativeReach), (App)ExtraCodecs.floatRange(0.0f, 64.0f).optionalFieldOf("max_creative_reach", (Object)Float.valueOf(5.0f)).forGetter(AttackRange::maxCreativeReach), (App)ExtraCodecs.floatRange(0.0f, 1.0f).optionalFieldOf("hitbox_margin", (Object)Float.valueOf(0.3f)).forGetter(AttackRange::hitboxMargin), (App)Codec.floatRange((float)0.0f, (float)2.0f).optionalFieldOf("mob_factor", (Object)Float.valueOf(1.0f)).forGetter(AttackRange::mobFactor)).apply((Applicative)i, AttackRange::new));
    public static final StreamCodec<ByteBuf, AttackRange> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, AttackRange::minReach, ByteBufCodecs.FLOAT, AttackRange::maxReach, ByteBufCodecs.FLOAT, AttackRange::minCreativeReach, ByteBufCodecs.FLOAT, AttackRange::maxCreativeReach, ByteBufCodecs.FLOAT, AttackRange::hitboxMargin, ByteBufCodecs.FLOAT, AttackRange::mobFactor, AttackRange::new);

    public static AttackRange defaultFor(LivingEntity livingEntity) {
        float interactionRange = (float)livingEntity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        return new AttackRange(0.0f, interactionRange, 0.0f, interactionRange, 0.0f, 1.0f);
    }

    public HitResult getClosesetHit(Entity attacker, float partial, Predicate<Entity> matching) {
        Either<BlockHitResult, Collection<EntityHitResult>> result = ProjectileUtil.getHitEntitiesAlong(attacker, this, matching, ClipContext.Block.OUTLINE);
        if (result.left().isPresent()) {
            return (HitResult)result.left().get();
        }
        Collection targets = (Collection)result.right().get();
        EntityHitResult entity = null;
        Vec3 attackerPos = attacker.getEyePosition(partial);
        double closestDistance = Double.MAX_VALUE;
        for (EntityHitResult target : targets) {
            double distance = attackerPos.distanceToSqr(target.getLocation());
            if (!(distance < closestDistance)) continue;
            closestDistance = distance;
            entity = target;
        }
        if (entity != null) {
            return entity;
        }
        Vec3 eyeGaze = attacker.getHeadLookAngle();
        Vec3 missPosition = attacker.getEyePosition(partial).add(eyeGaze);
        return BlockHitResult.miss(missPosition, Direction.getApproximateNearest(eyeGaze), BlockPos.containing(missPosition));
    }

    public float effectiveMinRange(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            return player.isCreative() ? this.minCreativeReach : this.minReach;
        }
        return this.minReach * this.mobFactor;
    }

    public float effectiveMaxRange(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            return player.isCreative() ? this.maxCreativeReach : this.maxReach;
        }
        return this.maxReach * this.mobFactor;
    }

    public boolean isInRange(LivingEntity attacker, Vec3 location) {
        return this.isInRange(attacker, location::distanceToSqr, 0.0);
    }

    public boolean isInRange(LivingEntity attacker, AABB boundingBox, double extraBuffer) {
        return this.isInRange(attacker, boundingBox::distanceToSqr, extraBuffer);
    }

    private boolean isInRange(LivingEntity attacker, ToDoubleFunction<Vec3> distanceFunction, double extraBuffer) {
        double distance = Math.sqrt(distanceFunction.applyAsDouble(attacker.getEyePosition()));
        double minReach = (double)(this.effectiveMinRange(attacker) - this.hitboxMargin) - extraBuffer;
        double maxReach = (double)(this.effectiveMaxRange(attacker) + this.hitboxMargin) + extraBuffer;
        return distance >= minReach && distance <= maxReach;
    }
}

