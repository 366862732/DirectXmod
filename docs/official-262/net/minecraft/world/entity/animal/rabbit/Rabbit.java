/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.rabbit;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Rabbit
extends Animal {
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.24f, 0.4f).withEyeHeight(0.39f);
    public static final double STROLL_SPEED_MOD = 0.6;
    public static final double BREED_SPEED_MOD = 0.8;
    public static final double FOLLOW_SPEED_MOD = 1.0;
    public static final double FLEE_SPEED_MOD = 2.2;
    public static final double ATTACK_SPEED_MOD = 1.4;
    private static final double BABY_JUMP_HEIGHT = 0.5;
    private static final double ADULT_JUMP_HEIGHT = 1.5;
    private static final int JUMP_DELAY_TICKS = 10;
    private static final int PANIC_JUMP_DELAY_TICKS = 3;
    private static final int JUMP_DURATION_IN_TICKS = 15;
    private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.INT);
    private static final int DEFAULT_MORE_CARROT_TICKS = 0;
    public final AnimationState hopAnimationState = new AnimationState();
    public final AnimationState idleHeadTiltAnimationState = new AnimationState();
    private static final int IDLE_MINIMAL_DURATION_TICKS = 180;
    private int idleAnimationTimeout = this.random.nextInt(40) + 180;
    private static final Identifier KILLER_BUNNY = Identifier.withDefaultNamespace("killer_bunny");
    private static final int DEFAULT_ATTACK_POWER = 3;
    private static final int EVIL_ATTACK_POWER_INCREMENT = 5;
    private static final Identifier EVIL_ATTACK_POWER_MODIFIER = Identifier.withDefaultNamespace("evil");
    private static final int EVIL_ARMOR_VALUE = 8;
    private static final int MORE_CARROTS_DELAY = 40;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int jumpDelayTicks;
    private int moreCarrotTicks = 0;

    public Rabbit(EntityType<? extends Rabbit> type, Level level) {
        super((EntityType<? extends Animal>)type, level);
        this.jumpControl = new RabbitJumpControl(this);
        this.moveControl = new RabbitMoveControl<Rabbit>(this);
        this.setSpeedModifier(0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(1, new RabbitPanicGoal(this, 2.2));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, i -> i.is(ItemTags.RABBIT_FOOD), false));
        this.goalSelector.addGoal(4, new RabbitAvoidEntityGoal<Player>(this, Player.class, 8.0f, 2.2, 2.2));
        this.goalSelector.addGoal(4, new RabbitAvoidEntityGoal<Wolf>(this, Wolf.class, 10.0f, 2.2, 2.2));
        this.goalSelector.addGoal(4, new RabbitAvoidEntityGoal<Monster>(this, Monster.class, 4.0f, 2.2, 2.2));
        this.goalSelector.addGoal(5, new RaidGardenGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0f));
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    protected float getJumpPower() {
        Path path;
        float baseJumpPower = 0.3f;
        if (this.moveControl.getSpeedModifier() <= 0.6) {
            baseJumpPower = 0.2f;
        }
        if ((path = this.navigation.getPath()) != null && !path.isDone()) {
            Vec3 currentPos = path.getNextEntityPos(this);
            if (currentPos.y > this.getY() + 0.5) {
                baseJumpPower = 0.5f;
            }
        }
        if (this.horizontalCollision || this.jumping && this.moveControl.getWantedY() > this.getY() + 0.5) {
            baseJumpPower = 0.5f;
        }
        return super.getJumpPower(baseJumpPower / 0.42f);
    }

    @Override
    public void jumpFromGround() {
        double current;
        super.jumpFromGround();
        double speedModifier = this.moveControl.getSpeedModifier();
        if (speedModifier > 0.0 && (current = this.getDeltaMovement().horizontalDistanceSqr()) < 0.01) {
            this.moveRelative(0.1f, new Vec3(0.0, this.isBaby() ? 0.5 : 1.5, 1.0));
        }
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte)1);
        }
    }

    public float getJumpCompletion(float a) {
        if (this.jumpDuration == 0) {
            return 0.0f;
        }
        return ((float)this.jumpTicks + a) / (float)this.jumpDuration;
    }

    public void setSpeedModifier(double speed) {
        this.getNavigation().setSpeedModifier(speed);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), speed);
    }

    @Override
    public void setJumping(boolean jump) {
        super.setJumping(jump);
        if (jump) {
            this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f) * 0.8f);
        }
    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 15;
        this.jumpTicks = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TYPE_ID, Variant.DEFAULT.id);
    }

    @Override
    public void customServerAiStep(ServerLevel level) {
        if (this.jumpDelayTicks > 0) {
            --this.jumpDelayTicks;
        }
        if (this.moreCarrotTicks > 0) {
            this.moreCarrotTicks -= this.random.nextInt(3);
            if (this.moreCarrotTicks < 0) {
                this.moreCarrotTicks = 0;
            }
        }
        if (this.onGround()) {
            RabbitJumpControl jumpControl;
            LivingEntity target;
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }
            if (this.getVariant() == Variant.EVIL && this.jumpDelayTicks == 0 && (target = this.getTarget()) != null && this.distanceToSqr(target) < 16.0) {
                this.facePoint(target.getX(), target.getZ());
                this.moveControl.setWantedPosition(target.getX(), target.getY(), target.getZ(), this.moveControl.getSpeedModifier());
                this.startJumping();
                this.wasOnGround = true;
            }
            if (!(jumpControl = (RabbitJumpControl)this.jumpControl).wantJump()) {
                if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
                    Path path = this.navigation.getPath();
                    Vec3 pos = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                    if (path != null && !path.isDone()) {
                        pos = path.getNextEntityPos(this);
                    }
                    this.facePoint(pos.x, pos.z);
                    this.startJumping();
                }
            } else if (!jumpControl.canJump()) {
                this.enableJumpControl();
            }
        }
        this.wasOnGround = this.onGround();
    }

    @Override
    public boolean canSpawnSprintParticle() {
        return false;
    }

    private void facePoint(double faceX, double faceZ) {
        this.setYRot((float)(Mth.atan2(faceZ - this.getZ(), faceX - this.getX()) * 57.2957763671875) - 90.0f);
    }

    private void enableJumpControl() {
        ((RabbitJumpControl)this.jumpControl).setCanJump(true);
    }

    private void disableJumpControl() {
        ((RabbitJumpControl)this.jumpControl).setCanJump(false);
    }

    private void setLandingDelay() {
        this.jumpDelayTicks = this.moveControl.getSpeedModifier() < 2.2 ? 10 : 3;
    }

    private void checkLandingDelay() {
        this.setLandingDelay();
        this.disableJumpControl();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.3f).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("RabbitType", Variant.LEGACY_CODEC, this.getVariant());
        output.putInt("MoreCarrotTicks", this.moreCarrotTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.read("RabbitType", Variant.LEGACY_CODEC).orElse(Variant.DEFAULT));
        this.moreCarrotTicks = input.getIntOr("MoreCarrotTicks", 0);
    }

    protected SoundEvent getJumpSound() {
        return SoundEvents.RABBIT_JUMP;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    public void playAttackSound() {
        if (this.getVariant() == Variant.EVIL) {
            this.playSound(SoundEvents.RABBIT_ATTACK, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
        }
    }

    @Override
    public SoundSource getSoundSource() {
        return this.getVariant() == Variant.EVIL ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
    }

    /*
     * Unable to fully structure code
     */
    @Override
    public @Nullable Rabbit getBreedOffspring(ServerLevel level, AgeableMob partner) {
        block2: {
            block3: {
                offspring = EntityTypes.RABBIT.create((Level)level, EntitySpawnReason.BREEDING);
                if (offspring == null) break block2;
                variant = Rabbit.getRandomRabbitVariant(level, this.blockPosition());
                if (this.random.nextInt(20) == 0) break block3;
                if (!(partner instanceof Rabbit)) ** GOTO lbl-1000
                rabbitPartner = (Rabbit)partner;
                if (this.random.nextBoolean()) {
                    variant = rabbitPartner.getVariant();
                } else lbl-1000:
                // 2 sources

                {
                    variant = this.getVariant();
                }
            }
            offspring.setVariant(variant);
        }
        return offspring;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.RABBIT_FOOD);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_TYPE_ID));
    }

    private void setVariant(Variant variant) {
        if (variant == Variant.EVIL) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
            this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.4, true));
            this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[0]));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true));
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Wolf>((Mob)this, Wolf.class, true));
            this.getAttribute(Attributes.ATTACK_DAMAGE).addOrUpdateTransientModifier(new AttributeModifier(EVIL_ATTACK_POWER_MODIFIER, 5.0, AttributeModifier.Operation.ADD_VALUE));
            if (!this.hasCustomName()) {
                this.setCustomName(Component.translatable(Util.makeDescriptionId("entity", KILLER_BUNNY)));
            }
        } else {
            this.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(EVIL_ATTACK_POWER_MODIFIER);
        }
        this.entityData.set(DATA_TYPE_ID, variant.id);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.RABBIT_VARIANT) {
            return Rabbit.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.RABBIT_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.RABBIT_VARIANT) {
            this.setVariant(Rabbit.castComponentValue(DataComponents.RABBIT_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        Variant variant = Rabbit.getRandomRabbitVariant(level, this.blockPosition());
        if (groupData instanceof RabbitGroupData) {
            variant = ((RabbitGroupData)groupData).variant;
        } else {
            groupData = new RabbitGroupData(variant);
        }
        this.setVariant(variant);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    private static Variant getRandomRabbitVariant(LevelAccessor level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        int randomVal = level.getRandom().nextInt(100);
        if (biome.is(BiomeTags.SPAWNS_WHITE_RABBITS)) {
            return randomVal < 80 ? Variant.WHITE : Variant.WHITE_SPLOTCHED;
        }
        if (biome.is(BiomeTags.SPAWNS_GOLD_RABBITS)) {
            return Variant.GOLD;
        }
        return randomVal < 50 ? Variant.BROWN : (randomVal < 90 ? Variant.SALT : Variant.BLACK);
    }

    public static boolean checkRabbitSpawnRules(EntityType<Rabbit> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.RABBITS_SPAWNABLE_ON) && Rabbit.isBrightEnoughToSpawn(level, pos);
    }

    private boolean wantsMoreFood() {
        return this.moreCarrotTicks <= 0;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 15;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    private void setupAnimationStates() {
        if (this.shouldPlayIdleAnimation()) {
            this.idleAnimationTimeout = this.random.nextInt(40) + 180;
            this.idleHeadTiltAnimationState.start(this.tickCount);
        } else if (this.jumpTicks > 0) {
            this.hopAnimationState.startIfStopped(this.tickCount);
            this.idleHeadTiltAnimationState.stop();
        } else {
            --this.idleAnimationTimeout;
            this.hopAnimationState.stop();
        }
    }

    private boolean shouldPlayIdleAnimation() {
        return this.idleAnimationTimeout <= 0 && (this.getLeashData() == null || this.getLeashData().leashHolder == null) && !this.isNoAi();
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        super.setLeashData(leashData);
        this.idleHeadTiltAnimationState.stop();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
    }

    public static class RabbitJumpControl
    extends JumpControl {
        private final Rabbit rabbit;
        private boolean canJump;

        public RabbitJumpControl(Rabbit rabbit) {
            super(rabbit);
            this.rabbit = rabbit;
        }

        public boolean wantJump() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean canJump) {
            this.canJump = canJump;
        }

        @Override
        public void tick() {
            if (this.jump) {
                this.rabbit.startJumping();
                this.jump = false;
            }
        }
    }

    private static class RabbitMoveControl<T extends Rabbit>
    extends MoveControl<T> {
        private double nextJumpSpeed;

        public RabbitMoveControl(T rabbit) {
            super(rabbit);
        }

        @Override
        public void tick() {
            if (((Rabbit)this.mob).onGround() && !((Rabbit)this.mob).jumping && !((RabbitJumpControl)((Rabbit)this.mob).jumpControl).wantJump()) {
                ((Rabbit)this.mob).setSpeedModifier(0.0);
            } else if (this.hasWanted() || this.operation == MoveControl.Operation.JUMPING) {
                ((Rabbit)this.mob).setSpeedModifier(this.nextJumpSpeed);
            }
            super.tick();
        }

        @Override
        public void setWantedPosition(double x, double y, double z, double speedModifier) {
            if (((Rabbit)this.mob).isInWater()) {
                speedModifier = 1.5;
            }
            super.setWantedPosition(x, y, z, speedModifier);
            if (speedModifier > 0.0) {
                this.nextJumpSpeed = speedModifier;
            }
        }
    }

    private static class RabbitPanicGoal
    extends PanicGoal {
        private final Rabbit rabbit;

        public RabbitPanicGoal(Rabbit rabbit, double speedModifier) {
            super(rabbit, speedModifier);
            this.rabbit = rabbit;
        }

        @Override
        public void tick() {
            super.tick();
            this.rabbit.setSpeedModifier(this.speedModifier);
        }
    }

    private static class RabbitAvoidEntityGoal<T extends LivingEntity>
    extends AvoidEntityGoal<T> {
        private final Rabbit rabbit;

        public RabbitAvoidEntityGoal(Rabbit rabbit, Class<T> avoidClass, float maxDist, double walkSpeedModifier, double sprintSpeedModifier) {
            super(rabbit, avoidClass, maxDist, walkSpeedModifier, sprintSpeedModifier);
            this.rabbit = rabbit;
        }

        @Override
        public boolean canUse() {
            return this.rabbit.getVariant() != Variant.EVIL && super.canUse();
        }
    }

    private static class RaidGardenGoal
    extends MoveToBlockGoal {
        private final Rabbit rabbit;
        private boolean wantsToRaid;
        private boolean canRaid;

        public RaidGardenGoal(Rabbit rabbit) {
            super(rabbit, 0.7f, 16);
            this.rabbit = rabbit;
        }

        @Override
        public boolean canUse() {
            if (this.nextStartTick <= 0) {
                if (!RaidGardenGoal.getServerLevel(this.rabbit).getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                    return false;
                }
                this.canRaid = false;
                this.wantsToRaid = this.rabbit.wantsMoreFood();
            }
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canRaid && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            this.rabbit.getLookControl().setLookAt((double)this.blockPos.getX() + 0.5, this.blockPos.getY() + 1, (double)this.blockPos.getZ() + 0.5, 10.0f, this.rabbit.getMaxHeadXRot());
            if (this.isReachedTarget()) {
                Level level = this.rabbit.level();
                BlockPos cropsPos = this.blockPos.above();
                BlockState blockState = level.getBlockState(cropsPos);
                Block block = blockState.getBlock();
                if (this.canRaid && block instanceof CarrotBlock) {
                    int carrotAge = blockState.getValue(CarrotBlock.AGE);
                    if (carrotAge == 0) {
                        level.setBlock(cropsPos, Blocks.AIR.defaultBlockState(), 2);
                        level.destroyBlock(cropsPos, true, this.rabbit);
                    } else {
                        level.setBlock(cropsPos, (BlockState)blockState.setValue(CarrotBlock.AGE, carrotAge - 1), 2);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, cropsPos, GameEvent.Context.of(this.rabbit));
                        level.levelEvent(2001, cropsPos, Block.getId(blockState));
                    }
                    this.rabbit.moreCarrotTicks = 40;
                }
                this.canRaid = false;
                this.nextStartTick = 10;
            }
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            CarrotBlock carrotBlock;
            Block block;
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.SUPPORTS_CROPS) && this.wantsToRaid && !this.canRaid && (block = (state = level.getBlockState(pos.above())).getBlock()) instanceof CarrotBlock && (carrotBlock = (CarrotBlock)block).isMaxAge(state)) {
                this.canRaid = true;
                return true;
            }
            return false;
        }
    }

    public static enum Variant implements StringRepresentable
    {
        BROWN(0, "brown"),
        WHITE(1, "white"),
        BLACK(2, "black"),
        WHITE_SPLOTCHED(3, "white_splotched"),
        GOLD(4, "gold"),
        SALT(5, "salt"),
        EVIL(99, "evil");

        public static final Variant DEFAULT;
        private static final IntFunction<Variant> BY_ID;
        public static final Codec<Variant> CODEC;
        @Deprecated
        public static final Codec<Variant> LEGACY_CODEC;
        public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC;
        private final int id;
        private final String name;

        private Variant(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public int id() {
            return this.id;
        }

        public static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        static {
            DEFAULT = BROWN;
            BY_ID = ByIdMap.sparse(Variant::id, Variant.values(), DEFAULT);
            CODEC = StringRepresentable.fromEnum(Variant::values);
            LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, Variant::id);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::id);
        }
    }

    public static class RabbitGroupData
    extends AgeableMob.AgeableMobGroupData {
        public final Variant variant;

        public RabbitGroupData(Variant variant) {
            super(1.0f);
            this.variant = variant;
        }
    }
}

