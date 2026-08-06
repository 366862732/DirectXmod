/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import net.minecraft.util.ARGB;

public record GizmoStyle(int stroke, float strokeWidth, int fill) {
    private static final float DEFAULT_WIDTH = 2.5f;

    public static GizmoStyle stroke(int argb) {
        return new GizmoStyle(argb, 2.5f, 0);
    }

    public static GizmoStyle stroke(int argb, float width) {
        return new GizmoStyle(argb, width, 0);
    }

    public static GizmoStyle fill(int argb) {
        return new GizmoStyle(0, 0.0f, argb);
    }

    public static GizmoStyle strokeAndFill(int stroke, float strokeWidth, int fill) {
        return new GizmoStyle(stroke, strokeWidth, fill);
    }

    public boolean hasFill() {
        return this.fill != 0;
    }

    public boolean hasStroke() {
        return this.stroke != 0 && this.strokeWidth > 0.0f;
    }

    public int multipliedStroke(float alphaMultiplier) {
        return ARGB.multiplyAlpha(this.stroke, alphaMultiplier);
    }

    public int multipliedFill(float alphaMultiplier) {
        return ARGB.multiplyAlpha(this.fill, alphaMultiplier);
    }
}

