/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.vehicle.minecart;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.InterpolationHandler;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class OldMinecartBehavior
extends MinecartBehavior {
    private static final double MINECART_RIDABLE_THRESHOLD = 0.01;
    private static final double MAX_SPEED_IN_WATER = 0.2;
    private static final double MAX_SPEED_ON_LAND = 0.4;
    private static final double ABSOLUTE_MAX_SPEED = 0.4;
    private final InterpolationHandler interpolation;
    private Vec3 targetDeltaMovement = Vec3.ZERO;

    public OldMinecartBehavior(AbstractMinecart minecart) {
        super(minecart);
        this.interpolation = new InterpolationHandler((Entity)minecart, this::onInterpolation);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    public void onInterpolation(InterpolationHandler interpolation) {
        this.setDeltaMovement(this.targetDeltaMovement);
    }

    @Override
    public void lerpMotion(Vec3 movement) {
        this.targetDeltaMovement = movement;
        this.setDeltaMovement(this.targetDeltaMovement);
    }

    @Override
    public void tick() {
        double rotDiff;
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            if (this.interpolation.hasActiveInterpolation()) {
                this.interpolation.interpolate();
            } else {
                this.minecart.reapplyPosition();
                this.setXRot(this.getXRot() % 360.0f);
                this.setYRot(this.getYRot() % 360.0f);
            }
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        this.minecart.applyGravity();
        BlockPos pos = this.minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(pos);
        boolean onRails = BaseRailBlock.isRail(state);
        this.minecart.setOnRails(onRails);
        if (onRails) {
            this.moveAlongTrack(level2);
            if (state.is(Blocks.ACTIVATOR_RAIL)) {
                this.minecart.activateMinecart(level2, pos.getX(), pos.getY(), pos.getZ(), state.getValue(PoweredRailBlock.POWERED));
            }
        } else {
            this.minecart.comeOffTrack(level2);
        }
        this.minecart.applyEffectsFromBlocks();
        this.setXRot(0.0f);
        double xDiff = this.minecart.xo - this.getX();
        double zDiff = this.minecart.zo - this.getZ();
        if (xDiff * xDiff + zDiff * zDiff > 0.001) {
            this.setYRot((float)(Mth.atan2(zDiff, xDiff) * 180.0 / Math.PI));
            if (this.minecart.isFlipped()) {
                this.setYRot(this.getYRot() + 180.0f);
            }
        }
        if ((rotDiff = (double)Mth.wrapDegrees(this.getYRot() - this.minecart.yRotO)) < -170.0 || rotDiff >= 170.0) {
            this.setYRot(this.getYRot() + 180.0f);
            this.minecart.setFlipped(!this.minecart.isFlipped());
        }
        this.setXRot(this.getXRot() % 360.0f);
        this.setYRot(this.getYRot() % 360.0f);
        this.pushAndPickupEntities();
    }

    @Override
    public void moveAlongTrack(ServerLevel level) {
        double otherPow;
        Vec3 vec3;
        double progress;
        Vec3 moveIntent;
        BlockPos pos = this.minecart.getCurrentBlockPosOrRailBelow();
        BlockState state = this.level().getBlockState(pos);
        this.minecart.resetFallDistance();
        double x = this.minecart.getX();
        double y = this.minecart.getY();
        double z = this.minecart.getZ();
        Vec3 oldPos = this.getPos(x, y, z);
        y = pos.getY();
        boolean powerTrack = false;
        boolean haltTrack = false;
        if (state.is(Blocks.POWERED_RAIL)) {
            powerTrack = state.getValue(PoweredRailBlock.POWERED);
            haltTrack = !powerTrack;
        }
        double slideSpeed = 0.0078125;
        if (this.minecart.isInWater()) {
            slideSpeed *= 0.2;
        }
        Vec3 movement = this.getDeltaMovement();
        RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
        switch (shape) {
            case ASCENDING_EAST: {
                this.setDeltaMovement(movement.add(-slideSpeed, 0.0, 0.0));
                y += 1.0;
                break;
            }
            case ASCENDING_WEST: {
                this.setDeltaMovement(movement.add(slideSpeed, 0.0, 0.0));
                y += 1.0;
                break;
            }
            case ASCENDING_NORTH: {
                this.setDeltaMovement(movement.add(0.0, 0.0, slideSpeed));
                y += 1.0;
                break;
            }
            case ASCENDING_SOUTH: {
                this.setDeltaMovement(movement.add(0.0, 0.0, -slideSpeed));
                y += 1.0;
            }
        }
        movement = this.getDeltaMovement();
        Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
        Vec3i exit0 = (Vec3i)exits.getFirst();
        Vec3i exit1 = (Vec3i)exits.getSecond();
        double xD = exit1.getX() - exit0.getX();
        double zD = exit1.getZ() - exit0.getZ();
        double length = Math.sqrt(xD * xD + zD * zD);
        double flip = movement.x * xD + movement.z * zD;
        if (flip < 0.0) {
            xD = -xD;
            zD = -zD;
        }
        double pow = Math.min(2.0, movement.horizontalDistance());
        movement = new Vec3(pow * xD / length, movement.y, pow * zD / length);
        this.setDeltaMovement(movement);
        Entity controllingPassenger = this.minecart.getFirstPassenger();
        Entity entity = this.minecart.getFirstPassenger();
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            moveIntent = player.getLastClientMoveIntent();
        } else {
            moveIntent = Vec3.ZERO;
        }
        if (controllingPassenger instanceof Player && moveIntent.lengthSqr() > 0.0) {
            Vec3 riderMovement = moveIntent.normalize();
            double ownDist = this.getDeltaMovement().horizontalDistanceSqr();
            if (riderMovement.lengthSqr() > 0.0 && ownDist < 0.01) {
                this.setDeltaMovement(this.getDeltaMovement().add(moveIntent.x * 0.001, 0.0, moveIntent.z * 0.001));
                haltTrack = false;
            }
        }
        if (haltTrack) {
            double speedLength = this.getDeltaMovement().horizontalDistance();
            if (speedLength < 0.03) {
                this.setDeltaMovement(Vec3.ZERO);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
            }
        }
        double x0 = (double)pos.getX() + 0.5 + (double)exit0.getX() * 0.5;
        double z0 = (double)pos.getZ() + 0.5 + (double)exit0.getZ() * 0.5;
        double x1 = (double)pos.getX() + 0.5 + (double)exit1.getX() * 0.5;
        double z1 = (double)pos.getZ() + 0.5 + (double)exit1.getZ() * 0.5;
        xD = x1 - x0;
        zD = z1 - z0;
        if (xD == 0.0) {
            progress = z - (double)pos.getZ();
        } else if (zD == 0.0) {
            progress = x - (double)pos.getX();
        } else {
            double xx = x - x0;
            double zz = z - z0;
            progress = (xx * xD + zz * zD) * 2.0;
        }
        x = x0 + xD * progress;
        z = z0 + zD * progress;
        this.setPos(x, y, z);
        double scale = this.minecart.isVehicle() ? 0.75 : 1.0;
        double maxSpeed = this.minecart.getMaxSpeed(level);
        movement = this.getDeltaMovement();
        this.minecart.move(MoverType.SELF, new Vec3(Mth.clamp(scale * movement.x, -maxSpeed, maxSpeed), 0.0, Mth.clamp(scale * movement.z, -maxSpeed, maxSpeed)));
        if (exit0.getY() != 0 && Mth.floor(this.minecart.getX()) - pos.getX() == exit0.getX() && Mth.floor(this.minecart.getZ()) - pos.getZ() == exit0.getZ()) {
            this.setPos(this.minecart.getX(), this.minecart.getY() + (double)exit0.getY(), this.minecart.getZ());
        } else if (exit1.getY() != 0 && Mth.floor(this.minecart.getX()) - pos.getX() == exit1.getX() && Mth.floor(this.minecart.getZ()) - pos.getZ() == exit1.getZ()) {
            this.setPos(this.minecart.getX(), this.minecart.getY() + (double)exit1.getY(), this.minecart.getZ());
        }
        this.setDeltaMovement(this.minecart.applyNaturalSlowdown(this.getDeltaMovement()));
        Vec3 newPos = this.getPos(this.minecart.getX(), this.minecart.getY(), this.minecart.getZ());
        if (newPos != null && oldPos != null) {
            double speed = (oldPos.y - newPos.y) * 0.05;
            vec3 = this.getDeltaMovement();
            otherPow = vec3.horizontalDistance();
            if (otherPow > 0.0) {
                this.setDeltaMovement(vec3.multiply((otherPow + speed) / otherPow, 1.0, (otherPow + speed) / otherPow));
            }
            this.setPos(this.minecart.getX(), newPos.y, this.minecart.getZ());
        }
        int xn = Mth.floor(this.minecart.getX());
        int zn = Mth.floor(this.minecart.getZ());
        if (xn != pos.getX() || zn != pos.getZ()) {
            vec3 = this.getDeltaMovement();
            otherPow = vec3.horizontalDistance();
            this.setDeltaMovement(otherPow * (double)(xn - pos.getX()), vec3.y, otherPow * (double)(zn - pos.getZ()));
        }
        if (powerTrack) {
            vec3 = this.getDeltaMovement();
            double speedLength = vec3.horizontalDistance();
            if (speedLength > 0.01) {
                double speed = 0.06;
                this.setDeltaMovement(vec3.add(vec3.x / speedLength * 0.06, 0.0, vec3.z / speedLength * 0.06));
            } else {
                Vec3 deltaMovement = this.getDeltaMovement();
                double dx = deltaMovement.x;
                double dz = deltaMovement.z;
                if (shape == RailShape.EAST_WEST) {
                    if (this.minecart.isRedstoneConductor(pos.west())) {
                        dx = 0.02;
                    } else if (this.minecart.isRedstoneConductor(pos.east())) {
                        dx = -0.02;
                    }
                } else if (shape == RailShape.NORTH_SOUTH) {
                    if (this.minecart.isRedstoneConductor(pos.north())) {
                        dz = 0.02;
                    } else if (this.minecart.isRedstoneConductor(pos.south())) {
                        dz = -0.02;
                    }
                } else {
                    return;
                }
                this.setDeltaMovement(dx, deltaMovement.y, dz);
            }
        }
    }

    public @Nullable Vec3 getPosOffs(double x, double y, double z, double offs) {
        BlockState state;
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
            --yt;
        }
        if (BaseRailBlock.isRail(state = this.level().getBlockState(new BlockPos(xt, yt, zt)))) {
            RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
            y = yt;
            if (shape.isSlope()) {
                y = yt + 1;
            }
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = (Vec3i)exits.getFirst();
            Vec3i exit1 = (Vec3i)exits.getSecond();
            double xD = exit1.getX() - exit0.getX();
            double zD = exit1.getZ() - exit0.getZ();
            double dd = Math.sqrt(xD * xD + zD * zD);
            if (exit0.getY() != 0 && Mth.floor(x += (xD /= dd) * offs) - xt == exit0.getX() && Mth.floor(z += (zD /= dd) * offs) - zt == exit0.getZ()) {
                y += (double)exit0.getY();
            } else if (exit1.getY() != 0 && Mth.floor(x) - xt == exit1.getX() && Mth.floor(z) - zt == exit1.getZ()) {
                y += (double)exit1.getY();
            }
            return this.getPos(x, y, z);
        }
        return null;
    }

    public @Nullable Vec3 getPos(double x, double y, double z) {
        BlockState state;
        int xt = Mth.floor(x);
        int yt = Mth.floor(y);
        int zt = Mth.floor(z);
        if (this.level().getBlockState(new BlockPos(xt, yt - 1, zt)).is(BlockTags.RAILS)) {
            --yt;
        }
        if (BaseRailBlock.isRail(state = this.level().getBlockState(new BlockPos(xt, yt, zt)))) {
            double progress;
            RailShape shape = state.getValue(((BaseRailBlock)state.getBlock()).getShapeProperty());
            Pair<Vec3i, Vec3i> exits = AbstractMinecart.exits(shape);
            Vec3i exit0 = (Vec3i)exits.getFirst();
            Vec3i exit1 = (Vec3i)exits.getSecond();
            double x0 = (double)xt + 0.5 + (double)exit0.getX() * 0.5;
            double y0 = (double)yt + 0.0625 + (double)exit0.getY() * 0.5;
            double z0 = (double)zt + 0.5 + (double)exit0.getZ() * 0.5;
            double x1 = (double)xt + 0.5 + (double)exit1.getX() * 0.5;
            double y1 = (double)yt + 0.0625 + (double)exit1.getY() * 0.5;
            double z1 = (double)zt + 0.5 + (double)exit1.getZ() * 0.5;
            double xD = x1 - x0;
            double yD = (y1 - y0) * 2.0;
            double zD = z1 - z0;
            if (xD == 0.0) {
                progress = z - (double)zt;
            } else if (zD == 0.0) {
                progress = x - (double)xt;
            } else {
                double xx = x - x0;
                double zz = z - z0;
                progress = (xx * xD + zz * zD) * 2.0;
            }
            x = x0 + xD * progress;
            y = y0 + yD * progress;
            z = z0 + zD * progress;
            if (yD < 0.0) {
                y += 1.0;
            } else if (yD > 0.0) {
                y += 0.5;
            }
            return new Vec3(x, y, z);
        }
        return null;
    }

    @Override
    public double stepAlongTrack(BlockPos pos, RailShape shape, double movementLeft) {
        return 0.0;
    }

    @Override
    public boolean pushAndPickupEntities() {
        block4: {
            AABB hitbox;
            block3: {
                hitbox = this.minecart.getBoundingBox().inflate(0.2f, 0.0, 0.2f);
                if (!this.minecart.isRideable() || !(this.getDeltaMovement().horizontalDistanceSqr() >= 0.01)) break block3;
                List<Entity> entities = this.level().getEntities(this.minecart, hitbox, EntitySelector.pushableBy(this.minecart));
                if (entities.isEmpty()) break block4;
                for (Entity entity : entities) {
                    if (entity instanceof Player || entity instanceof IronGolem || entity instanceof AbstractMinecart || this.minecart.isVehicle() || entity.isPassenger()) {
                        entity.push(this.minecart);
                        continue;
                    }
                    entity.startRiding(this.minecart);
                }
                break block4;
            }
            for (Entity entity : this.level().getEntities(this.minecart, hitbox)) {
                if (this.minecart.hasPassenger(entity) || !entity.isPushable() || !(entity instanceof AbstractMinecart)) continue;
                entity.push(this.minecart);
            }
        }
        return false;
    }

    @Override
    public Direction getMotionDirection() {
        return this.minecart.isFlipped() ? this.minecart.getDirection().getOpposite().getClockWise() : this.minecart.getDirection().getClockWise();
    }

    @Override
    public Vec3 getKnownMovement(Vec3 knownMovement) {
        if (Double.isNaN(knownMovement.x) || Double.isNaN(knownMovement.y) || Double.isNaN(knownMovement.z)) {
            return Vec3.ZERO;
        }
        return new Vec3(Mth.clamp(knownMovement.x, -0.4, 0.4), knownMovement.y, Mth.clamp(knownMovement.z, -0.4, 0.4));
    }

    @Override
    public double getMaxSpeed(ServerLevel level) {
        return this.minecart.isInWater() ? 0.2 : 0.4;
    }

    @Override
    public double getSlowdownFactor() {
        return this.minecart.isVehicle() ? 0.997 : 0.96;
    }
}

