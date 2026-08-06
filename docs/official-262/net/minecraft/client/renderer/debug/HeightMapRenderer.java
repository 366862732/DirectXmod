/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 */
package net.minecraft.client.renderer.debug;

import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

@Environment(value=EnvType.CLIENT)
public class HeightMapRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private static final int CHUNK_DIST = 2;
    private static final float BOX_HEIGHT = 0.09375f;

    public HeightMapRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        ClientLevel level = this.minecraft.level;
        BlockPos playerPos = BlockPos.containing(camX, 0.0, camZ);
        for (int chunkX = -2; chunkX <= 2; ++chunkX) {
            for (int chunkZ = -2; chunkZ <= 2; ++chunkZ) {
                ChunkAccess chunk = level.getChunk(playerPos.offset(chunkX * 16, 0, chunkZ * 16));
                for (Map.Entry<Heightmap.Types, Heightmap> heightmapEntry : chunk.getHeightmaps()) {
                    Heightmap.Types type = heightmapEntry.getKey();
                    ChunkPos chunkPos = chunk.getPos();
                    Vector3f color = this.getColor(type);
                    for (int relativeX = 0; relativeX < 16; ++relativeX) {
                        for (int relativeZ = 0; relativeZ < 16; ++relativeZ) {
                            int xx = SectionPos.sectionToBlockCoord(chunkPos.x(), relativeX);
                            int zz = SectionPos.sectionToBlockCoord(chunkPos.z(), relativeZ);
                            float height = (float)level.getHeight(type, xx, zz) + (float)type.ordinal() * 0.09375f;
                            Gizmos.cuboid(new AABB((float)xx + 0.25f, height, (float)zz + 0.25f, (float)xx + 0.75f, height + 0.09375f, (float)zz + 0.75f), GizmoStyle.fill(ARGB.colorFromFloat(1.0f, color.x(), color.y(), color.z())));
                        }
                    }
                }
            }
        }
    }

    private Vector3f getColor(Heightmap.Types type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case Heightmap.Types.WORLD_SURFACE_WG -> new Vector3f(1.0f, 1.0f, 0.0f);
            case Heightmap.Types.OCEAN_FLOOR_WG -> new Vector3f(1.0f, 0.0f, 1.0f);
            case Heightmap.Types.WORLD_SURFACE -> new Vector3f(0.0f, 0.7f, 0.0f);
            case Heightmap.Types.OCEAN_FLOOR -> new Vector3f(0.0f, 0.0f, 0.5f);
            case Heightmap.Types.MOTION_BLOCKING -> new Vector3f(0.0f, 0.3f, 0.3f);
            case Heightmap.Types.MOTION_BLOCKING_NO_LEAVES -> new Vector3f(0.0f, 0.5f, 0.5f);
        };
    }
}

