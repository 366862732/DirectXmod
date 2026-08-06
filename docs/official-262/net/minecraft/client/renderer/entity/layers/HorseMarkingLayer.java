/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Markings;

@Environment(value=EnvType.CLIENT)
public class HorseMarkingLayer
extends RenderLayer<HorseRenderState, HorseModel> {
    private static final Identifier INVISIBLE_TEXTURE = Identifier.withDefaultNamespace("invisible");
    private static final Map<Markings, HorseMarkingTextures> LOCATION_BY_MARKINGS = Maps.newEnumMap(Map.of(Markings.NONE, new HorseMarkingTextures(INVISIBLE_TEXTURE, INVISIBLE_TEXTURE), Markings.WHITE, new HorseMarkingTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_white.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_white_baby.png")), Markings.WHITE_FIELD, new HorseMarkingTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield_baby.png")), Markings.WHITE_DOTS, new HorseMarkingTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots_baby.png")), Markings.BLACK_DOTS, new HorseMarkingTextures(Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png"), Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots_baby.png"))));

    public HorseMarkingLayer(RenderLayerParent<HorseRenderState, HorseModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HorseRenderState state, float yRot, float xRot) {
        Identifier texture;
        HorseMarkingTextures variant = LOCATION_BY_MARKINGS.get((Object)state.markings);
        Identifier identifier = texture = state.isBaby ? variant.baby : variant.adult;
        if (texture == INVISIBLE_TEXTURE || state.isInvisible) {
            return;
        }
        submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.entityTranslucent(texture), lightCoords, LivingEntityRenderer.getOverlayCoords(state, 0.0f), state.outlineColor, (ModelFeatureRenderer.CrumblingOverlay)null);
    }

    @Environment(value=EnvType.CLIENT)
    private record HorseMarkingTextures(Identifier adult, Identifier baby) {
    }
}

