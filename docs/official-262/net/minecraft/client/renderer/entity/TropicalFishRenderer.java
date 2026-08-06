/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class TropicalFishRenderer
extends MobRenderer<TropicalFish, TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
    private final EntityModel<TropicalFishRenderState> smallModel = this.getModel();
    private final EntityModel<TropicalFishRenderState> largeModel;
    private static final Identifier SMALL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a.png");
    private static final Identifier LARGE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b.png");

    public TropicalFishRenderer(EntityRendererProvider.Context context) {
        super(context, new TropicalFishSmallModel(context.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15f);
        this.largeModel = new TropicalFishLargeModel(context.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
        this.addLayer(new TropicalFishPatternLayer(this, context.getModelSet()));
    }

    @Override
    public Identifier getTextureLocation(TropicalFishRenderState state) {
        return switch (state.pattern.base()) {
            default -> throw new MatchException(null, null);
            case TropicalFish.Base.SMALL -> SMALL_TEXTURE;
            case TropicalFish.Base.LARGE -> LARGE_TEXTURE;
        };
    }

    @Override
    public TropicalFishRenderState createRenderState() {
        return new TropicalFishRenderState();
    }

    @Override
    public void extractRenderState(TropicalFish entity, TropicalFishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.pattern = entity.getPattern();
        state.baseColor = entity.getBaseColor().getTextureDiffuseColor();
        state.patternColor = entity.getPatternColor().getTextureDiffuseColor();
    }

    @Override
    public void submit(TropicalFishRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = switch (state.pattern.base()) {
            default -> throw new MatchException(null, null);
            case TropicalFish.Base.SMALL -> this.smallModel;
            case TropicalFish.Base.LARGE -> this.largeModel;
        };
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected int getModelTint(TropicalFishRenderState state) {
        return state.baseColor;
    }

    @Override
    protected void setupRotations(TropicalFishRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float bodyZRot = 4.3f * Mth.sin(0.6f * state.ageInTicks);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(bodyZRot));
        if (!state.isInWater) {
            poseStack.translate(0.2f, 0.1f, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(90.0f));
        }
    }
}

