/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.zombie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.monster.skeleton.Parched;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public class Husk
extends Zombie {
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.49f, 0.98f).withEyeHeight(0.825f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, 0.0f, 0.1875f, 0.0f));

    public Husk(EntityType<? extends Husk> type, Level level) {
        super((EntityType<? extends Zombie>)type, level);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HUSK_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HUSK_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HUSK_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.HUSK_STEP;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean result = super.doHurtTarget(level, target);
        if (result && this.getMainHandItem().isEmpty() && target instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)target;
            float difficulty = level.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
            livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140 * (int)difficulty), this);
        }
        return result;
    }

    @Override
    protected boolean convertsInWater() {
        return true;
    }

    @Override
    protected void doUnderWaterConversion(ServerLevel level) {
        this.convertToZombieType(level, EntityTypes.ZOMBIE);
        if (!this.isSilent()) {
            level.levelEvent(null, 1041, this.blockPosition(), 0);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        RandomSource random = level.getRandom();
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        float difficultyModifier = difficulty.getSpecialMultiplier();
        if (spawnReason != EntitySpawnReason.CONVERSION) {
            this.setCanPickUpLoot(random.nextFloat() < 0.55f * difficultyModifier);
        }
        if (groupData != null) {
            groupData = new HuskGroupData((Zombie.ZombieGroupData)groupData);
            boolean bl = ((HuskGroupData)groupData).triedToSpawnCamelHusk = spawnReason != EntitySpawnReason.NATURAL;
        }
        if (groupData instanceof HuskGroupData) {
            BlockPos pos;
            HuskGroupData huskGroupData = (HuskGroupData)groupData;
            if (!huskGroupData.triedToSpawnCamelHusk && level.noCollision(EntityTypes.CAMEL_HUSK.getSpawnAABB((double)(pos = this.blockPosition()).getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5))) {
                huskGroupData.triedToSpawnCamelHusk = true;
                if (random.nextFloat() < 0.1f) {
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
                    CamelHusk camelHusk = EntityTypes.CAMEL_HUSK.create(this.level(), EntitySpawnReason.NATURAL);
                    if (camelHusk != null) {
                        camelHusk.setPos(this.getX(), this.getY(), this.getZ());
                        camelHusk.finalizeSpawn(level, difficulty, spawnReason, null);
                        this.startRiding(camelHusk, true, true);
                        level.addFreshEntity(camelHusk);
                        Parched parched = EntityTypes.PARCHED.create(this.level(), EntitySpawnReason.NATURAL);
                        if (parched != null) {
                            parched.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                            parched.finalizeSpawn(level, difficulty, spawnReason, null);
                            parched.startRiding(camelHusk, false, false);
                            level.addFreshEntityWithPassengers(parched);
                        }
                    }
                }
            }
        }
        return groupData;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    public static class HuskGroupData
    extends Zombie.ZombieGroupData {
        public boolean triedToSpawnCamelHusk = false;

        public HuskGroupData(Zombie.ZombieGroupData groupData) {
            super(groupData.isBaby, groupData.canSpawnJockey);
        }
    }
}

