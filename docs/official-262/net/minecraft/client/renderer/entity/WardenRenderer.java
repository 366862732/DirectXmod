/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;

@Environment(value=EnvType.CLIENT)
public class WardenRenderer
extends MobRenderer<Warden, WardenRenderState, WardenModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden.png");
    private static final Identifier BIOLUMINESCENT_LAYER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_bioluminescent_layer.png");
    private static final Identifier HEART_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_heart.png");
    private static final Identifier PULSATING_SPOTS_TEXTURE_1 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_1.png");
    private static final Identifier PULSATING_SPOTS_TEXTURE_2 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_2.png");

    public WardenRenderer(EntityRendererProvider.Context context) {
        super(context, new WardenModel(context.bakeLayer(ModelLayers.WARDEN)), 0.9f);
        WardenModel bioluminescentModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_BIOLUMINESCENT));
        WardenModel pulsatingSpotsModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_PULSATING_SPOTS));
        WardenModel tendrilsModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_TENDRILS));
        WardenModel heartModel = new WardenModel(context.bakeLayer(ModelLayers.WARDEN_HEART));
        this.addLayer(new LivingEntityEmissiveLayer<WardenRenderState, WardenModel>(this, renderState -> BIOLUMINESCENT_LAYER_TEXTURE, (warden, ageInTicks) -> 1.0f, bioluminescentModel, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<WardenRenderState, WardenModel>(this, renderState -> PULSATING_SPOTS_TEXTURE_1, (warden, ageInTicks) -> Math.max(0.0f, Mth.cos(ageInTicks * 0.045f) * 0.25f), pulsatingSpotsModel, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<WardenRenderState, WardenModel>(this, renderState -> PULSATING_SPOTS_TEXTURE_2, (warden, ageInTicks) -> Math.max(0.0f, Mth.cos(ageInTicks * 0.045f + (float)Math.PI) * 0.25f), pulsatingSpotsModel, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<WardenRenderState, WardenModel>(this, renderState -> TEXTURE, (warden, ageInTicks) -> warden.tendrilAnimation, tendrilsModel, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<WardenRenderState, WardenModel>(this, renderState -> HEART_TEXTURE, (warden, ageInTicks) -> warden.heartAnimation, heartModel, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(WardenRenderState state) {
        return TEXTURE;
    }

    @Override
    public WardenRenderState createRenderState() {
        return new WardenRenderState();
    }

    @Override
    public void extractRenderState(Warden entity, WardenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.tendrilAnimation = entity.getTendrilAnimation(partialTicks);
        state.heartAnimation = entity.getHeartAnimation(partialTicks);
        state.roarAnimationState.copyFrom(entity.roarAnimationState);
        state.sniffAnimationState.copyFrom(entity.sniffAnimationState);
        state.emergeAnimationState.copyFrom(entity.emergeAnimationState);
        state.diggingAnimationState.copyFrom(entity.diggingAnimationState);
        state.attackAnimationState.copyFrom(entity.attackAnimationState);
        state.sonicBoomAnimationState.copyFrom(entity.sonicBoomAnimationState);
    }
}

