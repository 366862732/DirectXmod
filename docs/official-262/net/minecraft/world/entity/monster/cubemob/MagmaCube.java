/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.cubemob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MagmaCube
extends AbstractCubeMob
implements Enemy {
    public MagmaCube(EntityType<? extends MagmaCube> type, Level level) {
        super((EntityType<? extends AbstractCubeMob>)type, level);
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new AbstractCubeMob.CubeMobAttackGoal(this));
    }

    @Override
    protected void addTargetingGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false, (target, level) -> Math.abs(target.getY() - this.getY()) <= 4.0));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<IronGolem>((Mob)this, IronGolem.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.2f);
    }

    public static boolean checkMagmaCubeSpawnRules(EntityType<MagmaCube> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL;
    }

    @Override
    public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        int actualSize = (Integer)this.entityData.get(ID_SIZE);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(actualSize);
        this.getAttribute(Attributes.ARMOR).setBaseValue(size * 3);
        this.xpReward = actualSize;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0f;
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.FLAME;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected int getJumpDelay() {
        return super.getJumpDelay() * 4;
    }

    @Override
    protected void decreaseSquish() {
        this.targetSquish *= 0.9f;
    }

    @Override
    public void jumpFromGround() {
        Vec3 movement = this.getDeltaMovement();
        float sizeJumpBoostPower = (float)this.getSize() * 0.1f;
        this.setDeltaMovement(movement.x, this.getJumpPower() + sizeJumpBoostPower, movement.z);
        this.needsSync = true;
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> type) {
        if (type == FluidTags.LAVA) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.22f + (float)this.getSize() * 0.05f, movement.z);
            this.needsSync = true;
        } else {
            super.jumpInLiquid(type);
        }
    }

    @Override
    protected boolean isDealsDamage() {
        return this.isEffectiveAi();
    }

    @Override
    protected float getAttackDamage() {
        return super.getAttackDamage() + 2.0f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isTiny()) {
            return SoundEvents.MAGMA_CUBE_HURT_SMALL;
        }
        return SoundEvents.MAGMA_CUBE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (this.isTiny()) {
            return SoundEvents.MAGMA_CUBE_DEATH_SMALL;
        }
        return SoundEvents.MAGMA_CUBE_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        if (this.isTiny()) {
            return SoundEvents.MAGMA_CUBE_SQUISH_SMALL;
        }
        return SoundEvents.MAGMA_CUBE_SQUISH;
    }

    @Override
    protected SoundEvent getJumpSound() {
        return SoundEvents.MAGMA_CUBE_JUMP;
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }
}

