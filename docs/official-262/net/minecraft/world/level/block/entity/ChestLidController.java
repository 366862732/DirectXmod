/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.util.Mth;

public class ChestLidController {
    private boolean shouldBeOpen;
    private float openness;
    private float oOpenness;

    public void tickLid() {
        this.oOpenness = this.openness;
        float speed = 0.1f;
        if (!this.shouldBeOpen && this.openness > 0.0f) {
            this.openness = Math.max(this.openness - 0.1f, 0.0f);
        } else if (this.shouldBeOpen && this.openness < 1.0f) {
            this.openness = Math.min(this.openness + 0.1f, 1.0f);
        }
    }

    public float getOpenness(float a) {
        return Mth.lerp(a, this.oOpenness, this.openness);
    }

    public void shouldBeOpen(boolean shouldBeOpen) {
        this.shouldBeOpen = shouldBeOpen;
    }
}

