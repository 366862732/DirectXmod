/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.entity.animal.sniffer;

import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.SnifferAi;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;

public class Sniffer
extends Animal {
    private static final Brain.Provider<Sniffer> BRAIN_PROVIDER = Brain.provider(List.of(MemoryModuleType.SNIFFER_EXPLORED_POSITIONS), List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.NEAREST_PLAYERS, SensorType.FOOD_TEMPTATIONS), sniffer -> SnifferAi.getActivities());
    private static final int DIGGING_PARTICLES_DELAY_TICKS = 1700;
    private static final int DIGGING_PARTICLES_DURATION_TICKS = 6000;
    private static final int DIGGING_PARTICLES_AMOUNT = 30;
    private static final int DIGGING_DROP_SEED_OFFSET_TICKS = 120;
    private static final int SNIFFER_BABY_START_AGE = -48000;
    private static final float DIGGING_BB_HEIGHT_OFFSET = 0.4f;
    private static final EntityDimensions DIGGING_DIMENSIONS = EntityDimensions.scalable(EntityTypes.SNIFFER.getWidth(), EntityTypes.SNIFFER.getHeight() - 0.4f).withEyeHeight(0.81f);
    private static final EntityDataAccessor<State> DATA_STATE = SynchedEntityData.defineId(Sniffer.class, EntityDataSerializers.SNIFFER_STATE);
    private static final EntityDataAccessor<Integer> DATA_DROP_SEED_AT_TICK = SynchedEntityData.defineId(Sniffer.class, EntityDataSerializers.INT);
    public final AnimationState feelingHappyAnimationState = new AnimationState();
    public final AnimationState scentingAnimationState = new AnimationState();
    public final AnimationState sniffingAnimationState = new AnimationState();
    public final AnimationState diggingAnimationState = new AnimationState();
    public final AnimationState risingAnimationState = new AnimationState();

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.1f).add(Attributes.MAX_HEALTH, 14.0);
    }

    public Sniffer(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.getNavigation().setCanFloat(true);
        this.setPathfindingMalus(PathType.WATER, -1.0f);
        this.setPathfindingMalus(PathType.ON_TOP_OF_POWDER_SNOW, -1.0f);
        this.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, -1.0f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_STATE, State.IDLING);
        entityData.define(DATA_DROP_SEED_AT_TICK, 0);
    }

    @Override
    public void onPathfindingStart() {
        super.onPathfindingStart();
        if (this.isOnFire() || this.isInWater()) {
            this.setPathfindingMalus(PathType.WATER, 0.0f);
        }
    }

    @Override
    public void onPathfindingDone() {
        this.setPathfindingMalus(PathType.WATER, -1.0f);
    }

    @Override
    public int getBabyStartAge() {
        return -48000;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.getState() == State.DIGGING) {
            return DIGGING_DIMENSIONS.scale(this.getAgeScale());
        }
        return super.getDefaultDimensions(pose);
    }

    public boolean isSearching() {
        return this.getState() == State.SEARCHING;
    }

    public boolean isTempted() {
        return this.brain.getMemory(MemoryModuleType.IS_TEMPTED).orElse(false);
    }

    public boolean canSniff() {
        return !this.isTempted() && !this.isPanicking() && !this.isInWater() && !this.isInLove() && this.onGround() && !this.isPassenger() && !this.isLeashed();
    }

    public boolean canPlayDiggingSound() {
        return this.getState() == State.DIGGING || this.getState() == State.SEARCHING;
    }

    private BlockPos getHeadBlock() {
        Vec3 position = this.getHeadPosition();
        return BlockPos.containing(position.x(), this.getY() + (double)0.2f, position.z());
    }

    private Vec3 getHeadPosition() {
        return this.position().add(this.getForward().scale(2.25));
    }

    @Override
    public boolean supportQuadLeash() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets(this, -0.01, 0.63, 0.38, 1.15);
    }

    private State getState() {
        return this.entityData.get(DATA_STATE);
    }

    private Sniffer setState(State state) {
        this.entityData.set(DATA_STATE, state);
        return this;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_STATE.equals(accessor)) {
            State state = this.getState();
            this.resetAnimations();
            switch (state.ordinal()) {
                case 2: {
                    this.scentingAnimationState.startIfStopped(this.tickCount);
                    break;
                }
                case 3: {
                    this.sniffingAnimationState.startIfStopped(this.tickCount);
                    break;
                }
                case 5: {
                    this.diggingAnimationState.startIfStopped(this.tickCount);
                    break;
                }
                case 6: {
                    this.risingAnimationState.startIfStopped(this.tickCount);
                    break;
                }
                case 1: {
                    this.feelingHappyAnimationState.startIfStopped(this.tickCount);
                }
            }
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    private void resetAnimations() {
        this.diggingAnimationState.stop();
        this.sniffingAnimationState.stop();
        this.risingAnimationState.stop();
        this.feelingHappyAnimationState.stop();
        this.scentingAnimationState.stop();
    }

    public Sniffer transitionTo(State state) {
        switch (state.ordinal()) {
            case 0: {
                this.setState(State.IDLING);
                break;
            }
            case 2: {
                this.setState(State.SCENTING).onScentingStart();
                break;
            }
            case 3: {
                this.playSound(SoundEvents.SNIFFER_SNIFFING, 1.0f, 1.0f);
                this.setState(State.SNIFFING);
                break;
            }
            case 4: {
                this.setState(State.SEARCHING);
                break;
            }
            case 5: {
                this.setState(State.DIGGING).onDiggingStart();
                break;
            }
            case 6: {
                this.playSound(SoundEvents.SNIFFER_DIGGING_STOP, 1.0f, 1.0f);
                this.setState(State.RISING);
                break;
            }
            case 1: {
                this.playSound(SoundEvents.SNIFFER_HAPPY, 1.0f, 1.0f);
                this.setState(State.FEELING_HAPPY);
            }
        }
        return this;
    }

    private Sniffer onScentingStart() {
        this.playSound(SoundEvents.SNIFFER_SCENTING, 1.0f, this.isBaby() ? 1.3f : 1.0f);
        return this;
    }

    private Sniffer onDiggingStart() {
        this.entityData.set(DATA_DROP_SEED_AT_TICK, this.tickCount + 120);
        this.level().broadcastEntityEvent(this, (byte)63);
        return this;
    }

    public Sniffer onDiggingComplete(boolean success) {
        if (success) {
            this.storeExploredPosition(this.getOnPos());
        }
        return this;
    }

    Optional<BlockPos> calculateDigPosition() {
        return IntStream.range(0, 5).mapToObj(idx -> LandRandomPos.getPos(this, 10 + 2 * idx, 3)).filter(Objects::nonNull).map(BlockPos::containing).filter(position -> this.level().getWorldBorder().isWithinBounds((BlockPos)position)).map(BlockPos::below).filter(this::canDig).findFirst();
    }

    boolean canDig() {
        return !this.isPanicking() && !this.isTempted() && !this.isBaby() && !this.isInWater() && this.onGround() && !this.isPassenger() && this.canDig(this.getHeadBlock().below());
    }

    private boolean canDig(BlockPos position) {
        return this.level().getBlockState(position).is(BlockTags.SNIFFER_DIGGABLE_BLOCK) && this.getExploredPositions().noneMatch(explored -> GlobalPos.of(this.level().dimension(), position).equals(explored)) && Optional.ofNullable(this.getNavigation().createPath(position, 1)).map(Path::canReach).orElse(false) != false;
    }

    private void dropSeed() {
        ServerLevel level;
        block3: {
            block2: {
                Level level2 = this.level();
                if (!(level2 instanceof ServerLevel)) break block2;
                level = (ServerLevel)level2;
                if (this.entityData.get(DATA_DROP_SEED_AT_TICK) == this.tickCount) break block3;
            }
            return;
        }
        BlockPos head = this.getHeadBlock();
        this.dropFromGiftLootTable(level, BuiltInLootTables.SNIFFER_DIGGING, (l, itemStack) -> {
            ItemEntity entity = new ItemEntity(this.level(), head.getX(), head.getY(), head.getZ(), (ItemStack)itemStack);
            entity.setDefaultPickUpDelay();
            l.addFreshEntity(entity);
        });
        this.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0f, 1.0f);
    }

    private Sniffer emitDiggingParticles(AnimationState state) {
        boolean emit;
        boolean bl = emit = state.getTimeInMillis(this.tickCount) > 1700L && state.getTimeInMillis(this.tickCount) < 6000L;
        if (emit) {
            BlockPos head = this.getHeadBlock();
            BlockState stateBelow = this.level().getBlockState(head.below());
            if (stateBelow.getRenderShape() != RenderShape.INVISIBLE) {
                for (int i = 0; i < 30; ++i) {
                    Vec3 centered = Vec3.atCenterOf(head).add(0.0, -0.65f, 0.0);
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, stateBelow), centered.x, centered.y, centered.z, 0.0, 0.0, 0.0);
                }
                if (this.tickCount % 10 == 0) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), stateBelow.getSoundType().getHitSound(), this.getSoundSource(), 0.5f, 0.5f, false);
                }
            }
        }
        if (this.tickCount % 10 == 0) {
            this.level().gameEvent(GameEvent.ENTITY_ACTION, this.getHeadBlock(), GameEvent.Context.of(this));
        }
        return this;
    }

    private Sniffer storeExploredPosition(BlockPos position) {
        List updated = this.getExploredPositions().limit(20L).collect(Collectors.toList());
        updated.add(0, GlobalPos.of(this.level().dimension(), position));
        this.getBrain().setMemory(MemoryModuleType.SNIFFER_EXPLORED_POSITIONS, updated);
        return this;
    }

    private Stream<GlobalPos> getExploredPositions() {
        return this.getBrain().getMemory(MemoryModuleType.SNIFFER_EXPLORED_POSITIONS).stream().flatMap(Collection::stream);
    }

    @Override
    public void jumpFromGround() {
        double current;
        super.jumpFromGround();
        double speedModifier = this.moveControl.getSpeedModifier();
        if (speedModifier > 0.0 && (current = this.getDeltaMovement().horizontalDistanceSqr()) < 0.01) {
            this.moveRelative(0.1f, new Vec3(0.0, 0.0, 1.0));
        }
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        ItemStack itemStack = new ItemStack(Items.SNIFFER_EGG);
        ItemEntity entity = new ItemEntity(level, this.position().x(), this.position().y(), this.position().z(), itemStack);
        entity.setDefaultPickUpDelay();
        this.finalizeSpawnChildFromBreeding(level, partner, null);
        this.playSound(SoundEvents.SNIFFER_EGG_PLOP, 1.0f, (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 0.5f);
        level.addFreshEntity(entity);
    }

    @Override
    public void die(DamageSource source) {
        this.transitionTo(State.IDLING);
        super.die(source);
    }

    @Override
    public void tick() {
        switch (this.getState().ordinal()) {
            case 5: {
                this.emitDiggingParticles(this.diggingAnimationState).dropSeed();
                break;
            }
            case 4: {
                this.playSearchingSound();
            }
        }
        super.tick();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        boolean isFood = this.isFood(heldItem);
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (interactionResult.consumesAction() && isFood) {
            this.playEatingSound();
        }
        return interactionResult;
    }

    @Override
    protected void playEatingSound() {
        this.level().playSound(null, this, SoundEvents.SNIFFER_EAT, SoundSource.NEUTRAL, 1.0f, Mth.randomBetween(this.level().getRandom(), 0.8f, 1.2f));
    }

    private void playSearchingSound() {
        if (this.level().isClientSide() && this.tickCount % 20 == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SNIFFER_SEARCHING, this.getSoundSource(), 1.0f, 1.0f, false);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.SNIFFER_STEP, 0.15f, 1.0f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Set.of(State.DIGGING, State.SEARCHING).contains((Object)this.getState()) ? null : SoundEvents.SNIFFER_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SNIFFER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SNIFFER_DEATH;
    }

    @Override
    public int getMaxHeadYRot() {
        return 50;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.SNIFFER.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean canMate(Animal partner) {
        if (partner instanceof Sniffer) {
            Sniffer snifferPartner = (Sniffer)partner;
            Set<State> states = Set.of(State.IDLING, State.SCENTING, State.FEELING_HAPPY);
            return states.contains((Object)this.getState()) && states.contains((Object)snifferPartner.getState()) && super.canMate(partner);
        }
        return false;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.SNIFFER_FOOD);
    }

    protected Brain<Sniffer> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    public Brain<Sniffer> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("snifferBrain");
        this.getBrain().tick(level, this);
        profiler.popPush("snifferActivityUpdate");
        SnifferAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    public static enum State {
        IDLING(0),
        FEELING_HAPPY(1),
        SCENTING(2),
        SNIFFING(3),
        SEARCHING(4),
        DIGGING(5),
        RISING(6);

        public static final IntFunction<State> BY_ID;
        public static final StreamCodec<ByteBuf, State> STREAM_CODEC;
        private final int id;

        private State(int id) {
            this.id = id;
        }

        public int id() {
            return this.id;
        }

        static {
            BY_ID = ByIdMap.continuous(State::id, State.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, State::id);
        }
    }
}

