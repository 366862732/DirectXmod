/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class FlyingMoveControl<T extends Mob>
extends MoveControl<T> {
    private final int maxTurn;
    private final boolean hoversInPlace;

    public FlyingMoveControl(T mob, int maxTurn, boolean hoversInPlace) {
        super(mob);
        this.maxTurn = maxTurn;
        this.hoversInPlace = hoversInPlace;
    }

    @Override
    public void tick() {
        if (this.operation == MoveControl.Operation.MOVE_TO) {
            this.operation = MoveControl.Operation.WAIT;
            this.mob.setNoGravity(true);
            double xd = this.wantedX - this.mob.getX();
            double yd = this.wantedY - this.mob.getY();
            double zd = this.wantedZ - this.mob.getZ();
            double dd = xd * xd + yd * yd + zd * zd;
            if (dd < 2.500000277905201E-7) {
                this.mob.setYya(0.0f);
                this.mob.setZza(0.0f);
                return;
            }
            float yRotD = (float)(Mth.atan2(zd, xd) * 57.2957763671875) - 90.0f;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 90.0f));
            float speed = this.mob.onGround() ? (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)) : (float)(this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED));
            this.mob.setSpeed(speed);
            double sd = Math.sqrt(xd * xd + zd * zd);
            if (Math.abs(yd) > (double)1.0E-5f || Math.abs(sd) > (double)1.0E-5f) {
                float xRotD = (float)(-(Mth.atan2(yd, sd) * 57.2957763671875));
                this.mob.setXRot(this.rotlerp(this.mob.getXRot(), xRotD, this.maxTurn));
                this.mob.setYya(yd > 0.0 ? speed : -speed);
            }
        } else {
            if (!this.hoversInPlace) {
                this.mob.setNoGravity(false);
            }
            this.mob.setYya(0.0f);
            this.mob.setZza(0.0f);
        }
    }
}

