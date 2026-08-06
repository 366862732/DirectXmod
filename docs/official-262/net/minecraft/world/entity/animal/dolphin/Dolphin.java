/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.dolphin;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.DolphinJumpGoal;
import net.minecraft.world.entity.ai.goal.FollowPlayerRiddenEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Dolphin
extends AgeableWaterCreature {
    private static final EntityDataAccessor<Boolean> GOT_FISH = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOISTNESS_LEVEL = SynchedEntityData.defineId(Dolphin.class, EntityDataSerializers.INT);
    private static final TargetingConditions SWIM_WITH_PLAYER_TARGETING = TargetingConditions.forNonCombat().range(10.0).ignoreLineOfSight();
    public static final int TOTAL_AIR_SUPPLY = 4800;
    private static final int TOTAL_MOISTNESS_LEVEL = 2400;
    public static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive() && e.isInWater();
    public static final float BABY_SCALE = 0.65f;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.DOLPHIN.getDimensions().scale(0.65f).withEyeHeight(0.09375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.3125f, 0.0f));
    private static final boolean DEFAULT_GOT_FISH = false;
    private @Nullable BlockPos treasurePos;

    public Dolphin(EntityType<? extends Dolphin> type, Level level) {
        super((EntityType<? extends AgeableWaterCreature>)type, level);
        this.moveControl = new SmoothSwimmingMoveControl<Dolphin>(this, 85, 10, 0.02f, 0.1f, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setCanPickUpLoot(true);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setAirSupply(this.getMaxAirSupply());
        this.setXRot(0.0f);
        SpawnGroupData spawnGroupData = Objects.requireNonNullElseGet(groupData, () -> new AgeableMob.AgeableMobGroupData(0.1f));
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    @Override
    public @Nullable Dolphin getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.DOLPHIN.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.65f : 1.0f;
    }

    @Override
    protected void handleAirSupply(int preTickAirSupply) {
    }

    public boolean gotFish() {
        return this.entityData.get(GOT_FISH);
    }

    public void setGotFish(boolean gotFish) {
        this.entityData.set(GOT_FISH, gotFish);
    }

    public int getMoistnessLevel() {
        return this.entityData.get(MOISTNESS_LEVEL);
    }

    public void setMoisntessLevel(int level) {
        this.entityData.set(MOISTNESS_LEVEL, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(GOT_FISH, false);
        entityData.define(MOISTNESS_LEVEL, 2400);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("GotFish", this.gotFish());
        output.putInt("Moistness", this.getMoistnessLevel());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setGotFish(input.getBooleanOr("GotFish", false));
        this.setMoisntessLevel(input.getIntOr("Moistness", 2400));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreathAirGoal(this));
        this.goalSelector.addGoal(0, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(1, new DolphinSwimToTreasureGoal(this));
        this.goalSelector.addGoal(2, new DolphinSwimWithPlayerGoal(this, 4.0));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(5, new DolphinJumpGoal(this, 10));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.2f, true));
        this.goalSelector.addGoal(8, new PlayWithItemsGoal(this));
        this.goalSelector.addGoal(8, new FollowPlayerRiddenEntityGoal(this, AbstractBoat.class));
        this.goalSelector.addGoal(8, new FollowPlayerRiddenEntityGoal(this, AbstractNautilus.class));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<Guardian>(this, Guardian.class, 8.0f, 1.0, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Guardian.class).setAlertOthers(new Class[0]));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 1.2f).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    public void playAttackSound() {
        this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0f, 1.0f);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !this.isBaby() && super.canAttack(target);
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    protected int increaseAirSupply(int currentSupply) {
        return this.getMaxAirSupply();
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND && this.canPickUpLoot();
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        ItemStack itemStack;
        if (this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty() && this.canHoldItem(itemStack = entity.getItem())) {
            this.onItemPickup(entity);
            this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
            this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            this.take(entity, itemStack.getCount());
            entity.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isNoAi()) {
            this.setAirSupply(this.getMaxAirSupply());
            return;
        }
        if (this.isInWaterOrRain()) {
            this.setMoisntessLevel(2400);
        } else {
            this.setMoisntessLevel(this.getMoistnessLevel() - 1);
            if (this.getMoistnessLevel() <= 0) {
                this.hurt(this.damageSources().dryOut(), 1.0f);
            }
            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0f - 1.0f) * 0.2f, 0.5, (this.random.nextFloat() * 2.0f - 1.0f) * 0.2f));
                this.setYRot(this.random.nextFloat() * 360.0f);
                this.setOnGround(false);
                this.needsSync = true;
            }
        }
        if (this.level().isClientSide() && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03) {
            Vec3 viewVector = this.getViewVector(0.0f);
            float c = Mth.cos(this.getYRot() * ((float)Math.PI / 180)) * 0.3f;
            float s = Mth.sin(this.getYRot() * ((float)Math.PI / 180)) * 0.3f;
            float multiplier = 1.2f - this.random.nextFloat() * 0.7f;
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(ParticleTypes.DOLPHIN, this.getX() - viewVector.x * (double)multiplier + (double)c, this.getY() - viewVector.y, this.getZ() - viewVector.z * (double)multiplier + (double)s, 0.0, 0.0, 0.0);
                this.level().addParticle(ParticleTypes.DOLPHIN, this.getX() - viewVector.x * (double)multiplier - (double)c, this.getY() - viewVector.y, this.getZ() - viewVector.z * (double)multiplier - (double)s, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 38) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void addParticlesAroundSelf(ParticleOptions particle) {
        for (int i = 0; i < 7; ++i) {
            double xa = this.random.nextGaussian() * 0.01;
            double ya = this.random.nextGaussian() * 0.01;
            double za = this.random.nextGaussian() * 0.01;
            this.level().addParticle(particle, this.getRandomX(1.0), this.getRandomY() + 0.2, this.getRandomZ(1.0), xa, ya, za);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.isEmpty() && itemStack.is(ItemTags.FISHES)) {
            if (!this.level().isClientSide()) {
                this.playSound(SoundEvents.DOLPHIN_EAT, 1.0f, 1.0f);
            }
            if (this.canAgeUp()) {
                itemStack.consume(1, player);
                this.ageUp(Dolphin.getSpeedUpSecondsWhenFeeding(-this.age), true);
            } else {
                this.setGotFish(true);
                itemStack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.DOLPHIN_AMBIENT_WATER : SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.DOLPHIN_SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    protected boolean closeToNextPos() {
        BlockPos target = this.getNavigation().getTargetPos();
        if (target != null) {
            return target.closerToCenterThan(this.position(), 12.0);
        }
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (this.getTarget() == null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.005, 0.0));
        }
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    private static class DolphinSwimToTreasureGoal
    extends Goal {
        private final Dolphin dolphin;
        private boolean stuck;

        public DolphinSwimToTreasureGoal(Dolphin dolphin) {
            this.dolphin = dolphin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public boolean canUse() {
            return this.dolphin.gotFish() && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos treasurePos = this.dolphin.treasurePos;
            if (treasurePos == null) {
                return false;
            }
            return !BlockPos.containing(treasurePos.getX(), this.dolphin.getY(), treasurePos.getZ()).closerToCenterThan(this.dolphin.position(), 4.0) && !this.stuck && this.dolphin.getAirSupply() >= 100;
        }

        @Override
        public void start() {
            if (!(this.dolphin.level() instanceof ServerLevel)) {
                return;
            }
            ServerLevel level = (ServerLevel)this.dolphin.level();
            this.stuck = false;
            this.dolphin.getNavigation().stop();
            BlockPos dolphinPos = this.dolphin.blockPosition();
            BlockPos treasurePos = level.findNearestMapStructure(StructureTags.DOLPHIN_LOCATED, dolphinPos, 50, false);
            if (treasurePos == null) {
                this.stuck = true;
                return;
            }
            this.dolphin.treasurePos = treasurePos;
            level.broadcastEntityEvent(this.dolphin, (byte)38);
        }

        @Override
        public void stop() {
            BlockPos treasurePos = this.dolphin.treasurePos;
            if (treasurePos == null || BlockPos.containing(treasurePos.getX(), this.dolphin.getY(), treasurePos.getZ()).closerToCenterThan(this.dolphin.position(), 4.0) || this.stuck) {
                this.dolphin.setGotFish(false);
            }
        }

        @Override
        public void tick() {
            if (this.dolphin.treasurePos == null) {
                return;
            }
            Level level = this.dolphin.level();
            if (this.dolphin.closeToNextPos() || this.dolphin.getNavigation().isDone()) {
                BlockPos next;
                Vec3 treasurePos = Vec3.atCenterOf(this.dolphin.treasurePos);
                Vec3 nextPos = DefaultRandomPos.getPosTowards(this.dolphin, 16, 1, treasurePos, 0.3926991f);
                if (nextPos == null) {
                    nextPos = DefaultRandomPos.getPosTowards(this.dolphin, 8, 4, treasurePos, 1.5707963705062866);
                }
                if (!(nextPos == null || level.getFluidState(next = BlockPos.containing(nextPos)).is(FluidTags.WATER) && level.getBlockState(next).isPathfindable(PathComputationType.WATER))) {
                    nextPos = DefaultRandomPos.getPosTowards(this.dolphin, 8, 5, treasurePos, 1.5707963705062866);
                }
                if (nextPos == null) {
                    this.stuck = true;
                    return;
                }
                this.dolphin.getLookControl().setLookAt(nextPos.x, nextPos.y, nextPos.z, this.dolphin.getMaxHeadYRot() + 20, this.dolphin.getMaxHeadXRot());
                this.dolphin.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, 1.3);
                if (level.getRandom().nextInt(this.adjustedTickDelay(80)) == 0) {
                    level.broadcastEntityEvent(this.dolphin, (byte)38);
                }
            }
        }
    }

    private static class DolphinSwimWithPlayerGoal
    extends Goal {
        private final Dolphin dolphin;
        private final double speedModifier;
        private @Nullable Player player;

        public DolphinSwimWithPlayerGoal(Dolphin dolphin, double speedModifier) {
            this.dolphin = dolphin;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            this.player = DolphinSwimWithPlayerGoal.getServerLevel(this.dolphin).getNearestPlayer(SWIM_WITH_PLAYER_TARGETING, this.dolphin);
            if (this.player == null) {
                return false;
            }
            return this.player.isSwimming() && this.dolphin.getTarget() != this.player;
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isSwimming() && this.dolphin.distanceToSqr(this.player) < 256.0;
        }

        @Override
        public void start() {
            this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
        }

        @Override
        public void stop() {
            this.player = null;
            this.dolphin.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.dolphin.getLookControl().setLookAt(this.player, this.dolphin.getMaxHeadYRot() + 20, this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.player) < 6.25) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.player, this.speedModifier);
            }
            if (this.player.isSwimming() && this.player.level().getRandom().nextInt(6) == 0) {
                this.player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100), this.dolphin);
            }
        }
    }

    private class PlayWithItemsGoal
    extends Goal {
        private int cooldown;
        final /* synthetic */ Dolphin this$0;

        public PlayWithItemsGoal(Dolphin dolphin) {
            Dolphin dolphin2 = dolphin;
            Objects.requireNonNull(dolphin2);
            this.this$0 = dolphin2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > this.this$0.tickCount) {
                return false;
            }
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            return !items.isEmpty() || !this.this$0.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
        }

        @Override
        public void start() {
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            if (!items.isEmpty()) {
                this.this$0.getNavigation().moveTo(items.get(0), (double)1.2f);
                this.this$0.playSound(SoundEvents.DOLPHIN_PLAY, 1.0f, 1.0f);
            }
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            ItemStack itemStack = this.this$0.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                this.this$0.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.cooldown = this.this$0.tickCount + this.this$0.random.nextInt(100);
            }
        }

        @Override
        public void tick() {
            List<ItemEntity> items = this.this$0.level().getEntitiesOfClass(ItemEntity.class, this.this$0.getBoundingBox().inflate(8.0, 8.0, 8.0), ALLOWED_ITEMS);
            ItemStack itemStack = this.this$0.getItemBySlot(EquipmentSlot.MAINHAND);
            if (!itemStack.isEmpty()) {
                this.drop(itemStack);
                this.this$0.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            } else if (!items.isEmpty()) {
                this.this$0.getNavigation().moveTo(items.get(0), (double)1.2f);
            }
        }

        private void drop(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return;
            }
            double yHandPos = this.this$0.getEyeY() - (double)0.3f;
            ItemEntity thrownItem = new ItemEntity(this.this$0.level(), this.this$0.getX(), yHandPos, this.this$0.getZ(), itemStack);
            thrownItem.setPickUpDelay(40);
            thrownItem.setThrower(this.this$0);
            float pow = 0.3f;
            float dir = this.this$0.random.nextFloat() * ((float)Math.PI * 2);
            float pow2 = 0.02f * this.this$0.random.nextFloat();
            thrownItem.setDeltaMovement(0.3f * -Mth.sin(this.this$0.getYRot() * ((float)Math.PI / 180)) * Mth.cos(this.this$0.getXRot() * ((float)Math.PI / 180)) + Mth.cos(dir) * pow2, 0.3f * Mth.sin(this.this$0.getXRot() * ((float)Math.PI / 180)) * 1.5f, 0.3f * Mth.cos(this.this$0.getYRot() * ((float)Math.PI / 180)) * Mth.cos(this.this$0.getXRot() * ((float)Math.PI / 180)) + Mth.sin(dir) * pow2);
            this.this$0.level().addFreshEntity(thrownItem);
        }
    }
}

