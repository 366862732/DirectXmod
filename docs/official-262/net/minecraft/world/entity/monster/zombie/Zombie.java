/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.zombie;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Zombie
extends Monster {
    private static final Identifier SPEED_MODIFIER_BABY_ID = Identifier.withDefaultNamespace("baby");
    private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(SPEED_MODIFIER_BABY_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final Identifier REINFORCEMENT_CALLER_CHARGE_ID = Identifier.withDefaultNamespace("reinforcement_caller_charge");
    private static final AttributeModifier ZOMBIE_REINFORCEMENT_CALLEE_CHARGE = new AttributeModifier(Identifier.withDefaultNamespace("reinforcement_callee_charge"), -0.05f, AttributeModifier.Operation.ADD_VALUE);
    private static final Identifier LEADER_ZOMBIE_BONUS_ID = Identifier.withDefaultNamespace("leader_zombie_bonus");
    private static final Identifier ZOMBIE_RANDOM_SPAWN_BONUS_ID = Identifier.withDefaultNamespace("zombie_random_spawn_bonus");
    private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPECIAL_TYPE_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DROWNED_CONVERSION_ID = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
    public static final float ZOMBIE_LEADER_CHANCE = 0.05f;
    public static final int REINFORCEMENT_ATTEMPTS = 50;
    public static final int REINFORCEMENT_RANGE_MAX = 40;
    public static final int REINFORCEMENT_RANGE_MIN = 7;
    private static final int NOT_CONVERTING = -1;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.49f, 0.98f).withEyeHeight(0.775f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, 0.0f, 0.1875f, 0.0f));
    private static final float BREAK_DOOR_CHANCE = 0.1f;
    private static final Predicate<Difficulty> DOOR_BREAKING_PREDICATE = d -> d == Difficulty.HARD;
    private static final boolean DEFAULT_BABY = false;
    private static final boolean DEFAULT_CAN_BREAK_DOORS = false;
    private static final int DEFAULT_IN_WATER_TIME = 0;
    private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE);
    private boolean canBreakDoors = false;
    private int inWaterTime = 0;
    private int conversionTime;

    public Zombie(EntityType<? extends Zombie> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
    }

    public Zombie(Level level) {
        this((EntityType<? extends Zombie>)EntityTypes.ZOMBIE, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new ZombieAttackTurtleEggGoal(this, (PathfinderMob)this, 1.0, 3));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.addBehaviourGoals();
    }

    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new SpearUseGoal<Zombie>(this, 1.0, 1.0, 10.0f, 2.0f));
        this.goalSelector.addGoal(3, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, true, 4, this::canBreakDoors));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(ZombifiedPiglin.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<AbstractVillager>((Mob)this, AbstractVillager.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<IronGolem>((Mob)this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<Turtle>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 35.0).add(Attributes.MOVEMENT_SPEED, 0.23f).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 2.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_BABY_ID, false);
        entityData.define(DATA_SPECIAL_TYPE_ID, 0);
        entityData.define(DATA_DROWNED_CONVERSION_ID, false);
    }

    public boolean isUnderWaterConverting() {
        return this.getEntityData().get(DATA_DROWNED_CONVERSION_ID);
    }

    public boolean canBreakDoors() {
        return this.canBreakDoors;
    }

    public void setCanBreakDoors(boolean canBreakDoors) {
        if (this.navigation.canNavigateGround()) {
            if (this.canBreakDoors != canBreakDoors) {
                this.canBreakDoors = canBreakDoors;
                this.navigation.setCanOpenDoors(canBreakDoors);
                if (canBreakDoors) {
                    this.goalSelector.addGoal(1, this.breakDoorGoal);
                } else {
                    this.goalSelector.removeGoal(this.breakDoorGoal);
                }
            }
        } else if (this.canBreakDoors) {
            this.goalSelector.removeGoal(this.breakDoorGoal);
            this.canBreakDoors = false;
        }
    }

    @Override
    public boolean isBaby() {
        return this.getEntityData().get(DATA_BABY_ID);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        if (this.isBaby()) {
            this.xpReward = (int)((double)this.xpReward * 2.5);
        }
        return super.getBaseExperienceReward(level);
    }

    @Override
    public void setBaby(boolean baby) {
        this.getEntityData().set(DATA_BABY_ID, baby);
        if (this.level() != null && !this.level().isClientSide()) {
            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            speed.removeModifier(SPEED_MODIFIER_BABY_ID);
            if (baby) {
                speed.addTransientModifier(SPEED_MODIFIER_BABY);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BABY_ID.equals(accessor)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    protected boolean convertsInWater() {
        return true;
    }

    @Override
    public void tick() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.isAlive() && !this.isNoAi()) {
                if (this.isUnderWaterConverting()) {
                    --this.conversionTime;
                    if (this.conversionTime < 0) {
                        this.doUnderWaterConversion(serverLevel);
                    }
                } else if (this.convertsInWater()) {
                    if (this.isEyeInFluid(FluidTags.WATER)) {
                        ++this.inWaterTime;
                        if (this.inWaterTime >= 600) {
                            this.startUnderWaterConversion(300);
                        }
                    } else {
                        this.inWaterTime = -1;
                    }
                }
            }
        }
        super.tick();
    }

    private void startUnderWaterConversion(int time) {
        this.conversionTime = time;
        this.getEntityData().set(DATA_DROWNED_CONVERSION_ID, true);
    }

    protected void doUnderWaterConversion(ServerLevel level) {
        this.convertToZombieType(level, EntityTypes.DROWNED);
        if (!this.isSilent()) {
            level.levelEvent(null, 1040, this.blockPosition(), 0);
        }
    }

    protected void convertToZombieType(ServerLevel level, EntityType<? extends Zombie> zombieType) {
        this.convertTo(zombieType, ConversionParams.single(this, true, true), newZombie -> newZombie.handleAttributes(level.getCurrentDifficultyAt(newZombie.blockPosition()).getSpecialMultiplier(), EntitySpawnReason.CONVERSION));
    }

    @VisibleForTesting
    public boolean convertVillagerToZombieVillager(ServerLevel level, Villager villager) {
        ZombieVillager zombieVillager = villager.convertTo(EntityTypes.ZOMBIE_VILLAGER, ConversionParams.single(villager, true, true), zombie -> {
            zombie.setVillagerDataFinalized(villager.getVillagerDataFinalized());
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.CONVERSION, new ZombieGroupData(false, true));
            zombie.setVillagerData(villager.getVillagerData());
            zombie.setGossips(villager.getGossips().copy());
            zombie.setTradeOffers(villager.getOffers().copy());
            zombie.setVillagerXp(villager.getVillagerXp());
            if (!this.isSilent()) {
                level.levelEvent(null, 1026, this.blockPosition(), 0);
            }
        });
        return zombieVillager != null;
    }

    protected boolean isSunSensitive() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!super.hurtServer(level, source, damage)) {
            return false;
        }
        LivingEntity target = this.getTarget();
        if (target == null && source.getEntity() instanceof LivingEntity) {
            target = (LivingEntity)source.getEntity();
        }
        if (target != null && level.getDifficulty() == Difficulty.HARD && (double)this.random.nextFloat() < this.getAttributeValue(Attributes.SPAWN_REINFORCEMENTS_CHANCE) && level.isSpawningMonsters()) {
            int x = Mth.floor(this.getX());
            int y = Mth.floor(this.getY());
            int z = Mth.floor(this.getZ());
            EntityType<? extends Zombie> type = this.getType();
            Zombie reinforcement = type.create((Level)level, EntitySpawnReason.REINFORCEMENT);
            if (reinforcement == null) {
                return true;
            }
            for (int i = 0; i < 50; ++i) {
                int zt;
                int yt;
                int xt = x + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1);
                BlockPos spawnPos = new BlockPos(xt, yt = y + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1), zt = z + Mth.nextInt(this.random, 7, 40) * Mth.nextInt(this.random, -1, 1));
                if (!SpawnPlacements.isSpawnPositionOk(type, level, spawnPos) || !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.REINFORCEMENT, spawnPos, level.getRandom())) continue;
                reinforcement.setPos(xt, yt, zt);
                if (level.hasNearbyAlivePlayer(xt, yt, zt, 7.0) || !level.isUnobstructed(reinforcement) || !level.noCollision(reinforcement) || !reinforcement.canSpawnInLiquids() && level.containsAnyLiquid(reinforcement.getBoundingBox())) continue;
                reinforcement.setTarget(target);
                reinforcement.finalizeSpawn(level, level.getCurrentDifficultyAt(reinforcement.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
                level.addFreshEntityWithPassengers(reinforcement);
                AttributeInstance attribute = this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
                AttributeModifier modifier = attribute.getModifier(REINFORCEMENT_CALLER_CHARGE_ID);
                double existingAmount = modifier != null ? modifier.amount() : 0.0;
                attribute.removeModifier(REINFORCEMENT_CALLER_CHARGE_ID);
                attribute.addPermanentModifier(new AttributeModifier(REINFORCEMENT_CALLER_CHARGE_ID, existingAmount - 0.05, AttributeModifier.Operation.ADD_VALUE));
                reinforcement.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).addPermanentModifier(ZOMBIE_REINFORCEMENT_CALLEE_CHARGE);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean result = super.doHurtTarget(level, target);
        if (result) {
            float difficulty = level.getCurrentDifficultyAt(this.blockPosition()).getEffectiveDifficulty();
            if (this.getMainHandItem().isEmpty() && this.isOnFire() && this.random.nextFloat() < difficulty * 0.3f) {
                target.igniteForSeconds(2 * (int)difficulty);
            }
        }
        return result;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    protected SoundEvent getStepSound() {
        return SoundEvents.ZOMBIE_STEP;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(this.getStepSound(), 0.15f, 1.0f);
    }

    public EntityType<? extends Zombie> getType() {
        return super.getType();
    }

    protected boolean canSpawnInLiquids() {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        float f = random.nextFloat();
        float f2 = this.level().getDifficulty() == Difficulty.HARD ? 0.05f : 0.01f;
        if (f < f2) {
            int rand = random.nextInt(6);
            if (rand == 0) {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            } else if (rand == 1) {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SPEAR));
            } else {
                this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsBaby", this.isBaby());
        output.putBoolean("CanBreakDoors", this.canBreakDoors());
        output.putInt("InWaterTime", this.isInWater() ? this.inWaterTime : -1);
        output.putInt("DrownedConversionTime", this.isUnderWaterConverting() ? this.conversionTime : -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setBaby(input.getBooleanOr("IsBaby", false));
        this.setCanBreakDoors(input.getBooleanOr("CanBreakDoors", false));
        this.inWaterTime = input.getIntOr("InWaterTime", 0);
        int conversionTime = input.getIntOr("DrownedConversionTime", -1);
        if (conversionTime != -1) {
            this.startUnderWaterConversion(conversionTime);
        } else {
            this.getEntityData().set(DATA_DROWNED_CONVERSION_ID, false);
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        boolean perished = super.killedEntity(level, entity, source);
        if ((level.getDifficulty() == Difficulty.NORMAL || level.getDifficulty() == Difficulty.HARD) && entity instanceof Villager) {
            Villager villager = (Villager)entity;
            if (level.getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
                return perished;
            }
            if (this.convertVillagerToZombieVillager(level, villager)) {
                perished = false;
            }
        }
        return perished;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        if (itemStack.is(ItemTags.EGGS) && this.isBaby() && this.isPassenger()) {
            return false;
        }
        return super.canHoldItem(itemStack);
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack itemStack) {
        if (itemStack.is(Items.GLOW_INK_SAC)) {
            return false;
        }
        return super.wantsToPickUp(level, itemStack);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        RandomSource random = level.getRandom();
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        float difficultyModifier = difficulty.getSpecialMultiplier();
        if (spawnReason != EntitySpawnReason.CONVERSION) {
            this.setCanPickUpLoot(random.nextFloat() < 0.55f * difficultyModifier);
        }
        if (groupData == null) {
            groupData = new ZombieGroupData(Zombie.getSpawnAsBabyOdds(random), true);
        }
        if (groupData instanceof ZombieGroupData) {
            ZombieGroupData zombieData = (ZombieGroupData)groupData;
            if (zombieData.isBaby) {
                this.setBaby(true);
                if (zombieData.canSpawnJockey) {
                    Chicken chicken;
                    if ((double)random.nextFloat() < 0.05) {
                        List<Entity> chickens = level.getEntitiesOfClass(Chicken.class, this.getBoundingBox().inflate(5.0, 3.0, 5.0), EntitySelector.ENTITY_NOT_BEING_RIDDEN);
                        if (!chickens.isEmpty()) {
                            Chicken chicken2 = (Chicken)chickens.get(0);
                            chicken2.setChickenJockey(true);
                            this.startRiding(chicken2, false, false);
                        }
                    } else if ((double)random.nextFloat() < 0.05 && (chicken = EntityTypes.CHICKEN.create(this.level(), EntitySpawnReason.JOCKEY)) != null) {
                        chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
                        chicken.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
                        chicken.setChickenJockey(true);
                        this.startRiding(chicken, false, false);
                        level.addFreshEntity(chicken);
                    }
                }
            }
            this.setCanBreakDoors(random.nextFloat() < difficultyModifier * 0.1f);
            if (spawnReason != EntitySpawnReason.CONVERSION) {
                this.populateDefaultEquipmentSlots(random, difficulty);
                this.populateDefaultEquipmentEnchantments(level, random, difficulty);
            }
        }
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && SpecialDates.isHalloween() && random.nextFloat() < 0.25f) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(random.nextFloat() < 0.1f ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
            this.setDropChance(EquipmentSlot.HEAD, 0.0f);
        }
        this.handleAttributes(difficultyModifier, spawnReason);
        return groupData;
    }

    @Override
    protected void onOffspringSpawnedFromEgg(Player spawner, Mob offspring) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            float difficultyModifier = serverLevel.getCurrentDifficultyAt(offspring.blockPosition()).getSpecialMultiplier();
            offspring.setCanPickUpLoot(this.random.nextFloat() < 0.55f * difficultyModifier);
        }
    }

    @VisibleForTesting
    public void setInWaterTime(int inWaterTime) {
        this.inWaterTime = inWaterTime;
    }

    @VisibleForTesting
    public void setConversionTime(int conversionTime) {
        this.conversionTime = conversionTime;
    }

    public static boolean getSpawnAsBabyOdds(RandomSource random) {
        return random.nextFloat() < 0.05f;
    }

    protected void handleAttributes(float difficultyModifier, EntitySpawnReason spawnReason) {
        this.randomizeReinforcementsChance();
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrReplacePermanentModifier(new AttributeModifier(RANDOM_SPAWN_BONUS_ID, this.random.nextDouble() * (double)0.05f, AttributeModifier.Operation.ADD_VALUE));
        double followRangeModifier = this.random.nextDouble() * 1.5 * (double)difficultyModifier;
        if (followRangeModifier > 1.0) {
            this.getAttribute(Attributes.FOLLOW_RANGE).addOrReplacePermanentModifier(new AttributeModifier(ZOMBIE_RANDOM_SPAWN_BONUS_ID, followRangeModifier, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (this.random.nextFloat() < difficultyModifier * 0.05f) {
            this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).addOrReplacePermanentModifier(new AttributeModifier(LEADER_ZOMBIE_BONUS_ID, this.random.nextDouble() * 0.25 + 0.5, AttributeModifier.Operation.ADD_VALUE));
            this.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(new AttributeModifier(LEADER_ZOMBIE_BONUS_ID, this.random.nextDouble() * 3.0 + 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (spawnReason != EntitySpawnReason.CONVERSION && spawnReason != EntitySpawnReason.LOAD && spawnReason != EntitySpawnReason.DIMENSION_TRAVEL) {
                this.setHealth(this.getMaxHealth());
            }
            this.setCanBreakDoors(true);
        }
    }

    protected void randomizeReinforcementsChance() {
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(this.random.nextDouble() * (double)0.1f);
    }

    private class ZombieAttackTurtleEggGoal
    extends RemoveBlockGoal {
        final /* synthetic */ Zombie this$0;

        public ZombieAttackTurtleEggGoal(Zombie zombie, PathfinderMob mob, double speedModifier, int verticalSearchRange) {
            Zombie zombie2 = zombie;
            Objects.requireNonNull(zombie2);
            this.this$0 = zombie2;
            super(Blocks.TURTLE_EGG, mob, speedModifier, verticalSearchRange);
        }

        @Override
        public void playDestroyProgressSound(LevelAccessor level, BlockPos pos) {
            level.playSound(null, pos, SoundEvents.ZOMBIE_DESTROY_EGG, SoundSource.HOSTILE, 0.5f, 0.9f + this.this$0.random.nextFloat() * 0.2f);
        }

        @Override
        public void playBreakSound(Level level, BlockPos pos) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }

        @Override
        public double acceptedDistance() {
            return 1.14;
        }
    }

    public static class ZombieGroupData
    implements SpawnGroupData {
        public final boolean isBaby;
        public final boolean canSpawnJockey;

        public ZombieGroupData(boolean baby, boolean canSpawnJockey) {
            this.isBaby = baby;
            this.canSpawnJockey = canSpawnJockey;
        }
    }
}

