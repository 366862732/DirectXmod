/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.boss.wither;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WitherBoss
extends Monster
implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_B = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_C = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A, DATA_TARGET_B, DATA_TARGET_C);
    private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(WitherBoss.class, EntityDataSerializers.INT);
    private static final int INVULNERABLE_TICKS = 220;
    private static final int DEFAULT_INVULNERABLE_TICKS = 0;
    private final float[] xRotHeads = new float[2];
    private final float[] yRotHeads = new float[2];
    private final float[] xRotOHeads = new float[2];
    private final float[] yRotOHeads = new float[2];
    private final int[] nextHeadUpdate = new int[2];
    private final int[] idleHeadUpdates = new int[2];
    private int destroyBlocksTick;
    private final ServerBossEvent bossEvent = Util.make(new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS), e -> e.setDarkenScreen(true));
    private static final TargetingConditions.Selector LIVING_ENTITY_SELECTOR = (target, level) -> !target.is(EntityTypeTags.WITHER_FRIENDS) && target.attackable();
    private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(20.0).selector(LIVING_ENTITY_SELECTOR);

    public WitherBoss(EntityType<? extends WitherBoss> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
        this.moveControl = new FlyingMoveControl<WitherBoss>(this, 10, false);
        this.setHealth(this.getMaxHealth());
        this.xpReward = 50;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, level);
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(true);
        return flyingPathNavigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WitherDoNothingGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 20.0f));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<LivingEntity>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TARGET_A, 0);
        entityData.define(DATA_TARGET_B, 0);
        entityData.define(DATA_TARGET_C, 0);
        entityData.define(DATA_ID_INV, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Invul", this.getInvulnerableTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setInvulnerableTicks(input.getIntOr("Invul", 0));
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void aiStep() {
        int i;
        Entity entity;
        Vec3 deltaMovement = this.getDeltaMovement().multiply(1.0, 0.6, 1.0);
        if (!this.level().isClientSide() && this.getAlternativeTarget(0) > 0 && (entity = this.level().getEntity(this.getAlternativeTarget(0))) != null) {
            double yd = deltaMovement.y;
            if (this.getY() < entity.getY() || !this.isPowered() && this.getY() < entity.getY() + 5.0) {
                yd = Math.max(0.0, yd);
                yd += 0.3 - yd * (double)0.6f;
            }
            deltaMovement = new Vec3(deltaMovement.x, yd, deltaMovement.z);
            Vec3 delta = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
            if (delta.horizontalDistanceSqr() > 9.0) {
                Vec3 scale = delta.normalize();
                deltaMovement = deltaMovement.add(scale.x * 0.3 - deltaMovement.x * 0.6, 0.0, scale.z * 0.3 - deltaMovement.z * 0.6);
            }
        }
        this.setDeltaMovement(deltaMovement);
        if (deltaMovement.horizontalDistanceSqr() > 0.05) {
            this.setYRot((float)Mth.atan2(deltaMovement.z, deltaMovement.x) * 57.295776f - 90.0f);
        }
        super.aiStep();
        for (i = 0; i < 2; ++i) {
            this.yRotOHeads[i] = this.yRotHeads[i];
            this.xRotOHeads[i] = this.xRotHeads[i];
        }
        for (i = 0; i < 2; ++i) {
            int entityId = this.getAlternativeTarget(i + 1);
            Entity entity2 = null;
            if (entityId > 0) {
                entity2 = this.level().getEntity(entityId);
            }
            if (entity2 != null) {
                double hx = this.getHeadX(i + 1);
                double hy = this.getHeadY(i + 1);
                double hz = this.getHeadZ(i + 1);
                double xd = entity2.getX() - hx;
                double yd = entity2.getEyeY() - hy;
                double zd = entity2.getZ() - hz;
                double sd = Math.sqrt(xd * xd + zd * zd);
                float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
                float xRotD = (float)(-(Mth.atan2(yd, sd) * 57.2957763671875));
                this.xRotHeads[i] = this.rotlerp(this.xRotHeads[i], xRotD, 40.0f);
                this.yRotHeads[i] = this.rotlerp(this.yRotHeads[i], yRotD, 10.0f);
                continue;
            }
            this.yRotHeads[i] = this.rotlerp(this.yRotHeads[i], this.yBodyRot, 10.0f);
        }
        boolean isPowered = this.isPowered();
        for (int i2 = 0; i2 < 3; ++i2) {
            double hx = this.getHeadX(i2);
            double hy = this.getHeadY(i2);
            double hz = this.getHeadZ(i2);
            float radius = 0.3f * this.getScale();
            this.level().addParticle(ParticleTypes.SMOKE, hx + this.random.nextGaussian() * (double)radius, hy + this.random.nextGaussian() * (double)radius, hz + this.random.nextGaussian() * (double)radius, 0.0, 0.0, 0.0);
            if (!isPowered || this.level().getRandom().nextInt(4) != 0) continue;
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7f, 0.7f, 0.5f), hx + this.random.nextGaussian() * (double)radius, hy + this.random.nextGaussian() * (double)radius, hz + this.random.nextGaussian() * (double)radius, 0.0, 0.0, 0.0);
        }
        if (this.getInvulnerableTicks() > 0) {
            float height = 3.3f * this.getScale();
            for (int i3 = 0; i3 < 3; ++i3) {
                this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7f, 0.7f, 0.9f), this.getX() + this.random.nextGaussian(), this.getY() + (double)(this.random.nextFloat() * height), this.getZ() + this.random.nextGaussian(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (this.getInvulnerableTicks() > 0) {
            int newCount = this.getInvulnerableTicks() - 1;
            this.bossEvent.setProgress(1.0f - (float)newCount / 220.0f);
            if (newCount <= 0) {
                level.explode((Entity)this, this.getX(), this.getEyeY(), this.getZ(), 7.0f, false, Level.ExplosionInteraction.MOB);
                if (!this.isSilent()) {
                    level.globalLevelEvent(1023, this.blockPosition(), 0);
                }
            }
            this.setInvulnerableTicks(newCount);
            if (this.tickCount % 10 == 0) {
                this.heal(10.0f);
            }
            return;
        }
        super.customServerAiStep(level);
        for (int i = 1; i < 3; ++i) {
            int headTarget;
            if (this.tickCount < this.nextHeadUpdate[i - 1]) continue;
            this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10);
            if (level.getDifficulty() == Difficulty.NORMAL || level.getDifficulty() == Difficulty.HARD) {
                int n = i - 1;
                int n2 = this.idleHeadUpdates[n];
                this.idleHeadUpdates[n] = n2 + 1;
                if (n2 > 15) {
                    float hrange = 10.0f;
                    float vrange = 5.0f;
                    double xt = Mth.nextDouble(this.random, this.getX() - 10.0, this.getX() + 10.0);
                    double yt = Mth.nextDouble(this.random, this.getY() - 5.0, this.getY() + 5.0);
                    double zt = Mth.nextDouble(this.random, this.getZ() - 10.0, this.getZ() + 10.0);
                    this.performRangedAttack(i + 1, xt, yt, zt, true);
                    this.idleHeadUpdates[i - 1] = 0;
                }
            }
            if ((headTarget = this.getAlternativeTarget(i)) > 0) {
                LivingEntity current = (LivingEntity)level.getEntity(headTarget);
                if (current == null || !this.canAttack(current) || this.distanceToSqr(current) > 900.0 || !this.hasLineOfSight(current)) {
                    this.setAlternativeTarget(i, 0);
                    continue;
                }
                this.performRangedAttack(i + 1, current);
                this.nextHeadUpdate[i - 1] = this.tickCount + 40 + this.random.nextInt(20);
                this.idleHeadUpdates[i - 1] = 0;
                continue;
            }
            List<LivingEntity> entities = level.getNearbyEntities(LivingEntity.class, TARGETING_CONDITIONS, this, this.getBoundingBox().inflate(20.0, 8.0, 20.0));
            if (entities.isEmpty()) continue;
            LivingEntity selected = entities.get(this.random.nextInt(entities.size()));
            this.setAlternativeTarget(i, selected.getId());
        }
        if (this.getTarget() != null) {
            this.setAlternativeTarget(0, this.getTarget().getId());
        } else {
            this.setAlternativeTarget(0, 0);
        }
        if (this.destroyBlocksTick > 0) {
            --this.destroyBlocksTick;
            if (this.destroyBlocksTick == 0 && level.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                boolean destroyed = false;
                int width = Mth.floor(this.getBbWidth() / 2.0f + 1.0f);
                int height = Mth.floor(this.getBbHeight());
                for (BlockPos blockPos : BlockPos.betweenClosed(this.getBlockX() - width, this.getBlockY(), this.getBlockZ() - width, this.getBlockX() + width, this.getBlockY() + height, this.getBlockZ() + width)) {
                    BlockState state = level.getBlockState(blockPos);
                    if (!WitherBoss.canDestroy(state)) continue;
                    destroyed = level.destroyBlock(blockPos, true, this) || destroyed;
                }
                if (destroyed) {
                    level.levelEvent(null, 1022, this.blockPosition(), 0);
                }
            }
        }
        if (this.tickCount % 20 == 0) {
            this.heal(1.0f);
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    public static boolean canDestroy(BlockState state) {
        return !state.isAir() && !state.is(BlockTags.WITHER_IMMUNE);
    }

    public void makeInvulnerable() {
        this.setInvulnerableTicks(220);
        this.bossEvent.setProgress(0.0f);
        this.setHealth(this.getMaxHealth() / 3.0f);
    }

    @Override
    public void makeStuckInBlock(BlockState blockState, Vec3 speedMultiplier) {
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private double getHeadX(int index) {
        if (index <= 0) {
            return this.getX();
        }
        float headAngle = (this.yBodyRot + (float)(180 * (index - 1))) * ((float)Math.PI / 180);
        float cos = Mth.cos(headAngle);
        return this.getX() + (double)cos * 1.3 * (double)this.getScale();
    }

    private double getHeadY(int index) {
        float height = index <= 0 ? 3.0f : 2.2f;
        return this.getY() + (double)(height * this.getScale());
    }

    private double getHeadZ(int index) {
        if (index <= 0) {
            return this.getZ();
        }
        float headAngle = (this.yBodyRot + (float)(180 * (index - 1))) * ((float)Math.PI / 180);
        float sin = Mth.sin(headAngle);
        return this.getZ() + (double)sin * 1.3 * (double)this.getScale();
    }

    private float rotlerp(float a, float b, float max) {
        float diff = Mth.wrapDegrees(b - a);
        if (diff > max) {
            diff = max;
        }
        if (diff < -max) {
            diff = -max;
        }
        return a + diff;
    }

    private void performRangedAttack(int head, LivingEntity target) {
        this.performRangedAttack(head, target.getX(), target.getY() + (double)target.getEyeHeight() * 0.5, target.getZ(), head == 0 && this.random.nextFloat() < 0.001f);
    }

    private void performRangedAttack(int head, double tx, double ty, double tz, boolean dangerous) {
        if (!this.isSilent()) {
            this.level().levelEvent(null, 1024, this.blockPosition(), 0);
        }
        double hx = this.getHeadX(head);
        double hy = this.getHeadY(head);
        double hz = this.getHeadZ(head);
        double xd = tx - hx;
        double yd = ty - hy;
        double zd = tz - hz;
        Vec3 direction = new Vec3(xd, yd, zd);
        WitherSkull entity = new WitherSkull(this.level(), this, direction.normalize());
        entity.setOwner(this);
        if (dangerous) {
            entity.setDangerous(true);
        }
        entity.setPos(hx, hy, hz);
        this.level().addFreshEntity(entity);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        this.performRangedAttack(0, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity directEntity;
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (source.is(DamageTypeTags.WITHER_IMMUNE_TO) || source.getEntity() instanceof WitherBoss) {
            return false;
        }
        if (this.getInvulnerableTicks() > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (this.isPowered() && ((directEntity = source.getDirectEntity()) instanceof AbstractArrow || directEntity instanceof WindCharge)) {
            return false;
        }
        Entity sourceEntity = source.getEntity();
        if (sourceEntity != null && sourceEntity.is(EntityTypeTags.WITHER_FRIENDS)) {
            return false;
        }
        if (this.destroyBlocksTick <= 0) {
            this.destroyBlocksTick = 20;
        }
        int i = 0;
        while (i < this.idleHeadUpdates.length) {
            int n = i++;
            this.idleHeadUpdates[n] = this.idleHeadUpdates[n] + 3;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        ItemEntity netherStar = this.spawnAtLocation(level, Items.NETHER_STAR);
        if (netherStar != null) {
            netherStar.setExtendedLifetime();
        }
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
            return;
        }
        this.noActionTime = 0;
    }

    @Override
    public boolean addEffect(MobEffectInstance newEffect, @Nullable Entity source) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.MOVEMENT_SPEED, 0.6f).add(Attributes.FLYING_SPEED, 0.6f).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 4.0);
    }

    public float[] getHeadYRots() {
        return this.yRotHeads;
    }

    public float[] getHeadXRots() {
        return this.xRotHeads;
    }

    public int getInvulnerableTicks() {
        return this.entityData.get(DATA_ID_INV);
    }

    public void setInvulnerableTicks(int invulnerableTicks) {
        this.entityData.set(DATA_ID_INV, invulnerableTicks);
    }

    public int getAlternativeTarget(int headIndex) {
        return this.entityData.get(DATA_TARGETS.get(headIndex));
    }

    public void setAlternativeTarget(int headIndex, int entityId) {
        this.entityData.set(DATA_TARGETS.get(headIndex), entityId);
    }

    public boolean isPowered() {
        return this.getHealth() <= this.getMaxHealth() / 2.0f;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance newEffect) {
        if (newEffect.is(MobEffects.WITHER)) {
            return false;
        }
        return super.canBeAffected(newEffect);
    }

    private class WitherDoNothingGoal
    extends Goal {
        final /* synthetic */ WitherBoss this$0;

        public WitherDoNothingGoal(WitherBoss witherBoss) {
            WitherBoss witherBoss2 = witherBoss;
            Objects.requireNonNull(witherBoss2);
            this.this$0 = witherBoss2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.this$0.getInvulnerableTicks() > 0;
        }
    }
}

