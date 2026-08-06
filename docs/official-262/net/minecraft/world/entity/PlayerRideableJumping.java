/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity;

import net.minecraft.world.entity.PlayerRideable;

public interface PlayerRideableJumping
extends PlayerRideable {
    public void onPlayerJump(int var1);

    public boolean canJump();

    public void handleStartJump(int var1);

    public void handleStopJump();

    default public int getJumpCooldown() {
        return 0;
    }

    default public float getPlayerJumpPendingScale(int jumpAmount) {
        return jumpAmount >= 90 ? 1.0f : 0.4f + 0.4f * (float)jumpAmount / 90.0f;
    }
}

