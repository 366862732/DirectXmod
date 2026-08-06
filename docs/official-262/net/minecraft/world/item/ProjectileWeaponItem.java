/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class ProjectileWeaponItem
extends Item {
    public static final Predicate<ItemStack> ARROW_ONLY = itemStack -> itemStack.is(ItemTags.ARROWS);
    public static final Predicate<ItemStack> ARROW_OR_FIREWORK = ARROW_ONLY.or(itemStack -> itemStack.is(Items.FIREWORK_ROCKET));

    public ProjectileWeaponItem(Item.Properties properties) {
        super(properties);
    }

    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return this.getAllSupportedProjectiles();
    }

    public abstract Predicate<ItemStack> getAllSupportedProjectiles();

    public static ItemStack getHeldProjectile(LivingEntity entity, Predicate<ItemStack> valid) {
        if (valid.test(entity.getItemInHand(InteractionHand.OFF_HAND))) {
            return entity.getItemInHand(InteractionHand.OFF_HAND);
        }
        if (valid.test(entity.getItemInHand(InteractionHand.MAIN_HAND))) {
            return entity.getItemInHand(InteractionHand.MAIN_HAND);
        }
        return ItemStack.EMPTY;
    }

    public abstract int getDefaultProjectileRange();

    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles, float power, float uncertainty, boolean isCrit, @Nullable LivingEntity targetOverride) {
        float maxAngle = EnchantmentHelper.processProjectileSpread(level, weapon, shooter, 0.0f);
        float angleStep = projectiles.size() == 1 ? 0.0f : 2.0f * maxAngle / (float)(projectiles.size() - 1);
        float angleOffset = (float)((projectiles.size() - 1) % 2) * angleStep / 2.0f;
        float direction = 1.0f;
        for (int i = 0; i < projectiles.size(); ++i) {
            ItemStack projectile = projectiles.get(i);
            if (projectile.isEmpty()) continue;
            float angle = angleOffset + direction * (float)((i + 1) / 2) * angleStep;
            direction = -direction;
            int index = i;
            Projectile.spawnProjectile(this.createProjectile(level, shooter, weapon, projectile, isCrit), level, projectile, projectileEntity -> this.shootProjectile(shooter, (Projectile)projectileEntity, index, power, uncertainty, angle, targetOverride));
            weapon.hurtAndBreak(this.getDurabilityUse(projectile), shooter, hand.asEquipmentSlot());
            if (weapon.isEmpty()) break;
        }
    }

    protected int getDurabilityUse(ItemStack projectile) {
        return 1;
    }

    protected abstract void shootProjectile(LivingEntity var1, Projectile var2, int var3, float var4, float var5, float var6, @Nullable LivingEntity var7);

    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
        ArrowItem arrowItem;
        Object arrow;
        Item item = projectile.getItem();
        if (item instanceof ArrowItem) {
            arrow = (ArrowItem)item;
            arrowItem = arrow;
        } else {
            arrowItem = (ArrowItem)Items.ARROW;
        }
        ArrowItem arrowItem2 = arrowItem;
        arrow = arrowItem2.createArrow(level, projectile, shooter, weapon);
        if (isCrit) {
            ((AbstractArrow)arrow).setCritArrow(true);
        }
        return arrow;
    }

    protected static List<ItemStack> draw(ItemStack weapon, ItemStack projectile, LivingEntity shooter) {
        int n;
        if (projectile.isEmpty()) {
            return List.of();
        }
        Level level = shooter.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            n = EnchantmentHelper.processProjectileCount(serverLevel, weapon, shooter, 1);
        } else {
            n = 1;
        }
        int numProjectiles = n;
        ArrayList<ItemStack> drawn = new ArrayList<ItemStack>(numProjectiles);
        ItemStack projectileCopy = projectile.copy();
        for (int i = 0; i < numProjectiles; ++i) {
            ItemStack drawnStack = ProjectileWeaponItem.useAmmo(weapon, i == 0 ? projectile : projectileCopy, shooter, i > 0);
            if (drawnStack.isEmpty()) continue;
            drawn.add(drawnStack);
        }
        return drawn;
    }

    protected static ItemStack useAmmo(ItemStack weapon, ItemStack projectile, LivingEntity holder, boolean forceInfinite) {
        int ammoToUse;
        Level level;
        if (!forceInfinite && !holder.hasInfiniteMaterials() && (level = holder.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            v0 = EnchantmentHelper.processAmmoUse(serverLevel, weapon, projectile, 1);
        } else {
            v0 = ammoToUse = 0;
        }
        if (ammoToUse > projectile.getCount()) {
            return ItemStack.EMPTY;
        }
        if (ammoToUse == 0) {
            ItemStack copy = projectile.copyWithCount(1);
            copy.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            return copy;
        }
        ItemStack used = projectile.split(ammoToUse);
        if (projectile.isEmpty() && holder instanceof Player) {
            Player player = (Player)holder;
            player.getInventory().removeItem(projectile);
        }
        return used;
    }
}

