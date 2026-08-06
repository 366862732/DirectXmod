/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class SmoothSwimmingMoveControl<T extends Mob>
extends MoveControl<T> {
    private static final float FULL_SPEED_TURN_THRESHOLD = 10.0f;
    private static final float STOP_TURN_THRESHOLD = 60.0f;
    private final int maxTurnX;
    private final int maxTurnY;
    private final float inWaterSpeedModifier;
    private final float outsideWaterSpeedModifier;
    private final boolean applyGravity;

    public SmoothSwimmingMoveControl(T mob, int maxTurnX, int maxTurnY, float inWaterSpeedModifier, float outsideWaterSpeedModifier, boolean applyGravity) {
        super(mob);
        this.maxTurnX = maxTurnX;
        this.maxTurnY = maxTurnY;
        this.inWaterSpeedModifier = inWaterSpeedModifier;
        this.outsideWaterSpeedModifier = outsideWaterSpeedModifier;
        this.applyGravity = applyGravity;
    }

    @Override
    public void tick() {
        double zd;
        double yd;
        if (this.applyGravity && this.mob.isInWater()) {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0.0, 0.005, 0.0));
        }
        if (this.operation != MoveControl.Operation.MOVE_TO || this.mob.getNavigation().isDone()) {
            this.mob.setSpeed(0.0f);
            this.mob.setXxa(0.0f);
            this.mob.setYya(0.0f);
            this.mob.setZza(0.0f);
            return;
        }
        double xd = this.wantedX - this.mob.getX();
        double dd = xd * xd + (yd = this.wantedY - this.mob.getY()) * yd + (zd = this.wantedZ - this.mob.getZ()) * zd;
        if (dd < 2.500000277905201E-7) {
            this.mob.setZza(0.0f);
            return;
        }
        float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, this.maxTurnY));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.yHeadRot = this.mob.getYRot();
        float speed = (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        if (this.mob.isInWater()) {
            this.mob.setSpeed(speed * this.inWaterSpeedModifier);
            double sqrt = Math.sqrt(xd * xd + zd * zd);
            if (Math.abs(yd) > (double)1.0E-5f || Math.abs(sqrt) > (double)1.0E-5f) {
                float xRotD = -((float)(Mth.atan2(yd, sqrt) * 57.2957763671875));
                xRotD = Mth.clamp(Mth.wrapDegrees(xRotD), (float)(-this.maxTurnX), (float)this.maxTurnX);
                this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), xRotD, 5.0f));
            }
            float cos = Mth.cos(this.mob.getXRot() * ((float)Math.PI / 180));
            float sin = Mth.sin(this.mob.getXRot() * ((float)Math.PI / 180));
            this.mob.zza = cos * speed;
            this.mob.yya = -sin * speed;
        } else {
            float leftToTurn = Math.abs(Mth.wrapDegrees(this.mob.getYRot() - yRotD));
            float turningSpeedFactor = SmoothSwimmingMoveControl.getTurningSpeedFactor(leftToTurn);
            this.mob.setSpeed(speed * this.outsideWaterSpeedModifier * turningSpeedFactor);
        }
    }

    private static float getTurningSpeedFactor(float leftToTurn) {
        return 1.0f - Mth.clamp((leftToTurn - 10.0f) / 50.0f, 0.0f, 1.0f);
    }
}

