/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;

public record PiercingWeapon(boolean dealsKnockback, boolean dismounts, Optional<Holder<SoundEvent>> sound, Optional<Holder<SoundEvent>> hitSound) {
    public static final Codec<PiercingWeapon> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("deals_knockback", (Object)true).forGetter(PiercingWeapon::dealsKnockback), (App)Codec.BOOL.optionalFieldOf("dismounts", (Object)false).forGetter(PiercingWeapon::dismounts), (App)SoundEvent.CODEC.optionalFieldOf("sound").forGetter(PiercingWeapon::sound), (App)SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(PiercingWeapon::hitSound)).apply((Applicative)i, PiercingWeapon::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PiercingWeapon> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, PiercingWeapon::dealsKnockback, ByteBufCodecs.BOOL, PiercingWeapon::dismounts, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), PiercingWeapon::sound, SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional), PiercingWeapon::hitSound, PiercingWeapon::new);

    public void makeSound(Entity causer) {
        this.sound.ifPresent(s -> causer.level().playSound(causer, causer.getX(), causer.getY(), causer.getZ(), (Holder<SoundEvent>)s, causer.getSoundSource(), 1.0f, 1.0f));
    }

    public void makeHitSound(Entity causer) {
        this.hitSound.ifPresent(s -> causer.level().playSound(null, causer.getX(), causer.getY(), causer.getZ(), (Holder<SoundEvent>)s, causer.getSoundSource(), 1.0f, 1.0f));
    }

    public static boolean canHitEntity(Entity jabber, Entity target) {
        if (target.isInvulnerableToPiercingWeapon() || !target.isAlive()) {
            return false;
        }
        if (target instanceof Interaction) {
            return true;
        }
        if (!target.canBeHitByProjectile()) {
            return false;
        }
        if (target instanceof Player) {
            Player jabbingPlayer;
            Player targetPlayer = (Player)target;
            if (jabber instanceof Player && !(jabbingPlayer = (Player)jabber).canHarmPlayer(targetPlayer)) {
                return false;
            }
        }
        return !jabber.isPassengerOfSameVehicle(target);
    }

    public void attack(LivingEntity attacker, EquipmentSlot hand) {
        float damage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack weaponItem = attacker.getItemBySlot(hand);
        AttackRange attackRange = attacker.getAttackRangeWith(weaponItem);
        boolean hitSomething = false;
        for (EntityHitResult hitResult : (Collection)ProjectileUtil.getHitEntitiesAlong(attacker, attackRange, e1 -> PiercingWeapon.canHitEntity(attacker, e1), ClipContext.Block.COLLIDER).map(a -> List.of(), e -> e)) {
            hitSomething |= attacker.stabAttack(hand, hitResult.getEntity(), damage, true, this.dealsKnockback, this.dismounts);
        }
        attacker.onAttack();
        attacker.postPiercingAttack();
        if (hitSomething) {
            this.makeHitSound(attacker);
        }
        this.makeSound(attacker);
        attacker.swing(InteractionHand.MAIN_HAND, false);
    }
}

