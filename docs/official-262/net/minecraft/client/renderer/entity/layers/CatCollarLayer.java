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
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

@Environment(value=EnvType.CLIENT)
public class CatCollarLayer
extends RenderLayer<CatRenderState, AbstractFelineModel<CatRenderState>> {
    private static final Identifier CAT_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/cat_collar.png");
    private static final Identifier CAT_BABY_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/cat_collar_baby.png");
    private final AdultCatModel adultModel;
    private final BabyCatModel babyModel;

    public CatCollarLayer(RenderLayerParent<CatRenderState, AbstractFelineModel<CatRenderState>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new AdultCatModel(modelSet.bakeLayer(ModelLayers.CAT_COLLAR));
        this.babyModel = new BabyCatModel(modelSet.bakeLayer(ModelLayers.CAT_BABY_COLLAR));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CatRenderState state, float yRot, float xRot) {
        DyeColor collarColor = state.collarColor;
        if (collarColor == null) {
            return;
        }
        int color = collarColor.getTextureDiffuseColor();
        AbstractFelineModel model = state.isBaby ? this.babyModel : this.adultModel;
        Identifier texture = state.isBaby ? CAT_BABY_COLLAR_LOCATION : CAT_COLLAR_LOCATION;
        CatCollarLayer.coloredCutoutModelCopyLayerRender(model, texture, poseStack, submitNodeCollector, lightCoords, state, color, 1);
    }
}

