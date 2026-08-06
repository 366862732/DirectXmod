/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TridentItem
extends Item
implements ProjectileItem {
    public static final int THROW_THRESHOLD_TIME = 10;
    public static final float BASE_DAMAGE = 8.0f;
    public static final float PROJECTILE_SHOOT_POWER = 2.5f;

    public TridentItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9f, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build();
    }

    public static Tool createToolProperties() {
        return new Tool(List.of(), 1.0f, 2, false);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity user) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = (Player)entity;
        int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
        if (timeHeld < 10) {
            return false;
        }
        float riptideStrength = EnchantmentHelper.getTridentSpinAttackStrength(itemStack, player);
        if (riptideStrength > 0.0f && (!player.isInWaterOrRain() || player.isPassenger())) {
            return false;
        }
        if (itemStack.nextDamageWillBreak()) {
            return false;
        }
        Holder<SoundEvent> sound = EnchantmentHelper.pickHighestLevel(itemStack, EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            itemStack.hurtWithoutBreaking(1, player);
            if (riptideStrength == 0.0f) {
                ItemStack thrownItemStack = itemStack.consumeAndReturn(1, player);
                ThrownTrident trident = Projectile.spawnProjectileFromRotation(ThrownTrident::new, serverLevel, thrownItemStack, player, 0.0f, 2.5f, 1.0f);
                if (player.hasInfiniteMaterials()) {
                    trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.playSound(null, trident, sound.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                return true;
            }
        }
        if (riptideStrength > 0.0f) {
            float yRot = player.getYRot();
            float xRot = player.getXRot();
            float xd = -Mth.sin(yRot * ((float)Math.PI / 180)) * Mth.cos(xRot * ((float)Math.PI / 180));
            float yd = -Mth.sin(xRot * ((float)Math.PI / 180));
            float zd = Mth.cos(yRot * ((float)Math.PI / 180)) * Mth.cos(xRot * ((float)Math.PI / 180));
            float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            player.push(xd *= riptideStrength / dist, yd *= riptideStrength / dist, zd *= riptideStrength / dist);
            player.startAutoSpinAttack(20, 8.0f, itemStack);
            if (player.onGround()) {
                float heightDifference = 1.1999999f;
                player.move(MoverType.SELF, new Vec3(0.0, 1.1999999284744263, 0.0));
            }
            level.playSound(null, player, sound.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.nextDamageWillBreak()) {
            return InteractionResult.FAIL;
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(itemInHand, player) > 0.0f && !player.isInWaterOrRain()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
        ThrownTrident trident = new ThrownTrident(level, position.x(), position.y(), position.z(), itemStack.copyWithCount(1));
        trident.pickup = AbstractArrow.Pickup.ALLOWED;
        return trident;
    }
}

