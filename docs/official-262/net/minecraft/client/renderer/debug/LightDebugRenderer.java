/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.debug;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class LightDebugRenderer
implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;
    private final boolean showBlockLight;
    private final boolean showSkyLight;
    private static final int MAX_RENDER_DIST = 10;

    public LightDebugRenderer(Minecraft minecraft, boolean showBlockLight, boolean showSkyLight) {
        this.minecraft = minecraft;
        this.showBlockLight = showBlockLight;
        this.showSkyLight = showSkyLight;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        ClientLevel level = this.minecraft.level;
        BlockPos playerPos = BlockPos.containing(camX, camY, camZ);
        LongOpenHashSet set = new LongOpenHashSet();
        for (BlockPos blockPos : BlockPos.betweenClosed(playerPos.offset(-10, -10, -10), playerPos.offset(10, 10, 10))) {
            int blockBrightness;
            int skyBrightness = level.getBrightness(LightLayer.SKY, blockPos);
            long sectionNode = SectionPos.blockToSection(blockPos.asLong());
            if (set.add(sectionNode)) {
                Gizmos.billboardText(level.getChunkSource().getLightEngine().getDebugData(LightLayer.SKY, SectionPos.of(sectionNode)), new Vec3(SectionPos.sectionToBlockCoord(SectionPos.x(sectionNode), 8), SectionPos.sectionToBlockCoord(SectionPos.y(sectionNode), 8), SectionPos.sectionToBlockCoord(SectionPos.z(sectionNode), 8)), TextGizmo.Style.forColorAndCentered(-65536).withScale(4.8f));
            }
            if (skyBrightness != 15 && this.showSkyLight) {
                int color = ARGB.srgbLerp((float)skyBrightness / 15.0f, -16776961, -16711681);
                Gizmos.billboardText(String.valueOf(skyBrightness), Vec3.atLowerCornerWithOffset(blockPos, 0.5, 0.25, 0.5), TextGizmo.Style.forColorAndCentered(color));
            }
            if (!this.showBlockLight || (blockBrightness = level.getBrightness(LightLayer.BLOCK, blockPos)) == 0) continue;
            int color = ARGB.srgbLerp((float)blockBrightness / 15.0f, -5636096, -256);
            Gizmos.billboardText(String.valueOf(level.getBrightness(LightLayer.BLOCK, blockPos)), Vec3.atCenterOf(blockPos), TextGizmo.Style.forColorAndCentered(color));
        }
    }
}

