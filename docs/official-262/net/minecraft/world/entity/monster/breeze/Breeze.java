/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.breeze;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.debug.DebugBreezeInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.breeze.BreezeAi;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Breeze
extends Monster {
    private static final int SLIDE_PARTICLES_AMOUNT = 20;
    private static final int IDLE_PARTICLES_AMOUNT = 1;
    private static final int JUMP_TRAIL_PARTICLES_AMOUNT = 3;
    private static final int JUMP_TRAIL_DURATION_TICKS = 5;
    private static final int JUMP_CIRCLE_DISTANCE_Y = 10;
    private static final float FALL_DISTANCE_SOUND_TRIGGER_THRESHOLD = 3.0f;
    private static final int WHIRL_SOUND_FREQUENCY_MIN = 1;
    private static final int WHIRL_SOUND_FREQUENCY_MAX = 80;
    private static final Brain.Provider<Breeze> BRAIN_PROVIDER = Brain.provider(List.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.HURT_BY, SensorType.NEAREST_PLAYERS, SensorType.BREEZE_ATTACK_ENTITY_SENSOR), BreezeAi::getActivities);
    public final AnimationState idle = new AnimationState();
    public final AnimationState slide = new AnimationState();
    public final AnimationState slideBack = new AnimationState();
    public final AnimationState longJump = new AnimationState();
    public final AnimationState shoot = new AnimationState();
    public final AnimationState inhale = new AnimationState();
    private int jumpTrailStartedTick = 0;
    private int soundTick = 0;
    private static final ProjectileDeflection PROJECTILE_DEFLECTION = (projectile, entity, random) -> {
        entity.level().playSound(null, entity, SoundEvents.BREEZE_DEFLECT, entity.getSoundSource(), 1.0f, 1.0f);
        ProjectileDeflection.REVERSE.deflect(projectile, entity, random);
    };

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.63f).add(Attributes.MAX_HEALTH, 30.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    public Breeze(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.ON_TOP_OF_TRAPDOOR, -1.0f);
        this.setPathfindingMalus(PathType.FIRE, -1.0f);
        this.xpReward = 10;
    }

    public Brain<Breeze> getBrain() {
        return super.getBrain();
    }

    protected Brain<Breeze> makeBrain(Brain.Packed input) {
        Brain<Breeze> brain = BRAIN_PROVIDER.makeBrain(this, input);
        brain.setDefaultActivity(Activity.FIGHT);
        brain.useDefaultActivity();
        return brain;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (this.level().isClientSide() && DATA_POSE.equals(accessor)) {
            this.resetAnimations();
            Pose pose = this.getPose();
            switch (pose) {
                case SHOOTING: {
                    this.shoot.startIfStopped(this.tickCount);
                    break;
                }
                case INHALING: {
                    this.inhale.startIfStopped(this.tickCount);
                    break;
                }
                case SLIDING: {
                    this.slide.startIfStopped(this.tickCount);
                }
            }
        }
        super.onSyncedDataUpdated(accessor);
    }

    private void resetAnimations() {
        this.shoot.stop();
        this.idle.stop();
        this.inhale.stop();
        this.longJump.stop();
    }

    @Override
    public void tick() {
        Pose pose = this.getPose();
        switch (pose) {
            case SLIDING: {
                this.emitGroundParticles(20);
                break;
            }
            case SHOOTING: 
            case INHALING: 
            case STANDING: {
                this.resetJumpTrail().emitGroundParticles(1 + this.getRandom().nextInt(1));
                break;
            }
            case LONG_JUMPING: {
                this.longJump.startIfStopped(this.tickCount);
                this.emitJumpTrailParticles();
            }
        }
        this.idle.startIfStopped(this.tickCount);
        if (pose != Pose.SLIDING && this.slide.isStarted()) {
            this.slideBack.start(this.tickCount);
            this.slide.stop();
        }
        int n = this.soundTick = this.soundTick == 0 ? this.random.nextIntBetweenInclusive(1, 80) : this.soundTick - 1;
        if (this.soundTick == 0) {
            this.playWhirlSound();
        }
        super.tick();
    }

    public Breeze resetJumpTrail() {
        this.jumpTrailStartedTick = 0;
        return this;
    }

    public void emitJumpTrailParticles() {
        if (++this.jumpTrailStartedTick > 5) {
            return;
        }
        BlockState ground = !this.getInBlockState().isAir() ? this.getInBlockState() : this.getBlockStateOn();
        Vec3 movement = this.getDeltaMovement();
        Vec3 centered = this.position().add(movement).add(0.0, 0.1f, 0.0);
        for (int i = 0; i < 3; ++i) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), centered.x, centered.y, centered.z, 0.0, 0.0, 0.0);
        }
    }

    public void emitGroundParticles(int amount) {
        BlockState ground;
        if (this.isPassenger()) {
            return;
        }
        Vec3 boundingBoxCenter = this.getBoundingBox().getCenter();
        Vec3 position = new Vec3(boundingBoxCenter.x, this.position().y, boundingBoxCenter.z);
        BlockState blockState = ground = !this.getInBlockState().isAir() ? this.getInBlockState() : this.getBlockStateOn();
        if (ground.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        for (int i = 0; i < amount; ++i) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), position.x, position.y, position.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void playAmbientSound() {
        if (this.getTarget() != null && this.onGround()) {
            return;
        }
        this.level().playLocalSound(this, this.getAmbientSound(), this.getSoundSource(), 1.0f, 1.0f);
    }

    public void playWhirlSound() {
        float pitch = 0.7f + 0.4f * this.random.nextFloat();
        float volume = 0.8f + 0.2f * this.random.nextFloat();
        this.level().playLocalSound(this, SoundEvents.BREEZE_WHIRL, this.getSoundSource(), volume, pitch);
    }

    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        if (projectile.is(EntityTypes.BREEZE_WIND_CHARGE) || projectile.is(EntityTypes.WIND_CHARGE)) {
            return ProjectileDeflection.NONE;
        }
        return this.is(EntityTypeTags.DEFLECTS_PROJECTILES) ? PROJECTILE_DEFLECTION : ProjectileDeflection.NONE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.onGround() ? SoundEvents.BREEZE_IDLE_GROUND : SoundEvents.BREEZE_IDLE_AIR;
    }

    public Optional<LivingEntity> getHurtBy() {
        return this.getBrain().getMemory(MemoryModuleType.HURT_BY).map(DamageSource::getEntity).filter(entity -> entity instanceof LivingEntity).map(entity -> (LivingEntity)entity);
    }

    public boolean withinInnerCircleRange(Vec3 target) {
        Vec3 ourPosition = Vec3.atCenterOf(this.blockPosition());
        return target.closerThan(ourPosition, 4.0, 10.0);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("breezeBrain");
        this.getBrain().tick(level, this);
        profiler.popPush("breezeActivityUpdate");
        BreezeAi.updateActivity(this);
        profiler.pop();
        super.customServerAiStep(level);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return (target.is(EntityTypes.PLAYER) || target.is(EntityTypes.IRON_GOLEM)) && super.canAttack(target);
    }

    @Override
    public int getMaxHeadYRot() {
        return 30;
    }

    @Override
    public int getHeadRotSpeed() {
        return 25;
    }

    public double getFiringYPosition() {
        return this.getY() + (double)(this.getBbHeight() / 2.0f) + (double)0.3f;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return source.getEntity() instanceof Breeze || super.isInvulnerableTo(level, source);
    }

    @Override
    public double getFluidJumpThreshold() {
        return this.getEyeHeight();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        if (fallDistance > 3.0) {
            this.playSound(SoundEvents.BREEZE_LAND, 1.0f, 1.0f);
        }
        return super.causeFallDamage(fallDistance, damageModifier, damageSource);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
        super.registerDebugValues(level, registration);
        registration.register(DebugSubscriptions.BREEZES, () -> new DebugBreezeInfo(this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(Entity::getId), this.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)));
    }
}

