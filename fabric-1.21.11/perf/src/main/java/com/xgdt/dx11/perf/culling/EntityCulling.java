package com.xgdt.dx11.perf.culling;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Occlusion-aware entity filter layered on top of vanilla's checks.
 *
 * <p>Vanilla already skips entities whose section has no compiled mesh or whose
 * fade visibility is too low.  It does not skip entities in sections the
 * occlusion graph culled, so entities inside caves and behind terrain still go
 * through extraction and submission.  This filter culls an entity only when
 * <em>every</em> section its bounding box touches is absent from the current
 * {@link VisibleSections} snapshot.</p>
 *
 * <p>The camera entity, the local player, and anything in their vehicle chain
 * are always kept: those are needed for third-person rendering and passenger
 * views even when their own section is not in the terrain graph.</p>
 */
public final class EntityCulling {
    private static long culledEntities;

    private EntityCulling() {
    }

    /**
     * @return {@code true} when the entity should not be extracted or rendered
     *         this frame
     */
    public static boolean shouldCull(Entity entity) {
        if (entity == null || !PerfState.regionActive()) {
            return false;
        }
        PerfConfig config = PerfState.config();
        if (!config.entityCulling || !VisibleSections.captured()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return false;
        }
        Entity camera = minecraft.getCameraEntity();
        Entity player = minecraft.player;
        if (entity == camera || entity == player) {
            return false;
        }
        Entity rootVehicle = entity.getRootVehicle();
        if (camera != null && rootVehicle == camera.getRootVehicle()) {
            return false;
        }
        if (player != null && rootVehicle == player.getRootVehicle()) {
            return false;
        }

        // Vanilla deliberately skips its compiled-section check outside build
        // height (for example a falling entity in the void); match that.
        BlockPos position = entity.blockPosition();
        if (entity.level().isOutsideBuildHeight(position.getY())) {
            return false;
        }

        if (VisibleSections.intersects(entity.getBoundingBox())) {
            return false;
        }
        culledEntities++;
        return true;
    }

    public static void resetStats() {
        culledEntities = 0;
    }

    public static String describe() {
        if (!PerfState.regionActive() || !PerfState.config().entityCulling) {
            return "entityCull=off";
        }
        return "entityCull=on"
            + " culled=" + culledEntities
            + " " + VisibleSections.describe();
    }
}
