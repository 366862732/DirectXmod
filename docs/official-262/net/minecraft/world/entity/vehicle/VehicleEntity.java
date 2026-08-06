/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.vehicle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;

public abstract class VehicleEntity
extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);

    public VehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isRemoved()) {
            return true;
        }
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + damage * 10.0f);
        this.gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        var6_4 = source.getEntity();
        if (!(var6_4 instanceof Player)) ** GOTO lbl-1000
        player = (Player)var6_4;
        if (player.getAbilities().instabuild) {
            v0 = true;
        } else lbl-1000:
        // 2 sources

        {
            v0 = creativePlayer = false;
        }
        if (creativePlayer == false && this.getDamage() > 40.0f || this.shouldSourceDestroy(source)) {
            this.destroy(level, source);
        } else if (creativePlayer) {
            this.discard();
        }
        return true;
    }

    protected boolean shouldSourceDestroy(DamageSource source) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return explosion.getIndirectSourceEntity() instanceof Mob && explosion.level().getGameRules().get(GameRules.MOB_GRIEFING) == false;
    }

    public void destroy(ServerLevel level, Item dropItem) {
        this.kill(level);
        if (!level.getGameRules().get(GameRules.ENTITY_DROPS).booleanValue()) {
            return;
        }
        ItemStack itemStack = new ItemStack(dropItem);
        itemStack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        this.spawnAtLocation(level, itemStack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_ID_HURT, 0);
        entityData.define(DATA_ID_HURTDIR, 1);
        entityData.define(DATA_ID_DAMAGE, Float.valueOf(0.0f));
    }

    public void setHurtTime(int hurtTime) {
        this.entityData.set(DATA_ID_HURT, hurtTime);
    }

    public void setHurtDir(int hurtDir) {
        this.entityData.set(DATA_ID_HURTDIR, hurtDir);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_ID_DAMAGE, Float.valueOf(damage));
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE).floatValue();
    }

    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURTDIR);
    }

    protected void destroy(ServerLevel level, DamageSource source) {
        this.destroy(level, this.getDropItem());
    }

    @Override
    public int getDimensionChangingDelay() {
        return 10;
    }

    protected abstract Item getDropItem();
}

