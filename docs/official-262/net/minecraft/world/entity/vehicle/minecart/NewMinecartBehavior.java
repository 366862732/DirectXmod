/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.vehicle.minecart;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class NewMinecartBehavior
extends MinecartBehavior {
    public static final int POS_ROT_LERP_TICKS = 3;
    public static final double ON_RAIL_Y_OFFSET = 0.1;
    public static final double OPPOSING_SLOPES_REST_AT_SPEED_THRESHOLD = 0.005;
    private @Nullable StepPartialTicks cacheIndexAlpha;
    private int cachedLerpDelay;
    private float cachedPartialTick;
    private int lerpDelay = 0;
    public final List<MinecartStep> lerpSteps = new LinkedList<MinecartStep>();
    public final List<MinecartStep> currentLerpSteps = new LinkedList<MinecartStep>();
    public double currentLerpStepsTotalWeight = 0.0;
    public MinecartStep oldLerp = MinecartStep.ZERO;

    public NewMinecartBehavior(AbstractMinecart minecart) {
        super(minecart);
    }

    @Override
    public void tick() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            this.lerpClientPositionAndRotation();
            boolean onRails = BaseRailBlock.isRail(this.level().getBlockState(this.minecart.getCurrentBlockPosOrRailBelow()));
            this.minecart.setOnRails(onRails);
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        BlockPos pos = this.minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(pos);
        if (this.minecart.isFirstTick()) {
            this.minecart.setOnRails(BaseRailBlock.isRail(state));
            this.adjustToRails(pos, state, true);
        }
        this.minecart.applyGravity();
        this.minecart.moveAlongTrack(serverLevel);
    }

    private void lerpClientPositionAndRotation() {
        if (--this.lerpDelay <= 0) {
            this.setOldLerpValues();
            this.currentLerpSteps.clear();
            if (!this.lerpSteps.isEmpty()) {
                this.currentLerpSteps.addAll(this.lerpSteps);
                this.lerpSteps.clear();
                this.currentLerpStepsTotalWeight = 0.0;
                for (MinecartStep minecartStep : this.currentLerpSteps) {
                    this.currentLerpStepsTotalWeight += (double)minecartStep.weight;
                }
                int n = this.lerpDelay = this.currentLerpStepsTotalWeight == 0.0 ? 0 : 3;
            }
        }
        if (this.cartHasPosRotLerp()) {
            this.setPos(this.getCartLerpPosition(1.0f));
            this.setDeltaMovement(this.getCartLerpMovements(1.0f));
            this.setXRot(this.getCartLerpXRot(1.0f));
            this.setYRot(this.getCartLerpYRot(1.0f));
        }
    }

    public void setOldLerpValues() {
        this.oldLerp = new MinecartStep(this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), 0.0f);
    }

    public boolean cartHasPosRotLerp() {
        return !this.currentLerpSteps.isEmpty();
    }

    public float getCartLerpXRot(float partialTicks) {
        StepPartialTicks currentStepPartialTicks = this.getCurrentLerpStep(partialTicks);
        return Mth.rotLerp(currentStepPartialTicks.partialTicksInStep, currentStepPartialTicks.previousStep.xRot, currentStepPartialTicks.currentStep.xRot);
    }

    public float getCartLerpYRot(float partialTicks) {
        StepPartialTicks currentStepPartialTicks = this.getCurrentLerpStep(partialTicks);
        return Mth.rotLerp(currentStepPartialTicks.partialTicksInStep, currentStepPartialTicks.previousStep.yRot, currentStepPartialTicks.currentStep.yRot);
    }

    public Vec3 getCartLerpPosition(float partialTicks) {
        StepPartialTicks currentStepPartialTicks = this.getCurrentLerpStep(partialTicks);
        return Mth.lerp((double)currentStepPartialTicks.partialTicksInStep, currentStepPartialTicks.previousStep.position, currentStepPartialTicks.currentStep.position);
    }

    public Vec3 getCartLerpMovements(float partialTicks) {
        StepPartialTicks currentStepPartialTicks = this.getCurrentLerpStep(partialTicks);
        return Mth.lerp((double)currentStepPartialTicks.partialTicksInStep, currentStepPartialTicks.previousStep.movement, currentStepPartialTicks.currentStep.movement);
    }

    private StepPartialTicks getCurrentLerpStep(float partialTick) {
        int index;
        if (partialTick == this.cachedPartialTick && this.lerpDelay == this.cachedLerpDelay && this.cacheIndexAlpha != null) {
            return this.cacheIndexAlpha;
        }
        float alpha = ((float)(3 - this.lerpDelay) + partialTick) / 3.0f;
        float countUp = 0.0f;
        float indexedPartialTick = 1.0f;
        boolean foundIndex = false;
        for (index = 0; index < this.currentLerpSteps.size(); ++index) {
            float weight = this.currentLerpSteps.get((int)index).weight;
            if (weight <= 0.0f || !((double)(countUp += weight) >= this.currentLerpStepsTotalWeight * (double)alpha)) continue;
            float current = countUp - weight;
            indexedPartialTick = (float)(((double)alpha * this.currentLerpStepsTotalWeight - (double)current) / (double)weight);
            foundIndex = true;
            break;
        }
        if (!foundIndex) {
            index = this.currentLerpSteps.size() - 1;
        }
        MinecartStep currentStep = this.currentLerpSteps.get(index);
        MinecartStep previousStep = index > 0 ? this.currentLerpSteps.get(index - 1) : this.oldLerp;
        this.cacheIndexAlpha = new StepPartialTicks(indexedPartialTick, currentStep, previousStep);
        this.cachedLerpDelay = this.lerpDelay;
        this.cachedPartialTick = partialTick;
        return this.cacheIndexAlpha;
    }

    public void adjustToRails(BlockPos targetBlockPos, BlockState currentState, boolean instant) {
        boolean inHill;
        Vec3 targetPosition;
        boolean inCorner;
        if (!BaseRailBlock.isRail(currentState)) {
            return;
        }
        RailShape shape = currentState.getValue(((BaseRailBlock)currentState.getBlock()).getShapeProperty());
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3 exit0 = new Vec3((Vec3i)exits.getFirst()).scale(0.5);
        Vec3 exit1 = new Vec3((Vec3i)exits.getSecond()).scale(0.5);
        Vec3 horizontalOutDirection = exit0.horizontal();
        Vec3 horizontalInDirection = exit1.horizontal();
        if (this.getDeltaMovement().length() > (double)1.0E-5f && this.getDeltaMovement().dot(horizontalOutDirection) < this.getDeltaMovement().dot(horizontalInDirection) || this.isDecending(horizontalInDirection, shape)) {
            Vec3 swap = horizontalOutDirection;
            horizontalOutDirection = horizontalInDirection;
            horizontalInDirection = swap;
        }
        float yRot = 180.0f - (float)(Math.atan2(horizontalOutDirection.z, horizontalOutDirection.x) * 180.0 / Math.PI);
        yRot += this.minecart.isFlipped() ? 180.0f : 0.0f;
        Vec3 previousPosition = this.position();
        boolean bl = inCorner = exit0.x() != exit1.x() && exit0.z() != exit1.z();
        if (inCorner) {
            Vec3 from0to1 = exit1.subtract(exit0);
            Vec3 from0toPos = previousPosition.subtract(Vec3.atBottomCenterOf(targetBlockPos)).subtract(exit0);
            Vec3 travelVectorFrom0 = from0to1.scale(from0to1.dot(from0toPos) / from0to1.dot(from0to1));
            targetPosition = Vec3.atBottomCenterOf(targetBlockPos).add(exit0).add(travelVectorFrom0);
            yRot = 180.0f - (float)(Math.atan2(travelVectorFrom0.z, travelVectorFrom0.x) * 180.0 / Math.PI);
            yRot += this.minecart.isFlipped() ? 180.0f : 0.0f;
        } else {
            boolean zSnap = exit0.subtract((Vec3)exit1).x != 0.0;
            boolean xSnap = exit0.subtract((Vec3)exit1).z != 0.0;
            targetPosition = new Vec3(xSnap ? Vec3.atCenterOf((Vec3i)targetBlockPos).x : previousPosition.x, targetBlockPos.getY(), zSnap ? Vec3.atCenterOf((Vec3i)targetBlockPos).z : previousPosition.z);
        }
        Vec3 diffFromBlock = targetPosition.subtract(previousPosition);
        this.setPos(previousPosition.add(diffFromBlock));
        float xRot = 0.0f;
        boolean bl2 = inHill = exit0.y() != exit1.y();
        if (inHill) {
            Vec3 inPosition = Vec3.atBottomCenterOf(targetBlockPos).add(horizontalInDirection);
            double horizontalDistanceFromIn = inPosition.distanceTo(this.position());
            this.setPos(this.position().add(0.0, horizontalDistanceFromIn + 0.1, 0.0));
            xRot = this.minecart.isFlipped() ? 45.0f : -45.0f;
        } else {
            this.setPos(this.position().add(0.0, 0.1, 0.0));
        }
        this.setRotation(yRot, xRot);
        double adjustDistance = previousPosition.distanceTo(this.position());
        if (adjustDistance > 0.0) {
            this.lerpSteps.add(new MinecartStep(this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), instant ? 0.0f : (float)adjustDistance));
        }
    }

    private void setRotation(float yRot, float xRot) {
        double yRotDiff = Math.abs(yRot - this.getYRot());
        if (yRotDiff >= 175.0 && yRotDiff <= 185.0) {
            this.minecart.setFlipped(!this.minecart.isFlipped());
            yRot -= 180.0f;
            xRot *= -1.0f;
        }
        xRot = Math.clamp(xRot, -45.0f, 45.0f);
        this.setXRot(xRot % 360.0f);
        this.setYRot(yRot % 360.0f);
    }

    @Override
    public void moveAlongTrack(ServerLevel level) {
        TrackIteration trackIteration = new TrackIteration();
        while (trackIteration.shouldIterate() && this.minecart.isAlive()) {
            Vec3 initialStepDeltaMovement = this.getDeltaMovement();
            BlockPos currentPos = this.minecart.getCurrentBlockPosOrRailBelow();
            BlockState currentState = this.level().getBlockState(currentPos);
            boolean onRails = BaseRailBlock.isRail(currentState);
            if (this.minecart.isOnRails() != onRails) {
                this.minecart.setOnRails(onRails);
                this.adjustToRails(currentPos, currentState, false);
            }
            if (onRails) {
                this.minecart.resetFallDistance();
                this.minecart.setOldPosAndRot();
                if (currentState.is(Blocks.ACTIVATOR_RAIL)) {
                    this.minecart.activateMinecart(level, currentPos.getX(), currentPos.getY(), currentPos.getZ(), currentState.getValue(PoweredRailBlock.POWERED));
                }
                RailShape shape = currentState.getValue(((BaseRailBlock)currentState.getBlock()).getShapeProperty());
                Vec3 newDeltaMovement = this.calculateTrackSpeed(level, initialStepDeltaMovement.horizontal(), trackIteration, currentPos, currentState, shape);
                trackIteration.movementLeft = trackIteration.firstIteration ? newDeltaMovement.horizontalDistance() : (trackIteration.movementLeft += newDeltaMovement.horizontalDistance() - initialStepDeltaMovement.horizontalDistance());
                this.setDeltaMovement(newDeltaMovement);
                trackIteration.movementLeft = this.minecart.makeStepAlongTrack(currentPos, shape, trackIteration.movementLeft);
            } else {
                this.minecart.comeOffTrack(level);
                trackIteration.movementLeft = 0.0;
            }
            Vec3 stepPosition = this.position();
            Vec3 stepDelta = stepPosition.subtract(this.minecart.oldPosition());
            double stepLength = stepDelta.length();
            if (stepLength > (double)1.0E-5f) {
                if (stepDelta.horizontalDistanceSqr() > (double)1.0E-5f) {
                    float yRot = 180.0f - (float)(Math.atan2(stepDelta.z, stepDelta.x) * 180.0 / Math.PI);
                    float xRot = this.minecart.onGround() && !this.minecart.isOnRails() ? 0.0f : 90.0f - (float)(Math.atan2(stepDelta.horizontalDistance(), stepDelta.y) * 180.0 / Math.PI);
                    this.setRotation(yRot += this.minecart.isFlipped() ? 180.0f : 0.0f, xRot *= this.minecart.isFlipped() ? -1.0f : 1.0f);
                } else if (!this.minecart.isOnRails()) {
                    this.setXRot(this.minecart.onGround() ? 0.0f : Mth.rotLerp(0.2f, this.getXRot(), 0.0f));
                }
                this.lerpSteps.add(new MinecartStep(stepPosition, this.getDeltaMovement(), this.getYRot(), this.getXRot(), (float)Math.min(stepLength, this.getMaxSpeed(level))));
            } else if (initialStepDeltaMovement.horizontalDistanceSqr() > 0.0) {
                this.lerpSteps.add(new MinecartStep(stepPosition, this.getDeltaMovement(), this.getYRot(), this.getXRot(), 1.0f));
            }
            if (stepLength > (double)1.0E-5f || trackIteration.firstIteration) {
                this.minecart.applyEffectsFromBlocks();
                this.minecart.applyEffectsFromBlocks();
            }
            trackIteration.firstIteration = false;
        }
    }

    private Vec3 calculateTrackSpeed(ServerLevel level, Vec3 deltaMovement, TrackIteration trackIteration, BlockPos currentPos, BlockState currentState, RailShape shape) {
        Vec3 boostedDeltaMovement;
        Vec3 haltedDeltaMovement;
        Vec3 playerInputMovement;
        Vec3 slopedDeltaMovement;
        Vec3 newDeltaMovement = deltaMovement;
        if (!trackIteration.hasGainedSlopeSpeed && (slopedDeltaMovement = this.calculateSlopeSpeed(newDeltaMovement, shape)).horizontalDistanceSqr() != newDeltaMovement.horizontalDistanceSqr()) {
            trackIteration.hasGainedSlopeSpeed = true;
            newDeltaMovement = slopedDeltaMovement;
        }
        if (trackIteration.firstIteration && (playerInputMovement = this.calculatePlayerInputSpeed(newDeltaMovement)).horizontalDistanceSqr() != newDeltaMovement.horizontalDistanceSqr()) {
            trackIteration.hasHalted = true;
            newDeltaMovement = playerInputMovement;
        }
        if (!trackIteration.hasHalted && (haltedDeltaMovement = this.calculateHaltTrackSpeed(newDeltaMovement, currentState)).horizontalDistanceSqr() != newDeltaMovement.horizontalDistanceSqr()) {
            trackIteration.hasHalted = true;
            newDeltaMovement = haltedDeltaMovement;
        }
        if (trackIteration.firstIteration && (newDeltaMovement = this.minecart.applyNaturalSlowdown(newDeltaMovement)).lengthSqr() > 0.0) {
            double speed = Math.min(newDeltaMovement.length(), this.minecart.getMaxSpeed(level));
            newDeltaMovement = newDeltaMovement.normalize().scale(speed);
        }
        if (!trackIteration.hasBoosted && (boostedDeltaMovement = this.calculateBoostTrackSpeed(newDeltaMovement, currentPos, currentState)).horizontalDistanceSqr() != newDeltaMovement.horizontalDistanceSqr()) {
            trackIteration.hasBoosted = true;
            newDeltaMovement = boostedDeltaMovement;
        }
        return newDeltaMovement;
    }

    private Vec3 calculateSlopeSpeed(Vec3 deltaMovement, RailShape shape) {
        double slideSpeed = Math.max(0.0078125, deltaMovement.horizontalDistance() * 0.02);
        if (this.minecart.isInWater()) {
            slideSpeed *= 0.2;
        }
        return switch (shape) {
            case RailShape.ASCENDING_EAST -> deltaMovement.add(-slideSpeed, 0.0, 0.0);
            case RailShape.ASCENDING_WEST -> deltaMovement.add(slideSpeed, 0.0, 0.0);
            case RailShape.ASCENDING_NORTH -> deltaMovement.add(0.0, 0.0, slideSpeed);
            case RailShape.ASCENDING_SOUTH -> deltaMovement.add(0.0, 0.0, -slideSpeed);
            default -> deltaMovement;
        };
    }

    private Vec3 calculatePlayerInputSpeed(Vec3 deltaMovement) {
        Entity entity = this.minecart.getFirstPassenger();
        if (!(entity instanceof ServerPlayer)) {
            return deltaMovement;
        }
        ServerPlayer player = (ServerPlayer)entity;
        Vec3 moveIntent = player.getLastClientMoveIntent();
        if (moveIntent.lengthSqr() > 0.0) {
            Vec3 riderMovement = moveIntent.normalize();
            double ownDist = deltaMovement.horizontalDistanceSqr();
            if (riderMovement.lengthSqr() > 0.0 && ownDist < 0.01) {
                return deltaMovement.add(new Vec3(riderMovement.x, 0.0, riderMovement.z).normalize().scale(0.001));
            }
        }
        return deltaMovement;
    }

    private Vec3 calculateHaltTrackSpeed(Vec3 deltaMovement, BlockState state) {
        if (!state.is(Blocks.POWERED_RAIL) || state.getValue(PoweredRailBlock.POWERED).booleanValue()) {
            return deltaMovement;
        }
        if (deltaMovement.length() < 0.03) {
            return Vec3.ZERO;
        }
        return deltaMovement.scale(0.5);
    }

    private Vec3 calculateBoostTrackSpeed(Vec3 deltaMovement, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.POWERED_RAIL) || !state.getValue(PoweredRailBlock.POWERED).booleanValue()) {
            return deltaMovement;
        }
        if (deltaMovement.length() > 0.01) {
            return deltaMovement.normalize().scale(deltaMovement.length() + 0.06);
        }
        Vec3 powerDirection = this.minecart.getRedstoneDirection(pos);
        if (powerDirection.lengthSqr() <= 0.0) {
            return deltaMovement;
        }
        return powerDirection.scale(deltaMovement.length() + 0.2);
    }

    @Override
    public double stepAlongTrack(BlockPos pos, RailShape shape, double movementLeft) {
        if (movementLeft < (double)1.0E-5f) {
            return 0.0;
        }
        Vec3 oldPosition = this.position();
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = (Vec3i)exits.getFirst();
        Vec3i exit1 = (Vec3i)exits.getSecond();
        Vec3 movement = this.getDeltaMovement().horizontal();
        if (movement.length() < (double)1.0E-5f) {
            this.setDeltaMovement(Vec3.ZERO);
            return 0.0;
        }
        boolean inHill = exit0.getY() != exit1.getY();
        Vec3 horizontalInDirection = new Vec3(exit1).scale(0.5).horizontal();
        Vec3 horizontalOutDirection = new Vec3(exit0).scale(0.5).horizontal();
        if (movement.dot(horizontalOutDirection) < movement.dot(horizontalInDirection)) {
            horizontalOutDirection = horizontalInDirection;
        }
        Vec3 outPosition = Vec3.atBottomCenterOf(pos).add(horizontalOutDirection).add(0.0, 0.1, 0.0).add(horizontalOutDirection.normalize().scale(1.0E-5f));
        if (inHill && !this.isDecending(movement, shape)) {
            outPosition = outPosition.add(0.0, 1.0, 0.0);
        }
        Vec3 towardsOut = outPosition.subtract(this.position()).normalize();
        movement = towardsOut.scale(movement.length() / towardsOut.horizontalDistance());
        Vec3 newPosition = oldPosition.add(movement.normalize().scale(movementLeft * (double)(inHill ? Mth.SQRT_OF_TWO : 1.0f)));
        if (oldPosition.distanceToSqr(outPosition) <= oldPosition.distanceToSqr(newPosition)) {
            movementLeft = outPosition.subtract(newPosition).horizontalDistance();
            newPosition = outPosition;
        } else {
            movementLeft = 0.0;
        }
        this.minecart.move(MoverType.SELF, newPosition.subtract(oldPosition));
        BlockState newBlockState = this.level().getBlockState(BlockPos.containing(newPosition));
        if (inHill) {
            RailShape newRailShape;
            if (BaseRailBlock.isRail(newBlockState) && this.restAtVShape(shape, newRailShape = newBlockState.getValue(((BaseRailBlock)newBlockState.getBlock()).getShapeProperty()))) {
                return 0.0;
            }
            double horizontalDistanceFromOut = outPosition.horizontal().distanceTo(this.position().horizontal());
            double projectYPos = outPosition.y + (this.isDecending(movement, shape) ? horizontalDistanceFromOut : -horizontalDistanceFromOut);
            if (this.position().y < projectYPos) {
                this.setPos(this.position().x, projectYPos, this.position().z);
            }
        }
        if (this.position().distanceTo(oldPosition) < (double)1.0E-5f && newPosition.distanceTo(oldPosition) > (double)1.0E-5f) {
            this.setDeltaMovement(Vec3.ZERO);
            return 0.0;
        }
        this.setDeltaMovement(movement);
        return movementLeft;
    }

    private boolean restAtVShape(RailShape currentRailShape, RailShape newRailShape) {
        if (this.getDeltaMovement().lengthSqr() < 0.005 && newRailShape.isSlope() && this.isDecending(this.getDeltaMovement(), currentRailShape) && !this.isDecending(this.getDeltaMovement(), newRailShape)) {
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        return false;
    }

    @Override
    public double getMaxSpeed(ServerLevel level) {
        return (double)level.getGameRules().get(GameRules.MAX_MINECART_SPEED).intValue() * (this.minecart.isInWater() ? 0.5 : 1.0) / 20.0;
    }

    private boolean isDecending(Vec3 movement, RailShape shape) {
        return switch (shape) {
            case RailShape.ASCENDING_EAST -> {
                if (movement.x < 0.0) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_WEST -> {
                if (movement.x > 0.0) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_NORTH -> {
                if (movement.z > 0.0) {
                    yield true;
                }
                yield false;
            }
            case RailShape.ASCENDING_SOUTH -> {
                if (movement.z < 0.0) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    @Override
    public double getSlowdownFactor() {
        return this.minecart.isVehicle() ? 0.997 : 0.975;
    }

    @Override
    public boolean pushAndPickupEntities() {
        boolean pickedUp = this.pickupEntities(this.minecart.getBoundingBox().inflate(0.2, 0.0, 0.2));
        if (this.minecart.horizontalCollision || this.minecart.verticalCollision) {
            boolean pushed = this.pushEntities(this.minecart.getBoundingBox().inflate(1.0E-7));
            return pickedUp && !pushed;
        }
        return false;
    }

    public boolean pickupEntities(AABB hitbox) {
        List<Entity> entities;
        if (this.minecart.isRideable() && !this.minecart.isVehicle() && !(entities = this.level().getEntities(this.minecart, hitbox, EntitySelector.pushableBy(this.minecart))).isEmpty()) {
            for (Entity entity : entities) {
                boolean pickedUp;
                if (entity instanceof Player || entity instanceof IronGolem || entity instanceof AbstractMinecart || this.minecart.isVehicle() || entity.isPassenger() || !(pickedUp = entity.startRiding(this.minecart))) continue;
                return true;
            }
        }
        return false;
    }

    public boolean pushEntities(AABB hitbox) {
        boolean pushed;
        block3: {
            block2: {
                pushed = false;
                if (!this.minecart.isRideable()) break block2;
                List<Entity> entities = this.level().getEntities(this.minecart, hitbox, EntitySelector.pushableBy(this.minecart));
                if (entities.isEmpty()) break block3;
                for (Entity entity : entities) {
                    if (!(entity instanceof Player) && !(entity instanceof IronGolem) && !(entity instanceof AbstractMinecart) && !this.minecart.isVehicle() && !entity.isPassenger()) continue;
                    entity.push(this.minecart);
                    pushed = true;
                }
                break block3;
            }
            for (Entity entity : this.level().getEntities(this.minecart, hitbox)) {
                if (this.minecart.hasPassenger(entity) || !entity.isPushable() || !(entity instanceof AbstractMinecart)) continue;
                entity.push(this.minecart);
                pushed = true;
            }
        }
        return pushed;
    }

    public record MinecartStep(Vec3 position, Vec3 movement, float yRot, float xRot, float weight) {
        public static final StreamCodec<ByteBuf, MinecartStep> STREAM_CODEC = StreamCodec.composite(Vec3.STREAM_CODEC, MinecartStep::position, Vec3.STREAM_CODEC, MinecartStep::movement, ByteBufCodecs.ROTATION_BYTE, MinecartStep::yRot, ByteBufCodecs.ROTATION_BYTE, MinecartStep::xRot, ByteBufCodecs.FLOAT, MinecartStep::weight, MinecartStep::new);
        public static final MinecartStep ZERO = new MinecartStep(Vec3.ZERO, Vec3.ZERO, 0.0f, 0.0f, 0.0f);
    }

    private record StepPartialTicks(float partialTicksInStep, MinecartStep currentStep, MinecartStep previousStep) {
    }

    private static class TrackIteration {
        public double movementLeft = 0.0;
        public boolean firstIteration = true;
        public boolean hasGainedSlopeSpeed = false;
        public boolean hasHalted = false;
        public boolean hasBoosted = false;

        private TrackIteration() {
        }

        public boolean shouldIterate() {
            return this.firstIteration || this.movementLeft > (double)1.0E-5f;
        }
    }
}

