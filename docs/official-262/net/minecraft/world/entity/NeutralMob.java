/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public interface NeutralMob {
    public static final String TAG_ANGER_END_TIME = "anger_end_time";
    public static final String TAG_ANGRY_AT = "angry_at";
    public static final long NO_ANGER_END_TIME = -1L;

    public long getPersistentAngerEndTime();

    default public void setTimeToRemainAngry(long remainingTime) {
        this.setPersistentAngerEndTime(this.level().getGameTime() + remainingTime);
    }

    public void setPersistentAngerEndTime(long var1);

    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget();

    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> var1);

    public void startPersistentAngerTimer();

    public Level level();

    default public void addPersistentAngerSaveData(ValueOutput output) {
        output.putLong(TAG_ANGER_END_TIME, this.getPersistentAngerEndTime());
        output.storeNullable(TAG_ANGRY_AT, EntityReference.codec(), this.getPersistentAngerTarget());
    }

    default public void readPersistentAngerSaveData(Level level, ValueInput input) {
        Optional<Long> endTime = input.getLong(TAG_ANGER_END_TIME);
        if (endTime.isPresent()) {
            this.setPersistentAngerEndTime(endTime.get());
        } else {
            Optional<Integer> angerTime = input.getInt("AngerTime");
            if (angerTime.isPresent()) {
                this.setTimeToRemainAngry(angerTime.get().intValue());
            } else {
                this.setPersistentAngerEndTime(-1L);
            }
        }
        if (!(level instanceof ServerLevel)) {
            return;
        }
        this.setPersistentAngerTarget(EntityReference.read(input, TAG_ANGRY_AT));
        this.setTarget(EntityReference.getLivingEntity(this.getPersistentAngerTarget(), level));
    }

    default public void updatePersistentAnger(ServerLevel level, boolean stayAngryIfTargetPresent) {
        Player player;
        LivingEntity persistentTarget;
        LivingEntity previousTarget = this.getTargetUnchecked();
        EntityReference<LivingEntity> persistentAngerTarget = this.getPersistentAngerTarget();
        if (previousTarget != null && previousTarget.isDeadOrDying() && persistentAngerTarget != null && persistentAngerTarget.matches(previousTarget) && previousTarget instanceof Mob) {
            this.stopBeingAngry();
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null) {
            boolean newTarget;
            boolean bl = newTarget = persistentAngerTarget == null || !persistentAngerTarget.matches(target);
            if (newTarget) {
                this.setPersistentAngerTarget(EntityReference.of(target));
            }
            if (newTarget || stayAngryIfTargetPresent) {
                this.startPersistentAngerTimer();
            }
        }
        if (!(persistentAngerTarget == null || this.isAngry() || target != null && NeutralMob.isValidPlayerTarget(target) && stayAngryIfTargetPresent)) {
            this.stopBeingAngry();
        }
        if ((persistentTarget = EntityReference.getLivingEntity(persistentAngerTarget, level)) instanceof Player && ((player = (Player)persistentTarget).isCreative() || player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL)) {
            this.stopBeingAngry();
        }
    }

    private static boolean isValidPlayerTarget(LivingEntity target) {
        Player player;
        return target instanceof Player && !(player = (Player)target).isCreative() && !player.isSpectator() && player.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    default public boolean isAngryAt(LivingEntity entity, ServerLevel level) {
        if (!this.canAttack(entity)) {
            return false;
        }
        if (NeutralMob.isValidPlayerTarget(entity) && this.isAngryAtAllPlayers(level)) {
            return true;
        }
        EntityReference<LivingEntity> persistentAngerTarget = this.getPersistentAngerTarget();
        return persistentAngerTarget != null && persistentAngerTarget.matches(entity);
    }

    default public boolean isAngryAtAllPlayers(ServerLevel level) {
        return level.getGameRules().get(GameRules.UNIVERSAL_ANGER) != false && this.isAngry() && this.getPersistentAngerTarget() == null;
    }

    default public boolean isAngry() {
        long endTime = this.getPersistentAngerEndTime();
        if (endTime > 0L) {
            long remaining = endTime - this.level().getGameTime();
            return remaining > 0L;
        }
        return false;
    }

    default public void playerDied(ServerLevel level, Player player) {
        if (!level.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS).booleanValue()) {
            return;
        }
        EntityReference<LivingEntity> persistentAngerTarget = this.getPersistentAngerTarget();
        if (persistentAngerTarget == null || !persistentAngerTarget.matches(player)) {
            return;
        }
        this.stopBeingAngry();
    }

    default public void forgetCurrentTargetAndRefreshUniversalAnger() {
        this.stopBeingAngry();
        this.startPersistentAngerTimer();
    }

    default public void stopBeingAngry() {
        this.setLastHurtByMob(null);
        this.setPersistentAngerTarget(null);
        this.setTarget(null);
        this.setPersistentAngerEndTime(-1L);
    }

    public @Nullable LivingEntity getLastHurtByMob();

    public void setLastHurtByMob(@Nullable LivingEntity var1);

    public void setTarget(@Nullable LivingEntity var1);

    public boolean canAttack(LivingEntity var1);

    public @Nullable LivingEntity getTarget();

    public @Nullable LivingEntity getTargetUnchecked();
}

