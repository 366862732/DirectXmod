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
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.AdultPiglinModel;
import net.minecraft.client.model.monster.piglin.BabyPiglinModel;
import net.minecraft.client.model.monster.piglin.PiglinModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.CrossbowItem;

@Environment(value=EnvType.CLIENT)
public class PiglinRenderer
extends HumanoidMobRenderer<AbstractPiglin, PiglinRenderState, PiglinModel> {
    private static final Identifier PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png");
    private static final Identifier PIGLIN_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/piglin_baby.png");
    private static final Identifier PIGLIN_BRUTE_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/piglin_brute.png");
    public static final CustomHeadLayer.Transforms PIGLIN_CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(0.0f, 0.0f, 1.0019531f);

    public PiglinRenderer(EntityRendererProvider.Context context, ModelLayerLocation body, ModelLayerLocation babyBody, ArmorModelSet<ModelLayerLocation> armorSet, ArmorModelSet<ModelLayerLocation> babyArmorSet) {
        super(context, new AdultPiglinModel(context.bakeLayer(body)), new BabyPiglinModel(context.bakeLayer(babyBody)), 0.5f, PIGLIN_CUSTOM_HEAD_TRANSFORMS);
        this.addLayer(new HumanoidArmorLayer<PiglinRenderState, PiglinModel, PiglinModel>(this, ArmorModelSet.bake(armorSet, context.getModelSet(), AdultPiglinModel::new), ArmorModelSet.bake(babyArmorSet, context.getModelSet(), BabyPiglinModel::new), context.getEquipmentRenderer()));
    }

    @Override
    public Identifier getTextureLocation(PiglinRenderState state) {
        return state.isBrute ? PIGLIN_BRUTE_LOCATION : (state.isBaby ? PIGLIN_BABY_LOCATION : PIGLIN_LOCATION);
    }

    @Override
    public PiglinRenderState createRenderState() {
        return new PiglinRenderState();
    }

    @Override
    public void extractRenderState(AbstractPiglin entity, PiglinRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isBrute = entity.is(EntityTypes.PIGLIN_BRUTE);
        state.armPose = entity.getArmPose();
        state.maxCrossbowChageDuration = CrossbowItem.getChargeDuration(entity.getUseItem(), entity);
        state.isConverting = entity.isConverting();
    }

    @Override
    protected boolean isShaking(PiglinRenderState state) {
        return super.isShaking(state) || state.isConverting;
    }
}

