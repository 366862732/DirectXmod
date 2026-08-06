/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MaceItem
extends Item {
    private static final int DEFAULT_ATTACK_DAMAGE = 5;
    private static final float DEFAULT_ATTACK_SPEED = -3.4f;
    public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5f;
    private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0f;
    public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5f;
    private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7f;

    public MaceItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4f, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
    }

    public static Tool createToolProperties() {
        return new Tool(List.of(), 1.0f, 2, false);
    }

    @Override
    public void hurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
        if (MaceItem.canSmashAttack(attacker)) {
            ServerPlayer player;
            ServerLevel level = (ServerLevel)attacker.level();
            attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01f));
            attacker.setIgnoreFallDamageFromCurrentImpulse(true, this.calculateImpactPosition(attacker));
            if (attacker instanceof ServerPlayer) {
                player = (ServerPlayer)attacker;
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            if (mob.onGround()) {
                if (attacker instanceof ServerPlayer) {
                    player = (ServerPlayer)attacker;
                    player.setSpawnExtraParticlesOnFall(true);
                }
                SoundEvent sound = attacker.fallDistance > 5.0 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundSource(), 1.0f, 1.0f);
            } else {
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.MACE_SMASH_AIR, attacker.getSoundSource(), 1.0f, 1.0f);
            }
            MaceItem.knockback(level, attacker, mob);
        }
    }

    private Vec3 calculateImpactPosition(LivingEntity attacker) {
        if (attacker.isIgnoringFallDamageFromCurrentImpulse() && attacker.currentImpulseImpactPos.y <= attacker.position().y) {
            return attacker.currentImpulseImpactPos;
        }
        return attacker.position();
    }

    @Override
    public void postHurtEnemy(ItemStack itemStack, LivingEntity mob, LivingEntity attacker) {
        if (MaceItem.canSmashAttack(attacker)) {
            attacker.resetFallDistance();
        }
    }

    @Override
    public float getAttackDamageBonus(Entity victim, float ignoredDamage, DamageSource damageSource) {
        Entity entity = damageSource.getDirectEntity();
        if (!(entity instanceof LivingEntity)) {
            return 0.0f;
        }
        LivingEntity attacker = (LivingEntity)entity;
        if (!MaceItem.canSmashAttack(attacker)) {
            return 0.0f;
        }
        double fallHeightThreshold1 = 3.0;
        double fallHeightThreshold2 = 8.0;
        double fallDistance = attacker.fallDistance;
        double damage = fallDistance <= 3.0 ? 4.0 * fallDistance : (fallDistance <= 8.0 ? 12.0 + 2.0 * (fallDistance - 3.0) : 22.0 + fallDistance - 8.0);
        Level level = attacker.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            return (float)(damage + (double)EnchantmentHelper.modifyFallBasedDamage(level2, attacker.getWeaponItem(), victim, damageSource, 0.0f) * fallDistance);
        }
        return (float)damage;
    }

    private static void knockback(Level level, Entity attacker, Entity entity) {
        level.levelEvent(2013, entity.getOnPos(), 750);
        level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.5), MaceItem.knockbackPredicate(attacker, entity)).forEach(nearby -> {
            Vec3 direction = nearby.position().subtract(entity.position());
            double knockbackPower = MaceItem.getKnockbackPower(attacker, nearby, direction);
            Vec3 knockbackVector = direction.normalize().scale(knockbackPower);
            if (knockbackPower > 0.0) {
                nearby.push(knockbackVector.x, 0.7f, knockbackVector.z);
                if (nearby instanceof ServerPlayer) {
                    ServerPlayer otherPlayer = (ServerPlayer)nearby;
                    otherPlayer.connection.send(new ClientboundSetEntityMotionPacket(otherPlayer));
                }
            }
        });
    }

    private static Predicate<LivingEntity> knockbackPredicate(Entity attacker, Entity entity) {
        return arg_0 -> MaceItem.lambda$knockbackPredicate$0(attacker, entity, arg_0);
    }

    private static double getKnockbackPower(Entity attacker, LivingEntity nearby, Vec3 direction) {
        return (3.5 - direction.length()) * (double)0.7f * (double)(attacker.fallDistance > 5.0 ? 2 : 1) * (1.0 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    public static boolean canSmashAttack(LivingEntity attacker) {
        return attacker.fallDistance > 1.5 && !attacker.isFallFlying();
    }

    @Override
    public @Nullable DamageSource getItemDamageSource(LivingEntity attacker) {
        if (MaceItem.canSmashAttack(attacker)) {
            return attacker.damageSources().mace(attacker);
        }
        return super.getItemDamageSource(attacker);
    }

    /*
     * Unable to fully structure code
     */
    private static /* synthetic */ boolean lambda$knockbackPredicate$0(Entity attacker, Entity entity, LivingEntity nearby) {
        notSpectator = nearby.isSpectator() == false;
        notPlayer = nearby != attacker && nearby != entity;
        v0 = notAlliedToPlayer = attacker.isAlliedTo(nearby) == false;
        if (!(nearby instanceof TamableAnimal)) ** GOTO lbl-1000
        animal = (TamableAnimal)nearby;
        if (!(entity instanceof LivingEntity)) ** GOTO lbl-1000
        livingAttacker = (LivingEntity)entity;
        if (animal.isTame() && animal.isOwnedBy(livingAttacker)) {
            v1 = true;
        } else lbl-1000:
        // 3 sources

        {
            v1 = false;
        }
        notTamedByPlayer = v1 == false;
        notArmorStand = nearby instanceof ArmorStand == false || (armorStand = (ArmorStand)nearby).isMarker() == false;
        withinRange = entity.distanceToSqr(nearby) <= Math.pow(3.5, 2.0);
        notFlyingInCreative = (nearby instanceof Player != false && (player = (Player)nearby).isCreative() != false && player.getAbilities().flying != false) == false;
        return notSpectator != false && notPlayer != false && notAlliedToPlayer != false && notTamedByPlayer != false && notArmorStand != false && withinRange != false && notFlyingInCreative != false;
    }
}

