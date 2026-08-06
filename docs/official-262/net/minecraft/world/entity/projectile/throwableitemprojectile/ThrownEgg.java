/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg
extends ThrowableItemProjectile {
    private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0f, 0.0f);

    public ThrownEgg(EntityType<? extends ThrownEgg> type, Level level) {
        super((EntityType<? extends ThrowableItemProjectile>)type, level);
    }

    public ThrownEgg(Level level, LivingEntity mob, ItemStack itemStack) {
        super(EntityTypes.EGG, mob, level, itemStack);
    }

    public ThrownEgg(Level level, double x, double y, double z, ItemStack itemStack) {
        super(EntityTypes.EGG, x, y, z, level, itemStack);
    }

    @Override
    public void handleEntityEvent(byte id) {
        ItemStack item;
        if (id == 3 && !(item = this.getItem()).isEmpty()) {
            ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(item));
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(breakParticle, this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5) * 0.08, ((double)this.random.nextFloat() - 0.5) * 0.08, ((double)this.random.nextFloat() - 0.5) * 0.08);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        hitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0f);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            if (this.random.nextInt(8) == 0) {
                int count = 1;
                if (this.random.nextInt(32) == 0) {
                    count = 4;
                }
                for (int i = 0; i < count; ++i) {
                    Chicken chicken = EntityTypes.CHICKEN.create(this.level(), EntitySpawnReason.TRIGGERED);
                    if (chicken == null) continue;
                    chicken.setAge(-24000);
                    chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                    Optional.ofNullable(this.getItem().get(DataComponents.CHICKEN_VARIANT)).ifPresent(chicken::setVariant);
                    if (!chicken.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) break;
                    this.level().addFreshEntity(chicken);
                }
            }
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }
}

