/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import net.minecraft.core.Direction;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.Vec3;

public record RectGizmo(Vec3 a, Vec3 b, Vec3 c, Vec3 d, GizmoStyle style) implements Gizmo
{
    public static RectGizmo fromCuboidFace(Vec3 cuboidCornerA, Vec3 cuboidCornerB, Direction face, GizmoStyle style) {
        return switch (face) {
            default -> throw new MatchException(null, null);
            case Direction.DOWN -> new RectGizmo(new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z), new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z), style);
            case Direction.UP -> new RectGizmo(new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z), new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z), style);
            case Direction.NORTH -> new RectGizmo(new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z), new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z), style);
            case Direction.SOUTH -> new RectGizmo(new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z), new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z), new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z), style);
            case Direction.WEST -> new RectGizmo(new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerA.z), new Vec3(cuboidCornerA.x, cuboidCornerA.y, cuboidCornerB.z), new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerB.z), new Vec3(cuboidCornerA.x, cuboidCornerB.y, cuboidCornerA.z), style);
            case Direction.EAST -> new RectGizmo(new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerA.z), new Vec3(cuboidCornerB.x, cuboidCornerB.y, cuboidCornerB.z), new Vec3(cuboidCornerB.x, cuboidCornerA.y, cuboidCornerB.z), style);
        };
    }

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        int color;
        if (this.style.hasFill()) {
            color = this.style.multipliedFill(alphaMultiplier);
            primitives.addQuad(this.a, this.b, this.c, this.d, color);
        }
        if (this.style.hasStroke()) {
            color = this.style.multipliedStroke(alphaMultiplier);
            primitives.addLine(this.a, this.b, color, this.style.strokeWidth());
            primitives.addLine(this.b, this.c, color, this.style.strokeWidth());
            primitives.addLine(this.c, this.d, color, this.style.strokeWidth());
            primitives.addLine(this.d, this.a, color, this.style.strokeWidth());
        }
    }
}

