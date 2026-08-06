/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.Vec3;

public record CircleGizmo(Vec3 pos, float radius, GizmoStyle style) implements Gizmo
{
    private static final int CIRCLE_VERTICES = 20;
    private static final float SEGMENT_SIZE_RADIANS = 0.31415927f;

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        int color;
        if (!this.style.hasStroke() && !this.style.hasFill()) {
            return;
        }
        Vec3[] points = new Vec3[21];
        for (int i = 0; i < 20; ++i) {
            Vec3 point;
            float theta = (float)i * 0.31415927f;
            points[i] = point = this.pos.add((float)((double)this.radius * Math.cos(theta)), 0.0, (float)((double)this.radius * Math.sin(theta)));
        }
        points[20] = points[0];
        if (this.style.hasFill()) {
            color = this.style.multipliedFill(alphaMultiplier);
            primitives.addTriangleFan(points, color);
        }
        if (this.style.hasStroke()) {
            color = this.style.multipliedStroke(alphaMultiplier);
            for (int i = 0; i < 20; ++i) {
                primitives.addLine(points[i], points[i + 1], color, this.style.strokeWidth());
            }
        }
    }
}

