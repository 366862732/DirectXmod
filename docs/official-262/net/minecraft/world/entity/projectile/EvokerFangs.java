/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.projectile;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EvokerFangs
extends Entity
implements TraceableEntity {
    public static final int ATTACK_DURATION = 20;
    public static final int LIFE_OFFSET = 2;
    public static final int ATTACK_TRIGGER_TICKS = 14;
    private static final int DEFAULT_WARMUP_DELAY = 0;
    private int warmupDelayTicks = 0;
    private boolean sentSpikeEvent;
    private int lifeTicks = 22;
    private boolean clientSideAttackStarted;
    private @Nullable EntityReference<LivingEntity> owner;

    public EvokerFangs(EntityType<? extends EvokerFangs> type, Level level) {
        super(type, level);
    }

    public EvokerFangs(Level level, double x, double y, double z, float rotaionRadians, int warmupDelayTicks, LivingEntity owner) {
        this((EntityType<? extends EvokerFangs>)EntityTypes.EVOKER_FANGS, level);
        this.warmupDelayTicks = warmupDelayTicks;
        this.setOwner(owner);
        this.setYRot(rotaionRadians * 57.295776f);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = EntityReference.of(owner);
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.owner, this.level());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.warmupDelayTicks = input.getIntOr("Warmup", 0);
        this.owner = EntityReference.read(input, "Owner");
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Warmup", this.warmupDelayTicks);
        EntityReference.store(this.owner, output, "Owner");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.clientSideAttackStarted) {
                --this.lifeTicks;
                if (this.lifeTicks == 14) {
                    for (int i = 0; i < 12; ++i) {
                        double x = this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * (double)this.getBbWidth() * 0.5;
                        double y = this.getY() + 0.05 + this.random.nextDouble();
                        double z = this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * (double)this.getBbWidth() * 0.5;
                        double xd = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        double yd = 0.3 + this.random.nextDouble() * 0.3;
                        double zd = (this.random.nextDouble() * 2.0 - 1.0) * 0.3;
                        this.level().addParticle(ParticleTypes.CRIT, x, y + 1.0, z, xd, yd, zd);
                    }
                }
            }
        } else if (--this.warmupDelayTicks < 0) {
            if (this.warmupDelayTicks == -8) {
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2));
                for (LivingEntity entity : entities) {
                    this.dealDamageTo(entity);
                }
            }
            if (!this.sentSpikeEvent) {
                this.level().broadcastEntityEvent(this, (byte)4);
                this.sentSpikeEvent = true;
            }
            if (--this.lifeTicks < 0) {
                this.discard();
            }
        }
    }

    private void dealDamageTo(LivingEntity entity) {
        LivingEntity currentOwner = this.getOwner();
        if (!entity.isAlive() || entity.isInvulnerable() || entity == currentOwner) {
            return;
        }
        if (currentOwner == null) {
            entity.hurt(this.damageSources().magic(), 6.0f);
        } else {
            ServerLevel serverLevel;
            if (currentOwner.isAlliedTo(entity)) {
                return;
            }
            DamageSource damageSource = this.damageSources().indirectMagic(this, currentOwner);
            Level level = this.level();
            if (level instanceof ServerLevel && entity.hurtServer(serverLevel = (ServerLevel)level, damageSource, 6.0f)) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 4) {
            this.clientSideAttackStarted = true;
            if (!this.isSilent()) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_FANGS_ATTACK, this.getSoundSource(), 1.0f, this.random.nextFloat() * 0.2f + 0.85f, false);
            }
        }
    }

    public float getAnimationProgress(float a) {
        if (!this.clientSideAttackStarted) {
            return 0.0f;
        }
        int remainingLife = this.lifeTicks - 2;
        if (remainingLife <= 0) {
            return 1.0f;
        }
        return 1.0f - ((float)remainingLife - a) / 20.0f;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}

