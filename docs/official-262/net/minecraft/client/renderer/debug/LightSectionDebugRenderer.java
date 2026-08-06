/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.debug;

import java.time.Duration;
import java.time.Instant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class LightSectionDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private static final Duration REFRESH_INTERVAL = Duration.ofMillis(500L);
    private static final int RADIUS = 10;
    private static final int LIGHT_AND_BLOCKS_COLOR = ARGB.colorFromFloat(0.25f, 1.0f, 1.0f, 0.0f);
    private static final int LIGHT_ONLY_COLOR = ARGB.colorFromFloat(0.125f, 0.25f, 0.125f, 0.0f);
    private final Minecraft minecraft;
    private final LightLayer lightLayer;
    private Instant lastUpdateTime = Instant.now();
    private @Nullable SectionData data;

    public LightSectionDebugRenderer(Minecraft minecraft, LightLayer lightLayer) {
        this.minecraft = minecraft;
        this.lightLayer = lightLayer;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Instant time = Instant.now();
        if (this.data == null || Duration.between(this.lastUpdateTime, time).compareTo(REFRESH_INTERVAL) > 0) {
            this.lastUpdateTime = time;
            this.data = new SectionData(this.minecraft.level.getLightEngine(), SectionPos.of(this.minecraft.player.blockPosition()), 10, this.lightLayer);
        }
        LightSectionDebugRenderer.renderEdges(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
        LightSectionDebugRenderer.renderEdges(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
        LightSectionDebugRenderer.renderFaces(this.data.lightAndBlocksShape, this.data.minPos, LIGHT_AND_BLOCKS_COLOR);
        LightSectionDebugRenderer.renderFaces(this.data.lightShape, this.data.minPos, LIGHT_ONLY_COLOR);
    }

    private static void renderFaces(DiscreteVoxelShape shape, SectionPos minSection, int color) {
        shape.forAllFaces((direction, x, y, z) -> {
            int sectionX = x + minSection.getX();
            int sectionY = y + minSection.getY();
            int sectionZ = z + minSection.getZ();
            LightSectionDebugRenderer.renderFace(direction, sectionX, sectionY, sectionZ, color);
        });
    }

    private static void renderEdges(DiscreteVoxelShape shape, SectionPos minSection, int color) {
        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
            int sectionX0 = x0 + minSection.getX();
            int sectionY0 = y0 + minSection.getY();
            int sectionZ0 = z0 + minSection.getZ();
            int sectionX1 = x1 + minSection.getX();
            int sectionY1 = y1 + minSection.getY();
            int sectionZ1 = z1 + minSection.getZ();
            LightSectionDebugRenderer.renderEdge(sectionX0, sectionY0, sectionZ0, sectionX1, sectionY1, sectionZ1, color);
        }, true);
    }

    private static void renderFace(Direction direction, int sectionX, int sectionY, int sectionZ, int color) {
        Vec3 cuboidCornerA = new Vec3(SectionPos.sectionToBlockCoord(sectionX), SectionPos.sectionToBlockCoord(sectionY), SectionPos.sectionToBlockCoord(sectionZ));
        Vec3 cuboidCornerB = cuboidCornerA.add(16.0, 16.0, 16.0);
        Gizmos.rect(cuboidCornerA, cuboidCornerB, direction, GizmoStyle.fill(color));
    }

    private static void renderEdge(int sectionX0, int sectionY0, int sectionZ0, int sectionX1, int sectionY1, int sectionZ1, int color) {
        double x0 = SectionPos.sectionToBlockCoord(sectionX0);
        double y0 = SectionPos.sectionToBlockCoord(sectionY0);
        double z0 = SectionPos.sectionToBlockCoord(sectionZ0);
        double x1 = SectionPos.sectionToBlockCoord(sectionX1);
        double y1 = SectionPos.sectionToBlockCoord(sectionY1);
        double z1 = SectionPos.sectionToBlockCoord(sectionZ1);
        int opaqueColor = ARGB.opaque(color);
        Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), opaqueColor);
    }

    @Environment(value=EnvType.CLIENT)
    private static final class SectionData {
        private final DiscreteVoxelShape lightAndBlocksShape;
        private final DiscreteVoxelShape lightShape;
        private final SectionPos minPos;

        private SectionData(LevelLightEngine engine, SectionPos centerPos, int radius, LightLayer lightLayer) {
            int size = radius * 2 + 1;
            this.lightAndBlocksShape = new BitSetDiscreteVoxelShape(size, size, size);
            this.lightShape = new BitSetDiscreteVoxelShape(size, size, size);
            for (int z = 0; z < size; ++z) {
                for (int y = 0; y < size; ++y) {
                    for (int x = 0; x < size; ++x) {
                        SectionPos pos = SectionPos.of(centerPos.x() + x - radius, centerPos.y() + y - radius, centerPos.z() + z - radius);
                        LayerLightSectionStorage.SectionType type = engine.getDebugSectionType(lightLayer, pos);
                        if (type == LayerLightSectionStorage.SectionType.LIGHT_AND_DATA) {
                            this.lightAndBlocksShape.fill(x, y, z);
                            this.lightShape.fill(x, y, z);
                            continue;
                        }
                        if (type != LayerLightSectionStorage.SectionType.LIGHT_ONLY) continue;
                        this.lightShape.fill(x, y, z);
                    }
                }
            }
            this.minPos = SectionPos.of(centerPos.x() - radius, centerPos.y() - radius, centerPos.z() - radius);
        }
    }
}

