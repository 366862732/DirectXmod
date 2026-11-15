package com.xgdt.dx11.perf.culling;

import com.xgdt.dx11.mixin.EntityRendererAccessor;
import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
 * <p>Labels, outlines and forced-visible entities are never culled.  Culling
 * happens in {@code EntityRenderDispatcher.shouldRender}, before the render
 * state is extracted, so removing a named entity would also remove its name tag
 * even though the tag is drawn through terrain.  The same applies to glowing
 * entities and to players, whose name tags are always rendered.</p>
 *
 * <p>The camera entity, the local player, and anything in their vehicle chain
 * are always kept: those are needed for third-person rendering and passenger
 * views even when their own section is not in the terrain graph.</p>
 */
public final class EntityCulling {
    /**
     * Beyond this distance the section snapshot is not a reliable stand-in for
     * what the player can see, and vanilla's own distance/frustum checks already
     * remove most entities.  Skipping the test avoids visible popping at the
     * edge of the render distance.
     */
    private static final double TRACING_DISTANCE = 128.0;
    private static final double TRACING_DISTANCE_SQ = TRACING_DISTANCE * TRACING_DISTANCE;

    /**
     * Boxes wider or taller than this span many sections, so a single visible
     * section would keep them anyway; testing them is not worth the overlap
     * work (and a bounded box lets the section iterator stay allocation-free).
     */
    private static final double HITBOX_LIMIT = 50.0;

    /** Vanilla inflates the culling box by half a block; match it. */
    private static final double BOX_MARGIN = 0.5;

    private static long culledEntities;
    private static long testedEntities;

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
        if (camera == null) {
            return false;
        }
        if (entity == camera || entity == player) {
            return false;
        }
        Entity rootVehicle = entity.getRootVehicle();
        if (rootVehicle == camera.getRootVehicle()) {
            return false;
        }
        if (player != null && rootVehicle == player.getRootVehicle()) {
            return false;
        }

        // Name tags and glowing outlines are drawn through terrain.  Culling the
        // entity here would delete them, because name-tag data lives in the
        // render state that the cancelled call would have produced.
        if (entity.hasCustomName() || entity.shouldShowName() || entity.isCurrentlyGlowing()) {
            return false;
        }
        // Another player's name tag is always rendered by PlayerRenderer.
        if (entity instanceof Player) {
            return false;
        }

        if (entity.distanceToSqr(camera.getX(), camera.getY(), camera.getZ()) > TRACING_DISTANCE_SQ) {
            return false;
        }

        // Vanilla deliberately skips its compiled-section check outside build
        // height (for example a falling entity in the void); match that.
        BlockPos position = entity.blockPosition();
        if (entity.level().isOutsideBuildHeight(position.getY())) {
            return false;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        EntityRenderer<?, ?> renderer = dispatcher.getRenderer(entity);

        // Renderers can opt out of culling, and some draw geometry outside the
        // entity box (dragon wings, for example).  Respect both.
        if (renderer instanceof EntityRendererAccessor accessor) {
            if (!accessor.dx11$affectedByCulling(entity)) {
                return false;
            }
        }

        AABB box = cullingBox(renderer, entity);
        if (box == null || box.hasNaN() || box.getSize() == 0.0D) {
            return false;
        }
        if (box.getXsize() > HITBOX_LIMIT
            || box.getYsize() > HITBOX_LIMIT
            || box.getZsize() > HITBOX_LIMIT) {
            return false;
        }
        box = box.inflate(BOX_MARGIN);

        // Expand along the movement vector so fast entities are not culled at a
        // section border just before they cross into a visible one.
        Vec3 movement = entity.getDeltaMovement();
        if (movement != null) {
            box = box.expandTowards(movement.x, movement.y, movement.z);
        }

        testedEntities++;
        if (VisibleSections.intersects(box)) {
            return false;
        }
        culledEntities++;
        return true;
    }

    private static AABB cullingBox(EntityRenderer<?, ?> renderer, Entity entity) {
        if (renderer instanceof EntityRendererAccessor accessor) {
            AABB box = accessor.dx11$getBoundingBoxForCulling(entity);
            if (box != null) {
                return box;
            }
        }
        return entity.getBoundingBox();
    }

    public static void resetStats() {
        culledEntities = 0;
        testedEntities = 0;
    }

    public static String describe() {
        if (!PerfState.regionActive() || !PerfState.config().entityCulling) {
            return "entityCull=off";
        }
        return "entityCull=on"
            + " tested=" + testedEntities
            + " culled=" + culledEntities
            + " " + VisibleSections.describe();
    }
}
