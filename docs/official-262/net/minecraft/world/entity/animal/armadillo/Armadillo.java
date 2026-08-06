/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.armadillo;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.armadillo.ArmadilloAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class Armadillo
extends Animal {
    public static final float BABY_SCALE = 0.6f;
    public static final float MAX_HEAD_ROTATION_EXTENT = 32.5f;
    public static final int SCARE_CHECK_INTERVAL = 80;
    private static final double SCARE_DISTANCE_HORIZONTAL = 7.0;
    private static final double SCARE_DISTANCE_VERTICAL = 2.0;
    private static final EntityDimensions BABY_DIMENSIONS = EntityTypes.ARMADILLO.getDimensions().scale(0.6f).withEyeHeight(0.21875f);
    private static final Brain.Provider<Armadillo> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.FOOD_TEMPTATIONS, SensorType.NEAREST_ADULT, SensorType.ARMADILLO_SCARE_DETECTED), armadillo -> ArmadilloAi.getActivities());
    private static final EntityDataAccessor<ArmadilloState> ARMADILLO_STATE = SynchedEntityData.defineId(Armadillo.class, EntityDataSerializers.ARMADILLO_STATE);
    private long inStateTicks = 0L;
    public final AnimationState rollOutAnimationState = new AnimationState();
    public final AnimationState rollUpAnimationState = new AnimationState();
    public final AnimationState peekAnimationState = new AnimationState();
    private int scuteTime;
    private boolean peekReceivedClient = false;

    public Armadillo(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.getNavigation().setCanFloat(true);
        this.scuteTime = this.pickNextScuteDropTime();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.ARMADILLO.create((Level)level, EntitySpawnReason.BREEDING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.14);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(ARMADILLO_STATE, ArmadilloState.IDLE);
    }

    public boolean isScared() {
        return this.entityData.get(ARMADILLO_STATE) != ArmadilloState.IDLE;
    }

    public boolean shouldHideInShell() {
        return this.getState().shouldHideInShell(this.inStateTicks);
    }

    public boolean shouldSwitchToScaredState() {
        return this.getState() == ArmadilloState.ROLLING && this.inStateTicks > (long)ArmadilloState.ROLLING.animationDuration();
    }

    public ArmadilloState getState() {
        return this.entityData.get(ARMADILLO_STATE);
    }

    public void switchToState(ArmadilloState state) {
        this.entityData.set(ARMADILLO_STATE, state);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (ARMADILLO_STATE.equals(accessor)) {
            this.inStateTicks = 0L;
        }
        super.onSyncedDataUpdated(accessor);
    }

    protected Brain<Armadillo> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<Armadillo> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("armadilloBrain");
        this.getBrain().tick(level, this);
        profiler.pop();
        profiler.push("armadilloActivityUpdate");
        ArmadilloAi.updateActivity(this);
        profiler.pop();
        if (this.isAlive() && --this.scuteTime <= 0 && this.shouldDropLoot(level)) {
            if (this.dropFromGiftLootTable(level, BuiltInLootTables.ARMADILLO_SHED, this::spawnAtLocation)) {
                this.playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f);
                this.gameEvent(GameEvent.ENTITY_PLACE);
            }
            this.scuteTime = this.pickNextScuteDropTime();
        }
        super.customServerAiStep(level);
    }

    private int pickNextScuteDropTime() {
        return this.random.nextInt(20 * TimeUtil.SECONDS_PER_MINUTE * 5) + 20 * TimeUtil.SECONDS_PER_MINUTE * 5;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        }
        if (this.isScared()) {
            this.clampHeadRotationToBody();
        }
        ++this.inStateTicks;
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.6f : 1.0f;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    private void setupAnimationStates() {
        switch (this.getState().ordinal()) {
            case 0: {
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.stop();
                this.peekAnimationState.stop();
                break;
            }
            case 3: {
                this.rollOutAnimationState.startIfStopped(this.tickCount);
                this.rollUpAnimationState.stop();
                this.peekAnimationState.stop();
                break;
            }
            case 1: {
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.startIfStopped(this.tickCount);
                this.peekAnimationState.stop();
                break;
            }
            case 2: {
                this.rollOutAnimationState.stop();
                this.rollUpAnimationState.stop();
                if (this.peekReceivedClient) {
                    this.peekAnimationState.stop();
                    this.peekReceivedClient = false;
                }
                if (this.inStateTicks == 0L) {
                    this.peekAnimationState.start(this.tickCount);
                    this.peekAnimationState.fastForward(ArmadilloState.SCARED.animationDuration(), 1.0f);
                    break;
                }
                this.peekAnimationState.startIfStopped(this.tickCount);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 64 && this.level().isClientSide()) {
            this.peekReceivedClient = true;
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ARMADILLO_PEEK, this.getSoundSource(), 1.0f, 1.0f, false);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.ARMADILLO_FOOD);
    }

    public static boolean checkArmadilloSpawnRules(EntityType<Armadillo> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.ARMADILLO_SPAWNABLE_ON) && Armadillo.isBrightEnoughToSpawn(level, pos);
    }

    public boolean isScaredBy(LivingEntity livingEntity) {
        if (!this.getBoundingBox().inflate(7.0, 2.0, 7.0).intersects(livingEntity.getBoundingBox())) {
            return false;
        }
        if (livingEntity.is(EntityTypeTags.UNDEAD)) {
            return true;
        }
        if (this.getLastHurtByMob() == livingEntity) {
            return true;
        }
        if (livingEntity instanceof Player) {
            Player player = (Player)livingEntity;
            if (player.isSpectator()) {
                return false;
            }
            return player.isSprinting() || player.isPassenger();
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("state", ArmadilloState.CODEC, this.getState());
        output.putInt("scute_time", this.scuteTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.switchToState(input.read("state", ArmadilloState.CODEC).orElse(ArmadilloState.IDLE));
        input.getInt("scute_time").ifPresent(time -> {
            this.scuteTime = time;
        });
    }

    public void rollUp() {
        if (this.isScared()) {
            return;
        }
        this.stopInPlace();
        this.resetLove();
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.ARMADILLO_ROLL);
        this.switchToState(ArmadilloState.ROLLING);
    }

    public void rollOut() {
        if (!this.isScared()) {
            return;
        }
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.ARMADILLO_UNROLL_FINISH);
        this.switchToState(ArmadilloState.IDLE);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isScared()) {
            damage = (damage - 1.0f) / 2.0f;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float dmg) {
        super.actuallyHurt(level, source, dmg);
        if (this.isNoAi() || this.isDeadOrDying()) {
            return;
        }
        if (source.getEntity() instanceof LivingEntity) {
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.DANGER_DETECTED_RECENTLY, true, 80L);
            if (this.canStayRolledUp()) {
                this.rollUp();
            }
        } else if (source.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)) {
            this.rollOut();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.BRUSH) && this.brushOffScute(player, itemStack)) {
            itemStack.hurtAndBreak(16, (LivingEntity)player, hand.asEquipmentSlot());
            return InteractionResult.SUCCESS;
        }
        if (this.isScared()) {
            return InteractionResult.FAIL;
        }
        return super.mobInteract(player, hand);
    }

    public boolean brushOffScute(@Nullable Entity interactingEntity, ItemStack tool) {
        if (this.isBaby()) {
            return false;
        }
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            this.dropFromEntityInteractLootTable(level2, BuiltInLootTables.ARMADILLO_BRUSH, interactingEntity, tool, this::spawnAtLocation);
            this.playSound(SoundEvents.ARMADILLO_BRUSH);
            this.gameEvent(GameEvent.ENTITY_INTERACT);
        }
        return true;
    }

    public boolean canStayRolledUp() {
        return !this.isPanicking() && !this.isInLiquid() && !this.isLeashed() && !this.isPassenger() && !this.isVehicle();
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !this.isScared();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isScared()) {
            return null;
        }
        return SoundEvents.ARMADILLO_AMBIENT;
    }

    @Override
    protected void playEatingSound() {
        this.makeSound(SoundEvents.ARMADILLO_EAT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMADILLO_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (this.isScared()) {
            return SoundEvents.ARMADILLO_HURT_REDUCED;
        }
        return SoundEvents.ARMADILLO_HURT;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.ARMADILLO_STEP, 0.15f, 1.0f);
    }

    @Override
    public int getMaxHeadYRot() {
        if (this.isScared()) {
            return 0;
        }
        return 32;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this, this){
            final /* synthetic */ Armadillo this$0;
            {
                Armadillo armadillo = this$0;
                Objects.requireNonNull(armadillo);
                this.this$0 = armadillo;
                super(mob);
            }

            @Override
            public void clientTick() {
                if (!this.this$0.isScared()) {
                    super.clientTick();
                }
            }
        };
    }

    public static enum ArmadilloState implements StringRepresentable
    {
        IDLE("idle", false, 0, 0){

            @Override
            public boolean shouldHideInShell(long ticksInState) {
                return false;
            }
        }
        ,
        ROLLING("rolling", true, 10, 1){

            @Override
            public boolean shouldHideInShell(long ticksInState) {
                return ticksInState > 5L;
            }
        }
        ,
        SCARED("scared", true, 50, 2){

            @Override
            public boolean shouldHideInShell(long ticksInState) {
                return true;
            }
        }
        ,
        UNROLLING("unrolling", true, 30, 3){

            @Override
            public boolean shouldHideInShell(long ticksInState) {
                return ticksInState < 26L;
            }
        };

        private static final Codec<ArmadilloState> CODEC;
        private static final IntFunction<ArmadilloState> BY_ID;
        public static final StreamCodec<ByteBuf, ArmadilloState> STREAM_CODEC;
        private final String name;
        private final boolean isThreatened;
        private final int animationDuration;
        private final int id;

        private ArmadilloState(String name, boolean isThreatened, int animationDuration, int id) {
            this.name = name;
            this.isThreatened = isThreatened;
            this.animationDuration = animationDuration;
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        private int id() {
            return this.id;
        }

        public abstract boolean shouldHideInShell(long var1);

        public boolean isThreatened() {
            return this.isThreatened;
        }

        public int animationDuration() {
            return this.animationDuration;
        }

        static {
            CODEC = StringRepresentable.fromEnum(ArmadilloState::values);
            BY_ID = ByIdMap.continuous(ArmadilloState::id, ArmadilloState.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ArmadilloState::id);
        }
    }
}

