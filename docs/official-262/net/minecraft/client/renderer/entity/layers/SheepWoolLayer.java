/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class SheepWoolLayer
extends RenderLayer<SheepRenderState, SheepModel> {
    private static final Identifier SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
    private static final Identifier BABY_SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_baby.png");
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, SheepRenderState state, float yRot, float xRot) {
        Identifier location;
        if (state.isSheared) {
            return;
        }
        EntityModel<SheepRenderState> model = state.isBaby ? this.babyModel : this.adultModel;
        Identifier identifier = location = state.isBaby ? BABY_SHEEP_WOOL_LOCATION : SHEEP_WOOL_LOCATION;
        if (state.isInvisible) {
            if (state.appearsGlowing()) {
                submitNodeCollector.submitModel(model, state, poseStack, RenderTypes.outline(location), lightCoords, LivingEntityRenderer.getOverlayCoords(state, 0.0f), -16777216, null, state.outlineColor, null);
            }
            return;
        }
        SheepWoolLayer.coloredCutoutModelCopyLayerRender(model, location, poseStack, submitNodeCollector, lightCoords, state, state.getWoolColor(), state.isBaby ? 1 : 0);
    }
}

