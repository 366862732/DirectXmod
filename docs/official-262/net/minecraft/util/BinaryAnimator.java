/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util;

import net.minecraft.util.EasingType;
import net.minecraft.util.Mth;

public class BinaryAnimator {
    private final int animationLength;
    private final EasingType easing;
    private int ticks;
    private int ticksOld;

    public BinaryAnimator(int animationLength, EasingType easing) {
        this.animationLength = animationLength;
        this.easing = easing;
    }

    public BinaryAnimator(int animationLength) {
        this(animationLength, EasingType.LINEAR);
    }

    public void tick(boolean active) {
        this.ticksOld = this.ticks;
        if (active) {
            if (this.ticks < this.animationLength) {
                ++this.ticks;
            }
        } else if (this.ticks > 0) {
            --this.ticks;
        }
    }

    public float getFactor(float partialTicks) {
        float factor = Mth.lerp(partialTicks, this.ticksOld, this.ticks) / (float)this.animationLength;
        return this.easing.apply(factor);
    }
}

