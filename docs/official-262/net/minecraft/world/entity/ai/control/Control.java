/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;

public interface Control {
    default public float rotateTowards(float fromAngle, float toAngle, float maxRot) {
        float diff = Mth.degreesDifference(fromAngle, toAngle);
        float diffClamped = Mth.clamp(diff, -maxRot, maxRot);
        return fromAngle + diffClamped;
    }
}

