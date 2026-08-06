/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class ChunkBorderRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final float THICK_WIDTH = 4.0f;
    private static final float THIN_WIDTH = 1.0f;
    private final Minecraft minecraft;
    private static final int CELL_BORDER = ARGB.color(255, 0, 155, 155);
    private static final int YELLOW = ARGB.color(255, 255, 255, 0);
    private static final int MAJOR_LINES = ARGB.colorFromFloat(1.0f, 0.25f, 0.25f, 1.0f);

    public ChunkBorderRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        int y;
        int color;
        int x;
        Entity cameraEntity = this.minecraft.getCameraEntity();
        float ymin = this.minecraft.level.getMinY();
        float ymax = this.minecraft.level.getMaxY() + 1;
        SectionPos cameraPos = SectionPos.of(cameraEntity.blockPosition());
        double xstart = cameraPos.minBlockX();
        double zstart = cameraPos.minBlockZ();
        for (x = -16; x <= 32; x += 16) {
            for (int z = -16; z <= 32; z += 16) {
                Gizmos.line(new Vec3(xstart + (double)x, ymin, zstart + (double)z), new Vec3(xstart + (double)x, ymax, zstart + (double)z), ARGB.colorFromFloat(0.5f, 1.0f, 0.0f, 0.0f), 4.0f);
            }
        }
        for (x = 2; x < 16; x += 2) {
            color = x % 4 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line(new Vec3(xstart + (double)x, ymin, zstart), new Vec3(xstart + (double)x, ymax, zstart), color, 1.0f);
            Gizmos.line(new Vec3(xstart + (double)x, ymin, zstart + 16.0), new Vec3(xstart + (double)x, ymax, zstart + 16.0), color, 1.0f);
        }
        for (int z = 2; z < 16; z += 2) {
            color = z % 4 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line(new Vec3(xstart, ymin, zstart + (double)z), new Vec3(xstart, ymax, zstart + (double)z), color, 1.0f);
            Gizmos.line(new Vec3(xstart + 16.0, ymin, zstart + (double)z), new Vec3(xstart + 16.0, ymax, zstart + (double)z), color, 1.0f);
        }
        for (y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += 2) {
            float yline = y;
            int color2 = y % 8 == 0 ? CELL_BORDER : YELLOW;
            Gizmos.line(new Vec3(xstart, yline, zstart), new Vec3(xstart, yline, zstart + 16.0), color2, 1.0f);
            Gizmos.line(new Vec3(xstart, yline, zstart + 16.0), new Vec3(xstart + 16.0, yline, zstart + 16.0), color2, 1.0f);
            Gizmos.line(new Vec3(xstart + 16.0, yline, zstart + 16.0), new Vec3(xstart + 16.0, yline, zstart), color2, 1.0f);
            Gizmos.line(new Vec3(xstart + 16.0, yline, zstart), new Vec3(xstart, yline, zstart), color2, 1.0f);
        }
        for (x = 0; x <= 16; x += 16) {
            for (int z = 0; z <= 16; z += 16) {
                Gizmos.line(new Vec3(xstart + (double)x, ymin, zstart + (double)z), new Vec3(xstart + (double)x, ymax, zstart + (double)z), MAJOR_LINES, 4.0f);
            }
        }
        Gizmos.cuboid(new AABB(cameraPos.minBlockX(), cameraPos.minBlockY(), cameraPos.minBlockZ(), cameraPos.maxBlockX() + 1, cameraPos.maxBlockY() + 1, cameraPos.maxBlockZ() + 1), GizmoStyle.stroke(MAJOR_LINES, 1.0f)).setAlwaysOnTop();
        for (y = this.minecraft.level.getMinY(); y <= this.minecraft.level.getMaxY() + 1; y += 16) {
            Gizmos.line(new Vec3(xstart, y, zstart), new Vec3(xstart, y, zstart + 16.0), MAJOR_LINES, 4.0f);
            Gizmos.line(new Vec3(xstart, y, zstart + 16.0), new Vec3(xstart + 16.0, y, zstart + 16.0), MAJOR_LINES, 4.0f);
            Gizmos.line(new Vec3(xstart + 16.0, y, zstart + 16.0), new Vec3(xstart + 16.0, y, zstart), MAJOR_LINES, 4.0f);
            Gizmos.line(new Vec3(xstart + 16.0, y, zstart), new Vec3(xstart, y, zstart), MAJOR_LINES, 4.0f);
        }
    }
}

