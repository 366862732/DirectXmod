/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class MinecartTNT
extends AbstractMinecart {
    private static final String TAG_EXPLOSION_POWER = "explosion_power";
    private static final String TAG_EXPLOSION_SPEED_FACTOR = "explosion_speed_factor";
    private static final float DEFAULT_EXPLOSION_POWER_BASE = 4.0f;
    private static final float DEFAULT_EXPLOSION_SPEED_FACTOR = 1.0f;
    private @Nullable DamageSource ignitionSource;
    private int fuse = -1;
    private float explosionPowerBase = 4.0f;
    private float explosionSpeedFactor = 1.0f;

    public MinecartTNT(EntityType<? extends MinecartTNT> type, Level level) {
        super(type, level);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.TNT.defaultBlockState();
    }

    @Override
    public void tick() {
        double speedSqr;
        super.tick();
        if (this.fuse > 0) {
            --this.fuse;
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
        } else if (this.fuse == 0) {
            this.explode(this.ignitionSource, this.getDeltaMovement().horizontalDistanceSqr());
        }
        if (this.horizontalCollision && (speedSqr = this.getDeltaMovement().horizontalDistanceSqr()) >= (double)0.01f) {
            this.explode(this.ignitionSource, speedSqr);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        AbstractArrow projectile;
        Entity sourceEntity = source.getDirectEntity();
        if (sourceEntity instanceof AbstractArrow && (projectile = (AbstractArrow)sourceEntity).isOnFire()) {
            DamageSource damageSource = this.damageSources().explosion(this, source.getEntity());
            this.explode(damageSource, projectile.getDeltaMovement().lengthSqr());
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
        if (MinecartTNT.damageSourceIgnitesTnt(source) || speedSqr >= (double)0.01f) {
            if (this.fuse < 0) {
                this.primeFuse(source);
                this.fuse = this.random.nextInt(20) + this.random.nextInt(20);
            }
            return;
        }
        this.destroy(level, this.getDropItem());
    }

    @Override
    protected Item getDropItem() {
        return Items.TNT_MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.TNT_MINECART);
    }

    protected void explode(@Nullable DamageSource damageSource, double speedSqr) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            if (level2.getGameRules().get(GameRules.TNT_EXPLODES).booleanValue()) {
                double speed = Math.min(Math.sqrt(speedSqr), 5.0);
                level2.explode(this, damageSource, null, this.getX(), this.getY(), this.getZ(), (float)((double)this.explosionPowerBase + (double)this.explosionSpeedFactor * this.random.nextDouble() * 1.5 * speed), false, Level.ExplosionInteraction.TNT);
                this.discard();
            } else if (this.isPrimed()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        if (fallDistance >= 3.0) {
            double power = fallDistance / 10.0;
            this.explode(this.ignitionSource, power * power);
        }
        return super.causeFallDamage(fallDistance, damageModifier, damageSource);
    }

    @Override
    public void activateMinecart(ServerLevel level, int xt, int yt, int zt, boolean state) {
        if (state && this.fuse < 0) {
            this.primeFuse(null);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 70) {
            this.primeFuse(null);
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void primeFuse(@Nullable DamageSource source) {
        ServerLevel serverLevel;
        Level level = this.level();
        if (level instanceof ServerLevel && !(serverLevel = (ServerLevel)level).getGameRules().get(GameRules.TNT_EXPLODES).booleanValue()) {
            return;
        }
        this.fuse = 80;
        if (!this.level().isClientSide()) {
            if (source != null && this.ignitionSource == null) {
                this.ignitionSource = this.damageSources().explosion(this, source.getEntity());
            }
            this.level().broadcastEntityEvent(this, (byte)70);
            if (!this.isSilent()) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
    }

    public int getFuse() {
        return this.fuse;
    }

    public boolean isPrimed() {
        return this.fuse > -1;
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid, float resistance) {
        if (this.isPrimed() && (block.is(BlockTags.RAILS) || level.getBlockState(pos.above()).is(BlockTags.RAILS))) {
            return 0.0f;
        }
        return super.getBlockExplosionResistance(explosion, level, pos, block, fluid, resistance);
    }

    @Override
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        if (this.isPrimed() && (state.is(BlockTags.RAILS) || level.getBlockState(pos.above()).is(BlockTags.RAILS))) {
            return false;
        }
        return super.shouldBlockExplode(explosion, level, pos, state, power);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fuse = input.getIntOr("fuse", -1);
        this.explosionPowerBase = Mth.clamp(input.getFloatOr(TAG_EXPLOSION_POWER, 4.0f), 0.0f, 128.0f);
        this.explosionSpeedFactor = Mth.clamp(input.getFloatOr(TAG_EXPLOSION_SPEED_FACTOR, 1.0f), 0.0f, 128.0f);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("fuse", this.fuse);
        if (this.explosionPowerBase != 4.0f) {
            output.putFloat(TAG_EXPLOSION_POWER, this.explosionPowerBase);
        }
        if (this.explosionSpeedFactor != 1.0f) {
            output.putFloat(TAG_EXPLOSION_SPEED_FACTOR, this.explosionSpeedFactor);
        }
    }

    @Override
    protected boolean shouldSourceDestroy(DamageSource source) {
        return MinecartTNT.damageSourceIgnitesTnt(source);
    }

    private static boolean damageSourceIgnitesTnt(DamageSource source) {
        Entity entity = source.getDirectEntity();
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            return projectile.isOnFire();
        }
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION);
    }
}

