/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.gizmos;

import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record ArrowGizmo(Vec3 start, Vec3 end, int color, float width) implements Gizmo
{
    public static final float DEFAULT_WIDTH = 2.5f;

    @Override
    public void emit(GizmoPrimitives primitives, float alphaMultiplier) {
        Vector3f[] tips;
        int color = ARGB.multiplyAlpha(this.color, alphaMultiplier);
        primitives.addLine(this.start, this.end, color, this.width);
        Quaternionf rotation = new Quaternionf().rotationTo((Vector3fc)new Vector3f(1.0f, 0.0f, 0.0f), (Vector3fc)this.end.subtract(this.start).toVector3f().normalize());
        float len = (float)Mth.clamp(this.end.distanceTo(this.start) * (double)0.1f, (double)0.1f, 1.0);
        for (Vector3f tip : tips = new Vector3f[]{rotation.transform(-len, len, 0.0f, new Vector3f()), rotation.transform(-len, 0.0f, len, new Vector3f()), rotation.transform(-len, -len, 0.0f, new Vector3f()), rotation.transform(-len, 0.0f, -len, new Vector3f())}) {
            primitives.addLine(this.end.add(tip.x, tip.y, tip.z), this.end, color, this.width);
        }
    }
}

