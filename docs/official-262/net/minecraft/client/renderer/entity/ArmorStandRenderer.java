/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Quaternionfc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.object.armorstand.ArmorStandModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ArmorStandRenderer
extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
    public static final Identifier DEFAULT_SKIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/armorstand/armorstand.png");
    private final ArmorStandArmorModel bigModel = (ArmorStandArmorModel)this.getModel();
    private final ArmorStandArmorModel smallModel;

    public ArmorStandRenderer(EntityRendererProvider.Context context) {
        super(context, new ArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND)), 0.0f);
        this.smallModel = new ArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND_SMALL));
        this.addLayer(new HumanoidArmorLayer<ArmorStandRenderState, ArmorStandArmorModel, ArmorStandArmorModel>(this, ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new), ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, context.getModelSet(), ArmorStandArmorModel::new), context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer<ArmorStandRenderState, ArmorStandArmorModel>(this));
        this.addLayer(new WingsLayer<ArmorStandRenderState, ArmorStandArmorModel>(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer(new CustomHeadLayer<ArmorStandRenderState, ArmorStandArmorModel>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
    }

    @Override
    public Identifier getTextureLocation(ArmorStandRenderState state) {
        return DEFAULT_SKIN_LOCATION;
    }

    @Override
    public ArmorStandRenderState createRenderState() {
        return new ArmorStandRenderState();
    }

    @Override
    public void extractRenderState(ArmorStand entity, ArmorStandRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.isMarker = entity.isMarker();
        state.isSmall = entity.isSmall();
        state.showArms = entity.showArms();
        state.showBasePlate = entity.showBasePlate();
        state.bodyPose = entity.getBodyPose();
        state.headPose = entity.getHeadPose();
        state.leftArmPose = entity.getLeftArmPose();
        state.rightArmPose = entity.getRightArmPose();
        state.leftLegPose = entity.getLeftLegPose();
        state.rightLegPose = entity.getRightLegPose();
        state.wiggle = (float)(entity.level().getGameTime() - entity.lastHit) + partialTicks;
    }

    @Override
    public void submit(ArmorStandRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = state.isSmall ? this.smallModel : this.bigModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected void setupRotations(ArmorStandRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(180.0f - bodyRot));
        if (state.wiggle < 5.0f) {
            poseStack.mulPose((Quaternionfc)Axis.YP.rotationDegrees(Mth.sin(state.wiggle / 1.5f * (float)Math.PI) * 3.0f));
        }
    }

    @Override
    protected boolean shouldShowName(ArmorStand entity, double distanceToCameraSq) {
        return entity.isCustomNameVisible();
    }

    @Override
    protected @Nullable RenderType getRenderType(ArmorStandRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        if (!state.isMarker) {
            return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
        }
        Identifier texture = this.getTextureLocation(state);
        if (forceTransparent) {
            return RenderTypes.entityTranslucent(texture, false);
        }
        if (isBodyVisible) {
            return RenderTypes.entityCutout(texture, false);
        }
        return null;
    }
}

