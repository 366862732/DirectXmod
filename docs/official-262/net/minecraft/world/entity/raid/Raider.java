/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Raider
extends PatrollingMonster {
    protected static final EntityDataAccessor<Boolean> IS_CELEBRATING = SynchedEntityData.defineId(Raider.class, EntityDataSerializers.BOOLEAN);
    private static final Predicate<ItemEntity> ALLOWED_ITEMS = e -> !e.hasPickUpDelay() && e.isAlive() && ItemStack.matches(e.getItem(), Raid.getOminousBannerInstance(e.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
    private static final int DEFAULT_WAVE = 0;
    private static final boolean DEFAULT_CAN_JOIN_RAID = false;
    protected @Nullable Raid raid;
    private int wave = 0;
    private boolean canJoinRaid = false;
    private int ticksOutsideRaid;

    protected Raider(EntityType<? extends Raider> type, Level level) {
        super((EntityType<? extends PatrollingMonster>)type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new ObtainRaidLeaderBannerGoal(this, this));
        this.goalSelector.addGoal(3, new PathfindToRaidGoal<Raider>(this));
        this.goalSelector.addGoal(4, new RaiderMoveThroughVillageGoal(this, 1.05f, 1));
        this.goalSelector.addGoal(5, new RaiderCelebration(this, this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(IS_CELEBRATING, false);
    }

    public abstract void applyRaidBuffs(ServerLevel var1, int var2, boolean var3);

    public boolean canJoinRaid() {
        return this.canJoinRaid;
    }

    public void setCanJoinRaid(boolean canJoinRaid) {
        this.canJoinRaid = canJoinRaid;
    }

    @Override
    public void aiStep() {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            if (this.isAlive()) {
                Raid currentRaid = this.getCurrentRaid();
                if (this.canJoinRaid()) {
                    if (currentRaid == null) {
                        Raid nearbyRaid;
                        if (this.level().getGameTime() % 20L == 0L && (nearbyRaid = level2.getRaidAt(this.blockPosition())) != null && Raids.canJoinRaid(this)) {
                            nearbyRaid.joinRaid(level2, nearbyRaid.getGroupsSpawned(), this, null, true);
                        }
                    } else {
                        LivingEntity target = this.getTarget();
                        if (target != null && (target.is(EntityTypes.PLAYER) || target.is(EntityTypes.IRON_GOLEM))) {
                            this.noActionTime = 0;
                        }
                    }
                }
            }
        }
        super.aiStep();
    }

    @Override
    protected void updateNoActionTime() {
        this.noActionTime += 2;
    }

    @Override
    public void die(DamageSource source) {
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Entity killer = source.getEntity();
            Raid raidWhenKilled = this.getCurrentRaid();
            if (raidWhenKilled != null) {
                if (this.isPatrolLeader()) {
                    raidWhenKilled.removeLeader(this.getWave());
                }
                if (killer != null && killer.is(EntityTypes.PLAYER)) {
                    raidWhenKilled.addHeroOfTheVillage(killer);
                }
                raidWhenKilled.removeFromRaid(serverLevel, this, false);
            }
        }
        super.die(source);
    }

    @Override
    public boolean canJoinPatrol() {
        return !this.hasActiveRaid();
    }

    public void setCurrentRaid(@Nullable Raid raid) {
        this.raid = raid;
    }

    public @Nullable Raid getCurrentRaid() {
        return this.raid;
    }

    public boolean isCaptain() {
        ItemStack banner = this.getItemBySlot(EquipmentSlot.HEAD);
        boolean hasCaptainBanner = !banner.isEmpty() && ItemStack.matches(banner, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        boolean patrolLeader = this.isPatrolLeader();
        return hasCaptainBanner && patrolLeader;
    }

    public boolean hasRaid() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        return this.getCurrentRaid() != null || serverLevel.getRaidAt(this.blockPosition()) != null;
    }

    public boolean hasActiveRaid() {
        return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public int getWave() {
        return this.wave;
    }

    public boolean isCelebrating() {
        return this.entityData.get(IS_CELEBRATING);
    }

    public void setCelebrating(boolean celebrating) {
        this.entityData.set(IS_CELEBRATING, celebrating);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        Level level;
        super.addAdditionalSaveData(output);
        output.putInt("Wave", this.wave);
        output.putBoolean("CanJoinRaid", this.canJoinRaid);
        if (this.raid != null && (level = this.level()) instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            level2.getRaids().getId(this.raid).ifPresent(id -> output.putInt("RaidId", id));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.wave = input.getIntOr("Wave", 0);
        this.canJoinRaid = input.getBooleanOr("CanJoinRaid", false);
        Level level = this.level();
        if (level instanceof ServerLevel) {
            ServerLevel level2 = (ServerLevel)level;
            input.getInt("RaidId").ifPresent(raidId -> {
                this.raid = level2.getRaids().get((int)raidId);
                if (this.raid != null) {
                    this.raid.addWaveMob(level2, this.wave, this, false);
                    if (this.isPatrolLeader()) {
                        this.raid.setLeader(this.wave, this);
                    }
                }
            });
        }
    }

    @Override
    protected void pickUpItem(ServerLevel level, ItemEntity entity) {
        boolean hasRaidLeader;
        ItemStack itemStack = entity.getItem();
        boolean bl = hasRaidLeader = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
        if (this.hasActiveRaid() && !hasRaidLeader && ItemStack.matches(itemStack, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))) {
            EquipmentSlot slot = EquipmentSlot.HEAD;
            ItemStack current = this.getItemBySlot(slot);
            double dropChance = this.getDropChances().byEquipment(slot);
            if (!current.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1f, 0.0f) < dropChance) {
                this.spawnAtLocation(level, current);
            }
            this.onItemPickup(entity);
            this.setItemSlot(slot, itemStack);
            this.take(entity, itemStack.getCount());
            entity.discard();
            this.getCurrentRaid().setLeader(this.getWave(), this);
            this.setPatrolLeader(true);
        } else {
            super.pickUpItem(level, entity);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        if (this.getCurrentRaid() == null) {
            return super.removeWhenFarAway(distSqr);
        }
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
    }

    public int getTicksOutsideRaid() {
        return this.ticksOutsideRaid;
    }

    public void setTicksOutsideRaid(int ticksOutsideRaid) {
        this.ticksOutsideRaid = ticksOutsideRaid;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.hasActiveRaid()) {
            this.getCurrentRaid().updateBossbar();
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setCanJoinRaid(!this.is(EntityTypes.WITCH) || spawnReason != EntitySpawnReason.NATURAL);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    public abstract SoundEvent getCelebrateSound();

    public class ObtainRaidLeaderBannerGoal<T extends Raider>
    extends Goal {
        private final T mob;
        private Int2LongOpenHashMap unreachableBannerCache;
        private @Nullable Path pathToBanner;
        private @Nullable ItemEntity pursuedBannerItemEntity;
        final /* synthetic */ Raider this$0;

        public ObtainRaidLeaderBannerGoal(T mob) {
            reference v0 = this$0;
            Objects.requireNonNull(v0);
            this.this$0 = v0;
            this.unreachableBannerCache = new Int2LongOpenHashMap();
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cannotPickUpBanner()) {
                return false;
            }
            Int2LongOpenHashMap tempCache = new Int2LongOpenHashMap();
            double followRange = this.this$0.getAttributeValue(Attributes.FOLLOW_RANGE);
            List<ItemEntity> items = ((Entity)this.mob).level().getEntitiesOfClass(ItemEntity.class, ((Entity)this.mob).getBoundingBox().inflate(followRange, 8.0, followRange), ALLOWED_ITEMS);
            for (ItemEntity banner : items) {
                long unreachableUntilTime = this.unreachableBannerCache.getOrDefault(banner.getId(), Long.MIN_VALUE);
                if (this.this$0.level().getGameTime() < unreachableUntilTime) {
                    tempCache.put(banner.getId(), unreachableUntilTime);
                    continue;
                }
                Path path = ((Mob)this.mob).getNavigation().createPath(banner, 1);
                if (path != null && path.canReach()) {
                    this.pathToBanner = path;
                    this.pursuedBannerItemEntity = banner;
                    return true;
                }
                tempCache.put(banner.getId(), this.this$0.level().getGameTime() + 600L);
            }
            this.unreachableBannerCache = tempCache;
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.pursuedBannerItemEntity == null || this.pathToBanner == null) {
                return false;
            }
            if (this.pursuedBannerItemEntity.isRemoved()) {
                return false;
            }
            if (this.pathToBanner.isDone()) {
                return false;
            }
            return !this.cannotPickUpBanner();
        }

        private boolean cannotPickUpBanner() {
            if (!((Raider)this.mob).hasActiveRaid()) {
                return true;
            }
            if (((Raider)this.mob).getCurrentRaid().isOver()) {
                return true;
            }
            if (!((PatrollingMonster)this.mob).canBeLeader()) {
                return true;
            }
            if (ItemStack.matches(((LivingEntity)this.mob).getItemBySlot(EquipmentSlot.HEAD), Raid.getOminousBannerInstance(((Entity)this.mob).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))) {
                return true;
            }
            Raider leader = this.this$0.raid.getLeader(((Raider)this.mob).getWave());
            return leader != null && leader.isAlive();
        }

        @Override
        public void start() {
            ((Mob)this.mob).getNavigation().moveTo(this.pathToBanner, (double)1.15f);
        }

        @Override
        public void stop() {
            this.pathToBanner = null;
            this.pursuedBannerItemEntity = null;
        }

        @Override
        public void tick() {
            if (this.pursuedBannerItemEntity != null && this.pursuedBannerItemEntity.closerThan((Entity)this.mob, 1.414)) {
                ((Raider)this.mob).pickUpItem(ObtainRaidLeaderBannerGoal.getServerLevel(this.this$0.level()), this.pursuedBannerItemEntity);
            }
        }
    }

    private static class RaiderMoveThroughVillageGoal
    extends Goal {
        private final Raider raider;
        private final double speedModifier;
        private BlockPos poiPos;
        private final List<BlockPos> visited = Lists.newArrayList();
        private final int distanceToPoi;
        private boolean stuck;

        public RaiderMoveThroughVillageGoal(Raider mob, double speedModifier, int distanceToPoi) {
            this.raider = mob;
            this.speedModifier = speedModifier;
            this.distanceToPoi = distanceToPoi;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.updateVisited();
            return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getTarget() == null;
        }

        private boolean isValidRaid() {
            return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
        }

        private boolean hasSuitablePoi() {
            ServerLevel level = (ServerLevel)this.raider.level();
            BlockPos pos = this.raider.blockPosition();
            Optional<BlockPos> homePos = level.getPoiManager().getRandom(p -> p.is(PoiTypes.HOME), this::hasNotVisited, PoiManager.Occupancy.ANY, pos, 48, this.raider.random);
            if (homePos.isEmpty()) {
                return false;
            }
            this.poiPos = homePos.get().immutable();
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.raider.getNavigation().isDone()) {
                return false;
            }
            return this.raider.getTarget() == null && !this.poiPos.closerToCenterThan(this.raider.position(), this.raider.getBbWidth() + (float)this.distanceToPoi) && !this.stuck;
        }

        @Override
        public void stop() {
            if (this.poiPos.closerToCenterThan(this.raider.position(), this.distanceToPoi)) {
                this.visited.add(this.poiPos);
            }
        }

        @Override
        public void start() {
            super.start();
            this.raider.setNoActionTime(0);
            this.raider.getNavigation().moveTo(this.poiPos.getX(), this.poiPos.getY(), this.poiPos.getZ(), this.speedModifier);
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.raider.getNavigation().isDone()) {
                Vec3 poiVec = Vec3.atBottomCenterOf(this.poiPos);
                Vec3 nextPos = DefaultRandomPos.getPosTowards(this.raider, 16, 7, poiVec, 0.3141592741012573);
                if (nextPos == null) {
                    nextPos = DefaultRandomPos.getPosTowards(this.raider, 8, 7, poiVec, 1.5707963705062866);
                }
                if (nextPos == null) {
                    this.stuck = true;
                    return;
                }
                this.raider.getNavigation().moveTo(nextPos.x, nextPos.y, nextPos.z, this.speedModifier);
            }
        }

        private boolean hasNotVisited(BlockPos poi) {
            for (BlockPos visitedPoi : this.visited) {
                if (!Objects.equals(poi, visitedPoi)) continue;
                return false;
            }
            return true;
        }

        private void updateVisited() {
            if (this.visited.size() > 2) {
                this.visited.remove(0);
            }
        }
    }

    public class RaiderCelebration
    extends Goal {
        private final Raider mob;
        final /* synthetic */ Raider this$0;

        public RaiderCelebration(Raider this$0, Raider mob) {
            Raider raider = this$0;
            Objects.requireNonNull(raider);
            this.this$0 = raider;
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid currentRaid = this.mob.getCurrentRaid();
            return this.mob.isAlive() && this.mob.getTarget() == null && currentRaid != null && currentRaid.isLoss();
        }

        @Override
        public void start() {
            this.mob.setCelebrating(true);
            super.start();
        }

        @Override
        public void stop() {
            this.mob.setCelebrating(false);
            super.stop();
        }

        @Override
        public void tick() {
            if (!this.mob.isSilent() && this.mob.random.nextInt(this.adjustedTickDelay(100)) == 0) {
                this.this$0.makeSound(this.this$0.getCelebrateSound());
            }
            if (!this.mob.isPassenger() && this.mob.random.nextInt(this.adjustedTickDelay(50)) == 0) {
                this.mob.getJumpControl().jump();
            }
            super.tick();
        }
    }

    protected static class HoldGroundAttackGoal
    extends Goal {
        private final Raider mob;
        private final float hostileRadiusSqr;
        public final TargetingConditions shoutTargeting = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight().ignoreInvisibilityTesting();

        public HoldGroundAttackGoal(AbstractIllager mob, float hostileRadius) {
            this.mob = mob;
            this.hostileRadiusSqr = hostileRadius * hostileRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity lastHurtByMob = this.mob.getLastHurtByMob();
            return this.mob.getCurrentRaid() == null && this.mob.isPatrolling() && this.mob.getTarget() != null && !this.mob.isAggressive() && (lastHurtByMob == null || !lastHurtByMob.is(EntityTypes.PLAYER));
        }

        @Override
        public void start() {
            super.start();
            this.mob.getNavigation().stop();
            List<Raider> nearbyEntities = HoldGroundAttackGoal.getServerLevel(this.mob).getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0));
            for (Raider entity : nearbyEntities) {
                entity.setTarget(this.mob.getTarget());
            }
        }

        @Override
        public void stop() {
            super.stop();
            LivingEntity target = this.mob.getTarget();
            if (target != null) {
                List<Raider> nearbyEntities = HoldGroundAttackGoal.getServerLevel(this.mob).getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0));
                for (Raider entity : nearbyEntities) {
                    entity.setTarget(target);
                    entity.setAggressive(true);
                }
                this.mob.setAggressive(true);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            if (this.mob.distanceToSqr(target) > (double)this.hostileRadiusSqr) {
                this.mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
                if (this.mob.random.nextInt(50) == 0) {
                    this.mob.playAmbientSound();
                }
            } else {
                this.mob.setAggressive(true);
            }
            super.tick();
        }
    }
}

