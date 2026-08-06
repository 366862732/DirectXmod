/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public record KineticWeapon(int contactCooldownTicks, int delayTicks, Optional<Condition> dismountConditions, Optional<Condition> knockbackConditions, Optional<Condition> damageConditions, float forwardMovement, float damageMultiplier, Optional<Holder<SoundEvent>> sound, Optional<Holder<SoundEvent>> hitSound) {
    public static final int HIT_FEEDBACK_TICKS = 10;
    public static final Codec<KineticWeapon> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("contact_cooldown_ticks", (Object)10).forGetter(KineticWeapon::contactCooldownTicks), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("delay_ticks", (Object)0).forGetter(KineticWeapon::delayTicks), (App)Condition.CODEC.optionalFieldOf("dismount_conditions").forGetter(KineticWeapon::dismountConditions), (App)Condition.CODEC.optionalFieldOf("knockback_conditions").forGetter(KineticWeapon::knockbackConditions), (App)Condition.CODEC.optionalFieldOf("damage_conditions").forGetter(KineticWeapon::damageConditions), (App)Codec.FLOAT.optionalFieldOf("forward_movement", (Object)Float.valueOf(0.0f)).forGetter(KineticWeapon::forwardMovement), (App)Codec.FLOAT.optionalFieldOf("damage_multiplier", (Object)Float.valueOf(1.0f)).forGetter(KineticWeapon::damageMultiplier), (App)SoundEvent.CODEC.optionalFieldOf("sound").forGetter(KineticWeapon::sound), (App)SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(KineticWeapon::hitSound)).apply((Applicative)i, KineticWeapon::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KineticWeapon> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, KineticWeapon::contactCooldownTicks, ByteBufCodecs.VAR_INT, KineticWeapon::delayTicks, Condition.STREAM_CODEC.apply(ByteBufCodecs::optional), KineticWeapon::dismountConditions, Condition.STREAM_CODEC.apply(ByteBufCodecs::optional), KineticWeapon::knockbackConditions, Condition.STREAM_CODEC.apply(ByteBufCodecs::optional), KineticWeapon::damageConditions, ByteBufCodecs.FLOAT, KineticWeapon::forwardMovement, ByteBufCodecs.FLOAT, KineticWeapon::damageMultiplier, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), KineticWeapon::sound, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), KineticWeapon::hitSound, KineticWeapon::new);

    public static Vec3 getMotion(Entity livingEntity) {
        if (!(livingEntity instanceof Player) && livingEntity.isPassenger()) {
            livingEntity = livingEntity.getRootVehicle();
        }
        return livingEntity.getKnownSpeed().scale(20.0);
    }

    public void makeSound(Entity causer) {
        this.sound.ifPresent(s -> causer.level().playSound(causer, causer.getX(), causer.getY(), causer.getZ(), (Holder<SoundEvent>)s, causer.getSoundSource(), 1.0f, 1.0f));
    }

    public void makeLocalHitSound(Entity causer) {
        this.hitSound.ifPresent(s -> causer.level().playLocalSound(causer, (SoundEvent)s.value(), causer.getSoundSource(), 1.0f, 1.0f));
    }

    public int computeDamageUseDuration() {
        return this.delayTicks + this.damageConditions.map(Condition::maxDurationTicks).orElse(0);
    }

    public void damageEntities(ItemStack stack, int ticksRemaining, LivingEntity livingEntity, EquipmentSlot equipmentSlot) {
        int ticksUsed = stack.getUseDuration(livingEntity) - ticksRemaining;
        if (ticksUsed < this.delayTicks) {
            return;
        }
        ticksUsed -= this.delayTicks;
        Vec3 attackerLookVector = livingEntity.getLookAngle();
        double attackerSpeedProjection = attackerLookVector.dot(KineticWeapon.getMotion(livingEntity));
        float actionFactor = livingEntity instanceof Player ? 1.0f : 0.2f;
        AttackRange attackRange = livingEntity.getAttackRangeWith(stack);
        double baseMobDamage = livingEntity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        boolean affected = false;
        for (EntityHitResult hitResult : (Collection)ProjectileUtil.getHitEntitiesAlong(livingEntity, attackRange, e -> PiercingWeapon.canHitEntity(livingEntity, e), ClipContext.Block.COLLIDER).map(a -> List.of(), e -> e)) {
            boolean dealsDamage;
            boolean wasStabbed;
            Entity otherEntity = hitResult.getEntity();
            if (otherEntity instanceof EnderDragonPart) {
                EnderDragonPart dragonPart = (EnderDragonPart)otherEntity;
                otherEntity = dragonPart.parentMob;
            }
            if (wasStabbed = livingEntity.wasRecentlyStabbed(otherEntity, this.contactCooldownTicks)) continue;
            livingEntity.rememberStabbedEntity(otherEntity);
            double targetSpeedProjection = attackerLookVector.dot(KineticWeapon.getMotion(otherEntity));
            double relativeSpeed = Math.max(0.0, attackerSpeedProjection - targetSpeedProjection);
            boolean dealsDismount = this.dismountConditions.isPresent() && this.dismountConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
            boolean dealsKnockback = this.knockbackConditions.isPresent() && this.knockbackConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
            boolean bl = dealsDamage = this.damageConditions.isPresent() && this.damageConditions.get().test(ticksUsed, attackerSpeedProjection, relativeSpeed, actionFactor);
            if (!dealsDismount && !dealsKnockback && !dealsDamage) continue;
            float damageDealt = (float)baseMobDamage + (float)Mth.floor(relativeSpeed * (double)this.damageMultiplier);
            affected |= livingEntity.stabAttack(equipmentSlot, otherEntity, damageDealt, dealsDamage, dealsKnockback, dealsDismount);
        }
        if (affected) {
            livingEntity.level().broadcastEntityEvent(livingEntity, (byte)2);
            if (livingEntity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)livingEntity;
                CriteriaTriggers.SPEAR_MOBS_TRIGGER.trigger(player, livingEntity.stabbedEntities(e -> e instanceof LivingEntity));
            }
        }
    }

    public record Condition(int maxDurationTicks, float minSpeed, float minRelativeSpeed) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.NON_NEGATIVE_INT.fieldOf("max_duration_ticks").forGetter(Condition::maxDurationTicks), (App)Codec.FLOAT.optionalFieldOf("min_speed", (Object)Float.valueOf(0.0f)).forGetter(Condition::minSpeed), (App)Codec.FLOAT.optionalFieldOf("min_relative_speed", (Object)Float.valueOf(0.0f)).forGetter(Condition::minRelativeSpeed)).apply((Applicative)i, Condition::new));
        public static final StreamCodec<ByteBuf, Condition> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Condition::maxDurationTicks, ByteBufCodecs.FLOAT, Condition::minSpeed, ByteBufCodecs.FLOAT, Condition::minRelativeSpeed, Condition::new);

        public boolean test(int ticksUsed, double attackerSpeed, double relativeSpeed, double entityFactor) {
            return ticksUsed <= this.maxDurationTicks && attackerSpeed >= (double)this.minSpeed * entityFactor && relativeSpeed >= (double)this.minRelativeSpeed * entityFactor;
        }

        public static Optional<Condition> ofAttackerSpeed(int untilTicks, float minAttackerSpeed) {
            return Optional.of(new Condition(untilTicks, minAttackerSpeed, 0.0f));
        }

        public static Optional<Condition> ofRelativeSpeed(int untilTicks, float minRelativeSpeed) {
            return Optional.of(new Condition(untilTicks, 0.0f, minRelativeSpeed));
        }
    }
}

