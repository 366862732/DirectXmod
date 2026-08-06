/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AgeableMob
extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(AgeableMob.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AGE_LOCKED = SynchedEntityData.defineId(AgeableMob.class, EntityDataSerializers.BOOLEAN);
    public static final int BABY_START_AGE = -24000;
    public static final int AGE_LOCK_COOLDOWN_TICKS = 40;
    public static final float AGE_LOCK_DOWNWARDS_MOVING_PARTICLE_Y_OFFSET = 0.2f;
    private static final int FORCED_AGE_PARTICLE_TICKS = 40;
    protected static final int DEFAULT_AGE = 0;
    protected static final int DEFAULT_FORCED_AGE = 0;
    protected int age = 0;
    protected int forcedAge = 0;
    protected int forcedAgeTimer;
    protected int ageLockParticleTimer = 0;

    protected AgeableMob(EntityType<? extends AgeableMob> type, Level level) {
        super((EntityType<? extends PathfinderMob>)type, level);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        AgeableMobGroupData ageableMobGroupData;
        if (groupData == null) {
            groupData = new AgeableMobGroupData(true);
        }
        if ((ageableMobGroupData = (AgeableMobGroupData)groupData).isShouldSpawnBaby() && ageableMobGroupData.getGroupSize() > 0 && level.getRandom().nextFloat() <= ageableMobGroupData.getBabySpawnChance()) {
            this.setAge(this.getBabyStartAge());
        }
        ageableMobGroupData.increaseGroupSizeByOne();
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public abstract @Nullable AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2);

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (AgeableMob.canUseGoldenDandelion(itemInHand, this.isBaby(), this.ageLockParticleTimer, this)) {
            AgeableMob.setAgeLocked(this, this::isAgeLocked, player, itemInHand, mob -> this.setAgeLockedData());
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public static boolean canUseGoldenDandelion(ItemStack itemInHand, boolean isBaby, int cooldown, Mob mob) {
        return itemInHand.getItem() == Items.GOLDEN_DANDELION && isBaby && cooldown == 0 && !mob.is(EntityTypeTags.CANNOT_BE_AGE_LOCKED);
    }

    private void setAgeLockedData() {
        this.setAgeLocked(!this.isAgeLocked());
        this.setAge(this.getBabyStartAge());
        this.ageLockParticleTimer = 40;
    }

    public static void setAgeLocked(Mob mob, Supplier<Boolean> isAgedLocked, Player player, ItemStack itemInHand, Consumer<Mob> setAgeLockData) {
        setAgeLockData.accept(mob);
        itemInHand.consume(1, player);
        boolean isAgeLocked = isAgedLocked.get();
        if (isAgeLocked) {
            mob.setPersistenceRequired();
        }
        mob.level().playSound(null, mob.blockPosition(), isAgeLocked ? SoundEvents.GOLDEN_DANDELION_USE : SoundEvents.GOLDEN_DANDELION_UNUSE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_BABY_ID, false);
        entityData.define(AGE_LOCKED, false);
    }

    public boolean canBreed() {
        return false;
    }

    public int getAge() {
        if (this.level().isClientSide()) {
            return this.entityData.get(DATA_BABY_ID) != false ? -1 : 1;
        }
        return this.age;
    }

    public void ageUp(int seconds, boolean forced) {
        int age;
        int oldAge = age = this.getAge();
        if ((age += seconds * 20) > 0) {
            age = 0;
        }
        int delta = age - oldAge;
        this.setAge(age);
        if (forced) {
            this.forcedAge += delta;
            if (this.forcedAgeTimer == 0) {
                this.forcedAgeTimer = 40;
            }
        }
        if (this.getAge() == 0) {
            this.setAge(this.forcedAge);
        }
    }

    public void ageUp(int seconds) {
        this.ageUp(seconds, false);
    }

    public void setAge(int newAge) {
        int oldAge = this.getAge();
        this.age = newAge;
        if (oldAge < 0 && newAge >= 0 || oldAge >= 0 && newAge < 0) {
            this.entityData.set(DATA_BABY_ID, newAge < 0);
            this.ageBoundaryReached();
        }
    }

    protected void setAgeLocked(boolean locked) {
        this.entityData.set(AGE_LOCKED, locked);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.canBeABaby()) {
            output.putInt("Age", this.getAge());
            output.putInt("ForcedAge", this.forcedAge);
            output.putBoolean("AgeLocked", this.isAgeLocked());
        }
    }

    protected boolean canBeABaby() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (this.canBeABaby()) {
            this.setAge(input.getIntOr("Age", 0));
            this.forcedAge = input.getIntOr("ForcedAge", 0);
            this.setAgeLocked(input.getBooleanOr("AgeLocked", false));
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BABY_ID.equals(accessor)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.forcedAgeTimer > 0) {
                if (this.forcedAgeTimer % 4 == 0) {
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), 0.0, 0.0, 0.0);
                }
                --this.forcedAgeTimer;
            }
        } else if (this.isAlive()) {
            int age = this.getAge();
            if (this.canAgeUp()) {
                this.setAge(++age);
            } else if (age > 0) {
                this.setAge(--age);
            }
        }
        this.ageLockParticleTimer = AgeableMob.makeAgeLockedParticle(this.level(), this, this.ageLockParticleTimer, this.isAgeLocked());
    }

    public boolean canAgeUp() {
        return this.isBaby() && !this.isAgeLocked();
    }

    public static int makeAgeLockedParticle(Level level, Mob mob, int ageLockParticleTimer, boolean isAgeLocked) {
        if (ageLockParticleTimer > 0) {
            if (level.isClientSide() && ageLockParticleTimer % 2 == 0) {
                float yParticleOffset = isAgeLocked ? 0.2f : 0.0f;
                Vec3 spawnPosition = new Vec3(mob.getRandomX(1.0), mob.getRandomY(0.2) + (double)mob.getBbHeight() + (double)yParticleOffset, mob.getRandomZ(1.0));
                level.addParticle(isAgeLocked ? ParticleTypes.PAUSE_MOB_GROWTH : ParticleTypes.RESET_MOB_GROWTH, spawnPosition.x, spawnPosition.y, spawnPosition.z, 0.0, 0.0, 0.0);
            }
            --ageLockParticleTimer;
        }
        return ageLockParticleTimer;
    }

    protected void ageBoundaryReached() {
        AbstractBoat boat;
        Entity entity;
        if (!this.isBaby() && this.isPassenger() && (entity = this.getVehicle()) instanceof AbstractBoat && !(boat = (AbstractBoat)entity).hasEnoughSpaceFor(this)) {
            this.stopRiding();
        }
    }

    protected int getBabyStartAge() {
        return -24000;
    }

    @Override
    public final boolean isBaby() {
        return this.canBeABaby() && this.getAge() < 0;
    }

    @Override
    public final void setBaby(boolean baby) {
        if (this.canBeABaby()) {
            this.setAge(baby ? this.getBabyStartAge() : 0);
        }
    }

    public static int getSpeedUpSecondsWhenFeeding(int ticksUntilAdult) {
        return (int)((float)(ticksUntilAdult / 20) * 0.1f);
    }

    @VisibleForTesting
    public int getForcedAge() {
        return this.forcedAge;
    }

    @VisibleForTesting
    public int getForcedAgeTimer() {
        return this.forcedAgeTimer;
    }

    public boolean isAgeLocked() {
        return this.entityData.get(AGE_LOCKED);
    }

    public static class AgeableMobGroupData
    implements SpawnGroupData {
        private int groupSize;
        private final boolean shouldSpawnBaby;
        private final float babySpawnChance;

        public AgeableMobGroupData(boolean shouldSpawnBaby, float babySpawnChance) {
            this.shouldSpawnBaby = shouldSpawnBaby;
            this.babySpawnChance = babySpawnChance;
        }

        public AgeableMobGroupData(boolean shouldSpawnBaby) {
            this(shouldSpawnBaby, 0.05f);
        }

        public AgeableMobGroupData(float babySpawnChance) {
            this(true, babySpawnChance);
        }

        public int getGroupSize() {
            return this.groupSize;
        }

        public void increaseGroupSizeByOne() {
            ++this.groupSize;
        }

        public boolean isShouldSpawnBaby() {
            return this.shouldSpawnBaby;
        }

        public float getBabySpawnChance() {
            return this.babySpawnChance;
        }
    }
}

