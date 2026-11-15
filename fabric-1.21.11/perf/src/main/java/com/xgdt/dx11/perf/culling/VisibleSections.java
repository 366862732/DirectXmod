package com.xgdt.dx11.perf.culling;

import com.xgdt.dx11.perf.PerfConfig;
import com.xgdt.dx11.perf.PerfState;
import java.util.List;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;

/**
 * Snapshot of vanilla's occlusion-filtered visible section list.
 *
 * <p>{@code LevelRenderer.cullTerrain} rebuilds {@code visibleSections} from the
 * section occlusion graph before section uploads and entity extraction run.
 * Copying the section origins into a primitive set gives the Phase 6 features a
 * stable, allocation-free membership test for the current frame.</p>
 *
 * <p>The snapshot is only captured while {@code entityCulling} or the
 * upload-budget off-screen ranking is enabled, so the default path pays
 * nothing.  An empty or missing snapshot disables the dependent features for
 * that frame instead of culling everything.</p>
 */
public final class VisibleSections {
    private static final SectionVisibilitySet SECTIONS = new SectionVisibilitySet();

    private VisibleSections() {
    }

    /** True when at least one Phase 6 consumer needs the snapshot. */
    public static boolean needed() {
        try {
            PerfConfig config = PerfState.config();
            return PerfState.regionActive()
                && (config.entityCulling
                    || (config.deferredOffscreenUpdates && config.uploadBudget));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Replaces the snapshot from the current visible-section list.  Called at
     * the return of {@code LevelRenderer.cullTerrain}.
     */
    public static void capture(List<SectionRenderDispatcher.RenderSection> visible) {
        SECTIONS.clear();
        if (!needed() || visible == null || visible.isEmpty()) {
            return;
        }
        for (SectionRenderDispatcher.RenderSection section : visible) {
            if (section == null) {
                continue;
            }
            BlockPos origin = section.getRenderOrigin();
            SECTIONS.add(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        }
        SECTIONS.freeze();
    }

    /** Drops the snapshot, for example when the renderer changes level. */
    public static void clear() {
        SECTIONS.clear();
    }

    /** True when the current snapshot can be used for culling/ranking. */
    public static boolean captured() {
        return SECTIONS.captured() && SECTIONS.size() > 0;
    }

    public static int size() {
        return SECTIONS.size();
    }

    public static boolean isVisibleSection(int sectionX, int sectionY, int sectionZ) {
        return SECTIONS.contains(sectionX, sectionY, sectionZ);
    }

    /** Section-coordinate membership for a block position. */
    public static boolean isVisibleBlock(int blockX, int blockY, int blockZ) {
        return SECTIONS.contains(blockX >> 4, blockY >> 4, blockZ >> 4);
    }

    /** True when any section touched by the box is visible. */
    public static boolean intersects(AABB box) {
        int minX = SectionPos.posToSectionCoord(box.minX);
        int minY = SectionPos.posToSectionCoord(box.minY);
        int minZ = SectionPos.posToSectionCoord(box.minZ);
        int maxX = SectionPos.posToSectionCoord(box.maxX);
        int maxY = SectionPos.posToSectionCoord(box.maxY);
        int maxZ = SectionPos.posToSectionCoord(box.maxZ);
        return SECTIONS.intersects(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static String describe() {
        return "visibleSections=" + SECTIONS.size() + " captured=" + SECTIONS.captured();
    }
}
