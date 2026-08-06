/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster;

import com.google.common.collect.Sets;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class Strider
extends Animal
implements ItemSteerable {
    private static final Identifier SUFFOCATING_MODIFIER_ID = Identifier.withDefaultNamespace("suffocating");
    private static final AttributeModifier SUFFOCATING_MODIFIER = new AttributeModifier(SUFFOCATING_MODIFIER_ID, -0.34f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final float SUFFOCATE_STEERING_MODIFIER = 0.35f;
    private static final float STEERING_MODIFIER = 0.55f;
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SUFFOCATING = SynchedEntityData.defineId(Strider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45f, 0.85f).withEyeHeight(0.4375f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.65625f, 0.0f));
    private final ItemBasedSteering steering;
    private @Nullable TemptGoal temptGoal;

    public Strider(EntityType<? extends Strider> strider, Level level) {
        super((EntityType<? extends Animal>)strider, level);
        this.steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
        this.blocksBuilding = true;
        this.setPathfindingMalus(PathType.WATER, -1.0f);
        this.setPathfindingMalus(PathType.LAVA, 0.0f);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0f);
        this.setPathfindingMalus(PathType.FIRE, 0.0f);
    }

    public static boolean checkStriderSpawnRules(EntityType<Strider> ignoredType, LevelAccessor level, EntitySpawnReason ignoredSpawnType, BlockPos pos, RandomSource ignoredRandom) {
        BlockPos.MutableBlockPos checkPos = pos.mutable();
        do {
            checkPos.move(Direction.UP);
        } while (level.getFluidState(checkPos).is(FluidTags.LAVA));
        return level.getBlockState(checkPos).isAir();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
            this.steering.onSynced();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_BOOST_TIME, 0);
        entityData.define(DATA_SUFFOCATING, false);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.SADDLE) {
            return this.isAlive() && !this.isBaby();
        }
        return super.canUseSlot(slot);
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(slot);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        if (slot == EquipmentSlot.SADDLE) {
            return SoundEvents.STRIDER_SADDLE;
        }
        return super.getEquipSound(slot, stack, equippable);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.65));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.temptGoal = new TemptGoal(this, 1.4, i -> i.is(ItemTags.STRIDER_TEMPT_ITEMS), false);
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(4, new StriderGoToLavaGoal(this, 1.0));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0, 60));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Strider.class, 8.0f));
    }

    public void setSuffocating(boolean flag) {
        this.entityData.set(DATA_SUFFOCATING, flag);
        AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            if (flag) {
                attribute.addOrUpdateTransientModifier(SUFFOCATING_MODIFIER);
            } else {
                attribute.removeModifier(SUFFOCATING_MODIFIER_ID);
            }
        }
    }

    public boolean isSuffocating() {
        return this.entityData.get(DATA_SUFFOCATING);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        if (!this.level().isClientSide()) {
            return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
        }
        float animSpeed = Math.min(0.25f, this.walkAnimation.speed());
        float animPos = this.walkAnimation.position();
        float offset = 0.12f * Mth.cos(animPos * 1.5f) * 2.0f * animSpeed;
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale).add(0.0, offset * scale, 0.0);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Player player;
        Entity entity;
        if (this.isSaddled() && (entity = this.getFirstPassenger()) instanceof Player && (player = (Player)entity).isHolding(Items.WARPED_FUNGUS_ON_A_STICK)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3[] directions = new Vec3[]{Strider.getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot()), Strider.getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() - 22.5f), Strider.getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() + 22.5f), Strider.getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() - 45.0f), Strider.getCollisionHorizontalEscapeVector(this.getBbWidth(), passenger.getBbWidth(), passenger.getYRot() + 45.0f)};
        LinkedHashSet targetBlockPositions = Sets.newLinkedHashSet();
        double colliderTop = this.getBoundingBox().maxY;
        double colliderBottom = this.getBoundingBox().minY - 0.5;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (Vec3 direction : directions) {
            blockPos.set(this.getX() + direction.x, colliderTop, this.getZ() + direction.z);
            for (double y = colliderTop; y > colliderBottom; y -= 1.0) {
                targetBlockPositions.add(blockPos.immutable());
                blockPos.move(Direction.DOWN);
            }
        }
        for (BlockPos targetBlockPos : targetBlockPositions) {
            double blockFloorHeight;
            if (this.level().getFluidState(targetBlockPos).is(FluidTags.LAVA) || !DismountHelper.isBlockFloorValid(blockFloorHeight = this.level().getBlockFloorHeight(targetBlockPos))) continue;
            Vec3 location = Vec3.upFromBottomCenterOf(targetBlockPos, blockFloorHeight);
            for (Pose dismountPose : passenger.getDismountPoses()) {
                AABB poseCollisionBox = passenger.getLocalBoundsForPose(dismountPose);
                if (!DismountHelper.canDismountTo(this.level(), passenger, poseCollisionBox.move(location))) continue;
                passenger.setPose(dismountPose);
                return location;
            }
        }
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5f);
        this.yBodyRot = this.yHeadRot = this.getYRot();
        this.yRotO = this.yHeadRot;
        this.steering.tickBoost();
        super.tickRidden(controller, riddenInput);
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)(this.isSuffocating() ? 0.35f : 0.55f) * (double)this.steering.boostFactor());
    }

    @Override
    protected float nextStep() {
        return this.moveDist + 0.6f;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(this.isInLava() ? SoundEvents.STRIDER_STEP_LAVA : SoundEvents.STRIDER_STEP, 1.0f, 1.0f);
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (this.isInLava()) {
            this.resetFallDistance();
            return;
        }
        super.checkFallDamage(ya, onGround, onState, pos);
    }

    @Override
    public void tick() {
        if (this.isBeingTempted() && this.random.nextInt(140) == 0) {
            this.makeSound(SoundEvents.STRIDER_HAPPY);
        } else if (this.isPanicking() && this.random.nextInt(60) == 0) {
            this.makeSound(SoundEvents.STRIDER_RETREAT);
        }
        if (!this.isNoAi()) {
            Strider strider;
            BlockState stateInside = this.level().getBlockState(this.blockPosition());
            BlockState stateOn = this.getBlockStateOnLegacy();
            boolean inWarmBlocks = stateInside.is(BlockTags.STRIDER_WARM_BLOCKS) || stateOn.is(BlockTags.STRIDER_WARM_BLOCKS) || this.getFluidHeight(FluidTags.LAVA) > 0.0;
            Entity entity = this.getVehicle();
            boolean onWarmStrider = entity instanceof Strider && !(strider = (Strider)entity).isSuffocating();
            this.setSuffocating(!inWarmBlocks && !onWarmStrider);
        }
        super.tick();
        this.floatStrider();
    }

    private boolean isBeingTempted() {
        return this.temptGoal != null && this.temptGoal.isRunning();
    }

    @Override
    protected boolean shouldPassengersInheritMalus() {
        return true;
    }

    private void floatStrider() {
        if (this.isInLava()) {
            CollisionContext context = CollisionContext.of(this);
            if (!context.isAbove(this.getLiquidCollisionShape(), this.blockPosition(), true) || this.level().getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
            } else {
                this.setOnGround(true);
            }
        }
    }

    @Override
    public VoxelShape getLiquidCollisionShape() {
        return Block.column(16.0, 0.0, 8.0);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.175f);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (this.isPanicking() || this.isBeingTempted()) {
            return null;
        }
        return SoundEvents.STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !this.isVehicle() && !this.isEyeInFluid(FluidTags.LAVA);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new StriderPathNavigation(this, level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos).getFluidState().is(FluidTags.LAVA)) {
            return 10.0f;
        }
        return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0f;
    }

    @Override
    public @Nullable Strider getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return EntityTypes.STRIDER.create((Level)level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ItemTags.STRIDER_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean hasFood = this.isFood(player.getItemInHand(hand));
        if (!hasFood && this.isSaddled() && !this.isVehicle() && !player.isSecondaryUseActive()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult interactionResult = super.mobInteract(player, hand);
        if (!interactionResult.consumesAction()) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (this.isEquippableInSlot(itemStack, EquipmentSlot.SADDLE)) {
                return itemStack.interactLivingEntity(player, this, hand);
            }
            return InteractionResult.PASS;
        }
        if (hasFood && !this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.STRIDER_EAT, this.getSoundSource(), 1.0f, 1.0f + (this.random.nextFloat() - this.random.nextFloat()) * 0.2f);
        }
        return interactionResult;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6f * this.getEyeHeight(), this.getBbWidth() * 0.4f);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        if (this.isBaby()) {
            return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(30) == 0) {
            Mob jockey = EntityTypes.ZOMBIFIED_PIGLIN.create((Level)level.getLevel(), EntitySpawnReason.JOCKEY);
            if (jockey != null) {
                groupData = this.spawnJockey(level, difficulty, jockey, new Zombie.ZombieGroupData(Zombie.getSpawnAsBabyOdds(random), false));
                jockey.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK));
                this.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
                this.setGuaranteedDrop(EquipmentSlot.SADDLE);
            }
        } else if (random.nextInt(10) == 0) {
            AgeableMob jockey = EntityTypes.STRIDER.create((Level)level.getLevel(), EntitySpawnReason.JOCKEY);
            if (jockey != null) {
                jockey.setAge(-24000);
                groupData = this.spawnJockey(level, difficulty, jockey, null);
            }
        } else {
            groupData = new AgeableMob.AgeableMobGroupData(0.5f);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    private SpawnGroupData spawnJockey(ServerLevelAccessor level, DifficultyInstance difficulty, Mob jockey, @Nullable SpawnGroupData jockeyGroupData) {
        jockey.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
        jockey.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, jockeyGroupData);
        jockey.startRiding(this, true, false);
        return new AgeableMob.AgeableMobGroupData(0.0f);
    }

    private static class StriderGoToLavaGoal
    extends MoveToBlockGoal {
        private final Strider strider;

        private StriderGoToLavaGoal(Strider strider, double speedModifier) {
            super(strider, speedModifier, 8, 2);
            this.strider = strider;
        }

        @Override
        public BlockPos getMoveToTarget() {
            return this.blockPos;
        }

        @Override
        public boolean canContinueToUse() {
            return !this.strider.isInLava() && this.isValidTarget(this.strider.level(), this.blockPos);
        }

        @Override
        public boolean canUse() {
            return !this.strider.isInLava() && super.canUse();
        }

        @Override
        public boolean shouldRecalculatePath() {
            return this.tryTicks % 20 == 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).is(Blocks.LAVA) && level.getBlockState(pos.above()).isPathfindable(PathComputationType.LAND);
        }
    }

    private static class StriderPathNavigation
    extends GroundPathNavigation {
        public StriderPathNavigation(Strider mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            return this.level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
        }
    }
}

