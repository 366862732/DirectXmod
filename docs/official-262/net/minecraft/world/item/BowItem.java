/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class BowItem
extends ProjectileWeaponItem {
    public static final int MAX_DRAW_DURATION = 20;
    public static final int DEFAULT_RANGE = 15;

    public BowItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = (Player)entity;
        ItemStack projectile = player.getProjectile(itemStack);
        if (projectile.isEmpty()) {
            return false;
        }
        int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
        float pow = BowItem.getPowerForTime(timeHeld);
        if ((double)pow < 0.1) {
            return false;
        }
        List<ItemStack> firedProjectiles = BowItem.draw(itemStack, projectile, player);
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (!firedProjectiles.isEmpty()) {
                this.shoot(serverLevel, player, player.getUsedItemHand(), itemStack, firedProjectiles, pow * 3.0f, 1.0f, pow == 1.0f, null);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + pow * 0.5f);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectileEntity, int index, float power, float uncertainty, float angle, @Nullable LivingEntity targetOverrride) {
        projectileEntity.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0f, power, uncertainty);
    }

    public static float getPowerForTime(int timeHeld) {
        float pow = (float)timeHeld / 20.0f;
        if ((pow = (pow * pow + pow * 2.0f) / 3.0f) > 1.0f) {
            pow = 1.0f;
        }
        return pow;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        boolean foundProjectile;
        ItemStack itemStack = player.getItemInHand(hand);
        boolean bl = foundProjectile = !player.getProjectile(itemStack).isEmpty();
        if (player.hasInfiniteMaterials() || foundProjectile) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }
}

