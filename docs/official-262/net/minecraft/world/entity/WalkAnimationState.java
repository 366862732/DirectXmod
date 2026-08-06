/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.util.Mth;

public class WalkAnimationState {
    private float speedOld;
    private float speed;
    private float position;
    private float positionScale = 1.0f;

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void update(float targetSpeed, float factor, float positionScale) {
        this.speedOld = this.speed;
        this.speed += (targetSpeed - this.speed) * factor;
        this.position += this.speed;
        this.positionScale = positionScale;
    }

    public void stop() {
        this.speedOld = 0.0f;
        this.speed = 0.0f;
        this.position = 0.0f;
    }

    public float speed() {
        return this.speed;
    }

    public float speed(float partialTicks) {
        return Math.min(Mth.lerp(partialTicks, this.speedOld, this.speed), 1.0f);
    }

    public float position() {
        return this.position * this.positionScale;
    }

    public float position(float partialTicks) {
        return (this.position - this.speed * (1.0f - partialTicks)) * this.positionScale;
    }

    public boolean isMoving() {
        return this.speed > 1.0E-5f;
    }
}

