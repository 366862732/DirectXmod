/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record LineGizmo(Vec3 start, Vec3 end, int color, float width) implements Gizmo
{
    public static final float DEFAULT_WIDTH = 3.0f;

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        primitives.addLine(this.start, this.end, ARGB.multiplyAlpha(this.color, alphaMultiplier), this.width);
    }
}

