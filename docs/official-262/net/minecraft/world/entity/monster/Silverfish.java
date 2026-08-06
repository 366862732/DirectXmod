/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class Silverfish
extends Monster {
    private @Nullable SilverfishWakeUpFriendsGoal friendsGoal;

    public Silverfish(EntityType<? extends Silverfish> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
    }

    @Override
    protected void registerGoals() {
        this.friendsGoal = new SilverfishWakeUpFriendsGoal(this);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
        this.goalSelector.addGoal(3, this.friendsGoal);
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new SilverfishMergeWithStoneGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>((Mob)this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.15f, 1.0f);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if ((source.getEntity() != null || source.is(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH)) && this.friendsGoal != null) {
            this.friendsGoal.notifyHurt();
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        this.setYRot(yBodyRot);
        super.setYBodyRot(yBodyRot);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (InfestedBlock.isCompatibleHostBlock(level.getBlockState(pos.below()))) {
            return 10.0f;
        }
        return super.getWalkTargetValue(pos, level);
    }

    public static boolean checkSilverfishSpawnRules(EntityType<Silverfish> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if (!Silverfish.checkAnyLightMonsterSpawnRules(type, level, spawnReason, pos, random)) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(spawnReason)) {
            return true;
        }
        Player nearestPlayer = level.getNearestPlayer((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 5.0, true);
        return nearestPlayer == null;
    }

    private static class SilverfishWakeUpFriendsGoal
    extends Goal {
        private final Silverfish silverfish;
        private int lookForFriends;

        public SilverfishWakeUpFriendsGoal(Silverfish silverfish) {
            this.silverfish = silverfish;
        }

        public void notifyHurt() {
            if (this.lookForFriends == 0) {
                this.lookForFriends = this.adjustedTickDelay(20);
            }
        }

        @Override
        public boolean canUse() {
            return this.lookForFriends > 0;
        }

        @Override
        public void tick() {
            --this.lookForFriends;
            if (this.lookForFriends <= 0) {
                Level level = this.silverfish.level();
                RandomSource random = this.silverfish.getRandom();
                BlockPos basePos = this.silverfish.blockPosition();
                int yOff = 0;
                block0: while (yOff <= 5 && yOff >= -5) {
                    int xOff = 0;
                    while (xOff <= 10 && xOff >= -10) {
                        int zOff = 0;
                        while (zOff <= 10 && zOff >= -10) {
                            BlockPos testPos = basePos.offset(xOff, yOff, zOff);
                            BlockState blockState = level.getBlockState(testPos);
                            Block block = blockState.getBlock();
                            if (block instanceof InfestedBlock) {
                                InfestedBlock infestedBlock = (InfestedBlock)block;
                                if (SilverfishWakeUpFriendsGoal.getServerLevel(level).getGameRules().get(GameRules.MOB_GRIEFING).booleanValue()) {
                                    level.destroyBlock(testPos, true, this.silverfish);
                                } else {
                                    level.setBlock(testPos, infestedBlock.hostStateByInfested(level.getBlockState(testPos)), 3);
                                }
                                if (random.nextBoolean()) break block0;
                            }
                            zOff = (zOff <= 0 ? 1 : 0) - zOff;
                        }
                        xOff = (xOff <= 0 ? 1 : 0) - xOff;
                    }
                    yOff = (yOff <= 0 ? 1 : 0) - yOff;
                }
            }
        }
    }

    private static class SilverfishMergeWithStoneGoal
    extends RandomStrollGoal {
        private @Nullable Direction selectedDirection;
        private boolean doMerge;

        public SilverfishMergeWithStoneGoal(Silverfish silverfish) {
            super(silverfish, 1.0, 10);
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null) {
                return false;
            }
            if (!this.mob.getNavigation().isDone()) {
                return false;
            }
            RandomSource random = this.mob.getRandom();
            if (SilverfishMergeWithStoneGoal.getServerLevel(this.mob).getGameRules().get(GameRules.MOB_GRIEFING).booleanValue() && random.nextInt(SilverfishMergeWithStoneGoal.reducedTickDelay(10)) == 0) {
                this.selectedDirection = Direction.getRandom(random);
                BlockPos pos = BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).relative(this.selectedDirection);
                BlockState blockState = this.mob.level().getBlockState(pos);
                if (InfestedBlock.isCompatibleHostBlock(blockState)) {
                    this.doMerge = true;
                    return true;
                }
            }
            this.doMerge = false;
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (this.doMerge) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void start() {
            BlockPos pos;
            if (!this.doMerge) {
                super.start();
                return;
            }
            Level level = this.mob.level();
            BlockState blockState = level.getBlockState(pos = BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()).relative(this.selectedDirection));
            if (InfestedBlock.isCompatibleHostBlock(blockState)) {
                level.setBlock(pos, InfestedBlock.infestedStateByHost(blockState), 3);
                this.mob.spawnAnim();
                this.mob.discard();
            }
        }
    }
}

