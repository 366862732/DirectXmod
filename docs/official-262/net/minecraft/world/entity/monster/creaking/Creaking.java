/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.creaking;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.CreakingAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Creaking
extends Monster {
    private static final EntityDataAccessor<Boolean> CAN_MOVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ACTIVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_TEARING_DOWN = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final Brain.Provider<Creaking> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS), CreakingAi::getActivities);
    private static final int ATTACK_ANIMATION_DURATION = 15;
    private static final int MAX_HEALTH = 1;
    private static final float ATTACK_DAMAGE = 3.0f;
    private static final float FOLLOW_RANGE = 32.0f;
    private static final float ACTIVATION_RANGE_SQ = 144.0f;
    public static final int ATTACK_INTERVAL = 40;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.4f;
    public static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.3f;
    public static final int CREAKING_ORANGE = 16545810;
    public static final int CREAKING_GRAY = 0x5F5F5F;
    public static final int INVULNERABILITY_ANIMATION_DURATION = 8;
    public static final int TWITCH_DEATH_DURATION = 45;
    private static final int MAX_PLAYER_STUCK_COUNTER = 4;
    private int attackAnimationRemainingTicks;
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    private int invulnerabilityAnimationRemainingTicks;
    private boolean eyesGlowing;
    private int nextFlickerTime;
    private int playerStuckCounter;

    public Creaking(EntityType<? extends Creaking> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
        this.lookControl = new CreakingLookControl(this, this);
        this.moveControl = new CreakingMoveControl<Creaking>(this);
        this.jumpControl = new CreakingJumpControl(this, this);
        GroundPathNavigation navigation = (GroundPathNavigation)this.getNavigation();
        navigation.setCanFloat(true);
        this.xpReward = 0;
    }

    public void setTransient(BlockPos pos) {
        this.setHomePos(pos);
        this.setPathfindingMalus(PathType.DAMAGING, 8.0f);
        this.setPathfindingMalus(PathType.POWDER_SNOW, 8.0f);
        this.setPathfindingMalus(PathType.LAVA, 8.0f);
        this.setPathfindingMalus(PathType.FIRE, 0.0f);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0f);
    }

    public boolean isHeartBound() {
        return this.getHomePos() != null;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new CreakingBodyRotationControl(this, this);
    }

    protected Brain<Creaking> makeBrain(Brain.Packed packedBrain) {
        return BRAIN_PROVIDER.makeBrain(this, packedBrain);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(CAN_MOVE, true);
        entityData.define(IS_ACTIVE, false);
        entityData.define(IS_TEARING_DOWN, false);
        entityData.define(HOME_POS, Optional.empty());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.4f).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0625);
    }

    public boolean canMove() {
        return this.entityData.get(CAN_MOVE);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }
        this.attackAnimationRemainingTicks = 15;
        this.level().broadcastEntityEvent(this, (byte)4);
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        CreakingHeartBlockEntity creakingHeartBlockEntity;
        BlockPos homePos = this.getHomePos();
        if (homePos == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        if (this.isInvulnerableTo(level, source) || this.invulnerabilityAnimationRemainingTicks > 0 || this.isDeadOrDying()) {
            return false;
        }
        Player responsiblePlayer = this.blameSourceForDamage(source);
        Entity directEntity = source.getDirectEntity();
        if (!(directEntity instanceof LivingEntity) && !(directEntity instanceof Projectile) && responsiblePlayer == null) {
            return false;
        }
        this.invulnerabilityAnimationRemainingTicks = 8;
        this.level().broadcastEntityEvent(this, (byte)66);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        BlockEntity blockEntity = this.level().getBlockEntity(homePos);
        if (blockEntity instanceof CreakingHeartBlockEntity && (creakingHeartBlockEntity = (CreakingHeartBlockEntity)blockEntity).isProtector(this)) {
            if (responsiblePlayer != null) {
                creakingHeartBlockEntity.creakingHurt();
            }
            this.playHurtSound(source);
        }
        return true;
    }

    public Player blameSourceForDamage(DamageSource source) {
        this.resolveMobResponsibleForDamage(source);
        return this.resolvePlayerResponsibleForDamage(source);
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.canMove();
    }

    @Override
    public void push(double xa, double ya, double za) {
        if (!this.canMove()) {
            return;
        }
        super.push(xa, ya, za);
    }

    public Brain<Creaking> getBrain() {
        return super.getBrain();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("creakingBrain");
        this.getBrain().tick((ServerLevel)this.level(), this);
        profiler.pop();
        CreakingAi.updateActivity(this);
    }

    @Override
    public void aiStep() {
        if (this.invulnerabilityAnimationRemainingTicks > 0) {
            --this.invulnerabilityAnimationRemainingTicks;
        }
        if (this.attackAnimationRemainingTicks > 0) {
            --this.attackAnimationRemainingTicks;
        }
        if (!this.level().isClientSide()) {
            boolean canMove = this.entityData.get(CAN_MOVE);
            boolean nowCanMove = this.checkCanMove();
            if (nowCanMove != canMove) {
                this.gameEvent(GameEvent.ENTITY_ACTION);
                if (nowCanMove) {
                    this.makeSound(SoundEvents.CREAKING_UNFREEZE);
                } else {
                    this.stopInPlace();
                    this.makeSound(SoundEvents.CREAKING_FREEZE);
                }
            }
            this.entityData.set(CAN_MOVE, nowCanMove);
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        BlockPos homePos;
        if (!this.level().isClientSide() && (homePos = this.getHomePos()) != null) {
            CreakingHeartBlockEntity creakingHeartBlockEntity;
            boolean hasProtectionFromCreakingHeart;
            BlockEntity blockEntity = this.level().getBlockEntity(homePos);
            boolean bl = hasProtectionFromCreakingHeart = blockEntity instanceof CreakingHeartBlockEntity && (creakingHeartBlockEntity = (CreakingHeartBlockEntity)blockEntity).isProtector(this);
            if (!hasProtectionFromCreakingHeart) {
                this.setHealth(0.0f);
            }
        }
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
            this.checkEyeBlink();
        }
    }

    @Override
    protected void tickDeath() {
        if (this.isHeartBound() && this.isTearingDown()) {
            ++this.deathTime;
            if (!this.level().isClientSide() && this.deathTime > 45 && !this.isRemoved()) {
                this.tearDown();
            }
        } else {
            super.tickDeath();
        }
    }

    @Override
    protected void updateWalkAnimation(float distance) {
        float targetSpeed = Math.min(distance * 25.0f, 3.0f);
        this.walkAnimation.update(targetSpeed, 0.4f, 1.0f);
    }

    private void setupAnimationStates() {
        this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
        this.invulnerabilityAnimationState.animateWhen(this.invulnerabilityAnimationRemainingTicks > 0, this.tickCount);
        this.deathAnimationState.animateWhen(this.isTearingDown(), this.tickCount);
    }

    public void tearDown() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            AABB box = this.getBoundingBox();
            Vec3 center = box.getCenter();
            double xSpread = box.getXsize() * 0.3;
            double ySpread = box.getYsize() * 0.3;
            double zSpread = box.getZsize() * 0.3;
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.PALE_OAK_WOOD.defaultBlockState()), center.x, center.y, center.z, 100, xSpread, ySpread, zSpread, 0.0);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, (BlockState)Blocks.CREAKING_HEART.defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE)), center.x, center.y, center.z, 10, xSpread, ySpread, zSpread, 0.0);
        }
        this.makeSound(this.getDeathSound());
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    public void creakingDeathEffects(DamageSource source) {
        this.blameSourceForDamage(source);
        this.die(source);
        this.makeSound(SoundEvents.CREAKING_TWITCH);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 66) {
            this.invulnerabilityAnimationRemainingTicks = 8;
            this.playHurtSound(this.damageSources().generic());
        } else if (id == 4) {
            this.attackAnimationRemainingTicks = 15;
            this.playAttackSound();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean fireImmune() {
        return this.isHeartBound() || super.fireImmune();
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return !this.isHeartBound() && super.canUsePortal(ignorePassenger);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new CreakingPathNavigation(this, this, level);
    }

    public boolean playerIsStuckInYou() {
        List players = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        if (players.isEmpty()) {
            this.playerStuckCounter = 0;
            return false;
        }
        AABB ownBox = this.getBoundingBox();
        for (Player player : players) {
            if (!ownBox.contains(player.getEyePosition())) continue;
            ++this.playerStuckCounter;
            return this.playerStuckCounter > 4;
        }
        this.playerStuckCounter = 0;
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("home_pos", BlockPos.CODEC).ifPresent(this::setTransient);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("home_pos", BlockPos.CODEC, this.getHomePos());
    }

    public void setHomePos(BlockPos pos) {
        this.entityData.set(HOME_POS, Optional.of(pos));
    }

    public @Nullable BlockPos getHomePos() {
        return this.entityData.get(HOME_POS).orElse(null);
    }

    public void setTearingDown() {
        this.entityData.set(IS_TEARING_DOWN, true);
    }

    public boolean isTearingDown() {
        return this.entityData.get(IS_TEARING_DOWN);
    }

    public boolean hasGlowingEyes() {
        return this.eyesGlowing;
    }

    public void checkEyeBlink() {
        if (this.deathTime > this.nextFlickerTime) {
            this.nextFlickerTime = this.deathTime + this.getRandom().nextIntBetweenInclusive(this.eyesGlowing ? 2 : this.deathTime / 4, this.eyesGlowing ? 8 : this.deathTime / 2);
            this.eyesGlowing = !this.eyesGlowing;
        }
    }

    @Override
    public void playAttackSound() {
        this.makeSound(SoundEvents.CREAKING_ATTACK);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isActive()) {
            return null;
        }
        return SoundEvents.CREAKING_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isHeartBound() ? SoundEvents.CREAKING_SWAY : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREAKING_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.CREAKING_STEP, 0.15f, 1.0f);
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    @Override
    public void knockback(double power, double xd, double zd, DamageSource source, float damage) {
        if (!this.canMove()) {
            return;
        }
        super.knockback(power, xd, zd, source, damage);
    }

    public boolean checkCanMove() {
        List players = this.brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());
        boolean active = this.isActive();
        if (players.isEmpty()) {
            if (active) {
                this.deactivate();
            }
            return true;
        }
        boolean hasPotentialTarget = false;
        for (Player player : players) {
            if (!this.canAttack(player) || this.isAlliedTo(player)) continue;
            hasPotentialTarget = true;
            if (active && !LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(player) || !this.isLookingAtMe(player, 0.5, false, true, this.getEyeY(), this.getY() + 0.5 * (double)this.getScale(), (this.getEyeY() + this.getY()) / 2.0)) continue;
            if (active) {
                return false;
            }
            if (!(player.distanceToSqr(this) < 144.0)) continue;
            this.activate(player);
            return false;
        }
        if (!hasPotentialTarget && active) {
            this.deactivate();
        }
        return true;
    }

    public void activate(Player player) {
        this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.CREAKING_ACTIVATE);
        this.setIsActive(true);
    }

    public void deactivate() {
        this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        this.gameEvent(GameEvent.ENTITY_ACTION);
        this.makeSound(SoundEvents.CREAKING_DEACTIVATE);
        this.setIsActive(false);
    }

    public void setIsActive(boolean active) {
        this.entityData.set(IS_ACTIVE, active);
    }

    public boolean isActive() {
        return this.entityData.get(IS_ACTIVE);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0f;
    }

    private class CreakingLookControl
    extends LookControl {
        final /* synthetic */ Creaking this$0;

        public CreakingLookControl(Creaking creaking, Creaking creaking2) {
            Creaking creaking3 = creaking;
            Objects.requireNonNull(creaking3);
            this.this$0 = creaking3;
            super(creaking2);
        }

        @Override
        public void tick() {
            if (this.this$0.canMove()) {
                super.tick();
            }
        }
    }

    private static class CreakingMoveControl<T extends Creaking>
    extends MoveControl<T> {
        public CreakingMoveControl(T creaking) {
            super(creaking);
        }

        @Override
        public void tick() {
            if (((Creaking)this.mob).canMove()) {
                super.tick();
            }
        }
    }

    private class CreakingJumpControl
    extends JumpControl {
        final /* synthetic */ Creaking this$0;

        public CreakingJumpControl(Creaking creaking, Creaking creaking2) {
            Creaking creaking3 = creaking;
            Objects.requireNonNull(creaking3);
            this.this$0 = creaking3;
            super(creaking2);
        }

        @Override
        public void tick() {
            if (this.this$0.canMove()) {
                super.tick();
            } else {
                this.this$0.setJumping(false);
            }
        }
    }

    private class CreakingBodyRotationControl
    extends BodyRotationControl {
        final /* synthetic */ Creaking this$0;

        public CreakingBodyRotationControl(Creaking creaking, Creaking creaking2) {
            Creaking creaking3 = creaking;
            Objects.requireNonNull(creaking3);
            this.this$0 = creaking3;
            super(creaking2);
        }

        @Override
        public void clientTick() {
            if (this.this$0.canMove()) {
                super.clientTick();
            }
        }
    }

    private class CreakingPathNavigation
    extends GroundPathNavigation {
        final /* synthetic */ Creaking this$0;

        public CreakingPathNavigation(Creaking creaking, Creaking mob, Level level) {
            Creaking creaking2 = creaking;
            Objects.requireNonNull(creaking2);
            this.this$0 = creaking2;
            super(mob, level);
        }

        @Override
        public void tick() {
            if (this.this$0.canMove()) {
                super.tick();
            }
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new HomeNodeEvaluator(this.this$0);
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    }

    private class HomeNodeEvaluator
    extends WalkNodeEvaluator {
        private static final int MAX_DISTANCE_TO_HOME_SQ = 1024;
        final /* synthetic */ Creaking this$0;

        private HomeNodeEvaluator(Creaking creaking) {
            Creaking creaking2 = creaking;
            Objects.requireNonNull(creaking2);
            this.this$0 = creaking2;
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            BlockPos homePos = this.this$0.getHomePos();
            if (homePos == null) {
                return super.getPathType(context, x, y, z);
            }
            double homeDistance = homePos.distSqr(new Vec3i(x, y, z));
            if (homeDistance > 1024.0 && homeDistance >= homePos.distSqr(context.mobPosition())) {
                return PathType.BLOCKED;
            }
            return super.getPathType(context, x, y, z);
        }
    }
}

