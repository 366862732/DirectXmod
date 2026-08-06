/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.gizmos;

import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

public interface GizmoPrimitives {
    public void addPoint(Vec3 var1, int var2, float var3);

    public void addLine(Vec3 var1, Vec3 var2, int var3, float var4);

    public void addTriangleFan(Vec3[] var1, int var2);

    public void addQuad(Vec3 var1, Vec3 var2, Vec3 var3, Vec3 var4, int var5);

    public void addText(Vec3 var1, String var2, TextGizmo.Style var3);
}

