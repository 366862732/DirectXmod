/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.bee;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.debug.DebugBeeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Bee
extends Animal
implements NeutralMob {
    public static final float FLAP_DEGREES_PER_TICK = 120.32113f;
    public static final int TICKS_PER_FLAP = Mth.ceil(1.4959966f);
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Bee.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Bee.class, EntityDataSerializers.LONG);
    private static final int FLAG_ROLL = 2;
    private static final int FLAG_HAS_STUNG = 4;
    private static final int FLAG_HAS_NECTAR = 8;
    private static final int STING_DEATH_COUNTDOWN = 1200;
    private static final int TICKS_BEFORE_GOING_TO_KNOWN_FLOWER = 600;
    private static final int TICKS_WITHOUT_NECTAR_BEFORE_GOING_HOME = 3600;
    private static final int MIN_ATTACK_DIST = 4;
    private static final int MAX_CROPS_GROWABLE = 10;
    private static final int POISON_SECONDS_NORMAL = 10;
    private static final int POISON_SECONDS_HARD = 18;
    private static final int TOO_FAR_DISTANCE = 48;
    private static final int HIVE_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int RESTRICTED_WANDER_DISTANCE_REDUCTION = 24;
    private static final int DEFAULT_WANDER_DISTANCE_REDUCTION = 16;
    private static final int PATHFIND_TO_HIVE_WHEN_CLOSER_THAN = 16;
    private static final int HIVE_SEARCH_DISTANCE = 20;
    public static final String TAG_CROPS_GROWN_SINCE_POLLINATION = "CropsGrownSincePollination";
    public static final String TAG_CANNOT_ENTER_HIVE_TICKS = "CannotEnterHiveTicks";
    public static final String TAG_TICKS_SINCE_POLLINATION = "TicksSincePollination";
    public static final String TAG_HAS_STUNG = "HasStung";
    public static final String TAG_HAS_NECTAR = "HasNectar";
    public static final String TAG_FLOWER_POS = "flower_pos";
    public static final String TAG_HIVE_POS = "hive_pos";
    public static final boolean DEFAULT_HAS_NECTAR = false;
    private static final boolean DEFAULT_HAS_STUNG = false;
    private static final int DEFAULT_TICKS_SINCE_POLLINATION = 0;
    private static final int DEFAULT_CANNOT_ENTER_HIVE_TICKS = 0;
    private static final int DEFAULT_CROPS_GROWN_SINCE_POLLINATION = 0;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;
    private float rollAmount;
    private float rollAmountO;
    private int timeSinceSting;
    private int ticksWithoutNectarSinceExitingHive = 0;
    private int stayOutOfHiveCountdown = 0;
    private int numCropsGrownSincePollination = 0;
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_HIVE = 200;
    private int remainingCooldownBeforeLocatingNewHive;
    private static final int COOLDOWN_BEFORE_LOCATING_NEW_FLOWER = 200;
    private static final int MIN_FIND_FLOWER_RETRY_COOLDOWN = 20;
    private static final int MAX_FIND_FLOWER_RETRY_COOLDOWN = 60;
    private int remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(this.random, 20, 60);
    private @Nullable BlockPos savedFlowerPos;
    private @Nullable BlockPos hivePos;
    private BeePollinateGoal beePollinateGoal;
    private BeeGoToHiveGoal goToHiveGoal;
    private BeeGoToKnownFlowerGoal goToKnownFlowerGoal;
    private int underWaterTicks;

    public Bee(EntityType<? extends Bee> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.moveControl = new FlyingMoveControl<Bee>(this, 20, true);
        this.lookControl = new BeeLookControl(this, this);
        this.setPathfindingMalus(PathType.FIRE, -1.0f);
        this.setPathfindingMalus(PathType.WATER, -1.0f);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0f);
        this.setPathfindingMalus(PathType.COCOA, -1.0f);
        this.setPathfindingMalus(PathType.FENCE, -1.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_FLAGS_ID, (byte)0);
        entityData.define(DATA_ANGER_END_TIME, -1L);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos).isAir()) {
            return 10.0f;
        }
        return 0.0f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BeeAttackGoal(this, this, 1.4f, true));
        this.goalSelector.addGoal(1, new BeeEnterHiveGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25, i -> i.is(ItemTags.BEE_FOOD), false));
        this.goalSelector.addGoal(3, new ValidateHiveGoal(this));
        this.goalSelector.addGoal(3, new ValidateFlowerGoal(this));
        this.beePollinateGoal = new BeePollinateGoal(this);
        this.goalSelector.addGoal(4, this.beePollinateGoal);
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new BeeLocateHiveGoal(this));
        this.goToHiveGoal = new BeeGoToHiveGoal(this);
        this.goalSelector.addGoal(5, this.goToHiveGoal);
        this.goToKnownFlowerGoal = new BeeGoToKnownFlowerGoal(this);
        this.goalSelector.addGoal(6, this.goToKnownFlowerGoal);
        this.goalSelector.addGoal(7, new BeeGrowCropGoal(this));
        this.goalSelector.addGoal(8, new BeeWanderGoal(this));
        this.goalSelector.addGoal(9, new FloatGoal(this));
        this.targetSelector.addGoal(1, new BeeHurtByOtherGoal(this, this).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new BeeBecomeAngryTargetGoal(this));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<Bee>(this, true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable(TAG_HIVE_POS, BlockPos.CODEC, this.hivePos);
        output.storeNullable(TAG_FLOWER_POS, BlockPos.CODEC, this.savedFlowerPos);
        output.putBoolean(TAG_HAS_NECTAR, this.hasNectar());
        output.putBoolean(TAG_HAS_STUNG, this.hasStung());
        output.putInt(TAG_TICKS_SINCE_POLLINATION, this.ticksWithoutNectarSinceExitingHive);
        output.putInt(TAG_CANNOT_ENTER_HIVE_TICKS, this.stayOutOfHiveCountdown);
        output.putInt(TAG_CROPS_GROWN_SINCE_POLLINATION, this.numCropsGrownSincePollination);
        this.addPersistentAngerSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setHasNectar(input.getBooleanOr(TAG_HAS_NECTAR, false));
        this.setHasStung(input.getBooleanOr(TAG_HAS_STUNG, false));
        this.ticksWithoutNectarSinceExitingHive = input.getIntOr(TAG_TICKS_SINCE_POLLINATION, 0);
        this.stayOutOfHiveCountdown = input.getIntOr(TAG_CANNOT_ENTER_HIVE_TICKS, 0);
        this.numCropsGrownSincePollination = input.getIntOr(TAG_CROPS_GROWN_SINCE_POLLINATION, 0);
        this.hivePos = input.read(TAG_HIVE_POS, BlockPos.CODEC).orElse(null);
        this.savedFlowerPos = input.read(TAG_FLOWER_POS, BlockPos.CODEC).orElse(null);
        this.readPersistentAngerSaveData(this.level(), input);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        DamageSource damageSource = this.damageSources().sting(this);
        boolean wasHurt = target.hurtServer(level, damageSource, (int)this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (wasHurt) {
            EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
            if (target instanceof LivingEntity) {
                LivingEntity livingTarget = (LivingEntity)target;
                livingTarget.setStingerCount(livingTarget.getStingerCount() + 1);
                int poisonTime = 0;
                if (this.level().getDifficulty() == Difficulty.NORMAL) {
                    poisonTime = 10;
                } else if (this.level().getDifficulty() == Difficulty.HARD) {
                    poisonTime = 18;
                }
                if (poisonTime > 0) {
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTime * 20, 0), this);
                }
            }
            this.setHasStung(true);
            this.stopBeingAngry();
            this.playSound(SoundEvents.BEE_STING, 1.0f, 1.0f);
        }
        return wasHurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.hasNectar() && this.getCropsGrownSincePollination() < 10 && this.random.nextFloat() < 0.05f) {
            for (int i = 0; i < this.random.nextInt(2) + 1; ++i) {
                this.spawnFluidParticle(this.level(), this.getX() - (double)0.3f, this.getX() + (double)0.3f, this.getZ() - (double)0.3f, this.getZ() + (double)0.3f, this.getY(0.5), ParticleTypes.FALLING_NECTAR);
            }
        }
        this.updateRollAmount();
    }

    private void spawnFluidParticle(Level level, double x1, double x2, double z1, double z2, double y, ParticleOptions dripParticle) {
        level.addParticle(dripParticle, Mth.lerp(level.getRandom().nextDouble(), x1, x2), y, Mth.lerp(level.getRandom().nextDouble(), z1, z2), 0.0, 0.0, 0.0);
    }

    private void pathfindRandomlyTowards(BlockPos targetPos) {
        Vec3 nextPosTowards;
        Vec3 targetVec = Vec3.atBottomCenterOf(targetPos);
        int yAdjust = 0;
        BlockPos beePos = this.blockPosition();
        int yDelta = (int)targetVec.y - beePos.getY();
        if (yDelta > 2) {
            yAdjust = 4;
        } else if (yDelta < -2) {
            yAdjust = -4;
        }
        int xzDist = 6;
        int yDist = 8;
        int dist = beePos.distManhattan(targetPos);
        if (dist < 15) {
            xzDist = dist / 2;
            yDist = dist / 2;
        }
        if ((nextPosTowards = AirRandomPos.getPosTowards(this, xzDist, yDist, yAdjust, targetVec, 0.3141592741012573)) == null) {
            return;
        }
        this.navigation.setMaxVisitedNodesMultiplier(0.5f);
        this.navigation.moveTo(nextPosTowards.x, nextPosTowards.y, nextPosTowards.z, 1.0);
    }

    public @Nullable BlockPos getSavedFlowerPos() {
        return this.savedFlowerPos;
    }

    public boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    public void setSavedFlowerPos(BlockPos savedFlowerPos) {
        this.savedFlowerPos = savedFlowerPos;
    }

    @VisibleForDebug
    public int getTravellingTicks() {
        return Math.max(this.goToHiveGoal.travellingTicks, this.goToKnownFlowerGoal.travellingTicks);
    }

    @VisibleForDebug
    public List<BlockPos> getBlacklistedHives() {
        return this.goToHiveGoal.blacklistedTargets;
    }

    private boolean isTiredOfLookingForNectar() {
        return this.ticksWithoutNectarSinceExitingHive > 3600;
    }

    private void dropHive() {
        this.hivePos = null;
        this.remainingCooldownBeforeLocatingNewHive = 200;
    }

    private void dropFlower() {
        this.savedFlowerPos = null;
        this.remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(this.random, 20, 60);
    }

    private boolean wantsToEnterHive() {
        if (this.stayOutOfHiveCountdown > 0 || this.beePollinateGoal.isPollinating() || this.hasStung() || this.getTarget() != null) {
            return false;
        }
        boolean wantsToEnterHive = this.hasNectar() || this.isTiredOfLookingForNectar() || this.level().environmentAttributes().getValue(EnvironmentAttributes.BEES_STAY_IN_HIVE, this.position()) != false;
        return wantsToEnterHive && !this.isHiveNearFire();
    }

    public void setStayOutOfHiveCountdown(int ticks) {
        this.stayOutOfHiveCountdown = ticks;
    }

    public float getRollAmount(float a) {
        return Mth.lerp(a, this.rollAmountO, this.rollAmount);
    }

    private void updateRollAmount() {
        this.rollAmountO = this.rollAmount;
        this.rollAmount = this.isRolling() ? Math.min(1.0f, this.rollAmount + 0.2f) : Math.max(0.0f, this.rollAmount - 0.24f);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        boolean hasStung = this.hasStung();
        this.underWaterTicks = this.isInWater() ? ++this.underWaterTicks : 0;
        if (this.underWaterTicks > 20) {
            this.hurtServer(level, this.damageSources().drown(), 1.0f);
        }
        if (hasStung) {
            ++this.timeSinceSting;
            if (this.timeSinceSting % 5 == 0 && this.random.nextInt(Mth.clamp(1200 - this.timeSinceSting, 1, 1200)) == 0) {
                this.hurtServer(level, this.damageSources().generic(), this.getHealth());
            }
        }
        if (!this.hasNectar()) {
            ++this.ticksWithoutNectarSinceExitingHive;
        }
        this.updatePersistentAnger(level, false);
    }

    public void resetTicksWithoutNectarSinceExitingHive() {
        this.ticksWithoutNectarSinceExitingHive = 0;
    }

    private boolean isHiveNearFire() {
        BeehiveBlockEntity beehiveBlockEntity = this.getBeehiveBlockEntity();
        return beehiveBlockEntity != null && beehiveBlockEntity.isFireNearby();
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.entityData.get(DATA_ANGER_END_TIME);
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.entityData.set(DATA_ANGER_END_TIME, endTime);
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    private boolean doesHiveHaveSpace(BlockPos hivePos) {
        BlockEntity blockEntity = this.level().getBlockEntity(hivePos);
        if (blockEntity instanceof BeehiveBlockEntity) {
            BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity)blockEntity;
            return !beehiveBlockEntity.isFull();
        }
        return false;
    }

    @VisibleForDebug
    public boolean hasHive() {
        return this.hivePos != null;
    }

    @VisibleForDebug
    public @Nullable BlockPos getHivePos() {
        return this.hivePos;
    }

    private int getCropsGrownSincePollination() {
        return this.numCropsGrownSincePollination;
    }

    private void resetNumCropsGrownSincePollination() {
        this.numCropsGrownSincePollination = 0;
    }

    private void incrementNumCropsGrownSincePollination() {
        ++this.numCropsGrownSincePollination;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            if (this.stayOutOfHiveCountdown > 0) {
                --this.stayOutOfHiveCountdown;
            }
            if (this.remainingCooldownBeforeLocatingNewHive > 0) {
                --this.remainingCooldownBeforeLocatingNewHive;
            }
            if (this.remainingCooldownBeforeLocatingNewFlower > 0) {
                --this.remainingCooldownBeforeLocatingNewFlower;
            }
            boolean shouldRoll = this.isAngry() && !this.hasStung() && this.getTarget() != null && this.getTarget().distanceToSqr(this) < 4.0;
            this.setRolling(shouldRoll);
            if (this.tickCount % 20 == 0 && !this.isHiveValid()) {
                this.hivePos = null;
            }
        }
    }

    private @Nullable BeehiveBlockEntity getBeehiveBlockEntity() {
        if (this.hivePos == null) {
            return null;
        }
        if (this.isTooFarAway(this.hivePos)) {
            return null;
        }
        return this.level().getBlockEntity(this.hivePos, BlockEntityTypes.BEEHIVE).orElse(null);
    }

    private boolean isHiveValid() {
        return this.getBeehiveBlockEntity() != null;
    }

    public boolean hasNectar() {
        return this.getFlag(8);
    }

    private void setHasNectar(boolean hasNectar) {
        if (hasNectar) {
            this.resetTicksWithoutNectarSinceExitingHive();
        }
        this.setFlag(8, hasNectar);
    }

    public boolean hasStung() {
        return this.getFlag(4);
    }

    private void setHasStung(boolean hasStung) {
        this.setFlag(4, hasStung);
    }

    private boolean isRolling() {
        return this.getFlag(2);
    }

    private void setRolling(boolean rolling) {
        this.setFlag(2, rolling);
    }

    private boolean isTooFarAway(BlockPos targetPos) {
        return !this.closerThan(targetPos, 48);
    }

    private void setFlag(int flag, boolean value) {
        if (value) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) | flag));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(this.entityData.get(DATA_FLAGS_ID) & ~flag));
        }
    }

    private boolean getFlag(int flag) {
        return (this.entityData.get(DATA_FLAGS_ID) & flag) != 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FLYING_SPEED, 0.6f).add(Attributes.MOVEMENT_SPEED, 0.3f).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingPathNavigation = new FlyingPathNavigation(this, this, level){
            final /* synthetic */ Bee this$0;
            {
                Bee bee = this$0;
                Objects.requireNonNull(bee);
                this.this$0 = bee;
                super(mob, level);
            }

            @Override
            public boolean isStableDestination(BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }

            @Override
            public void tick() {
                if (this.this$0.beePollinateGoal.isPollinating()) {
                    return;
                }
                super.tick();
            }
        };
        flyingPathNavigation.setCanOpenDoors(false);
        flyingPathNavigation.setCanFloat(false);
        flyingPathNavigation.setRequiredPathLength(48.0f);
        return flyingPathNavigation;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        FlowerBlock flower;
        MobEffectInstance effect;
        BlockItem blockItem;
        FeatureElement featureElement;
        ItemStack heldItem = player.getItemInHand(hand);
        if (this.isFood(heldItem) && (featureElement = heldItem.getItem()) instanceof BlockItem && (featureElement = (blockItem = (BlockItem)featureElement).getBlock()) instanceof FlowerBlock && (effect = (flower = (FlowerBlock)featureElement).getBeeInteractionEffect()) != null) {
            this.usePlayerItem(player, hand, heldItem);
            if (!this.level().isClientSide()) {
                this.addEffect(effect);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.BEE_FOOD);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BEE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    public @Nullable Bee getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.BEE.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean isFlapping() {
        return this.isFlying() && this.tickCount % TICKS_PER_FLAP == 0;
    }

    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected boolean omnidirectionalAirMover() {
        return true;
    }

    public void dropOffNectar() {
        this.setHasNectar(false);
        this.resetNumCropsGrownSincePollination();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        this.beePollinateGoal.stopPollinating();
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void jumpInLiquid(TagKey<Fluid> type) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.01, 0.0));
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.5f * this.getEyeHeight(), this.getBbWidth() * 0.2f);
    }

    private boolean closerThan(BlockPos targetPos, int distance) {
        return targetPos.closerThan(this.blockPosition(), distance);
    }

    public void setHivePos(BlockPos hivePos) {
        this.hivePos = hivePos;
    }

    public static boolean attractsBees(BlockState state) {
        if (state.is(BlockTags.BEE_ATTRACTIVE)) {
            if (state.getValueOrElse(BlockStateProperties.WATERLOGGED, false).booleanValue()) {
                return false;
            }
            if (state.is(Blocks.SUNFLOWER)) {
                return state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
            }
            return true;
        }
        return false;
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        super.registerDebugValues(level, registration);
        registration.register(DebugSubscriptions.BEES, () -> new DebugBeeInfo(Optional.ofNullable(this.getHivePos()), Optional.ofNullable(this.getSavedFlowerPos()), this.getTravellingTicks(), this.getBlacklistedHives()));
    }

    private class BeeLookControl
    extends LookControl {
        final /* synthetic */ Bee this$0;

        public BeeLookControl(Bee bee, Mob mob) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(mob);
        }

        @Override
        public void tick() {
            if (this.this$0.isAngry()) {
                return;
            }
            super.tick();
        }

        @Override
        protected boolean resetXRotOnTick() {
            return !this.this$0.beePollinateGoal.isPollinating();
        }
    }

    private class BeeAttackGoal
    extends MeleeAttackGoal {
        final /* synthetic */ Bee this$0;

        public BeeAttackGoal(Bee bee, PathfinderMob mob, double speedModifier, boolean trackTarget) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(mob, speedModifier, trackTarget);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.this$0.isAngry() && !this.this$0.hasStung();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.this$0.isAngry() && !this.this$0.hasStung();
        }
    }

    private class BeeEnterHiveGoal
    extends BaseBeeGoal {
        final /* synthetic */ Bee this$0;

        private BeeEnterHiveGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
        }

        @Override
        public boolean canBeeUse() {
            BeehiveBlockEntity beehiveBlockEntity;
            if (this.this$0.hivePos != null && this.this$0.wantsToEnterHive() && this.this$0.hivePos.closerToCenterThan(this.this$0.position(), 2.0) && (beehiveBlockEntity = this.this$0.getBeehiveBlockEntity()) != null) {
                if (beehiveBlockEntity.isFull()) {
                    this.this$0.hivePos = null;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BeehiveBlockEntity beehiveBlockEntity = this.this$0.getBeehiveBlockEntity();
            if (beehiveBlockEntity != null) {
                beehiveBlockEntity.addOccupant(this.this$0);
            }
        }
    }

    private class ValidateHiveGoal
    extends BaseBeeGoal {
        private final int VALIDATE_HIVE_COOLDOWN;
        private long lastValidateTick;
        final /* synthetic */ Bee this$0;

        private ValidateHiveGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
            this.VALIDATE_HIVE_COOLDOWN = Mth.nextInt(this.this$0.random, 20, 40);
            this.lastValidateTick = -1L;
        }

        @Override
        public void start() {
            if (this.this$0.hivePos != null && this.this$0.level().isLoaded(this.this$0.hivePos) && !this.this$0.isHiveValid()) {
                this.this$0.dropHive();
            }
            this.lastValidateTick = this.this$0.level().getGameTime();
        }

        @Override
        public boolean canBeeUse() {
            return this.this$0.level().getGameTime() > this.lastValidateTick + (long)this.VALIDATE_HIVE_COOLDOWN;
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }
    }

    private class ValidateFlowerGoal
    extends BaseBeeGoal {
        private final int validateFlowerCooldown;
        private long lastValidateTick;
        final /* synthetic */ Bee this$0;

        private ValidateFlowerGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
            this.validateFlowerCooldown = Mth.nextInt(this.this$0.random, 20, 40);
            this.lastValidateTick = -1L;
        }

        @Override
        public void start() {
            if (this.this$0.savedFlowerPos != null && this.this$0.level().isLoaded(this.this$0.savedFlowerPos) && !this.isFlower(this.this$0.savedFlowerPos)) {
                this.this$0.dropFlower();
            }
            this.lastValidateTick = this.this$0.level().getGameTime();
        }

        @Override
        public boolean canBeeUse() {
            return this.this$0.level().getGameTime() > this.lastValidateTick + (long)this.validateFlowerCooldown;
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }

        private boolean isFlower(BlockPos flowerPos) {
            return Bee.attractsBees(this.this$0.level().getBlockState(flowerPos));
        }
    }

    private class BeePollinateGoal
    extends BaseBeeGoal {
        private static final int MIN_POLLINATION_TICKS = 400;
        private static final double ARRIVAL_THRESHOLD = 0.1;
        private static final int POSITION_CHANGE_CHANCE = 25;
        private static final float SPEED_MODIFIER = 0.35f;
        private static final float HOVER_HEIGHT_WITHIN_FLOWER = 0.6f;
        private static final float HOVER_POS_OFFSET = 0.33333334f;
        private static final int FLOWER_SEARCH_RADIUS = 5;
        private int successfulPollinatingTicks;
        private int lastSoundPlayedTick;
        private boolean pollinating;
        private @Nullable Vec3 hoverPos;
        private int pollinatingTicks;
        private static final int MAX_POLLINATING_TICKS = 600;
        private Long2LongOpenHashMap unreachableFlowerCache;
        final /* synthetic */ Bee this$0;

        public BeePollinateGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
            this.unreachableFlowerCache = new Long2LongOpenHashMap();
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            if (this.this$0.remainingCooldownBeforeLocatingNewFlower > 0) {
                return false;
            }
            if (this.this$0.hasNectar()) {
                return false;
            }
            if (this.this$0.level().isRaining()) {
                return false;
            }
            Optional<BlockPos> nearbyPos = this.findNearbyFlower();
            if (nearbyPos.isPresent()) {
                this.this$0.savedFlowerPos = nearbyPos.get();
                this.this$0.navigation.moveTo((double)this.this$0.savedFlowerPos.getX() + 0.5, (double)this.this$0.savedFlowerPos.getY() + 0.5, (double)this.this$0.savedFlowerPos.getZ() + 0.5, 1.2f);
                return true;
            }
            this.this$0.remainingCooldownBeforeLocatingNewFlower = Mth.nextInt(this.this$0.random, 20, 60);
            return false;
        }

        @Override
        public boolean canBeeContinueToUse() {
            if (!this.pollinating) {
                return false;
            }
            if (!this.this$0.hasSavedFlowerPos()) {
                return false;
            }
            if (this.this$0.level().isRaining()) {
                return false;
            }
            if (this.hasPollinatedLongEnough()) {
                return this.this$0.random.nextFloat() < 0.2f;
            }
            return true;
        }

        private boolean hasPollinatedLongEnough() {
            return this.successfulPollinatingTicks > 400;
        }

        private boolean isPollinating() {
            return this.pollinating;
        }

        private void stopPollinating() {
            this.pollinating = false;
        }

        @Override
        public void start() {
            this.successfulPollinatingTicks = 0;
            this.pollinatingTicks = 0;
            this.lastSoundPlayedTick = 0;
            this.pollinating = true;
            this.this$0.resetTicksWithoutNectarSinceExitingHive();
        }

        @Override
        public void stop() {
            if (this.hasPollinatedLongEnough()) {
                this.this$0.setHasNectar(true);
            }
            this.pollinating = false;
            this.this$0.navigation.stop();
            this.this$0.remainingCooldownBeforeLocatingNewFlower = 200;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!this.this$0.hasSavedFlowerPos()) {
                return;
            }
            ++this.pollinatingTicks;
            if (this.pollinatingTicks > 600) {
                this.this$0.dropFlower();
                this.pollinating = false;
                this.this$0.remainingCooldownBeforeLocatingNewFlower = 200;
                return;
            }
            Vec3 flowerPos = Vec3.atBottomCenterOf(this.this$0.savedFlowerPos).add(0.0, 0.6f, 0.0);
            if (flowerPos.distanceTo(this.this$0.position()) > 1.0) {
                this.hoverPos = flowerPos;
                this.setWantedPos();
                return;
            }
            if (this.hoverPos == null) {
                this.hoverPos = flowerPos;
            }
            boolean arrivedAtHoverPos = this.this$0.position().distanceTo(this.hoverPos) <= 0.1;
            boolean shouldSetWantedPos = true;
            if (!arrivedAtHoverPos && this.pollinatingTicks > 600) {
                this.this$0.dropFlower();
                return;
            }
            if (arrivedAtHoverPos) {
                boolean shouldChangeHoverPositions;
                boolean bl = shouldChangeHoverPositions = this.this$0.random.nextInt(25) == 0;
                if (shouldChangeHoverPositions) {
                    this.hoverPos = new Vec3(flowerPos.x() + (double)this.getOffset(), flowerPos.y(), flowerPos.z() + (double)this.getOffset());
                    this.this$0.navigation.stop();
                } else {
                    shouldSetWantedPos = false;
                }
                this.this$0.getLookControl().setLookAt(flowerPos.x(), flowerPos.y(), flowerPos.z());
            }
            if (shouldSetWantedPos) {
                this.setWantedPos();
            }
            ++this.successfulPollinatingTicks;
            if (this.this$0.random.nextFloat() < 0.05f && this.successfulPollinatingTicks > this.lastSoundPlayedTick + 60) {
                this.lastSoundPlayedTick = this.successfulPollinatingTicks;
                this.this$0.playSound(SoundEvents.BEE_POLLINATE, 1.0f, 1.0f);
            }
        }

        private void setWantedPos() {
            this.this$0.getMoveControl().setWantedPosition(this.hoverPos.x(), this.hoverPos.y(), this.hoverPos.z(), 0.35f);
        }

        private float getOffset() {
            return (this.this$0.random.nextFloat() * 2.0f - 1.0f) * 0.33333334f;
        }

        private Optional<BlockPos> findNearbyFlower() {
            Iterable<BlockPos> closestNearbyFlowers = BlockPos.withinManhattan(this.this$0.blockPosition(), 5, 5, 5);
            Long2LongOpenHashMap tempCache = new Long2LongOpenHashMap();
            for (BlockPos pos : closestNearbyFlowers) {
                long unreachableUntilTime = this.unreachableFlowerCache.getOrDefault(pos.asLong(), Long.MIN_VALUE);
                if (this.this$0.level().getGameTime() < unreachableUntilTime) {
                    tempCache.put(pos.asLong(), unreachableUntilTime);
                    continue;
                }
                if (!Bee.attractsBees(this.this$0.level().getBlockState(pos))) continue;
                Path path = this.this$0.navigation.createPath(pos, 1);
                if (path != null && path.canReach()) {
                    return Optional.of(pos);
                }
                tempCache.put(pos.asLong(), this.this$0.level().getGameTime() + 600L);
            }
            this.unreachableFlowerCache = tempCache;
            return Optional.empty();
        }
    }

    private class BeeLocateHiveGoal
    extends BaseBeeGoal {
        final /* synthetic */ Bee this$0;

        private BeeLocateHiveGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
        }

        @Override
        public boolean canBeeUse() {
            return this.this$0.remainingCooldownBeforeLocatingNewHive == 0 && !this.this$0.hasHive() && this.this$0.wantsToEnterHive();
        }

        @Override
        public boolean canBeeContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.this$0.remainingCooldownBeforeLocatingNewHive = 200;
            List<BlockPos> hivesWithSpace = this.findNearbyHivesWithSpace();
            if (hivesWithSpace.isEmpty()) {
                return;
            }
            for (BlockPos posToCheck : hivesWithSpace) {
                if (this.this$0.goToHiveGoal.isTargetBlacklisted(posToCheck)) continue;
                this.this$0.hivePos = posToCheck;
                return;
            }
            this.this$0.goToHiveGoal.clearBlacklist();
            this.this$0.hivePos = hivesWithSpace.get(0);
        }

        private List<BlockPos> findNearbyHivesWithSpace() {
            BlockPos beePos = this.this$0.blockPosition();
            PoiManager poiManager = ((ServerLevel)this.this$0.level()).getPoiManager();
            Stream<PoiRecord> nearbyHives = poiManager.getInRange(p -> p.is(PoiTypeTags.BEE_HOME), beePos, 20, PoiManager.Occupancy.ANY);
            return nearbyHives.map(PoiRecord::getPos).filter(this.this$0::doesHiveHaveSpace).sorted(Comparator.comparingDouble(pos -> pos.distSqr(beePos))).collect(Collectors.toList());
        }
    }

    @VisibleForDebug
    public class BeeGoToHiveGoal
    extends BaseBeeGoal {
        public static final int MAX_TRAVELLING_TICKS = 2400;
        private int travellingTicks;
        private static final int MAX_BLACKLISTED_TARGETS = 3;
        private final List<BlockPos> blacklistedTargets;
        private @Nullable Path lastPath;
        private static final int TICKS_BEFORE_HIVE_DROP = 60;
        private int ticksStuck;
        final /* synthetic */ Bee this$0;

        public BeeGoToHiveGoal(Bee this$0) {
            Bee bee = this$0;
            Objects.requireNonNull(bee);
            this.this$0 = bee;
            super(this$0);
            this.blacklistedTargets = Lists.newArrayList();
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            return this.this$0.hivePos != null && !this.this$0.isTooFarAway(this.this$0.hivePos) && !this.this$0.hasHome() && this.this$0.wantsToEnterHive() && !this.hasReachedTarget(this.this$0.hivePos) && this.this$0.level().getBlockState(this.this$0.hivePos).is(BlockTags.BEEHIVES);
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void start() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            super.start();
        }

        @Override
        public void stop() {
            this.travellingTicks = 0;
            this.ticksStuck = 0;
            this.this$0.navigation.stop();
            this.this$0.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            if (this.this$0.hivePos == null) {
                return;
            }
            ++this.travellingTicks;
            if (this.travellingTicks > this.adjustedTickDelay(2400)) {
                this.dropAndBlacklistHive();
                return;
            }
            if (this.this$0.navigation.isInProgress()) {
                return;
            }
            if (this.this$0.closerThan(this.this$0.hivePos, 16)) {
                boolean canReachAllTheWayToTarget = this.pathfindDirectlyTowards(this.this$0.hivePos);
                if (!canReachAllTheWayToTarget) {
                    this.dropAndBlacklistHive();
                } else if (this.lastPath != null && this.this$0.navigation.getPath().sameAs(this.lastPath)) {
                    ++this.ticksStuck;
                    if (this.ticksStuck > 60) {
                        this.this$0.dropHive();
                        this.ticksStuck = 0;
                    }
                } else {
                    this.lastPath = this.this$0.navigation.getPath();
                }
                return;
            }
            if (this.this$0.isTooFarAway(this.this$0.hivePos)) {
                this.this$0.dropHive();
                return;
            }
            this.this$0.pathfindRandomlyTowards(this.this$0.hivePos);
        }

        private boolean pathfindDirectlyTowards(BlockPos targetPos) {
            int closeEnough = this.this$0.closerThan(targetPos, 3) ? 1 : 2;
            this.this$0.navigation.setMaxVisitedNodesMultiplier(10.0f);
            this.this$0.navigation.moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), closeEnough, 1.0);
            return this.this$0.navigation.getPath() != null && this.this$0.navigation.getPath().canReach();
        }

        private boolean isTargetBlacklisted(BlockPos targetPos) {
            return this.blacklistedTargets.contains(targetPos);
        }

        private void blacklistTarget(BlockPos targetPos) {
            this.blacklistedTargets.add(targetPos);
            while (this.blacklistedTargets.size() > 3) {
                this.blacklistedTargets.remove(0);
            }
        }

        private void clearBlacklist() {
            this.blacklistedTargets.clear();
        }

        private void dropAndBlacklistHive() {
            if (this.this$0.hivePos != null) {
                this.blacklistTarget(this.this$0.hivePos);
            }
            this.this$0.dropHive();
        }

        private boolean hasReachedTarget(BlockPos targetPos) {
            if (this.this$0.closerThan(targetPos, 2)) {
                return true;
            }
            Path path = this.this$0.navigation.getPath();
            return path != null && path.getTarget().equals(targetPos) && path.canReach() && path.isDone();
        }
    }

    public class BeeGoToKnownFlowerGoal
    extends BaseBeeGoal {
        private static final int MAX_TRAVELLING_TICKS = 2400;
        private int travellingTicks;
        final /* synthetic */ Bee this$0;

        public BeeGoToKnownFlowerGoal(Bee this$0) {
            Bee bee = this$0;
            Objects.requireNonNull(bee);
            this.this$0 = bee;
            super(this$0);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canBeeUse() {
            return this.this$0.savedFlowerPos != null && !this.this$0.hasHome() && this.wantsToGoToKnownFlower() && !this.this$0.closerThan(this.this$0.savedFlowerPos, 2);
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void start() {
            this.travellingTicks = 0;
            super.start();
        }

        @Override
        public void stop() {
            this.travellingTicks = 0;
            this.this$0.navigation.stop();
            this.this$0.navigation.resetMaxVisitedNodesMultiplier();
        }

        @Override
        public void tick() {
            if (this.this$0.savedFlowerPos == null) {
                return;
            }
            ++this.travellingTicks;
            if (this.travellingTicks > this.adjustedTickDelay(2400)) {
                this.this$0.dropFlower();
                return;
            }
            if (this.this$0.navigation.isInProgress()) {
                return;
            }
            if (this.this$0.isTooFarAway(this.this$0.savedFlowerPos)) {
                this.this$0.dropFlower();
                return;
            }
            this.this$0.pathfindRandomlyTowards(this.this$0.savedFlowerPos);
        }

        private boolean wantsToGoToKnownFlower() {
            return this.this$0.ticksWithoutNectarSinceExitingHive > 600;
        }
    }

    private class BeeGrowCropGoal
    extends BaseBeeGoal {
        private static final int GROW_CHANCE = 30;
        final /* synthetic */ Bee this$0;

        private BeeGrowCropGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            super(bee);
        }

        @Override
        public boolean canBeeUse() {
            if (this.this$0.getCropsGrownSincePollination() >= 10) {
                return false;
            }
            if (this.this$0.random.nextFloat() < 0.3f) {
                return false;
            }
            return this.this$0.hasNectar() && this.this$0.isHiveValid();
        }

        @Override
        public boolean canBeeContinueToUse() {
            return this.canBeeUse();
        }

        @Override
        public void tick() {
            if (this.this$0.random.nextInt(this.adjustedTickDelay(30)) != 0) {
                return;
            }
            for (int i = 1; i <= 2; ++i) {
                BonemealableBlock bonemealableBlock;
                BlockPos belowPos = this.this$0.blockPosition().below(i);
                BlockState belowState = this.this$0.level().getBlockState(belowPos);
                Block belowBlock = belowState.getBlock();
                BlockState growState = null;
                if (!belowState.is(BlockTags.BEE_GROWABLES)) continue;
                if (belowBlock instanceof CropBlock) {
                    CropBlock cropBlockBelow = (CropBlock)belowBlock;
                    if (!cropBlockBelow.isMaxAge(belowState)) {
                        growState = cropBlockBelow.getStateForAge(cropBlockBelow.getAge(belowState) + 1);
                    }
                } else if (belowBlock instanceof StemBlock) {
                    int age = belowState.getValue(StemBlock.AGE);
                    if (age < 7) {
                        growState = (BlockState)belowState.setValue(StemBlock.AGE, age + 1);
                    }
                } else if (belowState.is(Blocks.SWEET_BERRY_BUSH)) {
                    int age = belowState.getValue(SweetBerryBushBlock.AGE);
                    if (age < 3) {
                        growState = (BlockState)belowState.setValue(SweetBerryBushBlock.AGE, age + 1);
                    }
                } else if ((belowState.is(Blocks.CAVE_VINES) || belowState.is(Blocks.CAVE_VINES_PLANT)) && (bonemealableBlock = (BonemealableBlock)((Object)belowState.getBlock())).isValidBonemealTarget(this.this$0.level(), belowPos, belowState)) {
                    bonemealableBlock.performBonemeal((ServerLevel)this.this$0.level(), this.this$0.random, belowPos, belowState);
                    growState = this.this$0.level().getBlockState(belowPos);
                }
                if (growState == null) continue;
                this.this$0.level().levelEvent(2011, belowPos, 15);
                this.this$0.level().setBlockAndUpdate(belowPos, growState);
                this.this$0.incrementNumCropsGrownSincePollination();
            }
        }
    }

    private class BeeWanderGoal
    extends Goal {
        final /* synthetic */ Bee this$0;

        public BeeWanderGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.this$0.navigation.isDone() && this.this$0.random.nextInt(10) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.this$0.navigation.isInProgress();
        }

        @Override
        public void start() {
            Vec3 targetPos = this.findPos();
            if (targetPos != null) {
                this.this$0.navigation.moveTo(this.this$0.navigation.createPath(BlockPos.containing(targetPos), 1), 1.0);
            }
        }

        private @Nullable Vec3 findPos() {
            Vec3 wanderDirection;
            if (this.this$0.isHiveValid() && !this.this$0.closerThan(this.this$0.hivePos, this.getWanderThreshold())) {
                Vec3 hivePosVec = Vec3.atCenterOf(this.this$0.hivePos);
                wanderDirection = hivePosVec.subtract(this.this$0.position()).normalize();
            } else {
                wanderDirection = this.this$0.getViewVector(0.0f);
            }
            int xzDist = 8;
            Vec3 groundBasedPosition = HoverRandomPos.getPos(this.this$0, 8, 7, wanderDirection.x, wanderDirection.z, 1.5707964f, 3, 1);
            if (groundBasedPosition != null) {
                return groundBasedPosition;
            }
            return AirAndWaterRandomPos.getPos(this.this$0, 8, 4, -2, wanderDirection.x, wanderDirection.z, 1.5707963705062866);
        }

        private int getWanderThreshold() {
            int distanceReduction = this.this$0.hasHive() || this.this$0.hasSavedFlowerPos() ? 24 : 16;
            return 48 - distanceReduction;
        }
    }

    private class BeeHurtByOtherGoal
    extends HurtByTargetGoal {
        final /* synthetic */ Bee this$0;

        public BeeHurtByOtherGoal(Bee bee, Bee bee2) {
            Bee bee3 = bee;
            Objects.requireNonNull(bee3);
            this.this$0 = bee3;
            super(bee2, new Class[0]);
        }

        @Override
        public boolean canContinueToUse() {
            return this.this$0.isAngry() && super.canContinueToUse();
        }

        @Override
        protected void alertOther(Mob other, LivingEntity hurtByMob) {
            if (other instanceof Bee && this.mob.hasLineOfSight(hurtByMob)) {
                other.setTarget(hurtByMob);
            }
        }
    }

    private static class BeeBecomeAngryTargetGoal
    extends NearestAttackableTargetGoal<Player> {
        public BeeBecomeAngryTargetGoal(Bee bee) {
            super(bee, Player.class, 10, true, false, bee::isAngryAt);
        }

        @Override
        public boolean canUse() {
            return this.beeCanTarget() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            boolean beeCanTarget = this.beeCanTarget();
            if (!beeCanTarget || this.mob.getTarget() == null) {
                this.targetMob = null;
                return false;
            }
            return super.canContinueToUse();
        }

        private boolean beeCanTarget() {
            Bee bee = (Bee)this.mob;
            return bee.isAngry() && !bee.hasStung();
        }
    }

    private abstract class BaseBeeGoal
    extends Goal {
        final /* synthetic */ Bee this$0;

        private BaseBeeGoal(Bee bee) {
            Bee bee2 = bee;
            Objects.requireNonNull(bee2);
            this.this$0 = bee2;
        }

        public abstract boolean canBeeUse();

        public abstract boolean canBeeContinueToUse();

        @Override
        public boolean canUse() {
            return this.canBeeUse() && !this.this$0.isAngry();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canBeeContinueToUse() && !this.this$0.isAngry();
        }
    }
}

