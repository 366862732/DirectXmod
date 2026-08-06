/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.vehicle.boat;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class AbstractBoat
extends VehicleEntity
implements Leashable {
    private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_LEFT = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_RIGHT = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ID_BUBBLE_TIME = SynchedEntityData.defineId(AbstractBoat.class, EntityDataSerializers.INT);
    public static final int PADDLE_LEFT = 0;
    public static final int PADDLE_RIGHT = 1;
    private static final int TIME_TO_EJECT = 60;
    private static final float PADDLE_SPEED = 0.3926991f;
    public static final double PADDLE_SOUND_TIME = 0.7853981852531433;
    public static final int BUBBLE_TIME = 60;
    private final float[] paddlePositions = new float[2];
    private float outOfControlTicks;
    private float deltaRotation;
    private final InterpolationHandler interpolation = new InterpolationHandler((Entity)this, 3);
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;
    private double waterLevel;
    private float landFriction;
    private Status status;
    private Status oldStatus;
    private double lastYd;
    private boolean isAboveBubbleColumn;
    private boolean bubbleColumnDirectionIsDown;
    private float bubbleMultiplier;
    private float bubbleAngle;
    private float bubbleAngleO;
    private @Nullable Leashable.LeashData leashData;
    private final Supplier<Item> dropItem;

    public AbstractBoat(EntityType<? extends AbstractBoat> type, Level level, Supplier<Item> dropItem) {
        super(type, level);
        this.dropItem = dropItem;
        this.blocksBuilding = true;
    }

    public void setInitialPos(double x, double y, double z) {
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_ID_PADDLE_LEFT, false);
        entityData.define(DATA_ID_PADDLE_RIGHT, false);
        entityData.define(DATA_ID_BUBBLE_TIME, 0);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return AbstractBoat.canVehicleCollide(this, entity);
    }

    public static boolean canVehicleCollide(Entity vehicle, Entity entity) {
        return (entity.canBeCollidedWith(vehicle) || entity.isPushable()) && !vehicle.isPassengerOfSameVehicle(entity);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portalArea) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portalArea));
    }

    protected abstract double rideHeight(EntityDimensions var1);

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        float offset = this.getSinglePassengerXOffset();
        if (this.getPassengers().size() > 1) {
            int index = this.getPassengers().indexOf(passenger);
            offset = index == 0 ? 0.2f : -0.6f;
            if (passenger instanceof Animal) {
                offset += 0.2f;
            }
        }
        return new Vec3(0.0, this.rideHeight(dimensions), offset).yRot(-this.getYRot() * ((float)Math.PI / 180));
    }

    @Override
    public void onAboveBubbleColumn(boolean dragDown, BlockPos pos) {
        if (this.level() instanceof ServerLevel) {
            this.isAboveBubbleColumn = true;
            this.bubbleColumnDirectionIsDown = dragDown;
            if (this.getBubbleTime() == 0) {
                this.setBubbleTime(60);
            }
        }
        if (!this.isUnderWater() && this.random.nextInt(100) == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0f, 0.8f + 0.4f * this.random.nextFloat(), false);
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + (double)this.random.nextFloat(), this.getY() + 0.7, this.getZ() + (double)this.random.nextFloat(), 0.0, 0.0, 0.0);
            this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
        }
    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof AbstractBoat) {
            if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(entity);
            }
        } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(entity);
        }
    }

    @Override
    public void animateHurt(float yaw) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0f);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public Direction getMotionDirection() {
        return this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        this.oldStatus = this.status;
        this.status = this.getStatus();
        this.outOfControlTicks = this.status == Status.UNDER_WATER || this.status == Status.UNDER_FLOWING_WATER ? (this.outOfControlTicks += 1.0f) : 0.0f;
        if (!this.level().isClientSide() && this.outOfControlTicks >= 60.0f) {
            this.ejectPassengers();
        }
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0f) {
            this.setDamage(this.getDamage() - 1.0f);
        }
        super.tick();
        this.interpolation.interpolate();
        if (this.isLocalInstanceAuthoritative()) {
            if (!(this.getFirstPassenger() instanceof Player)) {
                this.setPaddleState(false, false);
            }
            this.floatBoat();
            if (this.level().isClientSide()) {
                this.controlBoat();
                this.level().sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
        this.applyEffectsFromBlocks();
        this.applyEffectsFromBlocks();
        this.tickBubbleColumn();
        for (int i = 0; i <= 1; ++i) {
            if (this.getPaddleState(i)) {
                SoundEvent sound;
                if (!this.isSilent() && (double)(this.paddlePositions[i] % ((float)Math.PI * 2)) <= 0.7853981852531433 && (double)((this.paddlePositions[i] + 0.3926991f) % ((float)Math.PI * 2)) >= 0.7853981852531433 && (sound = this.getPaddleSound()) != null) {
                    Vec3 viewVector = this.getViewVector(1.0f);
                    double dx = i == 1 ? -viewVector.z : viewVector.z;
                    double dz = i == 1 ? viewVector.x : -viewVector.x;
                    this.level().playSound(null, this.getX() + dx, this.getY(), this.getZ() + dz, sound, this.getSoundSource(), 1.0f, 0.8f + 0.4f * this.random.nextFloat());
                }
                int n = i;
                this.paddlePositions[n] = this.paddlePositions[n] + 0.3926991f;
                continue;
            }
            this.paddlePositions[i] = 0.0f;
        }
        List<Entity> entities = this.level().getEntities(this, this.getBoundingBox().inflate(0.2f, -0.01f, 0.2f), EntitySelector.pushableBy(this));
        if (!entities.isEmpty()) {
            boolean addNewPassengers = !this.level().isClientSide() && !(this.getControllingPassenger() instanceof Player);
            for (Entity entity : entities) {
                if (entity.hasPassenger(this)) continue;
                if (addNewPassengers && this.getPassengers().size() < this.getMaxPassengers() && !entity.isPassenger() && this.hasEnoughSpaceFor(entity) && entity instanceof LivingEntity && !entity.is(EntityTypeTags.CANNOT_BE_PUSHED_ONTO_BOATS)) {
                    entity.startRiding(this);
                    continue;
                }
                this.push(entity);
            }
        }
    }

    private void tickBubbleColumn() {
        if (this.level().isClientSide()) {
            int clientBubbleTime = this.getBubbleTime();
            this.bubbleMultiplier = clientBubbleTime > 0 ? (this.bubbleMultiplier += 0.05f) : (this.bubbleMultiplier -= 0.1f);
            this.bubbleMultiplier = Mth.clamp(this.bubbleMultiplier, 0.0f, 1.0f);
            this.bubbleAngleO = this.bubbleAngle;
            this.bubbleAngle = 10.0f * (float)Math.sin(0.5 * (double)this.tickCount) * this.bubbleMultiplier;
        } else {
            int bubbleTime;
            if (!this.isAboveBubbleColumn) {
                this.setBubbleTime(0);
            }
            if ((bubbleTime = this.getBubbleTime()) > 0) {
                this.setBubbleTime(--bubbleTime);
                int diff = 60 - bubbleTime - 1;
                if (diff > 0 && bubbleTime == 0) {
                    this.setBubbleTime(0);
                    Vec3 movement = this.getDeltaMovement();
                    if (this.bubbleColumnDirectionIsDown) {
                        this.setDeltaMovement(movement.add(0.0, -0.7, 0.0));
                        this.ejectPassengers();
                    } else {
                        this.setDeltaMovement(movement.x, this.hasPassenger((Entity e) -> e instanceof Player) ? 2.7 : 0.6, movement.z);
                    }
                }
                this.isAboveBubbleColumn = false;
            }
        }
    }

    protected @Nullable SoundEvent getPaddleSound() {
        return switch (this.getStatus().ordinal()) {
            case 0, 1, 2 -> SoundEvents.BOAT_PADDLE_WATER;
            case 3 -> SoundEvents.BOAT_PADDLE_LAND;
            default -> null;
        };
    }

    public void setPaddleState(boolean left, boolean right) {
        this.entityData.set(DATA_ID_PADDLE_LEFT, left);
        this.entityData.set(DATA_ID_PADDLE_RIGHT, right);
    }

    public float getRowingTime(int side, float a) {
        if (this.getPaddleState(side)) {
            return Mth.clampedLerp(a, this.paddlePositions[side] - 0.3926991f, this.paddlePositions[side]);
        }
        return 0.0f;
    }

    @Override
    public @Nullable Leashable.LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.LeashData leashData) {
        this.leashData = leashData;
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.88f * this.getBbHeight(), 0.64f * this.getBbWidth());
    }

    @Override
    public boolean supportQuadLeash() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashOffsets() {
        return Leashable.createQuadLeashOffsets(this, 0.0, 0.64, 0.382, 0.88);
    }

    private Status getStatus() {
        Status waterStatus = this.isUnderwater();
        if (waterStatus != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return waterStatus;
        }
        if (this.checkInWater()) {
            return Status.IN_WATER;
        }
        float friction = this.getGroundFriction();
        if (friction > 0.0f) {
            this.landFriction = friction;
            return Status.ON_LAND;
        }
        return Status.IN_AIR;
    }

    public float getWaterLevelAbove() {
        AABB aabb = this.getBoundingBox();
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.maxY);
        int maxY = Mth.ceil(aabb.maxY - this.lastYd);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        block0: for (int y = minY; y < maxY; ++y) {
            float blockHeight = 0.0f;
            for (int x = minX; x < maxX; ++x) {
                for (int z = minZ; z < maxZ; ++z) {
                    pos.set(x, y, z);
                    FluidState fluidState = this.level().getFluidState(pos);
                    if (fluidState.is(FluidTags.WATER)) {
                        blockHeight = Math.max(blockHeight, fluidState.getHeight(this.level(), pos));
                    }
                    if (blockHeight >= 1.0f) continue block0;
                }
            }
            if (!(blockHeight < 1.0f)) continue;
            return (float)pos.getY() + blockHeight;
        }
        return maxY + 1;
    }

    public float getGroundFriction() {
        AABB bb = this.getBoundingBox();
        AABB box = new AABB(bb.minX, bb.minY - 0.001, bb.minZ, bb.maxX, bb.minY, bb.maxZ);
        int x0 = Mth.floor(box.minX) - 1;
        int x1 = Mth.ceil(box.maxX) + 1;
        int y0 = Mth.floor(box.minY) - 1;
        int y1 = Mth.ceil(box.maxY) + 1;
        int z0 = Mth.floor(box.minZ) - 1;
        int z1 = Mth.ceil(box.maxZ) + 1;
        VoxelShape boatShape = Shapes.create(box);
        float friction = 0.0f;
        int count = 0;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x1; ++x) {
            for (int z = z0; z < z1; ++z) {
                int edges = (x == x0 || x == x1 - 1 ? 1 : 0) + (z == z0 || z == z1 - 1 ? 1 : 0);
                if (edges == 2) continue;
                for (int y = y0; y < y1; ++y) {
                    if (edges > 0 && (y == y0 || y == y1 - 1)) continue;
                    blockPos.set(x, y, z);
                    BlockState blockState = this.level().getBlockState(blockPos);
                    if (blockState.getBlock() instanceof LilyPadBlock || !Shapes.joinIsNotEmpty(blockState.getCollisionShape(this.level(), blockPos).move(blockPos), boatShape, BooleanOp.AND)) continue;
                    friction += blockState.getBlock().getFriction();
                    ++count;
                }
            }
        }
        return friction / (float)count;
    }

    private boolean checkInWater() {
        AABB bb = this.getBoundingBox();
        int minX = Mth.floor(bb.minX);
        int maxX = Mth.ceil(bb.maxX);
        int minY = Mth.floor(bb.minY);
        int maxY = Mth.ceil(bb.minY + 0.001);
        int minZ = Mth.floor(bb.minZ);
        int maxZ = Mth.ceil(bb.maxZ);
        boolean inWater = false;
        this.waterLevel = -1.7976931348623157E308;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    pos.set(x, y, z);
                    FluidState fluidState = this.level().getFluidState(pos);
                    if (!fluidState.is(FluidTags.WATER)) continue;
                    float height = (float)y + fluidState.getHeight(this.level(), pos);
                    this.waterLevel = Math.max((double)height, this.waterLevel);
                    inWater |= bb.minY < (double)height;
                }
            }
        }
        return inWater;
    }

    private @Nullable Status isUnderwater() {
        AABB aabb = this.getBoundingBox();
        double maxY = aabb.maxY + 0.001;
        int x0 = Mth.floor(aabb.minX);
        int x1 = Mth.ceil(aabb.maxX);
        int y0 = Mth.floor(aabb.maxY);
        int y1 = Mth.ceil(maxY);
        int z0 = Mth.floor(aabb.minZ);
        int z1 = Mth.ceil(aabb.maxZ);
        boolean underWater = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x1; ++x) {
            for (int y = y0; y < y1; ++y) {
                for (int z = z0; z < z1; ++z) {
                    pos.set(x, y, z);
                    FluidState fluidState = this.level().getFluidState(pos);
                    if (!fluidState.is(FluidTags.WATER) || !(maxY < (double)((float)pos.getY() + fluidState.getHeight(this.level(), pos)))) continue;
                    if (fluidState.isSource()) {
                        underWater = true;
                        continue;
                    }
                    return Status.UNDER_FLOWING_WATER;
                }
            }
        }
        return underWater ? Status.UNDER_WATER : null;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    private void floatBoat() {
        double vspeed = -this.getGravity();
        double buoyancy = 0.0;
        float invFriction = 0.05f;
        if (this.oldStatus == Status.IN_AIR && this.status != Status.IN_AIR && this.status != Status.ON_LAND) {
            this.waterLevel = this.getY(1.0);
            double targetY = (double)(this.getWaterLevelAbove() - this.getBbHeight()) + 0.101;
            if (this.level().noCollision(this, this.getBoundingBox().move(0.0, targetY - this.getY(), 0.0))) {
                this.setPos(this.getX(), targetY, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                this.lastYd = 0.0;
            }
            this.status = Status.IN_WATER;
        } else {
            if (this.status == Status.IN_WATER) {
                buoyancy = (this.waterLevel - this.getY()) / (double)this.getBbHeight();
                invFriction = 0.9f;
            } else if (this.status == Status.UNDER_FLOWING_WATER) {
                vspeed = -7.0E-4;
                invFriction = 0.9f;
            } else if (this.status == Status.UNDER_WATER) {
                buoyancy = 0.01f;
                invFriction = 0.45f;
            } else if (this.status == Status.IN_AIR) {
                invFriction = 0.9f;
            } else if (this.status == Status.ON_LAND) {
                invFriction = this.landFriction;
                if (this.getControllingPassenger() instanceof Player) {
                    this.landFriction /= 2.0f;
                }
            }
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * (double)invFriction, movement.y + vspeed, movement.z * (double)invFriction);
            this.deltaRotation *= invFriction;
            if (buoyancy > 0.0) {
                Vec3 deltaMovement = this.getDeltaMovement();
                this.setDeltaMovement(deltaMovement.x, (deltaMovement.y + buoyancy * (this.getDefaultGravity() / 0.65)) * 0.75, deltaMovement.z);
            }
        }
    }

    @Override
    protected float getAirDrag() {
        return 1.0f;
    }

    private void controlBoat() {
        if (!this.isVehicle()) {
            return;
        }
        float acceleration = 0.0f;
        if (this.inputLeft) {
            this.deltaRotation -= 1.0f;
        }
        if (this.inputRight) {
            this.deltaRotation += 1.0f;
        }
        if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
            acceleration += 0.005f;
        }
        this.setYRot(this.getYRot() + this.deltaRotation);
        if (this.inputUp) {
            acceleration += 0.04f;
        }
        if (this.inputDown) {
            acceleration -= 0.005f;
        }
        this.setDeltaMovement(this.getDeltaMovement().add(Mth.sin(-this.getYRot() * ((float)Math.PI / 180)) * acceleration, 0.0, Mth.cos(this.getYRot() * ((float)Math.PI / 180)) * acceleration));
        this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
    }

    protected float getSinglePassengerXOffset() {
        return 0.0f;
    }

    public boolean hasEnoughSpaceFor(Entity entity) {
        return entity.getBbWidth() < this.getBbWidth();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger.is(EntityTypeTags.CAN_TURN_IN_BOATS)) {
            return;
        }
        passenger.setYRot(passenger.getYRot() + this.deltaRotation);
        passenger.setYHeadRot(passenger.getYHeadRot() + this.deltaRotation);
        this.clampRotation(passenger);
        if (passenger instanceof Animal) {
            Animal animal = (Animal)passenger;
            if (this.getPassengers().size() == this.getMaxPassengers()) {
                int rotationOffset = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(animal.yBodyRot + (float)rotationOffset);
                passenger.setYHeadRot(passenger.getYHeadRot() + (float)rotationOffset);
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 direction = AbstractBoat.getCollisionHorizontalEscapeVector(this.getBbWidth() * Mth.SQRT_OF_TWO, passenger.getBbWidth(), passenger.getYRot());
        double targetX = this.getX() + direction.x;
        double targetZ = this.getZ() + direction.z;
        BlockPos targetBlockPos = BlockPos.containing(targetX, this.getBoundingBox().maxY, targetZ);
        BlockPos belowBlockPos = targetBlockPos.below();
        if (!this.level().isWaterAt(belowBlockPos)) {
            double belowFloor;
            ArrayList targets = Lists.newArrayList();
            double targetFloor = this.level().getBlockFloorHeight(targetBlockPos);
            if (DismountHelper.isBlockFloorValid(targetFloor)) {
                targets.add(new Vec3(targetX, (double)targetBlockPos.getY() + targetFloor, targetZ));
            }
            if (DismountHelper.isBlockFloorValid(belowFloor = this.level().getBlockFloorHeight(belowBlockPos))) {
                targets.add(new Vec3(targetX, (double)belowBlockPos.getY() + belowFloor, targetZ));
            }
            for (Pose dismountPose : passenger.getDismountPoses()) {
                for (Vec3 target : targets) {
                    if (!DismountHelper.canDismountTo(this.level(), target, passenger, dismountPose)) continue;
                    passenger.setPose(dismountPose);
                    return target;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    protected void clampRotation(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float delta = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float targetDelta = Mth.clamp(delta, -105.0f, 105.0f);
        passenger.yRotO += targetDelta - delta;
        passenger.setYRot(passenger.getYRot() + targetDelta - delta);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        this.clampRotation(passenger);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        this.writeLeashData(output, this.leashData);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.readLeashData(input);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult superInteraction = super.interact(player, hand, location);
        if (superInteraction != InteractionResult.PASS) {
            return superInteraction;
        }
        if (!player.isSecondaryUseActive() && this.outOfControlTicks < 60.0f && (this.level().isClientSide() || player.startRiding(this))) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide() && reason.shouldDestroy() && this.isLeashed()) {
            this.dropLeash();
        }
        super.remove(reason);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        this.lastYd = this.getDeltaMovement().y;
        if (this.isPassenger()) {
            return;
        }
        if (onGround) {
            this.resetFallDistance();
        } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && ya < 0.0) {
            this.fallDistance -= (double)((float)ya);
        }
    }

    public boolean getPaddleState(int side) {
        return this.entityData.get(side == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) != false && this.getControllingPassenger() != null;
    }

    private void setBubbleTime(int val) {
        this.entityData.set(DATA_ID_BUBBLE_TIME, val);
    }

    private int getBubbleTime() {
        return this.entityData.get(DATA_ID_BUBBLE_TIME);
    }

    public float getBubbleAngle(float a) {
        return Mth.lerp(a, this.bubbleAngleO, this.bubbleAngle);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(FluidTags.WATER);
    }

    protected int getMaxPassengers() {
        return 2;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        LivingEntity passenger;
        Entity entity = this.getFirstPassenger();
        return entity instanceof LivingEntity ? (passenger = (LivingEntity)entity) : super.getControllingPassenger();
    }

    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        this.inputLeft = left;
        this.inputRight = right;
        this.inputUp = up;
        this.inputDown = down;
    }

    @Override
    public boolean isUnderWater() {
        return this.status == Status.UNDER_WATER || this.status == Status.UNDER_FLOWING_WATER;
    }

    @Override
    protected final Item getDropItem() {
        return this.dropItem.get();
    }

    @Override
    public final ItemStack getPickResult() {
        return new ItemStack(this.dropItem.get());
    }

    @Override
    protected @Nullable AABB modifyPassengerFluidInteractionBox(AABB passengerBox) {
        if (this.isUnderWater()) {
            return passengerBox;
        }
        AABB boatBox = this.getBoundingBox();
        if (boatBox.maxY >= passengerBox.maxY) {
            return null;
        }
        double minY = Math.max(passengerBox.minY, boatBox.maxY);
        return new AABB(passengerBox.minX, minY, passengerBox.minZ, passengerBox.maxX, passengerBox.maxY, passengerBox.maxZ);
    }

    public static enum Status {
        IN_WATER,
        UNDER_WATER,
        UNDER_FLOWING_WATER,
        ON_LAND,
        IN_AIR;

    }
}

