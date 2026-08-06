/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class MapRenderer {
    private static final float MAP_Z_OFFSET = -0.01f;
    private static final float DECORATION_Z_OFFSET = -0.001f;
    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;
    private final TextureAtlas decorationSprites;
    private final MapTextureManager mapTextureManager;

    public MapRenderer(AtlasManager atlasManager, MapTextureManager mapTextureManager) {
        this.decorationSprites = atlasManager.getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        this.mapTextureManager = mapTextureManager;
    }

    public void render(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords) {
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(mapRenderState.texture), (pose, buffer) -> {
            buffer.addVertex(pose, 0.0f, 128.0f, -0.01f).setColor(-1).setUv(0.0f, 1.0f).setLight(lightCoords);
            buffer.addVertex(pose, 128.0f, 128.0f, -0.01f).setColor(-1).setUv(1.0f, 1.0f).setLight(lightCoords);
            buffer.addVertex(pose, 128.0f, 0.0f, -0.01f).setColor(-1).setUv(1.0f, 0.0f).setLight(lightCoords);
            buffer.addVertex(pose, 0.0f, 0.0f, -0.01f).setColor(-1).setUv(0.0f, 0.0f).setLight(lightCoords);
        });
        int count = 0;
        for (MapRenderState.MapDecorationRenderState decoration : mapRenderState.decorations) {
            if (showOnlyFrame && !decoration.renderOnFrame) continue;
            poseStack.pushPose();
            poseStack.translate((float)decoration.x / 2.0f + 64.0f, (float)decoration.y / 2.0f + 64.0f, -0.02f);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees((float)(decoration.rot * 360) / 16.0f));
            poseStack.scale(4.0f, 4.0f, 3.0f);
            poseStack.translate(-0.125f, 0.125f, 0.0f);
            TextureAtlasSprite atlasSprite = decoration.atlasSprite;
            if (atlasSprite != null) {
                float z = (float)count * -0.001f;
                submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(atlasSprite.atlasLocation()), (pose, buffer) -> {
                    buffer.addVertex(pose, -1.0f, 1.0f, z).setColor(-1).setUv(atlasSprite.getU0(), atlasSprite.getV0()).setLight(lightCoords);
                    buffer.addVertex(pose, 1.0f, 1.0f, z).setColor(-1).setUv(atlasSprite.getU1(), atlasSprite.getV0()).setLight(lightCoords);
                    buffer.addVertex(pose, 1.0f, -1.0f, z).setColor(-1).setUv(atlasSprite.getU1(), atlasSprite.getV1()).setLight(lightCoords);
                    buffer.addVertex(pose, -1.0f, -1.0f, z).setColor(-1).setUv(atlasSprite.getU0(), atlasSprite.getV1()).setLight(lightCoords);
                });
                poseStack.popPose();
            }
            if (decoration.name != null) {
                Font font = Minecraft.getInstance().font;
                float width = font.width(decoration.name);
                float f = 25.0f / width;
                Objects.requireNonNull(font);
                float scale = Mth.clamp(f, 0.0f, 6.0f / 9.0f);
                poseStack.pushPose();
                poseStack.translate((float)decoration.x / 2.0f + 64.0f - width * scale / 2.0f, (float)decoration.y / 2.0f + 64.0f + 4.0f, -0.025f);
                poseStack.scale(scale, scale, -1.0f);
                poseStack.translate(0.0f, 0.0f, 0.1f);
                submitNodeCollector.order(1).submitText(poseStack, 0.0f, 0.0f, decoration.name.getVisualOrderText(), false, Font.DisplayMode.NORMAL, lightCoords, -1, Integer.MIN_VALUE, 0);
                poseStack.popPose();
            }
            ++count;
        }
    }

    public void extractRenderState(MapId mapId, MapItemSavedData mapData, MapRenderState mapRenderState) {
        mapRenderState.texture = this.mapTextureManager.prepareMapTexture(mapId, mapData);
        mapRenderState.decorations.clear();
        for (MapDecoration decoration : mapData.getDecorations()) {
            mapRenderState.decorations.add(this.extractDecorationRenderState(decoration));
        }
    }

    private MapRenderState.MapDecorationRenderState extractDecorationRenderState(MapDecoration decoration) {
        MapRenderState.MapDecorationRenderState state = new MapRenderState.MapDecorationRenderState();
        state.atlasSprite = this.decorationSprites.getSprite(decoration.getSpriteLocation());
        state.x = decoration.x();
        state.y = decoration.y();
        state.rot = decoration.rot();
        state.name = decoration.name().orElse(null);
        state.renderOnFrame = decoration.renderOnFrame();
        return state;
    }
}

