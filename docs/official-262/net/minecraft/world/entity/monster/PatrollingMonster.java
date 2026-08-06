/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class PatrollingMonster
extends Monster {
    private static final boolean DEFAULT_PATROL_LEADER = false;
    private static final boolean DEFAULT_PATROLLING = false;
    private @Nullable BlockPos patrolTarget;
    private boolean patrolLeader = false;
    private boolean patrolling = false;

    protected PatrollingMonster(EntityType<? extends PatrollingMonster> type, Level level) {
        super((EntityType<? extends Monster>)type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new LongDistancePatrolGoal<PatrollingMonster>(this, 0.7, 0.595));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("patrol_target", BlockPos.CODEC, this.patrolTarget);
        output.putBoolean("PatrolLeader", this.patrolLeader);
        output.putBoolean("Patrolling", this.patrolling);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.patrolTarget = input.read("patrol_target", BlockPos.CODEC).orElse(null);
        this.patrolLeader = input.getBooleanOr("PatrolLeader", false);
        this.patrolling = input.getBooleanOr("Patrolling", false);
    }

    public boolean canBeLeader() {
        return true;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (spawnReason != EntitySpawnReason.PATROL && spawnReason != EntitySpawnReason.EVENT && spawnReason != EntitySpawnReason.STRUCTURE && level.getRandom().nextFloat() < 0.06f && this.canBeLeader()) {
            this.patrolLeader = true;
        }
        if (this.isPatrolLeader()) {
            this.setItemSlot(EquipmentSlot.HEAD, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
            this.setDropChance(EquipmentSlot.HEAD, 2.0f);
        }
        if (spawnReason == EntitySpawnReason.PATROL) {
            this.patrolling = true;
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public static boolean checkPatrollingMonsterSpawnRules(EntityType<? extends PatrollingMonster> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 8) {
            return false;
        }
        return PatrollingMonster.checkAnyLightMonsterSpawnRules(type, level, spawnReason, pos, random);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.patrolling || distSqr > 16384.0;
    }

    public void setPatrolTarget(BlockPos target) {
        this.patrolTarget = target;
        this.patrolling = true;
    }

    public @Nullable BlockPos getPatrolTarget() {
        return this.patrolTarget;
    }

    public boolean hasPatrolTarget() {
        return this.patrolTarget != null;
    }

    public void setPatrolLeader(boolean isLeader) {
        this.patrolLeader = isLeader;
        this.patrolling = true;
    }

    public boolean isPatrolLeader() {
        return this.patrolLeader;
    }

    public boolean canJoinPatrol() {
        return true;
    }

    public void findPatrolTarget() {
        this.patrolTarget = this.blockPosition().offset(-500 + this.random.nextInt(1000), 0, -500 + this.random.nextInt(1000));
        this.patrolling = true;
    }

    protected boolean isPatrolling() {
        return this.patrolling;
    }

    protected void setPatrolling(boolean value) {
        this.patrolling = value;
    }

    public static class LongDistancePatrolGoal<T extends PatrollingMonster>
    extends Goal {
        private static final int NAVIGATION_FAILED_COOLDOWN = 200;
        private final T mob;
        private final double speedModifier;
        private final double leaderSpeedModifier;
        private long cooldownUntil;

        public LongDistancePatrolGoal(T mob, double speedModifier, double leaderSpeedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.leaderSpeedModifier = leaderSpeedModifier;
            this.cooldownUntil = -1L;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            boolean isOnCooldown = ((Entity)this.mob).level().getGameTime() < this.cooldownUntil;
            return ((PatrollingMonster)this.mob).isPatrolling() && ((Mob)this.mob).getTarget() == null && !((Entity)this.mob).hasControllingPassenger() && ((PatrollingMonster)this.mob).hasPatrolTarget() && !isOnCooldown;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
            boolean patrolLeader = ((PatrollingMonster)this.mob).isPatrolLeader();
            PathNavigation navigation = ((Mob)this.mob).getNavigation();
            if (navigation.isDone()) {
                List<PatrollingMonster> companions = this.findPatrolCompanions();
                if (((PatrollingMonster)this.mob).isPatrolling() && companions.isEmpty()) {
                    ((PatrollingMonster)this.mob).setPatrolling(false);
                } else if (!patrolLeader || !((PatrollingMonster)this.mob).getPatrolTarget().closerToCenterThan(((Entity)this.mob).position(), 10.0)) {
                    Vec3 longDistanceTarget = Vec3.atBottomCenterOf(((PatrollingMonster)this.mob).getPatrolTarget());
                    Vec3 selfVector = ((Entity)this.mob).position();
                    Vec3 distance = selfVector.subtract(longDistanceTarget);
                    longDistanceTarget = distance.yRot(90.0f).scale(0.4).add(longDistanceTarget);
                    Vec3 moveTarget = longDistanceTarget.subtract(selfVector).normalize().scale(10.0).add(selfVector);
                    BlockPos pathTarget = BlockPos.containing(moveTarget);
                    pathTarget = ((Entity)this.mob).level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pathTarget);
                    if (!navigation.moveTo(pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), patrolLeader ? this.leaderSpeedModifier : this.speedModifier)) {
                        this.moveRandomly();
                        this.cooldownUntil = ((Entity)this.mob).level().getGameTime() + 200L;
                    } else if (patrolLeader) {
                        for (PatrollingMonster companion : companions) {
                            companion.setPatrolTarget(pathTarget);
                        }
                    }
                } else {
                    ((PatrollingMonster)this.mob).findPatrolTarget();
                }
            }
        }

        private List<PatrollingMonster> findPatrolCompanions() {
            return ((Entity)this.mob).level().getEntitiesOfClass(PatrollingMonster.class, ((Entity)this.mob).getBoundingBox().inflate(16.0), mob -> mob.canJoinPatrol() && !mob.is((Entity)this.mob));
        }

        private boolean moveRandomly() {
            RandomSource random = ((Entity)this.mob).getRandom();
            BlockPos pathTarget = ((Entity)this.mob).level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ((Entity)this.mob).blockPosition().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
            return ((Mob)this.mob).getNavigation().moveTo(pathTarget.getX(), pathTarget.getY(), pathTarget.getZ(), this.speedModifier);
        }
    }
}

