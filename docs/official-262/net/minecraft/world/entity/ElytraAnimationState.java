/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ElytraAnimationState {
    private static final float DEFAULT_X_ROT = 0.2617994f;
    private static final float DEFAULT_Z_ROT = -0.2617994f;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float rotXOld;
    private float rotYOld;
    private float rotZOld;
    private final LivingEntity entity;

    public ElytraAnimationState(LivingEntity entity) {
        this.entity = entity;
    }

    public void tick() {
        float targetYRot;
        float targetZRot;
        float targetXRot;
        this.rotXOld = this.rotX;
        this.rotYOld = this.rotY;
        this.rotZOld = this.rotZ;
        if (this.entity.isFallFlying()) {
            float ratio = 1.0f;
            Vec3 movement = this.entity.getDeltaMovement();
            if (movement.y < 0.0) {
                Vec3 vec = movement.normalize();
                ratio = 1.0f - (float)Math.pow(-vec.y, 1.5);
            }
            targetXRot = Mth.lerp(ratio, 0.2617994f, 0.34906584f);
            targetZRot = Mth.lerp(ratio, -0.2617994f, -1.5707964f);
            targetYRot = 0.0f;
        } else if (this.entity.isCrouching()) {
            targetXRot = 0.6981317f;
            targetZRot = -0.7853982f;
            targetYRot = 0.08726646f;
        } else {
            targetXRot = 0.2617994f;
            targetZRot = -0.2617994f;
            targetYRot = 0.0f;
        }
        this.rotX += (targetXRot - this.rotX) * 0.3f;
        this.rotY += (targetYRot - this.rotY) * 0.3f;
        this.rotZ += (targetZRot - this.rotZ) * 0.3f;
    }

    public float getRotX(float partialTicks) {
        return Mth.lerp(partialTicks, this.rotXOld, this.rotX);
    }

    public float getRotY(float partialTicks) {
        return Mth.lerp(partialTicks, this.rotYOld, this.rotY);
    }

    public float getRotZ(float partialTicks) {
        return Mth.lerp(partialTicks, this.rotZOld, this.rotZ);
    }
}

