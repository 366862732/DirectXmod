/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public interface Bucketable {
    public boolean fromBucket();

    public void setFromBucket(boolean var1);

    public void saveToBucketTag(ItemStack var1);

    public void loadFromBucketTag(CompoundTag var1);

    public ItemStack getBucketItemStack();

    public SoundEvent getPickupSound();

    @Deprecated
    public static void saveDefaultDataToBucketTag(Mob entity, ItemStack bucket) {
        bucket.copyFrom(DataComponents.CUSTOM_NAME, entity);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            if (entity.isNoAi()) {
                tag.putBoolean("NoAI", entity.isNoAi());
            }
            if (entity.isSilent()) {
                tag.putBoolean("Silent", entity.isSilent());
            }
            if (entity.isNoGravity()) {
                tag.putBoolean("NoGravity", entity.isNoGravity());
            }
            if (entity.hasGlowingTag()) {
                tag.putBoolean("Glowing", entity.hasGlowingTag());
            }
            if (entity.isInvulnerable()) {
                tag.putBoolean("Invulnerable", entity.isInvulnerable());
            }
            if (entity.isPersistenceRequired()) {
                tag.putBoolean("PersistenceRequired", entity.isPersistenceRequired());
            }
            tag.putFloat("Health", entity.getHealth());
        });
    }

    @Deprecated
    public static void loadDefaultDataFromBucketTag(Mob entity, CompoundTag tag) {
        tag.getBoolean("NoAI").ifPresent(entity::setNoAi);
        tag.getBoolean("Silent").ifPresent(entity::setSilent);
        tag.getBoolean("NoGravity").ifPresent(entity::setNoGravity);
        tag.getBoolean("Glowing").ifPresent(entity::setGlowingTag);
        tag.getBoolean("Invulnerable").ifPresent(entity::setInvulnerable);
        tag.getBoolean("PersistenceRequired").ifPresent(required -> {
            if (required.booleanValue()) {
                entity.setPersistenceRequired();
            }
        });
        tag.getFloat("Health").ifPresent(entity::setHealth);
    }

    public static <T extends LivingEntity> Optional<InteractionResult> bucketMobPickup(Player player, InteractionHand hand, T pickupEntity) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (((Bucketable)((Object)pickupEntity)).canBePickedUpWithBucket(itemStack) && pickupEntity.isAlive()) {
            pickupEntity.playSound(((Bucketable)((Object)pickupEntity)).getPickupSound(), 1.0f, 1.0f);
            ItemStack bucket = ((Bucketable)((Object)pickupEntity)).getBucketItemStack();
            ((Bucketable)((Object)pickupEntity)).saveToBucketTag(bucket);
            ItemStack result = ItemUtils.createFilledResult(itemStack, player, bucket, false);
            player.setItemInHand(hand, result);
            Level level = pickupEntity.level();
            if (!level.isClientSide() && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, bucket);
            }
            if (pickupEntity instanceof Leashable) {
                Leashable leashable = (Leashable)((Object)pickupEntity);
                leashable.dropLeash();
            }
            pickupEntity.discard();
            return Optional.of(InteractionResult.SUCCESS);
        }
        return Optional.empty();
    }

    default public boolean canBePickedUpWithBucket(ItemStack itemStack) {
        return itemStack.getItem() == Items.WATER_BUCKET;
    }
}

