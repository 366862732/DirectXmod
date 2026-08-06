/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.gizmos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.ArrowGizmo;
import net.minecraft.gizmos.CircleGizmo;
import net.minecraft.gizmos.CuboidGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoCollector;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.LineGizmo;
import net.minecraft.gizmos.PointGizmo;
import net.minecraft.gizmos.RectGizmo;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Gizmos {
    private static final ThreadLocal<@Nullable GizmoCollector> collector = new ThreadLocal();

    private Gizmos() {
    }

    public static TemporaryCollection withCollector(GizmoCollector collector) {
        TemporaryCollection result = new TemporaryCollection();
        Gizmos.collector.set(collector);
        return result;
    }

    public static GizmoProperties addGizmo(Gizmo gizmo) {
        GizmoCollector collector = Gizmos.collector.get();
        if (collector == null) {
            throw new IllegalStateException("Gizmos cannot be created here! No GizmoCollector has been registered.");
        }
        return collector.add(gizmo);
    }

    public static GizmoProperties cuboid(AABB aabb, GizmoStyle style) {
        return Gizmos.cuboid(aabb, style, false);
    }

    public static GizmoProperties cuboid(AABB aabb, GizmoStyle style, boolean coloredCorner) {
        return Gizmos.addGizmo(new CuboidGizmo(aabb, style, coloredCorner));
    }

    public static GizmoProperties cuboid(BlockPos blockPos, GizmoStyle style) {
        return Gizmos.cuboid(new AABB(blockPos), style);
    }

    public static GizmoProperties cuboid(BlockPos blockPos, float padding, GizmoStyle style) {
        return Gizmos.cuboid(new AABB(blockPos).inflate(padding), style);
    }

    public static GizmoProperties circle(Vec3 pos, float radius, GizmoStyle style) {
        return Gizmos.addGizmo(new CircleGizmo(pos, radius, style));
    }

    public static GizmoProperties line(Vec3 start, Vec3 end, int argb) {
        return Gizmos.addGizmo(new LineGizmo(start, end, argb, 3.0f));
    }

    public static GizmoProperties line(Vec3 start, Vec3 end, int argb, float width) {
        return Gizmos.addGizmo(new LineGizmo(start, end, argb, width));
    }

    public static GizmoProperties arrow(Vec3 start, Vec3 end, int argb) {
        return Gizmos.addGizmo(new ArrowGizmo(start, end, argb, 2.5f));
    }

    public static GizmoProperties arrow(Vec3 start, Vec3 end, int argb, float width) {
        return Gizmos.addGizmo(new ArrowGizmo(start, end, argb, width));
    }

    public static GizmoProperties rect(Vec3 cuboidCornerA, Vec3 cuboidCornerB, Direction face, GizmoStyle style) {
        return Gizmos.addGizmo(RectGizmo.fromCuboidFace(cuboidCornerA, cuboidCornerB, face, style));
    }

    public static GizmoProperties rect(Vec3 cornerA, Vec3 cornerB, Vec3 cornerC, Vec3 cornerD, GizmoStyle style) {
        return Gizmos.addGizmo(new RectGizmo(cornerA, cornerB, cornerC, cornerD, style));
    }

    public static GizmoProperties point(Vec3 position, int argb, float size) {
        return Gizmos.addGizmo(new PointGizmo(position, argb, size));
    }

    public static GizmoProperties billboardTextOverBlock(String text, BlockPos pos, int row, int color, float scale) {
        double firstRowStartPosition = 1.3;
        double rowHeight = 0.2;
        GizmoProperties properties = Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.3 + (double)row * 0.2, 0.5), TextGizmo.Style.forColorAndCentered(color).withScale(scale));
        properties.setAlwaysOnTop();
        return properties;
    }

    public static GizmoProperties billboardTextOverMob(Entity entity, int row, String text, int color, float scale) {
        double firstRowStartPosition = 2.4;
        double rowHeight = 0.25;
        double x = (double)entity.getBlockX() + 0.5;
        double y = entity.getY() + 2.4 + (double)row * 0.25;
        double z = (double)entity.getBlockZ() + 0.5;
        float textAdjustLeft = 0.5f;
        GizmoProperties properties = Gizmos.billboardText(text, new Vec3(x, y, z), TextGizmo.Style.forColor(color).withScale(scale).withLeftAlignment(0.5f));
        properties.setAlwaysOnTop();
        return properties;
    }

    public static GizmoProperties billboardText(String name, Vec3 pos, TextGizmo.Style style) {
        return Gizmos.addGizmo(new TextGizmo(pos, name, style));
    }

    public static class TemporaryCollection
    implements AutoCloseable {
        private final @Nullable GizmoCollector old = collector.get();
        private boolean closed;

        private TemporaryCollection() {
        }

        @Override
        public void close() {
            if (!this.closed) {
                this.closed = true;
                collector.set(this.old);
            }
        }
    }
}

