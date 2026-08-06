/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;

@Environment(value=EnvType.CLIENT)
public class PoiDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
    private static final float TEXT_SCALE = 0.32f;
    private static final int ORANGE = -23296;
    private final BrainDebugRenderer brainRenderer;

    public PoiDebugRenderer(BrainDebugRenderer brainRenderer) {
        this.brainRenderer = brainRenderer;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        BlockPos playerPos = BlockPos.containing(camX, camY, camZ);
        debugValues.forEachBlock(DebugSubscriptions.POIS, (pos, poi) -> {
            if (playerPos.closerThan((Vec3i)pos, 30.0)) {
                PoiDebugRenderer.highlightPoi(pos);
                this.renderPoiInfo((DebugPoiInfo)poi, debugValues);
            }
        });
        this.brainRenderer.getGhostPois(debugValues).forEach((poiPos, value) -> {
            if (debugValues.getBlockValue(DebugSubscriptions.POIS, (BlockPos)poiPos) != null) {
                return;
            }
            if (playerPos.closerThan((Vec3i)poiPos, 30.0)) {
                this.renderGhostPoi((BlockPos)poiPos, (List<String>)value);
            }
        });
    }

    private static void highlightPoi(BlockPos poiPos) {
        float padding = 0.05f;
        Gizmos.cuboid(poiPos, 0.05f, GizmoStyle.fill(ARGB.colorFromFloat(0.3f, 0.2f, 0.2f, 1.0f)));
    }

    private void renderGhostPoi(BlockPos poiPos, List<String> names) {
        float padding = 0.05f;
        Gizmos.cuboid(poiPos, 0.05f, GizmoStyle.fill(ARGB.colorFromFloat(0.3f, 0.2f, 0.2f, 1.0f)));
        Gizmos.billboardTextOverBlock(names.toString(), poiPos, 0, -256, 0.32f);
        Gizmos.billboardTextOverBlock("Ghost POI", poiPos, 1, -65536, 0.32f);
    }

    private void renderPoiInfo(DebugPoiInfo poi, DebugValueAccess debugValues) {
        int row = 0;
        if (SharedConstants.DEBUG_BRAIN) {
            List<String> ticketHolderNames = this.getTicketHolderNames(poi, false, debugValues);
            if (ticketHolderNames.size() < 4) {
                PoiDebugRenderer.renderTextOverPoi("Owners: " + String.valueOf(ticketHolderNames), poi, row, -256);
            } else {
                PoiDebugRenderer.renderTextOverPoi(ticketHolderNames.size() + " ticket holders", poi, row, -256);
            }
            ++row;
            List<String> potentialTicketHolderNames = this.getTicketHolderNames(poi, true, debugValues);
            if (potentialTicketHolderNames.size() < 4) {
                PoiDebugRenderer.renderTextOverPoi("Candidates: " + String.valueOf(potentialTicketHolderNames), poi, row, -23296);
            } else {
                PoiDebugRenderer.renderTextOverPoi(potentialTicketHolderNames.size() + " potential owners", poi, row, -23296);
            }
            ++row;
        }
        PoiDebugRenderer.renderTextOverPoi("Free tickets: " + poi.freeTicketCount(), poi, row, -256);
        PoiDebugRenderer.renderTextOverPoi(poi.poiType().getRegisteredName(), poi, ++row, -1);
    }

    private static void renderTextOverPoi(String text, DebugPoiInfo poi, int row, int color) {
        Gizmos.billboardTextOverBlock(text, poi.pos(), row, color, 0.32f);
    }

    private List<String> getTicketHolderNames(DebugPoiInfo poi, boolean potential, DebugValueAccess debugValues) {
        ArrayList<String> names = new ArrayList<String>();
        debugValues.forEachEntity(DebugSubscriptions.BRAINS, (entity, brainDump) -> {
            boolean include;
            boolean bl = include = potential ? brainDump.hasPotentialPoi(poi.pos()) : brainDump.hasPoi(poi.pos());
            if (include) {
                names.add(DebugEntityNameGenerator.getEntityName(entity.getUUID()));
            }
        });
        return names;
    }
}

