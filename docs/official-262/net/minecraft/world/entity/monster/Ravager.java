/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Ravager
extends Raider {
    private static final Predicate<Entity> ROAR_TARGET_WITH_GRIEFING = entity -> !(entity instanceof Ravager) && entity.isAlive();
    private static final Predicate<Entity> ROAR_TARGET_WITHOUT_GRIEFING = entity -> ROAR_TARGET_WITH_GRIEFING.test((Entity)entity) && !entity.is(EntityTypes.ARMOR_STAND);
    private static final Predicate<LivingEntity> ROAR_TARGET_ON_CLIENT = e -> !(e instanceof Ravager) && e.isAlive() && e.isLocalInstanceAuthoritative();
    private static final double BASE_MOVEMENT_SPEED = 0.3;
    private static final double ATTACK_MOVEMENT_SPEED = 0.35;
    private static final int STUNNED_COLOR = 8356754;
    private static final float STUNNED_COLOR_BLUE = 0.57254905f;
    private static final float STUNNED_COLOR_GREEN = 0.5137255f;
    private static final float STUNNED_COLOR_RED = 0.49803922f;
    public static final int ATTACK_DURATION = 10;
    public static final int STUN_DURATION = 40;
    private static final int DEFAULT_ATTACK_TICK = 0;
    private static final int DEFAULT_STUN_TICK = 0;
    private static final int DEFAULT_ROAR_TICK = 0;
    private int attackTick = 0;
    private int stunnedTick = 0;
    private int roarTick = 0;

    public Ravager(EntityType<? extends Ravager> type, Level level) {
        super((EntityType<? extends Raider>)type, level);
        this.xpReward = 20;
        this.setPathfindingMalus(PathType.LEAVES, 0.0f);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, Raider.class).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<AbstractVillager>((Mob)this, AbstractVillager.class, true, (target, level) -> !target.isBaby()));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<IronGolem>((Mob)this, IronGolem.class, true));
    }

    @Override
    protected void updateControlFlags() {
        boolean noController = !(this.getControllingPassenger() instanceof Mob) || this.getControllingPassenger().is(EntityTypeTags.RAIDERS);
        boolean notInBoat = !(this.getVehicle() instanceof AbstractBoat);
        this.goalSelector.setControlFlag(Goal.Flag.MOVE, noController);
        this.goalSelector.setControlFlag(Goal.Flag.JUMP, noController && notInBoat);
        this.goalSelector.setControlFlag(Goal.Flag.LOOK, noController);
        this.goalSelector.setControlFlag(Goal.Flag.TARGET, noController);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.75).add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ATTACK_KNOCKBACK, 1.5).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("AttackTick", this.attackTick);
        output.putInt("StunTick", this.stunnedTick);
        output.putInt("RoarTick", this.roarTick);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.attackTick = input.getIntOr("AttackTick", 0);
        this.stunnedTick = input.getIntOr("StunTick", 0);
        this.roarTick = input.getIntOr("RoarTick", 0);
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.RAVAGER_CELEBRATE;
    }

    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.isAlive()) {
            return;
        }
        if (this.isImmobile()) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        } else {
            double maxSpeed = this.getTarget() != null ? 0.35 : 0.3;
            double baseValue = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Mth.lerp(0.1, baseValue, maxSpeed));
        }
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.horizontalCollision && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                boolean destroyedBlock = false;
                AABB bb = this.getBoundingBox().inflate(0.2);
                for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(bb.minX), Mth.floor(bb.minY), Mth.floor(bb.minZ), Mth.floor(bb.maxX), Mth.floor(bb.maxY), Mth.floor(bb.maxZ))) {
                    BlockState state = serverLevel.getBlockState(pos);
                    Block block = state.getBlock();
                    if (!(block instanceof LeavesBlock)) continue;
                    destroyedBlock = serverLevel.destroyBlock(pos, true, this) || destroyedBlock;
                }
                if (!destroyedBlock && this.onGround()) {
                    this.jumpFromGround();
                }
            }
        }
        if (this.roarTick > 0) {
            --this.roarTick;
            if (this.roarTick == 10) {
                this.roar();
            }
        }
        if (this.attackTick > 0) {
            --this.attackTick;
        }
        if (this.stunnedTick > 0) {
            --this.stunnedTick;
            this.stunEffect();
            if (this.stunnedTick == 0) {
                this.playSound(SoundEvents.RAVAGER_ROAR, 1.0f, 1.0f);
                this.roarTick = 20;
            }
        }
    }

    private void stunEffect() {
        if (this.random.nextInt(6) == 0) {
            double headX = this.getX() - (double)this.getBbWidth() * Math.sin(this.yBodyRot * ((float)Math.PI / 180)) + (this.random.nextDouble() * 0.6 - 0.3);
            double headY = this.getY() + (double)this.getBbHeight() - 0.3;
            double headZ = this.getZ() + (double)this.getBbWidth() * Math.cos(this.yBodyRot * ((float)Math.PI / 180)) + (this.random.nextDouble() * 0.6 - 0.3);
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.49803922f, 0.5137255f, 0.57254905f), headX, headY, headZ, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.attackTick > 0 || this.stunnedTick > 0 || this.roarTick > 0;
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        if (this.stunnedTick > 0 || this.roarTick > 0) {
            return false;
        }
        return super.hasLineOfSight(target);
    }

    @Override
    protected void blockedByItem(LivingEntity defender, DamageSource source, float damage) {
        if (this.roarTick == 0) {
            if (this.random.nextDouble() < 0.5) {
                this.stunnedTick = 40;
                this.playSound(SoundEvents.RAVAGER_STUNNED, 1.0f, 1.0f);
                this.level().broadcastEntityEvent(this, (byte)39);
                defender.push(this);
            } else {
                this.strongKnockback(defender);
            }
            defender.hurtMarked = true;
        }
    }

    private void roar() {
        Level level;
        if (this.isAlive() && (level = this.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            Predicate<Entity> targetSelector = level2.getGameRules().get(GameRules.MOB_GRIEFING) != false ? ROAR_TARGET_WITH_GRIEFING : ROAR_TARGET_WITHOUT_GRIEFING;
            List<Entity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0), targetSelector);
            for (LivingEntity livingEntity : entities) {
                if (!(livingEntity instanceof AbstractIllager)) {
                    livingEntity.hurtServer(level2, this.damageSources().mobAttack(this), 6.0f);
                }
                if (livingEntity instanceof Player) continue;
                this.strongKnockback(livingEntity);
            }
            this.gameEvent(GameEvent.ENTITY_ACTION);
            level2.broadcastEntityEvent(this, (byte)69);
        }
    }

    private void applyRoarKnockbackClient() {
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0), ROAR_TARGET_ON_CLIENT);
        for (LivingEntity entity : entities) {
            this.strongKnockback(entity);
        }
    }

    private void strongKnockback(Entity entity) {
        double xd = entity.getX() - this.getX();
        double zd = entity.getZ() - this.getZ();
        double dd = Math.max(xd * xd + zd * zd, 0.001);
        entity.push(xd / dd * 4.0, 0.2, zd / dd * 4.0);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackTick = 10;
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0f, 1.0f);
        } else if (id == 39) {
            this.stunnedTick = 40;
        } else if (id == 69) {
            this.addRoarParticleEffects();
            this.applyRoarKnockbackClient();
        }
        super.handleEntityEvent(id);
    }

    private void addRoarParticleEffects() {
        Vec3 center = this.getBoundingBox().getCenter();
        for (int i = 0; i < 40; ++i) {
            double velocityX = this.random.nextGaussian() * 0.2;
            double velocityY = this.random.nextGaussian() * 0.2;
            double velocityZ = this.random.nextGaussian() * 0.2;
            this.level().addParticle(ParticleTypes.POOF, center.x, center.y, center.z, velocityX, velocityY, velocityZ);
        }
    }

    public int getAttackTick() {
        return this.attackTick;
    }

    public int getStunnedTick() {
        return this.stunnedTick;
    }

    public int getRoarTick() {
        return this.roarTick;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.attackTick = 10;
        level.broadcastEntityEvent(this, (byte)4);
        this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0f, 1.0f);
        return super.doHurtTarget(level, target);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.15f, 1.0f);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return !level.containsAnyLiquid(this.getBoundingBox());
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    protected AABB getAttackBoundingBox(double horizontalExpansion) {
        AABB defaultBB = super.getAttackBoundingBox(horizontalExpansion);
        return defaultBB.deflate(0.05, 0.0, 0.05);
    }
}

