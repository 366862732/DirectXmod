/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.google.common.collect.UnmodifiableIterator
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.vehicle.minecart;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractMinecart
extends VehicleEntity {
    private static final Vec3 LOWERED_PASSENGER_ATTACHMENT = new Vec3(0.0, 0.0, 0.0);
    private static final EntityDataAccessor<Optional<BlockState>> DATA_ID_CUSTOM_DISPLAY_BLOCK = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
    private static final ImmutableMap<Pose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of((Object)Pose.STANDING, (Object)ImmutableList.of((Object)0, (Object)1, (Object)-1), (Object)Pose.CROUCHING, (Object)ImmutableList.of((Object)0, (Object)1, (Object)-1), (Object)Pose.SWIMMING, (Object)ImmutableList.of((Object)0, (Object)1));
    protected static final float WATER_SLOWDOWN_FACTOR = 0.95f;
    private static final boolean DEFAULT_FLIPPED_ROTATION = false;
    private boolean onRails;
    private boolean flipped = false;
    private final MinecartBehavior behavior;
    private static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Maps.newEnumMap((Map)((Map)Util.make(() -> {
        Vec3i xNeg = Direction.WEST.getUnitVec3i();
        Vec3i xPos = Direction.EAST.getUnitVec3i();
        Vec3i zNeg = Direction.NORTH.getUnitVec3i();
        Vec3i zPos = Direction.SOUTH.getUnitVec3i();
        Vec3i xNegBelow = xNeg.below();
        Vec3i xPosBelow = xPos.below();
        Vec3i zNegBelow = zNeg.below();
        Vec3i zPosBelow = zPos.below();
        return ImmutableMap.of((Object)RailShape.NORTH_SOUTH, (Object)Pair.of((Object)zNeg, (Object)zPos), (Object)RailShape.EAST_WEST, (Object)Pair.of((Object)xNeg, (Object)xPos), (Object)RailShape.ASCENDING_EAST, (Object)Pair.of((Object)xNegBelow, (Object)xPos), (Object)RailShape.ASCENDING_WEST, (Object)Pair.of((Object)xNeg, (Object)xPosBelow), (Object)RailShape.ASCENDING_NORTH, (Object)Pair.of((Object)zNeg, (Object)zPosBelow), (Object)RailShape.ASCENDING_SOUTH, (Object)Pair.of((Object)zNegBelow, (Object)zPos), (Object)RailShape.SOUTH_EAST, (Object)Pair.of((Object)zPos, (Object)xPos), (Object)RailShape.SOUTH_WEST, (Object)Pair.of((Object)zPos, (Object)xNeg), (Object)RailShape.NORTH_WEST, (Object)Pair.of((Object)zNeg, (Object)xNeg), (Object)RailShape.NORTH_EAST, (Object)Pair.of((Object)zNeg, (Object)xPos));
    })));

    protected AbstractMinecart(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.behavior = AbstractMinecart.useExperimentalMovement(level) ? new NewMinecartBehavior(this) : new OldMinecartBehavior(this);
    }

    protected AbstractMinecart(EntityType<?> type, Level level, double x, double y, double z) {
        this(type, level);
        this.setInitialPos(x, y, z);
    }

    public void setInitialPos(double x, double y, double z) {
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static <T extends AbstractMinecart> @Nullable T createMinecart(Level level, double x, double y, double z, EntityType<T> type, EntitySpawnReason reason, ItemStack itemStack, @Nullable Player player) {
        AbstractMinecart entity = (AbstractMinecart)type.create(level, reason);
        if (entity != null) {
            entity.setInitialPos(x, y, z);
            EntityType.createDefaultStackConfig(level, itemStack, player).apply(entity);
            MinecartBehavior minecartBehavior = entity.getBehavior();
            if (minecartBehavior instanceof NewMinecartBehavior) {
                NewMinecartBehavior newMinecartBehavior = (NewMinecartBehavior)minecartBehavior;
                BlockPos currentPos = entity.getCurrentBlockPosOrRailBelow();
                BlockState currentState = level.getBlockState(currentPos);
                newMinecartBehavior.adjustToRails(currentPos, currentState, true);
            }
        }
        return (T)entity;
    }

    public MinecartBehavior getBehavior() {
        return this.behavior;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_CUSTOM_DISPLAY_BLOCK, Optional.empty());
        entityData.define(DATA_ID_DISPLAY_OFFSET, this.getDefaultDisplayOffset());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return AbstractBoat.canVehicleCollide(this, entity);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portalArea) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portalArea));
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        boolean shouldLowerAttachmentPoint;
        boolean bl = shouldLowerAttachmentPoint = passenger instanceof Villager || passenger instanceof WanderingTrader;
        if (shouldLowerAttachmentPoint) {
            return LOWERED_PASSENGER_ATTACHMENT;
        }
        return super.getPassengerAttachmentPoint(passenger, dimensions, scale);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction forward = this.getMotionDirection();
        if (forward.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(passenger);
        }
        int[][] offsets = DismountHelper.offsetsForDirection(forward);
        BlockPos vehicleBlockPos = this.blockPosition();
        BlockPos.MutableBlockPos targetBlockPos = new BlockPos.MutableBlockPos();
        ImmutableList<Pose> dismountPoses = passenger.getDismountPoses();
        for (Pose pose : dismountPoses) {
            EntityDimensions passengerDimensions = passenger.getDimensions(pose);
            float dismountAreaReach = Math.min(passengerDimensions.width(), 1.0f) / 2.0f;
            UnmodifiableIterator unmodifiableIterator = ((ImmutableList)POSE_DISMOUNT_HEIGHTS.get((Object)pose)).iterator();
            while (unmodifiableIterator.hasNext()) {
                int offsetY = (Integer)unmodifiableIterator.next();
                for (int[] offsetXZ : offsets) {
                    targetBlockPos.set(vehicleBlockPos.getX() + offsetXZ[0], vehicleBlockPos.getY() + offsetY, vehicleBlockPos.getZ() + offsetXZ[1]);
                    double blockFloorHeight = this.level().getBlockFloorHeight(DismountHelper.nonClimbableShape(this.level(), targetBlockPos), () -> DismountHelper.nonClimbableShape(this.level(), (BlockPos)targetBlockPos.below()));
                    if (!DismountHelper.isBlockFloorValid(blockFloorHeight)) continue;
                    AABB dismountCollisionBox = new AABB(-dismountAreaReach, 0.0, -dismountAreaReach, dismountAreaReach, passengerDimensions.height(), dismountAreaReach);
                    Vec3 location = Vec3.upFromBottomCenterOf(targetBlockPos, blockFloorHeight);
                    if (!DismountHelper.canDismountTo(this.level(), passenger, dismountCollisionBox.move(location))) continue;
                    passenger.setPose(pose);
                    return location;
                }
            }
        }
        double vehicleTop = this.getBoundingBox().maxY;
        targetBlockPos.set((double)vehicleBlockPos.getX(), vehicleTop, (double)vehicleBlockPos.getZ());
        for (Pose pose : dismountPoses) {
            int blockCoverageY;
            double ceilingAboveVehicle;
            double poseHeight = passenger.getDimensions(pose).height();
            if (!(vehicleTop + poseHeight <= (ceilingAboveVehicle = DismountHelper.findCeilingFrom(targetBlockPos, blockCoverageY = Mth.ceil(vehicleTop - (double)targetBlockPos.getY() + poseHeight), pos -> this.level().getBlockState((BlockPos)pos).getCollisionShape(this.level(), (BlockPos)pos))))) continue;
            passenger.setPose(pose);
            break;
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    protected float getBlockSpeedFactor() {
        BlockState blockState = this.level().getBlockState(this.blockPosition());
        if (blockState.is(BlockTags.RAILS)) {
            return 1.0f;
        }
        return super.getBlockSpeedFactor();
    }

    @Override
    public void animateHurt(float yaw) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0f);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    public static Pair<Vec3i, Vec3i> exits(RailShape shape) {
        return EXITS.get(shape);
    }

    @Override
    public Direction getMotionDirection() {
        return this.behavior.getMotionDirection();
    }

    @Override
    protected double getDefaultGravity() {
        return this.isInWater() ? 0.005 : 0.04;
    }

    @Override
    public void tick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0f) {
            this.setDamage(this.getDamage() - 1.0f);
        }
        this.checkBelowWorld();
        this.computeSpeed();
        this.handlePortal();
        this.behavior.tick();
        this.updateFluidInteraction();
        if (this.isInLava()) {
            this.lavaIgnite();
            this.lavaHurt();
            this.fallDistance *= 0.5;
        }
        this.firstTick = false;
    }

    public boolean isFirstTick() {
        return this.firstTick;
    }

    public BlockPos getCurrentBlockPosOrRailBelow() {
        int xt = Mth.floor(this.getX());
        int yt = Mth.floor(this.getY());
        int zt = Mth.floor(this.getZ());
        if (AbstractMinecart.useExperimentalMovement(this.level())) {
            double y = this.getY() - 0.1 - (double)1.0E-5f;
            if (this.level().getBlockState(BlockPos.containing(xt, y, zt)).is(BlockTags.RAILS)) {
                yt = Mth.floor(y);
            }
        } else if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
            --yt;
        }
        return new BlockPos(xt, yt, zt);
    }

    protected double getMaxSpeed(ServerLevel level) {
        return this.behavior.getMaxSpeed(level);
    }

    public void activateMinecart(ServerLevel level, int xt, int yt, int zt, boolean state) {
    }

    @Override
    public void lerpPositionAndRotationStep(int stepsToTarget, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
        super.lerpPositionAndRotationStep(stepsToTarget, targetX, targetY, targetZ, targetYRot, targetXRot);
    }

    @Override
    public void applyGravity() {
        super.applyGravity();
    }

    @Override
    public void reapplyPosition() {
        super.reapplyPosition();
    }

    @Override
    public boolean updateFluidInteraction() {
        return super.updateFluidInteraction();
    }

    @Override
    public Vec3 getKnownMovement() {
        return this.behavior.getKnownMovement(super.getKnownMovement());
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.behavior.getInterpolation();
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.behavior.lerpMotion(this.getDeltaMovement());
    }

    @Override
    public void lerpMotion(Vec3 movement) {
        this.behavior.lerpMotion(movement);
    }

    protected void moveAlongTrack(ServerLevel level) {
        this.behavior.moveAlongTrack(level);
    }

    protected void comeOffTrack(ServerLevel level) {
        double maxSpeed = this.getMaxSpeed(level);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(movement.x, -maxSpeed, maxSpeed), movement.y, Mth.clamp(movement.z, -maxSpeed, maxSpeed));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
        }
    }

    @Override
    protected float getAirDrag() {
        return 0.95f;
    }

    protected double makeStepAlongTrack(BlockPos pos, RailShape shape, double movementLeft) {
        return this.behavior.stepAlongTrack(pos, shape, movementLeft);
    }

    @Override
    public void move(MoverType moverType, Vec3 delta) {
        if (AbstractMinecart.useExperimentalMovement(this.level())) {
            Vec3 toPosition = this.position().add(delta);
            super.move(moverType, delta);
            boolean shouldContinue = this.behavior.pushAndPickupEntities();
            if (shouldContinue) {
                super.move(moverType, toPosition.subtract(this.position()));
            }
            if (moverType.equals((Object)MoverType.PISTON)) {
                this.onRails = false;
            }
        } else {
            super.move(moverType, delta);
            this.applyEffectsFromBlocks();
        }
    }

    @Override
    public void applyEffectsFromBlocks() {
        if (AbstractMinecart.useExperimentalMovement(this.level())) {
            super.applyEffectsFromBlocks();
        } else {
            this.applyEffectsFromBlocks(this.position(), this.position());
            this.clearMovementThisTick();
        }
    }

    @Override
    public boolean isOnRails() {
        return this.onRails;
    }

    public void setOnRails(boolean onRails) {
        this.onRails = onRails;
    }

    public boolean isFlipped() {
        return this.flipped;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    public Vec3 getRedstoneDirection(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        if (!state.is(Blocks.POWERED_RAIL) || !state.getValue(PoweredRailBlock.POWERED).booleanValue()) {
            return Vec3.ZERO;
        }
        RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
        if (shape == RailShape.EAST_WEST) {
            if (this.isRedstoneConductor(pos.west())) {
                return new Vec3(1.0, 0.0, 0.0);
            }
            if (this.isRedstoneConductor(pos.east())) {
                return new Vec3(-1.0, 0.0, 0.0);
            }
        } else if (shape == RailShape.NORTH_SOUTH) {
            if (this.isRedstoneConductor(pos.north())) {
                return new Vec3(0.0, 0.0, 1.0);
            }
            if (this.isRedstoneConductor(pos.south())) {
                return new Vec3(0.0, 0.0, -1.0);
            }
        }
        return Vec3.ZERO;
    }

    public boolean isRedstoneConductor(BlockPos pos) {
        return this.level().getBlockState(pos).isRedstoneConductor(this.level(), pos);
    }

    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        double slowdownFactor = this.behavior.getSlowdownFactor();
        Vec3 newMovement = movement.multiply(slowdownFactor, 0.0, slowdownFactor);
        if (this.isInWater()) {
            newMovement = newMovement.scale(0.95f);
        }
        return newMovement;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setCustomDisplayBlockState(input.read("DisplayState", BlockState.CODEC));
        this.setDisplayOffset(input.getIntOr("DisplayOffset", this.getDefaultDisplayOffset()));
        this.flipped = input.getBooleanOr("FlippedRotation", false);
        this.firstTick = input.getBooleanOr("HasTicked", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        this.getCustomDisplayBlockState().ifPresent(blockState -> output.store("DisplayState", BlockState.CODEC, blockState));
        int displayOffset = this.getDisplayOffset();
        if (displayOffset != this.getDefaultDisplayOffset()) {
            output.putInt("DisplayOffset", displayOffset);
        }
        output.putBoolean("FlippedRotation", this.flipped);
        output.putBoolean("HasTicked", this.firstTick);
    }

    @Override
    public void push(Entity entity) {
        double za;
        if (this.level().isClientSide()) {
            return;
        }
        if (entity.noPhysics || this.noPhysics) {
            return;
        }
        if (this.hasPassenger(entity)) {
            return;
        }
        double xa = entity.getX() - this.getX();
        double dd = xa * xa + (za = entity.getZ() - this.getZ()) * za;
        if (dd >= (double)1.0E-4f) {
            dd = Math.sqrt(dd);
            xa /= dd;
            za /= dd;
            double pow = 1.0 / dd;
            if (pow > 1.0) {
                pow = 1.0;
            }
            xa *= pow;
            za *= pow;
            xa *= (double)0.1f;
            za *= (double)0.1f;
            xa *= 0.5;
            za *= 0.5;
            if (entity instanceof AbstractMinecart) {
                AbstractMinecart otherMinecart = (AbstractMinecart)entity;
                this.pushOtherMinecart(otherMinecart, xa, za);
            } else {
                this.push(-xa, 0.0, -za);
                entity.push(xa / 4.0, 0.0, za / 4.0);
            }
        }
    }

    private void pushOtherMinecart(AbstractMinecart otherMinecart, double xa, double za) {
        double zo;
        double xo;
        if (AbstractMinecart.useExperimentalMovement(this.level())) {
            xo = this.getDeltaMovement().x;
            zo = this.getDeltaMovement().z;
        } else {
            xo = otherMinecart.getX() - this.getX();
            zo = otherMinecart.getZ() - this.getZ();
        }
        Vec3 dir = new Vec3(xo, 0.0, zo).normalize();
        Vec3 facing = new Vec3(Mth.cos(this.getYRot() * ((float)Math.PI / 180)), 0.0, Mth.sin(this.getYRot() * ((float)Math.PI / 180))).normalize();
        double dot = Math.abs(dir.dot(facing));
        if (dot < (double)0.8f && !AbstractMinecart.useExperimentalMovement(this.level())) {
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        Vec3 entityMovement = otherMinecart.getDeltaMovement();
        if (otherMinecart.isFurnace() && !this.isFurnace()) {
            this.setDeltaMovement(movement.multiply(0.2, 1.0, 0.2));
            this.push(entityMovement.x - xa, 0.0, entityMovement.z - za);
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.95, 1.0, 0.95));
        } else if (!otherMinecart.isFurnace() && this.isFurnace()) {
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(movement.x + xa, 0.0, movement.z + za);
            this.setDeltaMovement(movement.multiply(0.95, 1.0, 0.95));
        } else {
            double xdd = (entityMovement.x + movement.x) / 2.0;
            double zdd = (entityMovement.z + movement.z) / 2.0;
            this.setDeltaMovement(movement.multiply(0.2, 1.0, 0.2));
            this.push(xdd - xa, 0.0, zdd - za);
            otherMinecart.setDeltaMovement(entityMovement.multiply(0.2, 1.0, 0.2));
            otherMinecart.push(xdd + xa, 0.0, zdd + za);
        }
    }

    public BlockState getDisplayBlockState() {
        return this.getCustomDisplayBlockState().orElseGet(this::getDefaultDisplayBlockState);
    }

    private Optional<BlockState> getCustomDisplayBlockState() {
        return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY_BLOCK);
    }

    public BlockState getDefaultDisplayBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    public int getDisplayOffset() {
        return this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
    }

    public int getDefaultDisplayOffset() {
        return 6;
    }

    public void setCustomDisplayBlockState(Optional<BlockState> state) {
        this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY_BLOCK, state);
    }

    public void setDisplayOffset(int offset) {
        this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, offset);
    }

    public static boolean useExperimentalMovement(Level level) {
        return level.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Override
    public abstract ItemStack getPickResult();

    public boolean isRideable() {
        return false;
    }

    public boolean isFurnace() {
        return false;
    }
}

