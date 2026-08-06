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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class RaidDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private static final float TEXT_SCALE = 0.64f;
    private final Minecraft minecraft;

    public RaidDebugRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        BlockPos playerPos = this.getCamera().blockPosition();
        debugValues.forEachChunk(DebugSubscriptions.RAIDS, (chunkPos, raidCenters) -> {
            for (BlockPos raidCenter : raidCenters) {
                if (!playerPos.closerThan(raidCenter, 160.0)) continue;
                RaidDebugRenderer.highlightRaidCenter(raidCenter);
            }
        });
    }

    private static void highlightRaidCenter(BlockPos raidCenter) {
        Gizmos.cuboid(raidCenter, GizmoStyle.fill(ARGB.colorFromFloat(0.15f, 1.0f, 0.0f, 0.0f)));
        RaidDebugRenderer.renderTextOverBlock("Raid center", raidCenter, -65536);
    }

    private static void renderTextOverBlock(String text, BlockPos pos, int color) {
        Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.3, 0.5), TextGizmo.Style.forColor(color).withScale(0.64f)).setAlwaysOnTop();
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.mainCamera();
    }
}

