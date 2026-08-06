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
import net.minecraft.client.model.animal.fish.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SalmonRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.Salmon;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class SalmonRenderer
extends MobRenderer<Salmon, SalmonRenderState, SalmonModel> {
    private static final Identifier SALMON_LOCATION = Identifier.withDefaultNamespace("textures/entity/fish/salmon.png");
    private final SalmonModel smallSalmonModel;
    private final SalmonModel mediumSalmonModel;
    private final SalmonModel largeSalmonModel;

    public SalmonRenderer(EntityRendererProvider.Context context) {
        super(context, new SalmonModel(context.bakeLayer(ModelLayers.SALMON)), 0.4f);
        this.smallSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON_SMALL));
        this.mediumSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON));
        this.largeSalmonModel = new SalmonModel(context.bakeLayer(ModelLayers.SALMON_LARGE));
    }

    @Override
    public void extractRenderState(Salmon entity, SalmonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.getVariant();
    }

    @Override
    public Identifier getTextureLocation(SalmonRenderState state) {
        return SALMON_LOCATION;
    }

    @Override
    public SalmonRenderState createRenderState() {
        return new SalmonRenderState();
    }

    @Override
    protected void setupRotations(SalmonRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        float amplitudeMultiplier = 1.0f;
        float angleMultiplier = 1.0f;
        if (!state.isInWater) {
            amplitudeMultiplier = 1.3f;
            angleMultiplier = 1.7f;
        }
        float bodyZRot = amplitudeMultiplier * 4.3f * Mth.sin(angleMultiplier * 0.6f * state.ageInTicks);
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(bodyZRot));
        if (!state.isInWater) {
            poseStack.translate(0.2f, 0.1f, 0.0f);
            poseStack.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(90.0f));
        }
    }

    @Override
    public void submit(SalmonRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = switch (state.variant) {
            default -> throw new MatchException(null, null);
            case Salmon.Variant.SMALL -> this.smallSalmonModel;
            case Salmon.Variant.MEDIUM -> this.mediumSalmonModel;
            case Salmon.Variant.LARGE -> this.largeSalmonModel;
        };
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}

