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
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class GameEventListenerRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final float BOX_HEIGHT = 1.0f;

    private void forEachListener(DebugValueAccess debugValues, ListenerVisitor visitor) {
        debugValues.forEachBlock(DebugSubscriptions.GAME_EVENT_LISTENERS, (blockPos, listener) -> visitor.accept(Vec3.atCenterOf(blockPos), listener.listenerRadius()));
        debugValues.forEachEntity(DebugSubscriptions.GAME_EVENT_LISTENERS, (entity, listener) -> visitor.accept(entity.position(), listener.listenerRadius()));
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        this.forEachListener(debugValues, (origin, radius) -> {
            double size = (double)radius * 2.0;
            Gizmos.cuboid(AABB.ofSize(origin, size, size, size), GizmoStyle.fill(ARGB.colorFromFloat(0.35f, 1.0f, 1.0f, 0.0f)));
        });
        this.forEachListener(debugValues, (origin, radius) -> Gizmos.cuboid(AABB.ofSize(origin, 0.5, 1.0, 0.5).move(0.0, 0.5, 0.0), GizmoStyle.fill(ARGB.colorFromFloat(0.35f, 1.0f, 1.0f, 0.0f))));
        this.forEachListener(debugValues, (origin, radius) -> {
            Gizmos.billboardText("Listener Origin", origin.add(0.0, 1.8, 0.0), TextGizmo.Style.whiteAndCentered().withScale(0.4f));
            Gizmos.billboardText(BlockPos.containing(origin).toString(), origin.add(0.0, 1.5, 0.0), TextGizmo.Style.forColorAndCentered(-6959665).withScale(0.4f));
        });
        debugValues.forEachEvent(DebugSubscriptions.GAME_EVENTS, (event, remainingTicks, totalLifetime) -> {
            Vec3 origin = event.pos();
            double size = 0.4;
            AABB box = AABB.ofSize(origin.add(0.0, 0.5, 0.0), 0.4, 0.9, 0.4);
            Gizmos.cuboid(box, GizmoStyle.fill(ARGB.colorFromFloat(0.2f, 1.0f, 1.0f, 1.0f)));
            Gizmos.billboardText(event.event().getRegisteredName(), origin.add(0.0, 0.85, 0.0), TextGizmo.Style.forColorAndCentered(-7564911).withScale(0.12f));
        });
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface ListenerVisitor {
        public void accept(Vec3 var1, int var2);
    }
}

