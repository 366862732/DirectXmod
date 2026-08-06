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
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class RedstoneWireOrientationsRenderer
implements DebugRenderer.SimpleDebugRenderer {
    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        debugValues.forEachBlock(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, (wirePos, orientation) -> {
            Vec3 center = Vec3.atBottomCenterOf(wirePos).subtract(0.0, 0.1, 0.0);
            Gizmos.arrow(center, center.add(orientation.getFront().getUnitVec3().scale(0.5)), -16776961);
            Gizmos.arrow(center, center.add(orientation.getUp().getUnitVec3().scale(0.4)), -65536);
            Gizmos.arrow(center, center.add(orientation.getSide().getUnitVec3().scale(0.3)), -256);
        });
    }
}

