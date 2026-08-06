/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.Control;
import net.minecraft.world.phys.Vec3;

public class LookControl
implements Control {
    protected final Mob mob;
    protected float yMaxRotSpeed;
    protected float xMaxRotAngle;
    protected int lookAtCooldown;
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;

    public LookControl(Mob mob) {
        this.mob = mob;
    }

    public void setLookAt(Vec3 vec) {
        this.setLookAt(vec.x, vec.y, vec.z);
    }

    public void setLookAt(Entity target) {
        this.setLookAt(target.getX(), target.getEyeY(), target.getZ());
    }

    public void setLookAt(Entity target, float yMaxRotSpeed, float xMaxRotAngle) {
        this.setLookAt(target.getX(), target.getEyeY(), target.getZ(), yMaxRotSpeed, xMaxRotAngle);
    }

    public void setLookAt(double x, double y, double z) {
        this.setLookAt(x, y, z, this.mob.getHeadRotSpeed(), this.mob.getMaxHeadXRot());
    }

    public void setLookAt(double x, double y, double z, float yMaxRotSpeed, float xMaxRotAngle) {
        this.wantedX = x;
        this.wantedY = y;
        this.wantedZ = z;
        this.yMaxRotSpeed = yMaxRotSpeed;
        this.xMaxRotAngle = xMaxRotAngle;
        this.lookAtCooldown = 2;
    }

    public void tick() {
        if (this.resetXRotOnTick()) {
            this.mob.setXRot(0.0f);
        }
        if (this.lookAtCooldown > 0) {
            --this.lookAtCooldown;
            this.getYRotD().ifPresent(yRotD -> {
                this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, yRotD.floatValue(), this.yMaxRotSpeed);
            });
            this.getXRotD().ifPresent(xRotD -> this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), xRotD.floatValue(), this.xMaxRotAngle)));
        } else {
            this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0f);
        }
        this.clampHeadRotationToBody();
    }

    protected void clampHeadRotationToBody() {
        if (!this.mob.getNavigation().isDone()) {
            this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, this.mob.getMaxHeadYRot());
        }
    }

    protected boolean resetXRotOnTick() {
        return true;
    }

    public boolean isLookingAtTarget() {
        return this.lookAtCooldown > 0;
    }

    public double getWantedX() {
        return this.wantedX;
    }

    public double getWantedY() {
        return this.wantedY;
    }

    public double getWantedZ() {
        return this.wantedZ;
    }

    protected Optional<Float> getXRotD() {
        double xd = this.wantedX - this.mob.getX();
        double yd = this.wantedY - this.mob.getEyeY();
        double zd = this.wantedZ - this.mob.getZ();
        double sd = Math.sqrt(xd * xd + zd * zd);
        return Math.abs(yd) > (double)1.0E-5f || Math.abs(sd) > (double)1.0E-5f ? Optional.of(Float.valueOf((float)(-(Mth.atan2(yd, sd) * 57.2957763671875)))) : Optional.empty();
    }

    protected Optional<Float> getYRotD() {
        double xd = this.wantedX - this.mob.getX();
        double zd = this.wantedZ - this.mob.getZ();
        return Math.abs(zd) > (double)1.0E-5f || Math.abs(xd) > (double)1.0E-5f ? Optional.of(Float.valueOf((float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f)) : Optional.empty();
    }
}

