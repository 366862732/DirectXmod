/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.illager;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

public class Evoker
extends SpellcasterIllager {
    private @Nullable Sheep wololoTarget;

    public Evoker(EntityType<? extends Evoker> type, Level level) {
        super((EntityType<? extends SpellcasterIllager>)type, level);
        this.xpReward = 10;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EvokerCastingSpellGoal(this));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<Player>(this, Player.class, 8.0f, 0.6, 1.0));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<Creaking>(this, Creaking.class, 8.0f, 0.6, 1.0));
        this.goalSelector.addGoal(4, new EvokerSummonSpellGoal(this));
        this.goalSelector.addGoal(5, new EvokerAttackSpellGoal(this));
        this.goalSelector.addGoal(6, new EvokerWololoSpellGoal(this));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0f, 1.0f));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<AbstractVillager>((Mob)this, AbstractVillager.class, false).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<IronGolem>((Mob)this, IronGolem.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 12.0).add(Attributes.MAX_HEALTH, 24.0);
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    protected boolean considersEntityAsAlly(Entity other) {
        Vex vex;
        LivingEntity rootOwner;
        if (other == this) {
            return true;
        }
        if (super.considersEntityAsAlly(other)) {
            return true;
        }
        return other instanceof Vex && (rootOwner = (vex = (Vex)other).getRootOwner()) != null && (rootOwner == this || super.considersEntityAsAlly(rootOwner));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    private void setWololoTarget(@Nullable Sheep wololoTarget) {
        this.wololoTarget = wololoTarget;
    }

    private @Nullable Sheep getWololoTarget() {
        return this.wololoTarget;
    }

    @Override
    protected SoundEvent getCastingSoundEvent() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
    }

    private class EvokerCastingSpellGoal
    extends SpellcasterIllager.SpellcasterCastingSpellGoal {
        final /* synthetic */ Evoker this$0;

        private EvokerCastingSpellGoal(Evoker evoker) {
            Evoker evoker2 = evoker;
            Objects.requireNonNull(evoker2);
            this.this$0 = evoker2;
            super(evoker);
        }

        @Override
        public void tick() {
            if (this.this$0.getTarget() != null) {
                this.this$0.getLookControl().setLookAt(this.this$0.getTarget(), this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
            } else if (this.this$0.getWololoTarget() != null) {
                this.this$0.getLookControl().setLookAt(this.this$0.getWololoTarget(), this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
            }
        }
    }

    private class EvokerSummonSpellGoal
    extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private final TargetingConditions vexCountTargeting;
        final /* synthetic */ Evoker this$0;

        private EvokerSummonSpellGoal(Evoker evoker) {
            Evoker evoker2 = evoker;
            Objects.requireNonNull(evoker2);
            this.this$0 = evoker2;
            super(evoker);
            this.vexCountTargeting = TargetingConditions.forNonCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();
        }

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            }
            int vexes = EvokerSummonSpellGoal.getServerLevel(this.this$0.level()).getNearbyEntities(Vex.class, this.vexCountTargeting, this.this$0, this.this$0.getBoundingBox().inflate(16.0)).size();
            return this.this$0.random.nextInt(8) + 1 > vexes;
        }

        @Override
        protected int getCastingTime() {
            return 100;
        }

        @Override
        protected int getCastingInterval() {
            return 340;
        }

        @Override
        protected void performSpellCasting() {
            ServerLevel serverLevel = (ServerLevel)this.this$0.level();
            PlayerTeam evokerTeam = this.this$0.getTeam();
            for (int i = 0; i < 3; ++i) {
                BlockPos pos = this.this$0.blockPosition().offset(-2 + this.this$0.random.nextInt(5), 1, -2 + this.this$0.random.nextInt(5));
                Vex vex = EntityTypes.VEX.create(this.this$0.level(), EntitySpawnReason.MOB_SUMMONED);
                if (vex == null) continue;
                vex.snapTo(pos, 0.0f, 0.0f);
                vex.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
                vex.setOwner(this.this$0);
                vex.setBoundOrigin(pos);
                vex.setLimitedLife(20 * (30 + this.this$0.random.nextInt(90)));
                if (evokerTeam != null) {
                    serverLevel.getScoreboard().addPlayerToTeam(vex.getScoreboardName(), evokerTeam);
                }
                serverLevel.addFreshEntityWithPassengers(vex);
                serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(this.this$0));
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.SUMMON_VEX;
        }
    }

    private class EvokerAttackSpellGoal
    extends SpellcasterIllager.SpellcasterUseSpellGoal {
        final /* synthetic */ Evoker this$0;

        private EvokerAttackSpellGoal(Evoker evoker) {
            Evoker evoker2 = evoker;
            Objects.requireNonNull(evoker2);
            this.this$0 = evoker2;
            super(evoker);
        }

        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 100;
        }

        @Override
        protected void performSpellCasting() {
            LivingEntity target = this.this$0.getTarget();
            double minY = Math.min(target.getY(), this.this$0.getY());
            double maxY = Math.max(target.getY(), this.this$0.getY()) + 1.0;
            float angleTowardsTarget = (float)Mth.atan2(target.getZ() - this.this$0.getZ(), target.getX() - this.this$0.getX());
            if (this.this$0.distanceToSqr(target) < 9.0) {
                float angle;
                int i;
                for (i = 0; i < 5; ++i) {
                    angle = angleTowardsTarget + (float)i * (float)Math.PI * 0.4f;
                    this.createSpellEntity(this.this$0.getX() + (double)Mth.cos(angle) * 1.5, this.this$0.getZ() + (double)Mth.sin(angle) * 1.5, minY, maxY, angle, 0);
                }
                for (i = 0; i < 8; ++i) {
                    angle = angleTowardsTarget + (float)i * (float)Math.PI * 2.0f / 8.0f + 1.2566371f;
                    this.createSpellEntity(this.this$0.getX() + (double)Mth.cos(angle) * 2.5, this.this$0.getZ() + (double)Mth.sin(angle) * 2.5, minY, maxY, angle, 3);
                }
            } else {
                for (int i = 0; i < 16; ++i) {
                    double reach = 1.25 * (double)(i + 1);
                    int spellSpeed = 1 * i;
                    this.createSpellEntity(this.this$0.getX() + (double)Mth.cos(angleTowardsTarget) * reach, this.this$0.getZ() + (double)Mth.sin(angleTowardsTarget) * reach, minY, maxY, angleTowardsTarget, spellSpeed);
                }
            }
        }

        private void createSpellEntity(double x, double z, double minY, double maxY, float angle, int delayTicks) {
            BlockPos pos = BlockPos.containing(x, maxY, z);
            boolean success = false;
            double topOffset = 0.0;
            do {
                BlockState blockState;
                VoxelShape shape;
                BlockPos below = pos.below();
                BlockState belowState = this.this$0.level().getBlockState(below);
                if (!belowState.isFaceSturdy(this.this$0.level(), below, Direction.UP)) continue;
                if (!this.this$0.level().isEmptyBlock(pos) && !(shape = (blockState = this.this$0.level().getBlockState(pos)).getCollisionShape(this.this$0.level(), pos)).isEmpty()) {
                    topOffset = shape.max(Direction.Axis.Y);
                }
                success = true;
                break;
            } while ((pos = pos.below()).getY() >= Mth.floor(minY) - 1);
            if (success) {
                this.this$0.level().addFreshEntity(new EvokerFangs(this.this$0.level(), x, (double)pos.getY() + topOffset, z, angle, delayTicks, this.this$0));
                this.this$0.level().gameEvent(GameEvent.ENTITY_PLACE, new Vec3(x, (double)pos.getY() + topOffset, z), GameEvent.Context.of(this.this$0));
            }
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_ATTACK;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.FANGS;
        }
    }

    public class EvokerWololoSpellGoal
    extends SpellcasterIllager.SpellcasterUseSpellGoal {
        private final TargetingConditions wololoTargeting;
        final /* synthetic */ Evoker this$0;

        public EvokerWololoSpellGoal(Evoker this$0) {
            Evoker evoker = this$0;
            Objects.requireNonNull(evoker);
            this.this$0 = evoker;
            super(this$0);
            this.wololoTargeting = TargetingConditions.forNonCombat().range(16.0).selector((target, level) -> ((Sheep)target).getColor() == DyeColor.BLUE);
        }

        @Override
        public boolean canUse() {
            if (this.this$0.getTarget() != null) {
                return false;
            }
            if (this.this$0.isCastingSpell()) {
                return false;
            }
            if (this.this$0.tickCount < this.nextAttackTickCount) {
                return false;
            }
            ServerLevel level = EvokerWololoSpellGoal.getServerLevel(this.this$0.level());
            if (!level.getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                return false;
            }
            List<Sheep> entities = level.getNearbyEntities(Sheep.class, this.wololoTargeting, this.this$0, this.this$0.getBoundingBox().inflate(16.0, 4.0, 16.0));
            if (entities.isEmpty()) {
                return false;
            }
            this.this$0.setWololoTarget(entities.get(this.this$0.random.nextInt(entities.size())));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.this$0.getWololoTarget() != null && this.attackWarmupDelay > 0;
        }

        @Override
        public void stop() {
            super.stop();
            this.this$0.setWololoTarget(null);
        }

        @Override
        protected void performSpellCasting() {
            Sheep wololoTarget = this.this$0.getWololoTarget();
            if (wololoTarget != null && wololoTarget.isAlive()) {
                wololoTarget.setColor(DyeColor.RED);
            }
        }

        @Override
        protected int getCastWarmupTime() {
            return 40;
        }

        @Override
        protected int getCastingTime() {
            return 60;
        }

        @Override
        protected int getCastingInterval() {
            return 140;
        }

        @Override
        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_WOLOLO;
        }

        @Override
        protected SpellcasterIllager.IllagerSpell getSpell() {
            return SpellcasterIllager.IllagerSpell.WOLOLO;
        }
    }
}

