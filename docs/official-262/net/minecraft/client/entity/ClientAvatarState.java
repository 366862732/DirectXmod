/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class ClientAvatarState {
    private Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
    private float walkDist;
    private float walkDistO;
    private double xCloak;
    private double yCloak;
    private double zCloak;
    private double xCloakO;
    private double yCloakO;
    private double zCloakO;
    private float bob;
    private float bobO;

    public void tick(Vec3 position, Vec3 deltaMovement) {
        this.walkDistO = this.walkDist;
        this.deltaMovementOnPreviousTick = deltaMovement;
        this.moveCloak(position);
    }

    public void addWalkDistance(float added) {
        this.walkDist += added;
    }

    public Vec3 deltaMovementOnPreviousTick() {
        return this.deltaMovementOnPreviousTick;
    }

    private void moveCloak(Vec3 pos) {
        this.xCloakO = this.xCloak;
        this.yCloakO = this.yCloak;
        this.zCloakO = this.zCloak;
        double x = pos.x() - this.xCloak;
        double y = pos.y() - this.yCloak;
        double z = pos.z() - this.zCloak;
        double teleportThreshold = 10.0;
        if (x > 10.0 || x < -10.0) {
            this.xCloakO = this.xCloak = pos.x();
        } else {
            this.xCloak += x * 0.25;
        }
        if (y > 10.0 || y < -10.0) {
            this.yCloakO = this.yCloak = pos.y();
        } else {
            this.yCloak += y * 0.25;
        }
        if (z > 10.0 || z < -10.0) {
            this.zCloakO = this.zCloak = pos.z();
        } else {
            this.zCloak += z * 0.25;
        }
    }

    public double getInterpolatedCloakX(float partialTicks) {
        return Mth.lerp((double)partialTicks, this.xCloakO, this.xCloak);
    }

    public double getInterpolatedCloakY(float partialTicks) {
        return Mth.lerp((double)partialTicks, this.yCloakO, this.yCloak);
    }

    public double getInterpolatedCloakZ(float partialTicks) {
        return Mth.lerp((double)partialTicks, this.zCloakO, this.zCloak);
    }

    public void updateBob(float tBob) {
        this.bobO = this.bob;
        this.bob += (tBob - this.bob) * 0.4f;
    }

    public void resetBob() {
        this.bobO = this.bob;
        this.bob = 0.0f;
    }

    public float getInterpolatedBob(float partialTicks) {
        return Mth.lerp(partialTicks, this.bobO, this.bob);
    }

    public float getBackwardsInterpolatedWalkDistance(float partialTicks) {
        float wda = this.walkDist - this.walkDistO;
        return -(this.walkDist + wda * partialTicks);
    }

    public float getInterpolatedWalkDistance(float partialTicks) {
        return Mth.lerp(partialTicks, this.walkDistO, this.walkDist);
    }
}

