/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
    protected final AbstractMinecart minecart;

    protected MinecartBehavior(AbstractMinecart minecart) {
        this.minecart = minecart;
    }

    public InterpolationHandler getInterpolation() {
        return null;
    }

    public void lerpMotion(Vec3 movement) {
        this.setDeltaMovement(movement);
    }

    public abstract void tick();

    public Level level() {
        return this.minecart.level();
    }

    public abstract void moveAlongTrack(ServerLevel var1);

    public abstract double stepAlongTrack(BlockPos var1, RailShape var2, double var3);

    public abstract boolean pushAndPickupEntities();

    public Vec3 getDeltaMovement() {
        return this.minecart.getDeltaMovement();
    }

    public void setDeltaMovement(Vec3 deltaMovement) {
        this.minecart.setDeltaMovement(deltaMovement);
    }

    public void setDeltaMovement(double x, double y, double z) {
        this.minecart.setDeltaMovement(x, y, z);
    }

    public Vec3 position() {
        return this.minecart.position();
    }

    public double getX() {
        return this.minecart.getX();
    }

    public double getY() {
        return this.minecart.getY();
    }

    public double getZ() {
        return this.minecart.getZ();
    }

    public void setPos(Vec3 pos) {
        this.minecart.setPos(pos);
    }

    public void setPos(double x, double y, double z) {
        this.minecart.setPos(x, y, z);
    }

    public float getXRot() {
        return this.minecart.getXRot();
    }

    public void setXRot(float rot) {
        this.minecart.setXRot(rot);
    }

    public float getYRot() {
        return this.minecart.getYRot();
    }

    public void setYRot(float rot) {
        this.minecart.setYRot(rot);
    }

    public Direction getMotionDirection() {
        return this.minecart.getDirection();
    }

    public Vec3 getKnownMovement(Vec3 knownMovement) {
        return knownMovement;
    }

    public abstract double getMaxSpeed(ServerLevel var1);

    public abstract double getSlowdownFactor();
}

