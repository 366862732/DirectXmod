/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.equine;

import java.util.EnumSet;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TraderLlama
extends Llama {
    private static final int DEFAULT_DESPAWN_DELAY = 47999;
    private int despawnDelay = 47999;

    public TraderLlama(EntityType<? extends TraderLlama> type, Level level) {
        super((EntityType<? extends Llama>)type, level);
    }

    @Override
    public boolean isTraderLlama() {
        return true;
    }

    @Override
    protected @Nullable Llama makeNewLlama() {
        TraderLlama baby = EntityTypes.TRADER_LLAMA.create(this.level(), EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setPersistenceRequired();
        }
        return baby;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("DespawnDelay", this.despawnDelay);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.despawnDelay = input.getIntOr("DespawnDelay", 47999);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0));
        this.targetSelector.addGoal(1, new TraderLlamaDefendWanderingTraderGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Zombie>((Mob)this, Zombie.class, true, (target, level) -> !target.is(EntityTypes.ZOMBIFIED_PIGLIN)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<AbstractIllager>((Mob)this, AbstractIllager.class, true));
    }

    public void setDespawnDelay(int despawnDelay) {
        this.despawnDelay = despawnDelay;
    }

    @Override
    protected void doPlayerRide(Player player) {
        Entity leashHolder = this.getLeashHolder();
        if (leashHolder instanceof WanderingTrader) {
            return;
        }
        super.doPlayerRide(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            this.maybeDespawn();
        }
    }

    private void maybeDespawn() {
        if (!this.canDespawn()) {
            return;
        }
        int n = this.despawnDelay = this.isLeashedToWanderingTrader() ? ((WanderingTrader)this.getLeashHolder()).getDespawnDelay() - 1 : this.despawnDelay - 1;
        if (this.despawnDelay <= 0) {
            this.removeLeash();
            this.discard();
        }
    }

    private boolean canDespawn() {
        return !this.isTamed() && !this.isLeashedToSomethingOtherThanTheWanderingTrader() && !this.hasExactlyOnePlayerPassenger() && !this.isAgeLocked() && !this.isPersistenceRequired();
    }

    private boolean isLeashedToWanderingTrader() {
        return this.getLeashHolder() instanceof WanderingTrader;
    }

    private boolean isLeashedToSomethingOtherThanTheWanderingTrader() {
        return this.isLeashed() && !this.isLeashedToWanderingTrader();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (spawnReason == EntitySpawnReason.EVENT) {
            this.setAge(0);
        }
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    protected static class TraderLlamaDefendWanderingTraderGoal
    extends TargetGoal {
        private final Llama llama;
        private LivingEntity ownerLastHurtBy;
        private int timestamp;

        public TraderLlamaDefendWanderingTraderGoal(Llama tameAnimal) {
            super(tameAnimal, false);
            this.llama = tameAnimal;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!this.llama.isLeashed()) {
                return false;
            }
            Entity leashHolder = this.llama.getLeashHolder();
            if (!(leashHolder instanceof WanderingTrader)) {
                return false;
            }
            WanderingTrader owner = (WanderingTrader)leashHolder;
            this.ownerLastHurtBy = owner.getLastHurtByMob();
            int timeStamp = owner.getLastHurtByMobTimestamp();
            return timeStamp != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.ownerLastHurtBy);
            Entity leashHolder = this.llama.getLeashHolder();
            if (leashHolder instanceof WanderingTrader) {
                WanderingTrader wanderingTrader = (WanderingTrader)leashHolder;
                this.timestamp = wanderingTrader.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }
}

